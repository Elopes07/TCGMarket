package com.pitcc.integration.catalog.yugioh;

import com.pitcc.integration.catalog.ExternalApiErrorType;
import com.pitcc.integration.catalog.ExternalApiErrors;
import com.pitcc.integration.catalog.ExternalApiException;
import com.pitcc.integration.catalog.yugioh.dto.YugiohCardDto;
import com.pitcc.integration.catalog.yugioh.dto.YugiohCardsResponse;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import com.pitcc.model.CardGame;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.Assert;
import org.springframework.web.client.RestClient;

public class YugiohClient {

  static final int SEARCH_PAGE_SIZE = 20;
  // A YGOPRODeck responde "sem resultado" com HTTP 400 e só esta mensagem distingue o caso.
  private static final String NO_CARD_MESSAGE = "no card matching your query was found";

  private final RestClient restClient;

  public YugiohClient(RestClient restClient) {
    this.restClient = Objects.requireNonNull(restClient, "restClient");
  }

  public List<YugiohCardDto> fetchAll() {
    return ExternalApiErrors.fetch(CardGame.YUGIOH, () -> restClient.get()
                    .uri("/cardinfo.php")
                    .retrieve()
                    .onStatus(
                            status -> status.isError(),
                            ExternalApiErrors.errorHandler(CardGame.YUGIOH)
                    )
                    .body(YugiohCardsResponse.class))
            .map(response -> cardsOf(response.data()))
            .orElseThrow(() -> new ExternalApiException(
                    CardGame.YUGIOH,
                    ExternalApiErrorType.INVALID_RESPONSE,
                    null,
                    "A API retornou uma resposta vazia na carga completa de Yu-Gi-Oh!.",
                    null
            ));
  }

  public List<YugiohCardDto> search(String query) {
    Assert.hasText(query, "query must not be blank");
    String term = query.trim();
    try {
      return ExternalApiErrors.fetch(com.pitcc.model.CardGame.YUGIOH, () -> restClient.get()
              .uri(uriBuilder -> uriBuilder
                  .path("/cardinfo.php")
                  .queryParam("fname", term)
                  .queryParam("num", SEARCH_PAGE_SIZE)
                  .queryParam("offset", 0)
                  .build())
              .retrieve()
              .onStatus(status -> status.value() == 400, this::handleBadRequest)
              .onStatus(status -> status.isError(), ExternalApiErrors.errorHandler(com.pitcc.model.CardGame.YUGIOH))
              .body(YugiohCardsResponse.class))
          .map(response -> cardsOf(response.data()))
          .orElse(List.of());
    } catch (NoMatchingCard exception) {
      return List.of();
    }
  }

  public Optional<YugiohCardDto> findByPasscode(long passcode) {
    try {
      return ExternalApiErrors.fetch(com.pitcc.model.CardGame.YUGIOH, () -> restClient.get()
              .uri(uriBuilder -> uriBuilder.path("/cardinfo.php").queryParam("id", passcode).build())
              .retrieve()
              .onStatus(status -> status.value() == 400, this::handleBadRequest)
              .onStatus(status -> status.isError(), ExternalApiErrors.errorHandler(com.pitcc.model.CardGame.YUGIOH))
              .body(YugiohCardsResponse.class))
          .flatMap(response -> cardsOf(response.data()).stream()
              .filter(Objects::nonNull)
              .filter(card -> Long.valueOf(passcode).equals(card.id()))
              .findFirst());
    } catch (NoMatchingCard exception) {
      return Optional.empty();
    }
  }

  private void handleBadRequest(HttpRequest request, ClientHttpResponse response) {
    String body;
    try {
      body = readBody(response);
    } catch (IOException exception) {
      throw badRequest(exception);
    }
    if (body.toLowerCase(Locale.ROOT).contains(NO_CARD_MESSAGE)) {
      throw new NoMatchingCard();
    }
    throw badRequest(null);
  }

  private static ExternalApiException badRequest(Throwable cause) {
    return new ExternalApiException(
        CardGame.YUGIOH,
        ExternalApiErrorType.INVALID_RESPONSE,
        400,
        "Yu-Gi-Oh! catalog returned HTTP 400",
        cause);
  }

  private static String readBody(ClientHttpResponse response) throws IOException {
    InputStream body = response.getBody();
    if (body == null) {
      return "";
    }
    try (body) {
      return new String(body.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  private static List<YugiohCardDto> cardsOf(List<YugiohCardDto> cards) {
    if (cards == null) {
      throw new ExternalApiException(CardGame.YUGIOH, ExternalApiErrorType.INVALID_RESPONSE,
          null, "Resposta de catálogo sem a lista data", null);
    }
    // Preserva entradas nulas para que a importação as registre como falhas de mapeamento.
    return new java.util.ArrayList<>(cards);
  }

  private static final class NoMatchingCard extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private NoMatchingCard() {
      super(null, null, false, false);
    }
  }
}
