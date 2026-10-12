package com.pitcc.integration.catalog.pokemon;

import com.pitcc.integration.catalog.ExternalApiErrorType;
import com.pitcc.integration.catalog.ExternalApiErrors;
import com.pitcc.integration.catalog.ExternalApiException;
import com.pitcc.integration.catalog.pokemon.dto.PokemonCardDto;
import com.pitcc.integration.catalog.pokemon.dto.PokemonCardResponse;
import com.pitcc.integration.catalog.pokemon.dto.PokemonCardsResponse;
import com.pitcc.model.CardGame;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;
import org.springframework.web.client.RestClient;

public class PokemonTcgClient {

  static final int SEARCH_PAGE_SIZE = 20;
  static final int IMPORT_PAGE_SIZE = 50;
  private static final int MAX_ATTEMPTS_PER_PAGE = 3;
  private static final long BETWEEN_PAGES_DELAY_MS = 2_500L;

  // Todos os campos utilizados por PokemonCardMapper.toPokemonCard(...).
  private static final String IMPORT_FIELDS =
          "id,name,hp,convertedRetreatCost,supertype,subtypes,number,rarity,set,images,types,attacks";

  private static final Logger log = LoggerFactory.getLogger(PokemonTcgClient.class);
  private final RestClient restClient;

  public PokemonTcgClient(RestClient restClient) {
    this.restClient = Objects.requireNonNull(restClient, "restClient");
  }

  public List<PokemonCardDto> search(String query) {
    Assert.hasText(query, "query must not be blank");
    String term = query.trim();
    return ExternalApiErrors.fetch(CardGame.POKEMON, () -> restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/cards")
                            .queryParam("q", nameQuery(term))
                            .queryParam("pageSize", SEARCH_PAGE_SIZE)
                            .build())
                    .retrieve()
                    .onStatus(status -> status.isError(), ExternalApiErrors.errorHandler(CardGame.POKEMON))
                    .body(PokemonCardsResponse.class))
            .map(response -> cardsOf(response.data()))
            .orElse(List.of());
  }

  public Optional<PokemonCardDto> findById(String externalId) {
    Assert.hasText(externalId, "externalId must not be blank");
    String id = externalId.trim();
    return ExternalApiErrors.fetch(CardGame.POKEMON, () -> restClient.get()
                    .uri("/cards/{id}", id)
                    .retrieve()
                    .onStatus(status -> status.isError(), ExternalApiErrors.errorHandler(CardGame.POKEMON))
                    .body(PokemonCardResponse.class))
            .map(PokemonCardResponse::data);
  }

  /**
   * Recupera o catálogo completo. Preserva entradas nulas para que o serviço de
   * importação possa reportar falhas de mapeamento individualmente.
   *
   * ATENÇÃO: este método busca todas as páginas antes de devolver a lista.
   * A persistência por página exigiria uma alteração no serviço de importação.
   */
  public List<PokemonCardDto> fetchAll() {
    List<PokemonCardDto> allCards = new ArrayList<>();
    int page = 1;

    while (true) {
      List<PokemonCardDto> cards = fetchPageWithRetry(page);
      if (cards.isEmpty()) {
        log.info("POKEMON_IMPORT_COMPLETE pages={} cards={}", page - 1, allCards.size());
        return allCards;
      }

      allCards.addAll(cards);
      log.info("POKEMON_IMPORT_PAGE page={} fetched={} accumulated={}",
              page, cards.size(), allCards.size());

      if (cards.size() < IMPORT_PAGE_SIZE) {
        log.info("POKEMON_IMPORT_COMPLETE pages={} cards={}", page, allCards.size());
        return allCards;
      }

      page++;
      pause(BETWEEN_PAGES_DELAY_MS);
    }
  }

  /**
   * Busca uma pagina do catalogo com as tentativas controladas ja existentes.
   * Utilizado pelo provider para permitir persistencia incremental.
   */
  public List<PokemonCardDto> fetchImportPage(int page) {
    Assert.isTrue(page >= 1, "page must be greater than zero");
    return fetchPageWithRetry(page);
  }

  private List<PokemonCardDto> fetchPageWithRetry(int page) {
    for (int attempt = 1; attempt <= MAX_ATTEMPTS_PER_PAGE; attempt++) {
      try {
        return fetchPage(page);
      } catch (ExternalApiException exception) {
        if (!isRetryable(exception) || attempt == MAX_ATTEMPTS_PER_PAGE) {
          log.error("POKEMON_IMPORT_PAGE_FAILED page={} attempt={} status={} type={}",
                  page, attempt, exception.getStatusCode(), exception.getErrorType());
          throw exception;
        }

        long delayMs = Integer.valueOf(429).equals(exception.getStatusCode())
                ? 30_000L * attempt
                : 3_000L * attempt;
        log.warn("POKEMON_IMPORT_RETRY page={} attempt={} status={} type={} waitMs={}",
                page, attempt, exception.getStatusCode(), exception.getErrorType(), delayMs);
        pause(delayMs);
      }
    }

    throw new IllegalStateException("Número de tentativas excedido na página " + page);
  }

  private List<PokemonCardDto> fetchPage(int page) {
    return ExternalApiErrors.fetch(CardGame.POKEMON, () -> restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/cards")
                            .queryParam("page", page)
                            .queryParam("pageSize", IMPORT_PAGE_SIZE)
                            .queryParam("select", IMPORT_FIELDS)
                            .build())
                    .retrieve()
                    .onStatus(status -> status.isError(), ExternalApiErrors.errorHandler(CardGame.POKEMON))
                    .body(PokemonCardsResponse.class))
            .map(response -> cardsOf(response.data()))
            .orElseThrow(() -> new ExternalApiException(
                    CardGame.POKEMON,
                    ExternalApiErrorType.INVALID_RESPONSE,
                    404,
                    "Página " + page + " do catálogo Pokémon não encontrada",
                    null));
  }

  private static boolean isRetryable(ExternalApiException exception) {
    Integer status = exception.getStatusCode();
    return (status != null && (status == 429 || status == 500 || status == 502
            || status == 503 || status == 504))
            || exception.getErrorType() == ExternalApiErrorType.TIMEOUT
            || exception.getErrorType() == ExternalApiErrorType.UNAVAILABLE;
  }

  private static void pause(long milliseconds) {
    try {
      Thread.sleep(milliseconds);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Importação Pokémon interrompida", exception);
    }
  }

  private static List<PokemonCardDto> cardsOf(List<PokemonCardDto> cards) {
    if (cards == null) {
      throw new ExternalApiException(CardGame.POKEMON, ExternalApiErrorType.INVALID_RESPONSE,
              null, "Resposta de catálogo sem a lista data", null);
    }
    return new ArrayList<>(cards);
  }

  private static String nameQuery(String query) {
    return "name:\"" + query.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
  }
}
