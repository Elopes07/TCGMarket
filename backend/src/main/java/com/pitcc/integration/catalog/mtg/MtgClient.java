package com.pitcc.integration.catalog.mtg;

import com.pitcc.integration.catalog.ExternalApiErrorType;
import com.pitcc.integration.catalog.ExternalApiErrors;
import com.pitcc.integration.catalog.ExternalApiException;
import com.pitcc.integration.catalog.mtg.dto.MtgCardDto;
import com.pitcc.integration.catalog.mtg.dto.MtgCardFaceDto;
import com.pitcc.integration.catalog.mtg.dto.MtgCardsResponse;
import com.pitcc.integration.catalog.mtg.dto.MtgImageUris;
import com.pitcc.model.CardGame;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.zip.GZIPInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Cliente Scryfall. Busca por termo na API e leitura incremental do arquivo
 * Bulk Data default_cards (JSONL gzip). Nao carrega o arquivo inteiro na RAM.
 */
public class MtgClient {

  private static final Logger log = LoggerFactory.getLogger(MtgClient.class);
  private static final CardGame GAME = CardGame.MAGIC_THE_GATHERING;
  private static final int IMPORT_BATCH_SIZE = 200;
  private static final String USER_AGENT =
          "TCGMarket-FATEC/1.0 (https://github.com/PI3-TCG/TCGMarket)";

  private static final ObjectMapper JSON = new ObjectMapper();
  private final RestClient restClient;
  private final HttpClient bulkHttpClient = HttpClient.newBuilder()
          .followRedirects(HttpClient.Redirect.NORMAL)
          .connectTimeout(Duration.ofSeconds(30))
          .build();

  private BufferedReader bulkReader;
  private int previousPage;
  private String lookahead;

  public MtgClient(RestClient restClient) {
    this.restClient = Objects.requireNonNull(restClient, "restClient");
  }

  /** Pesquisa apenas a primeira pagina. Mantido para navegacao e pesquisa. */
  public List<MtgCardDto> search(String query) {
    Assert.hasText(query, "query must not be blank");
    String term = query.trim();
    return ExternalApiErrors.fetch(GAME, () -> restClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/cards/search")
                            .queryParam("q", nameQuery(term))
                            .queryParam("unique", "prints")
                            .build())
                    .header("User-Agent", USER_AGENT)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .onStatus(status -> status.isError(), ExternalApiErrors.errorHandler(GAME))
                    .body(MtgCardsResponse.class))
            .map(response -> cardsOf(response.data()))
            .orElse(List.of());
  }

  public Optional<MtgCardDto> findById(String externalId) {
    Assert.hasText(externalId, "externalId must not be blank");
    String id = externalId.trim();
    return ExternalApiErrors.fetch(GAME, () -> restClient.get()
            .uri("/cards/{id}", id)
            .header("User-Agent", USER_AGENT)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .onStatus(status -> status.isError(), ExternalApiErrors.errorHandler(GAME))
            .body(MtgCardDto.class));
  }

  /**
   * Le sequencialmente lotes de impressões, sem paginar o endpoint /cards/search.
   * O argumento page serve apenas como indice local de lote e deve ser 1,2,3...
   */
  public synchronized BulkPage fetchBulkPage(int page) {
    if (page < 1) {
      throw new IllegalArgumentException("page deve ser maior ou igual a 1");
    }
    if (page == 1) {
      closeBulkReader();
      openBulkReader();
      previousPage = 0;
    }
    if (bulkReader == null || page != previousPage + 1) {
      throw new IllegalStateException(
              "O Scryfall Bulk Data deve ser lido sequencialmente, iniciando na pagina 1");
    }

    try {
      List<MtgCardDto> cards = new ArrayList<>(IMPORT_BATCH_SIZE);
      int fetched = 0;
      while (fetched < IMPORT_BATCH_SIZE) {
        String line = nextLine();
        if (line == null) break;
        fetched++;
        try {
          cards.add(parseCard(JSON.readTree(line)));
        } catch (RuntimeException exception) {
          // Registra como linha invalida no ImportBatch.map, sem perder outras cartas.
          log.warn("SCRYFALL_BULK_INVALID_ROW page={} row={}", page, fetched, exception);
          cards.add(null);
        }
      }
      if (lookahead == null) {
        lookahead = bulkReader.readLine();
      }
      boolean hasNext = lookahead != null;
      previousPage = page;
      log.info("SCRYFALL_BULK_PAGE page={} fetched={} hasNext={}", page, fetched, hasNext);
      if (!hasNext) closeBulkReader();
      return new BulkPage(cards, hasNext);
    } catch (IOException exception) {
      closeBulkReader();
      throw new ExternalApiException(GAME, ExternalApiErrorType.UNAVAILABLE,
              null, "Erro de leitura do Bulk Data do Scryfall", exception);
    }
  }

  private String nextLine() throws IOException {
    if (lookahead != null) {
      String result = lookahead;
      lookahead = null;
      return result;
    }
    return bulkReader.readLine();
  }

  private void openBulkReader() {
    String downloadUrl = bulkDownloadUrl();
    URI uri = URI.create(downloadUrl);
    if (!"https".equalsIgnoreCase(uri.getScheme()) ||
            !"data.scryfall.io".equalsIgnoreCase(uri.getHost())) {
      throw new ExternalApiException(GAME, ExternalApiErrorType.INVALID_RESPONSE,
              null, "URL inesperada de Bulk Data do Scryfall", null);
    }
    try {
      HttpRequest request = HttpRequest.newBuilder(uri)
              .GET()
              .header("User-Agent", USER_AGENT)
              .header("Accept", "application/gzip, application/octet-stream")
              .timeout(Duration.ofMinutes(15))
              .build();
      HttpResponse<InputStream> response = bulkHttpClient.send(
              request, HttpResponse.BodyHandlers.ofInputStream());
      if (response.statusCode() != 200) {
        response.body().close();
        throw new ExternalApiException(GAME, ExternalApiErrorType.UNAVAILABLE,
                response.statusCode(), "Bulk Data Scryfall HTTP " + response.statusCode(), null);
      }
      bulkReader = new BufferedReader(new InputStreamReader(
              new GZIPInputStream(response.body(), 64 * 1024), StandardCharsets.UTF_8), 64 * 1024);
      lookahead = null;
      log.info("SCRYFALL_BULK_OPEN format=jsonl.gz batchSize={}", IMPORT_BATCH_SIZE);
    } catch (IOException exception) {
      throw new ExternalApiException(GAME, ExternalApiErrorType.UNAVAILABLE,
              null, "Nao foi possivel abrir o Bulk Data do Scryfall", exception);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new ExternalApiException(GAME, ExternalApiErrorType.UNAVAILABLE,
              null, "Download do Bulk Data interrompido", exception);
    }
  }

  private String bulkDownloadUrl() {
    String json = ExternalApiErrors.fetch(GAME, () -> restClient.get()
                    .uri("/bulk-data/default_cards")
                    .header("User-Agent", USER_AGENT)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .onStatus(status -> status.isError(), ExternalApiErrors.errorHandler(GAME))
                    .body(String.class))
            .orElseThrow(() -> new ExternalApiException(GAME,
                    ExternalApiErrorType.INVALID_RESPONSE, null,
                    "Metadados do Bulk Data ausentes", null));
    try {
      JsonNode metadata = JSON.readTree(json);
      String url = text(metadata, "jsonl_download_uri");
      if (url == null || !url.endsWith(".jsonl.gz")) {
        throw new IllegalArgumentException("jsonl_download_uri ausente ou nao e gzip JSONL");
      }
      return url;
    } catch (RuntimeException exception) {
      throw new ExternalApiException(GAME, ExternalApiErrorType.INVALID_RESPONSE,
              null, "Metadados do Bulk Data invalidos", exception);
    }
  }

  private void closeBulkReader() {
    if (bulkReader != null) {
      try {
        bulkReader.close();
      } catch (IOException exception) {
        log.warn("Falha ao fechar stream Bulk Data Scryfall", exception);
      }
    }
    bulkReader = null;
    lookahead = null;
  }

  /** Mapeamento explicito garante as propriedades snake_case do JSONL. */
  private static MtgCardDto parseCard(JsonNode root) {
    List<MtgCardFaceDto> faces = new ArrayList<>();
    JsonNode faceNodes = root.get("card_faces");
    if (faceNodes != null && faceNodes.isArray()) {
      for (JsonNode face : faceNodes) {
        faces.add(new MtgCardFaceDto(
                images(face.get("image_uris")),
                text(face, "mana_cost"), text(face, "type_line"),
                stringList(face.get("colors")),
                text(face, "power"), text(face, "toughness"), text(face, "oracle_text")));
      }
    }
    JsonNode cmc = root.get("cmc");
    return new MtgCardDto(
            text(root, "id"), text(root, "oracle_id"), text(root, "name"),
            text(root, "set"), text(root, "set_name"),
            text(root, "collector_number"), text(root, "rarity"),
            images(root.get("image_uris")), faces,
            text(root, "mana_cost"),
            cmc != null && cmc.isNumber() ? cmc.doubleValue() : null,
            stringList(root.get("colors")), text(root, "type_line"),
            text(root, "power"), text(root, "toughness"), text(root, "oracle_text"));
  }

  private static String text(JsonNode root, String field) {
    if (root == null || root.isNull()) return null;
    JsonNode child = root.get(field);
    return child == null || child.isNull() ? null : child.asText();
  }

  private static MtgImageUris images(JsonNode root) {
    if (root == null || root.isNull()) return null;
    return new MtgImageUris(text(root, "small"), text(root, "normal"));
  }

  private static List<String> stringList(JsonNode root) {
    if (root == null || !root.isArray()) return null;
    List<String> result = new ArrayList<>();
    for (JsonNode child : root) {
      result.add(child.isNull() ? null : child.asText());
    }
    return result;
  }

  private static List<MtgCardDto> cardsOf(List<MtgCardDto> cards) {
    if (cards == null) {
      throw new ExternalApiException(GAME, ExternalApiErrorType.INVALID_RESPONSE,
              null, "Resposta de catalogo sem a lista data", null);
    }
    return new ArrayList<>(cards);
  }

  private static String nameQuery(String query) {
    return "name:\"" + query.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
  }

  public record BulkPage(List<MtgCardDto> cards, boolean hasNext) {}
}
