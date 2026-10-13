package com.pitcc.service;

import com.pitcc.dto.AdminCatalogCardResponse;
import com.pitcc.dto.AdminCatalogSearchRequest;
import com.pitcc.dto.AdminCatalogSummaryResponse;
import com.pitcc.dto.AdminCatalogSummaryResponse.GameSummary;
import com.pitcc.dto.CatalogPageResponse;
import com.pitcc.exception.CatalogCardNotFoundException;
import com.pitcc.exception.InvalidCatalogRequestException;
import com.pitcc.model.Card;
import com.pitcc.model.CardGame;
import com.pitcc.model.CardSource;
import com.pitcc.model.HiddenCard;
import com.pitcc.repository.CardRepository;
import com.pitcc.repository.HiddenCardRepository;
import com.pitcc.util.TextNormalizer;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.bson.Document;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
@PreAuthorize("hasRole('ADMIN')")
public class AdminCatalogService {

  private enum Visibility { ALL, VISIBLE, HIDDEN }

  private final CardRepository cards;
  private final HiddenCardRepository hiddenCards;
  private final MongoTemplate mongoTemplate;

  public AdminCatalogService(
      CardRepository cards, HiddenCardRepository hiddenCards, MongoTemplate mongoTemplate) {
    this.cards = cards;
    this.hiddenCards = hiddenCards;
    this.mongoTemplate = mongoTemplate;
  }

  public CatalogPageResponse<AdminCatalogCardResponse> search(AdminCatalogSearchRequest request) {
    CardGame game = TextNormalizer.normalizeOptionalText(request.game()) == null
        ? null : CatalogGameResolver.parse(request.game());
    Visibility visibility = visibility(request.visibility());
    PageRequest pageable =
        CatalogSearchService.pageRequest(request.page(), request.size(), request.sort());
    Criteria criteria = CatalogSearchService.criteria(
        game, request.q(), request.set(), request.rarity(), request.type());

    if (visibility != Visibility.ALL) {
      List<HiddenCard> hidden = game == null
          ? hiddenCards.findAll() : hiddenCards.findBySource(CatalogGameResolver.source(game));
      List<Criteria> matches = bySource(hidden);
      if (visibility == Visibility.HIDDEN) {
        if (matches.isEmpty()) {
          return CatalogPageResponse.of(
              List.of(), pageable.getPageNumber(), pageable.getPageSize(), 0);
        }
        criteria.orOperator(matches);
      } else if (!matches.isEmpty()) {
        criteria.norOperator(matches);
      }
    }
    Query query = new Query(criteria);

    long total = mongoTemplate.count(query, Card.class);
    List<Card> found =
        total == 0 ? List.of() : mongoTemplate.find(query.with(pageable), Card.class);
    return CatalogPageResponse.of(
        withVisibility(found), pageable.getPageNumber(), pageable.getPageSize(), total);
  }

  /** Ocultar de novo uma carta já oculta atualiza o motivo, o autor e a data. */
  public AdminCatalogCardResponse hide(String actorId, String cardId, String reason) {
    Card card = cards.findById(cardId).orElseThrow(CatalogCardNotFoundException::new);
    Query identity = Query.query(Criteria.where("source").is(card.getSource())
        .and("externalId").is(card.getExternalId()));
    Update update = new Update()
        .set("game", card.getGame())
        .set("reason", reason)
        .set("hiddenBy", actorId)
        .set("hiddenAt", Instant.now());
    HiddenCard hidden = mongoTemplate.findAndModify(identity, update,
        FindAndModifyOptions.options().upsert(true).returnNew(true), HiddenCard.class);
    return AdminCatalogCardResponse.from(card, hidden);
  }

  public AdminCatalogCardResponse unhide(String cardId) {
    Card card = cards.findById(cardId).orElseThrow(CatalogCardNotFoundException::new);
    hiddenCards.deleteBySourceAndExternalId(card.getSource(), card.getExternalId());
    return AdminCatalogCardResponse.from(card, null);
  }

  public AdminCatalogSummaryResponse summary() {
    Map<CardGame, Long> totals = countByGame("cards");
    Map<CardGame, Long> hidden = countByGame("hidden_cards");
    List<GameSummary> games = Arrays.stream(CardGame.values())
        .map(game -> new GameSummary(
            game, totals.getOrDefault(game, 0L), hidden.getOrDefault(game, 0L)))
        .toList();
    return new AdminCatalogSummaryResponse(
        games.stream().mapToLong(GameSummary::totalCards).sum(),
        games.stream().mapToLong(GameSummary::hiddenCards).sum(),
        games);
  }

  private List<AdminCatalogCardResponse> withVisibility(List<Card> found) {
    if (found.isEmpty()) {
      return List.of();
    }
    Map<String, HiddenCard> hidden = hiddenCards
        .findByExternalIdIn(found.stream().map(Card::getExternalId).toList()).stream()
        .collect(Collectors.toMap(
            item -> key(item.getSource(), item.getExternalId()), Function.identity()));
    return found.stream()
        .map(card -> AdminCatalogCardResponse.from(
            card, hidden.get(key(card.getSource(), card.getExternalId()))))
        .toList();
  }

  private Map<CardGame, Long> countByGame(String collection) {
    List<String> known = Arrays.stream(CardGame.values()).map(Enum::name).toList();
    return mongoTemplate
        .aggregate(Aggregation.newAggregation(Aggregation.group("game").count().as("total")),
            collection, Document.class)
        .getMappedResults().stream()
        .filter(row -> known.contains(row.get("_id")))
        .collect(Collectors.toMap(
            row -> CardGame.valueOf(row.getString("_id")),
            row -> ((Number) row.get("total")).longValue()));
  }

  private static List<Criteria> bySource(List<HiddenCard> hidden) {
    return hidden.stream()
        .collect(Collectors.groupingBy(HiddenCard::getSource,
            Collectors.mapping(HiddenCard::getExternalId, Collectors.toList())))
        .entrySet().stream()
        .map(entry -> Criteria.where("source").is(entry.getKey().name())
            .and("externalId").in(entry.getValue()))
        .toList();
  }

  private static String key(CardSource source, String externalId) {
    return source + ":" + externalId;
  }

  private static Visibility visibility(String value) {
    String normalized = TextNormalizer.normalizeOptionalText(value);
    if (normalized == null || normalized.equals("all")) {
      return Visibility.ALL;
    }
    return switch (normalized) {
      case "visible" -> Visibility.VISIBLE;
      case "hidden" -> Visibility.HIDDEN;
      default -> throw new InvalidCatalogRequestException(
          "Visibilidade inválida. Use all, visible ou hidden.");
    };
  }
}
