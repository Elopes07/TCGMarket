package com.pitcc.service;

import com.pitcc.dto.CatalogCardResponse;
import com.pitcc.dto.CatalogFiltersResponse;
import com.pitcc.dto.CatalogPageResponse;
import com.pitcc.dto.CatalogSearchRequest;
import com.pitcc.exception.CatalogCardNotFoundException;
import com.pitcc.exception.InvalidCatalogRequestException;
import com.pitcc.model.Card;
import com.pitcc.model.CardGame;
import com.pitcc.model.HiddenCard;
import com.pitcc.model.MagicCardType;
import com.pitcc.model.PokemonCardType;
import com.pitcc.model.YugiohCardType;
import com.pitcc.repository.CardRepository;
import com.pitcc.repository.HiddenCardRepository;
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
  private final HiddenCardRepository hiddenCards;
  private final MongoTemplate mongoTemplate;

  public CatalogSearchService(
      CardRepository repository, HiddenCardRepository hiddenCards, MongoTemplate mongoTemplate) {
    this.repository = repository;
    this.hiddenCards = hiddenCards;
    this.mongoTemplate = mongoTemplate;
  }

  public CatalogPageResponse<CatalogCardResponse> search(String game, CatalogSearchRequest request) {
    CardGame selected = CatalogGameResolver.parse(game);
    PageRequest pageable = pageRequest(request.page(), request.size(), request.sort());
    Criteria criteria =
        criteria(selected, request.q(), request.set(), request.rarity(), request.type());

    List<String> hidden = hiddenCards.findBySource(CatalogGameResolver.source(selected)).stream()
        .map(HiddenCard::getExternalId).toList();
    if (!hidden.isEmpty()) {
      criteria.and("externalId").nin(hidden);
    }
    Query query = new Query(criteria);

    long total = mongoTemplate.count(query, Card.class);
    List<CatalogCardResponse> content = total == 0 ? List.of()
        : mongoTemplate.find(query.with(pageable), Card.class)
            .stream().map(CatalogCardResponse::from).toList();
    return CatalogPageResponse.of(content, pageable.getPageNumber(), pageable.getPageSize(), total);
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

  /** Inclui cartas ocultas, que continuam válidas para coleções e anúncios que as referenciam. */
  public CatalogCardResponse findByExternalId(String game, String externalId) {
    if (externalId == null || externalId.isBlank()) {
      throw new InvalidCatalogRequestException("Informe o externalId.");
    }
    CardGame selected = CatalogGameResolver.parse(game);
    return repository.findBySourceAndExternalId(CatalogGameResolver.source(selected), externalId.trim())
        .map(CatalogCardResponse::from).orElseThrow(CatalogCardNotFoundException::new);
  }

  private List<String> distinct(Criteria filter, String field) {
    return mongoTemplate.findDistinct(new Query(filter), field, COLLECTION, String.class)
        .stream().filter(Objects::nonNull).sorted(String.CASE_INSENSITIVE_ORDER).toList();
  }

  /** Valida página, tamanho e ordenação; {@code page} começa em 0. */
  static PageRequest pageRequest(Integer page, Integer size, String sort) {
    int number = page == null ? 0 : page;
    int pageSize = size == null ? DEFAULT_PAGE_SIZE : size;
    if (number < 0) {
      throw new InvalidCatalogRequestException("A página deve ser maior ou igual a 0.");
    }
    if (pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
      throw new InvalidCatalogRequestException(
          "O tamanho da página deve estar entre 1 e " + MAX_PAGE_SIZE + ".");
    }
    return PageRequest.of(number, pageSize, sort(sort));
  }

  /** Sem {@code game}, pesquisa os três TCGs; o filtro por tipo exige um jogo. */
  static Criteria criteria(CardGame game, String q, String set, String rarity, String type) {
    Criteria criteria = game == null
        ? Criteria.where("game").in(Arrays.stream(CardGame.values()).map(Enum::name).toList())
        : Criteria.where("game").is(game.name());
    String name = TextNormalizer.normalizeOptionalText(q);
    if (name != null) {
      criteria.and("name").regex(Pattern.quote(name), "i");
    }
    String edition = TextNormalizer.normalizeOptionalText(set);
    if (edition != null) {
      criteria.and("edition").is(edition);
    }
    String officialRarity = TextNormalizer.normalizeOptionalText(rarity);
    if (officialRarity != null) {
      criteria.and("officialRarity").is(officialRarity);
    }
    String cardType = TextNormalizer.normalizeOptionalText(type);
    if (cardType != null) {
      if (game == null) {
        throw new InvalidCatalogRequestException("Selecione um TCG para filtrar por tipo.");
      }
      criteria.and(typeField(game)).is(type(game, cardType));
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
