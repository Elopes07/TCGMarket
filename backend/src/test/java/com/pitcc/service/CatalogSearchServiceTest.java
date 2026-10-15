package com.pitcc.service;

import com.pitcc.dto.CatalogSearchRequest;
import com.pitcc.exception.CatalogCardNotFoundException;
import com.pitcc.exception.InvalidCatalogRequestException;
import com.pitcc.model.*;
import com.pitcc.repository.CardRepository;
import java.util.List;
import java.util.Optional;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CatalogSearchServiceTest {
  @Mock CardRepository repository;
  @Mock MongoTemplate mongoTemplate;
  CatalogSearchService service;

  @BeforeEach void setUp() { service = new CatalogSearchService(repository, mongoTemplate); }

  private static CatalogSearchRequest request(String q, String set, String rarity, String type,
      Integer page, Integer size, String sort) {
    return new CatalogSearchRequest(q, set, rarity, type, page, size, sort);
  }

  private static Card pikachu() {
    return new PokemonCard("Pikachu", "Base", "base1", "25", "Common", null,
        "base1-25", 60, PokemonCardType.POKEMON, List.of(PokemonEnergyType.LIGHTNING), null, 1, List.of());
  }

  @Test void shouldListTheGameCatalogPagedAndSortedByNameWhenNoFilterIsGiven() {
    when(mongoTemplate.count(any(Query.class), eq(Card.class))).thenReturn(45L);
    when(mongoTemplate.find(any(Query.class), eq(Card.class))).thenReturn(List.of(pikachu()));

    var result = service.search("pokemon", request(null, null, null, null, null, null, null));

    ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
    verify(mongoTemplate).find(query.capture(), eq(Card.class));
    assertEquals(new Document("game", "POKEMON"), query.getValue().getQueryObject());
    assertEquals(new Document("name", 1).append("id", 1), query.getValue().getSortObject());
    assertEquals(0, query.getValue().getSkip());
    assertEquals(CatalogSearchService.DEFAULT_PAGE_SIZE, query.getValue().getLimit());

    assertEquals("base1-25", result.content().getFirst().externalId());
    assertEquals(0, result.page());
    assertEquals(20, result.size());
    assertEquals(45, result.totalElements());
    assertEquals(3, result.totalPages());
    verifyNoInteractions(repository);
  }

  @Test void shouldCombineNameSetRarityAndTypeFilters() {
    when(mongoTemplate.count(any(Query.class), eq(Card.class))).thenReturn(1L);
    when(mongoTemplate.find(any(Query.class), eq(Card.class))).thenReturn(List.of(pikachu()));

    service.search("pokemon", request(" Pika.chu ", " Base ", "Common", "pokemon", 2, 10, "name-desc"));

    ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
    verify(mongoTemplate).find(query.capture(), eq(Card.class));
    Document filter = query.getValue().getQueryObject();
    assertEquals("POKEMON", filter.get("game"));
    assertEquals("Base", filter.get("edition"));
    assertEquals("Common", filter.get("officialRarity"));
    assertEquals("POKEMON", filter.get("pokemonCardType"));
    var name = (java.util.regex.Pattern) filter.get("name");
    assertTrue(name.matcher("PIKA.CHU").find());
    assertFalse(name.matcher("Pikachu").find(), "o ponto da busca deve ser literal");
    assertEquals(new Document("name", -1).append("id", 1), query.getValue().getSortObject());
    assertEquals(20, query.getValue().getSkip());
    assertEquals(10, query.getValue().getLimit());
  }

  @Test void shouldUseTheTypeFieldOfEachGame() {
    when(mongoTemplate.count(any(Query.class), eq(Card.class))).thenReturn(0L);

    service.search("yugioh", request(null, null, null, "trap", null, null, null));
    service.search("magic", request(null, null, null, "CREATURE", null, null, null));

    ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
    verify(mongoTemplate, times(2)).count(query.capture(), eq(Card.class));
    assertEquals("TRAP", query.getAllValues().get(0).getQueryObject().get("yugiohCardType"));
    assertEquals("CREATURE", query.getAllValues().get(1).getQueryObject().get("cardTypes"));
  }

  @Test void shouldReturnAnEmptyPageWithoutQueryingTheCards() {
    when(mongoTemplate.count(any(Query.class), eq(Card.class))).thenReturn(0L);

    var result = service.search("mtg", request("missing", null, null, null, null, null, null));

    assertTrue(result.content().isEmpty());
    assertEquals(0, result.totalElements());
    assertEquals(0, result.totalPages());
    verify(mongoTemplate, never()).find(any(Query.class), eq(Card.class));
  }

  @Test void shouldRejectInvalidSearchParametersBeforeQueryingTheDatabase() {
    assertThrows(InvalidCatalogRequestException.class,
        () -> service.search("digimon", request("x", null, null, null, null, null, null)));
    assertThrows(InvalidCatalogRequestException.class,
        () -> service.search("pokemon", request(null, null, null, null, -1, null, null)));
    assertThrows(InvalidCatalogRequestException.class,
        () -> service.search("pokemon", request(null, null, null, null, null, 0, null)));
    assertThrows(InvalidCatalogRequestException.class,
        () -> service.search("pokemon", request(null, null, null, null, null, 51, null)));
    assertThrows(InvalidCatalogRequestException.class,
        () -> service.search("pokemon", request(null, null, null, null, null, null, "price")));
    assertThrows(InvalidCatalogRequestException.class,
        () -> service.search("pokemon", request(null, null, null, "TRAP", null, null, null)));
    verifyNoInteractions(mongoTemplate, repository);
  }

  @Test void shouldListOnlyTheFilterOptionsThatExistInTheGameCatalog() {
    when(mongoTemplate.findDistinct(any(Query.class), eq("edition"), eq("cards"), eq(String.class)))
        .thenReturn(List.of("Scarlet & Violet", "Base"));
    when(mongoTemplate.findDistinct(any(Query.class), eq("officialRarity"), eq("cards"), eq(String.class)))
        .thenReturn(java.util.Arrays.asList("Rare", null, "common"));
    when(mongoTemplate.findDistinct(any(Query.class), eq("pokemonCardType"), eq("cards"), eq(String.class)))
        .thenReturn(List.of("TRAINER", "POKEMON"));

    var result = service.filters("pokemon");

    assertEquals(List.of("Base", "Scarlet & Violet"), result.sets());
    assertEquals(List.of("common", "Rare"), result.rarities());
    assertEquals(List.of("POKEMON", "TRAINER"), result.types());

    ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
    verify(mongoTemplate, times(3)).findDistinct(query.capture(), anyString(), eq("cards"), eq(String.class));
    query.getAllValues().forEach(q -> assertEquals(new Document("game", "POKEMON"), q.getQueryObject()));
  }

  @Test void shouldUseSourceAsWellAsExternalId() {
    when(repository.findBySourceAndExternalId(CardSource.YGOPRODECK, "missing")).thenReturn(Optional.empty());
    assertThrows(CatalogCardNotFoundException.class, () -> service.findByExternalId("yugioh", " missing "));
    verify(repository).findBySourceAndExternalId(CardSource.YGOPRODECK, "missing");
  }

  @Test void shouldReturnTheDetailsOfAPokemonCardWithoutLookingForOtherPrintings() {
    Card card = new PokemonCard("Pikachu", "151", "sv3pt5", "025/165", "Common", null, "sv3pt5-25",
        60, PokemonCardType.POKEMON, List.of(PokemonEnergyType.LIGHTNING), PokemonEvolutionStage.BASIC, 1,
        List.of(new PokemonAttack("Thunder Jolt", List.of(PokemonEnergyType.LIGHTNING), "30")));
    when(repository.findById("card-1")).thenReturn(Optional.of(card));

    var result = service.findById("card-1");

    assertEquals("Pikachu", result.name());
    assertEquals(CardGame.POKEMON, result.cardGame());
    assertEquals("151", result.setName());
    assertEquals("sv3pt5", result.setCode());
    assertEquals(CardSource.POKEMON_TCG_API, result.source());
    assertEquals(60, result.pokemon().hp());
    assertEquals(1, result.pokemon().retreatCost());
    assertEquals("Thunder Jolt", result.pokemon().attacks().getFirst().name());
    assertNull(result.yugioh());
    assertNull(result.magic());
    assertTrue(result.otherPrintings().isEmpty());
    verify(repository, never()).findTop20ByGameAndConceptualIdAndIdNotOrderByEditionAsc(any(), any(), any());
  }

  @Test void shouldListTheOtherPrintingsOfTheSameConceptualCard() {
    Card card = new YugiohCard("Dark Magician", "Legend of Blue Eyes", "LOB", "LOB-005", "Ultra Rare",
        null, "46986414-LOB-005", 2500, 2100, 7, null, null, YugiohAttribute.DARK, "Spellcaster",
        YugiohCardType.MONSTER, List.of(YugiohInvocation.NORMAL));
    card.setConceptualId("46986414");
    Card other = new YugiohCard("Dark Magician", "Starter Deck: Yugi", "SDY", "SDY-006", "Ultra Rare",
        null, "46986414-SDY-006", 2500, 2100, 7, null, null, YugiohAttribute.DARK, "Spellcaster",
        YugiohCardType.MONSTER, List.of(YugiohInvocation.NORMAL));
    when(repository.findById("card-2")).thenReturn(Optional.of(card));
    when(repository.findTop20ByGameAndConceptualIdAndIdNotOrderByEditionAsc(CardGame.YUGIOH, "46986414", null))
        .thenReturn(List.of(other));

    var result = service.findById("card-2");

    assertEquals(2500, result.yugioh().atk());
    assertEquals(YugiohAttribute.DARK, result.yugioh().attribute());
    assertEquals(List.of("Starter Deck: Yugi"),
        result.otherPrintings().stream().map(p -> p.setName()).toList());
  }

  @Test void shouldFailWhenTheInternalIdDoesNotExist() {
    when(repository.findById("missing")).thenReturn(Optional.empty());
    assertThrows(CatalogCardNotFoundException.class, () -> service.findById("missing"));
  }

  @Test void shouldRejectInvalidLookupsBeforeQueryingTheDatabase() {
    assertThrows(InvalidCatalogRequestException.class, () -> service.findByExternalId("pokemon", " "));
    assertThrows(InvalidCatalogRequestException.class, () -> service.filters("digimon"));
    verifyNoInteractions(repository, mongoTemplate);
  }
}
