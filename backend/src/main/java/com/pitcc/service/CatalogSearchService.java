package com.pitcc.service;

import com.pitcc.dto.CatalogCardDetailsResponse;
import com.pitcc.dto.CatalogCardResponse;
import com.pitcc.dto.CatalogFiltersResponse;
import com.pitcc.dto.CatalogPageResponse;
import com.pitcc.dto.CatalogSearchRequest;
import com.pitcc.exception.CatalogCardNotFoundException;
import com.pitcc.exception.InvalidCatalogRequestException;
import com.pitcc.model.Card;
import com.pitcc.model.CardGame;
import com.pitcc.model.MagicCardType;
import com.pitcc.model.PokemonCardType;
import com.pitcc.model.YugiohCardType;
import com.pitcc.repository.CardRepository;
import com.pitcc.util.TextNormalizer;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

@Service
public class CatalogSearchService {
  static final int DEFAULT_PAGE_SIZE = 20;
  static final int MAX_PAGE_SIZE = 50;
  private static final String COLLECTION = "cards";

  private final CardRepository repository;
  private final MongoTemplate mongoTemplate;

  public CatalogSearchService(CardRepository repository, MongoTemplate mongoTemplate) {
    this.repository = repository;
    this.mongoTemplate = mongoTemplate;
  }

  public CatalogPageResponse<CatalogCardResponse> search(String game, CatalogSearchRequest request) {
    CardGame selected = CatalogGameResolver.parse(game);
    int page = request.page() == null ? 0 : request.page();
    int size = request.size() == null ? DEFAULT_PAGE_SIZE : request.size();
    if (page < 0) {
      throw new InvalidCatalogRequestException("A página deve ser maior ou igual a 0.");
    }
    if (size < 1 || size > MAX_PAGE_SIZE) {
      throw new InvalidCatalogRequestException(
          "O tamanho da página deve estar entre 1 e " + MAX_PAGE_SIZE + ".");
    }
    Sort sort = sort(request.sort());
    Query query = new Query(criteria(selected, request));

    long total = mongoTemplate.count(query, Card.class);
    List<CatalogCardResponse> content = total == 0 ? List.of()
        : mongoTemplate.find(query.with(PageRequest.of(page, size, sort)), Card.class)
            .stream().map(CatalogCardResponse::from).toList();
    return CatalogPageResponse.of(content, page, size, total);
  }

  public CatalogFiltersResponse filters(String game) {
    CardGame selected = CatalogGameResolver.parse(game);
    Criteria byGame = Criteria.where("game").is(selected.name());

    List<String> sets = distinct(byGame, "edition");
    List<String> rarities = distinct(byGame, "officialRarity");

    List<String> found = mongoTemplate
        .findDistinct(new Query(byGame), typeField(selected), COLLECTION, String.class);
    List<String> types = typesOf(selected).stream().filter(found::contains).toList();

    return new CatalogFiltersResponse(sets, rarities, types);
  }

  public CatalogCardResponse findByExternalId(String game, String externalId) {
    if (externalId == null || externalId.isBlank()) {
      throw new InvalidCatalogRequestException("Informe o externalId.");
    }
    CardGame selected = CatalogGameResolver.parse(game);
    return repository.findBySourceAndExternalId(CatalogGameResolver.source(selected), externalId.trim())
        .map(CatalogCardResponse::from).orElseThrow(CatalogCardNotFoundException::new);
  }

  public CatalogCardDetailsResponse findById(String id) {
    Card card = repository.findById(id).orElseThrow(CatalogCardNotFoundException::new);
    List<Card> otherPrintings = card.getConceptualId() == null ? List.of()
        : repository.findTop20ByGameAndConceptualIdAndIdNotOrderByEditionAsc(
            card.getGame(), card.getConceptualId(), card.getId());
    return CatalogCardDetailsResponse.from(card, otherPrintings);
  }

  private List<String> distinct(Criteria filter, String field) {
    return mongoTemplate.findDistinct(new Query(filter), field, COLLECTION, String.class)
        .stream().filter(Objects::nonNull).sorted(String.CASE_INSENSITIVE_ORDER).toList();
  }

  private static Criteria criteria(CardGame game, CatalogSearchRequest request) {
    Criteria criteria = Criteria.where("game").is(game.name());
    String name = TextNormalizer.normalizeOptionalText(request.q());
    if (name != null) {
      criteria.and("name").regex(Pattern.quote(name), "i");
    }
    String set = TextNormalizer.normalizeOptionalText(request.set());
    if (set != null) {
      criteria.and("edition").is(set);
    }
    String rarity = TextNormalizer.normalizeOptionalText(request.rarity());
    if (rarity != null) {
      criteria.and("officialRarity").is(rarity);
    }
    String type = TextNormalizer.normalizeOptionalText(request.type());
    if (type != null) {
      criteria.and(typeField(game)).is(type(game, type));
    }
    return criteria;
  }

  private static String type(CardGame game, String value) {
    String normalized = value.toUpperCase(Locale.ROOT);
    if (!typesOf(game).contains(normalized)) {
      throw new InvalidCatalogRequestException(
          "Tipo de carta inválido para este jogo: " + value + ".");
    }
    return normalized;
  }

  private static String typeField(CardGame game) {
    return switch (game) {
      case POKEMON -> "pokemonCardType";
      case YUGIOH -> "yugiohCardType";
      case MAGIC_THE_GATHERING -> "cardTypes";
    };
  }

  private static List<String> typesOf(CardGame game) {
    Enum<?>[] values = switch (game) {
      case POKEMON -> PokemonCardType.values();
      case YUGIOH -> YugiohCardType.values();
      case MAGIC_THE_GATHERING -> MagicCardType.values();
    };
    return Arrays.stream(values).map(Enum::name).toList();
  }

  private static Sort sort(String value) {
    String normalized = TextNormalizer.normalizeOptionalText(value);
    Sort.Direction direction;
    if (normalized == null || normalized.equals("name-asc")) {
      direction = Sort.Direction.ASC;
    } else if (normalized.equals("name-desc")) {
      direction = Sort.Direction.DESC;
    } else {
      throw new InvalidCatalogRequestException("Ordenação inválida. Use name-asc ou name-desc.");
    }
    return Sort.by(direction, "name").and(Sort.by("id"));
  }
}
