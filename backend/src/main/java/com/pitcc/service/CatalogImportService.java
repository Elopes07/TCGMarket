package com.pitcc.service;

import com.pitcc.exception.InvalidCatalogRequestException;
import com.pitcc.integration.catalog.*;
import com.pitcc.model.Card;
import com.pitcc.model.CardGame;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

@Service
public class CatalogImportService {

  private static final Logger log = LoggerFactory.getLogger(CatalogImportService.class);

  private final Map<CardGame, CatalogImportProvider> providers;
  private final CardImportService persistence;

  // Parametro opcional apenas para retomar a importacao do Pokemon.
  @Value("${catalog.import.pokemon.start-page:1}")
  private int pokemonStartPage = 1;

  public CatalogImportService(
          List<CatalogImportProvider> providers,
          CardImportService persistence) {

    this.providers = providers.stream()
            .collect(Collectors.toUnmodifiableMap(
                    CatalogImportProvider::getCardGame,
                    Function.identity()
            ));

    this.persistence = persistence;
  }

  public CatalogImportReport importByQuery(String game, String query) {
    CardGame selected = CatalogGameResolver.parse(game);

    if (query == null || query.isBlank()) {
      throw new InvalidCatalogRequestException("Informe um termo para importar.");
    }

    CatalogImportProvider provider = providerFor(selected);
    return executeImport(
            selected,
            provider,
            () -> provider.fetchForImport(query.trim())
    );
  }

  public CatalogImportReport importAll(String game) {
    CardGame selected = CatalogGameResolver.parse(game);
    CatalogImportProvider provider = providerFor(selected);

    // Apenas providers com suporte a paginação seguem este caminho.
    // Yu-Gi-Oh! e Magic permanecem no fluxo original.
    if (provider instanceof PagedCatalogImportProvider pagedProvider) {
      return executePagedImport(selected, pagedProvider);
    }

    return executeImport(selected, provider, provider::fetchAllForImport);
  }

  private CatalogImportProvider providerFor(CardGame game) {
    CatalogImportProvider provider = providers.get(game);
    if (provider == null) {
      throw new IllegalStateException("Provedor não registrado: " + game);
    }
    return provider;
  }

  /** Fluxo tradicional, utilizado por pesquisa e providers não paginados. */
  private CatalogImportReport executeImport(
          CardGame selected,
          CatalogImportProvider provider,
          Supplier<ImportBatch> fetchBatch) {

    log.info("IMPORT_START source={}", provider.getSource());
    ImportProgress progress = new ImportProgress();

    final ImportBatch batch;
    try {
      batch = fetchBatch.get();
    } catch (ExternalApiException exception) {
      log.error("Importação abortada ao consultar {}", provider.getSource(), exception);
      progress.abortFetch(provider, exception, null);
      return progress.toReport(provider);
    }

    progress.persistBatch(selected, provider, batch);
    return progress.toReport(provider);
  }

  /**
   * Busca, mapeia e persiste cada pagina antes de buscar a proxima.
   * Falhas de API encerram o processo, mas nao descartam paginas ja persistidas.
   */
  private CatalogImportReport executePagedImport(
          CardGame selected,
          PagedCatalogImportProvider provider) {

    log.info("IMPORT_START source={} mode=PAGED", provider.getSource());
    ImportProgress progress = new ImportProgress();
    int page = selected == CardGame.POKEMON ? pokemonStartPage : 1;
    if (page < 1) {
      throw new IllegalArgumentException(
              "catalog.import.pokemon.start-page deve ser maior ou igual a 1");
    }
    log.info("IMPORT_START_PAGE source={} page={}", provider.getSource(), page);

    while (true) {
      final CatalogImportPage result;
      try {
        result = provider.fetchPageForImport(page);
      } catch (ExternalApiException exception) {
        log.error("IMPORT_FETCH_FAILED source={} page={}",
                provider.getSource(), page, exception);
        progress.abortFetch(provider, exception, page);
        return progress.toReport(provider);
      }

      int persistedBefore = progress.persisted;
      progress.persistBatch(selected, provider, result.batch());

      log.info("IMPORT_PAGE_PERSISTED source={} page={} fetched={} mappedTotal={} "
                      + "persistedPage={} persistedTotal={} issuesTotal={}",
              provider.getSource(),
              page,
              result.fetched(),
              progress.mapped,
              progress.persisted - persistedBefore,
              progress.persisted,
              progress.issues.size());

      if (progress.aborted || !result.hasNext()) {
        return progress.toReport(provider);
      }
      page++;
    }
  }

  /** Acumula os resultados sem alterar a semantica existente de upsert e issues. */
  private final class ImportProgress {

    private int mapped;
    private int persisted;
    private int notAttempted;
    private boolean aborted;
    private final List<ImportIssue> issues = new ArrayList<>();

    private void abortFetch(CatalogImportProvider provider,
                            ExternalApiException exception,
                            Integer page) {
      aborted = true;
      String location = page == null ? "" : "page=" + page + " ";
      issues.add(new ImportIssue(
              provider.getSource(),
              null,
              "FETCH",
              location + exception.getErrorType() + ": " + exception.getMessage()
      ));
      // Nao contabilizar paginas futuras como 'notAttempted': quantidade desconhecida.
    }

    private void persistBatch(CardGame selected,
                              CatalogImportProvider provider,
                              ImportBatch batch) {
      if (aborted) {
        throw new IllegalStateException("A importação já foi abortada.");
      }

      issues.addAll(batch.issues());
      mapped += batch.cards().size();

      log.info("IMPORT_MAPPED source={} mappedBatch={} mappedTotal={} issuesTotal={}",
              provider.getSource(), batch.cards().size(), mapped, issues.size());

      for (int i = 0; i < batch.cards().size(); i++) {
        Card card = batch.cards().get(i);

        try {
          if (card.getSource() != provider.getSource()
                  || card.getGame() != selected) {
            throw new IllegalArgumentException("Origem ou jogo incompatível com o adapter.");
          }

          persistence.upsert(card);
          persisted++;

          if (persisted % 1000 == 0) {
            log.info("IMPORT_PROGRESS source={} persisted={} mapped={}",
                    provider.getSource(), persisted, mapped);
          }

        } catch (IllegalArgumentException exception) {
          issues.add(new ImportIssue(
                  provider.getSource(),
                  card.getExternalId(),
                  "VALIDATION",
                  exception.getMessage()
          ));

          log.warn("Carta rejeitada: source={}, externalId={}",
                  provider.getSource(), card.getExternalId(), exception);

        } catch (DataAccessException exception) {
          issues.add(new ImportIssue(
                  provider.getSource(),
                  card.getExternalId(),
                  "PERSISTENCE",
                  "Falha de persistência; confira a causa no log "
                          + "e reexecute o lote. O resultado da última "
                          + "escrita pode ser desconhecido."
          ));

          log.error("Importação interrompida: source={}, externalId={}",
                  provider.getSource(), card.getExternalId(), exception);

          notAttempted += batch.cards().size() - i - 1;
          aborted = true;
          return;
        }
      }
    }

    private CatalogImportReport toReport(CatalogImportProvider provider) {
      return new CatalogImportReport(
              provider.getSource(),
              mapped,
              persisted,
              notAttempted,
              aborted,
              issues
      );
    }
  }
}
