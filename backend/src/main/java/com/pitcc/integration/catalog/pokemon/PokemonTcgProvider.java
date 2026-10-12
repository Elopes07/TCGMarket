package com.pitcc.integration.catalog.pokemon;

import com.pitcc.integration.catalog.CardCatalogProvider;
import com.pitcc.integration.catalog.CatalogImportPage;
import com.pitcc.integration.catalog.ExternalCard;
import com.pitcc.integration.catalog.ImportBatch;
import com.pitcc.integration.catalog.PagedCatalogImportProvider;
import com.pitcc.integration.catalog.pokemon.dto.PokemonCardDto;
import com.pitcc.model.CardGame;
import com.pitcc.model.CardSource;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class PokemonTcgProvider implements CardCatalogProvider, PagedCatalogImportProvider {

  private static final Logger log = LoggerFactory.getLogger(PokemonTcgProvider.class);
  private final PokemonTcgClient client;

  public PokemonTcgProvider(PokemonTcgClient client) {
    this.client = client;
  }

  @Override
  public CardGame getCardGame() {
    return CardGame.POKEMON;
  }

  @Override
  public CardSource getSource() {
    return CardSource.POKEMON_TCG_API;
  }

  @Override
  public ImportBatch fetchForImport(String query) {
    return mapBatch(client.search(query));
  }

  /**
   * Mantido por compatibilidade com o contrato original.
   * O CatalogImportService usa fetchPageForImport() para a importacao completa.
   */
  @Override
  public ImportBatch fetchAllForImport() {
    return mapBatch(client.fetchAll());
  }

  @Override
  public CatalogImportPage fetchPageForImport(int page) {
    List<PokemonCardDto> dtos = client.fetchImportPage(page);
    ImportBatch batch = mapBatch(dtos);
    // Usar o total bruto de DTOs; batch.cards().size() pode ser menor
    // quando houver cartas rejeitadas durante o mapeamento.
    boolean hasNext = dtos.size() == PokemonTcgClient.IMPORT_PAGE_SIZE;
    return new CatalogImportPage(batch, dtos.size(), hasNext);
  }

  private ImportBatch mapBatch(List<PokemonCardDto> cards) {
    return ImportBatch.map(
            cards,
            getSource(),
            dto -> dto.id(),
            dto -> PokemonCardMapper.toPokemonCard(dto).stream().toList()
    );
  }

  @Override
  public List<ExternalCard> searchCards(String query) {
    return client.search(query).stream()
            .map(this::toCard)
            .flatMap(Optional::stream)
            .toList();
  }

  @Override
  public Optional<ExternalCard> findByExternalId(String externalId) {
    return client.findById(externalId).flatMap(this::toCard);
  }

  private Optional<ExternalCard> toCard(PokemonCardDto dto) {
    Optional<ExternalCard> card = PokemonCardMapper.toExternalCard(dto);
    if (card.isEmpty()) {
      log.warn("Ignorando carta do catalogo Pokemon sem id ou nome");
    }
    return card;
  }
}
