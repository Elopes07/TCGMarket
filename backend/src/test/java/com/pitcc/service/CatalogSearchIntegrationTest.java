package com.pitcc.service;

import com.pitcc.dto.CatalogCardResponse;
import com.pitcc.dto.CatalogSearchRequest;
import com.pitcc.model.*;
import com.pitcc.repository.CardRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import static org.junit.jupiter.api.Assertions.*;

/** Pesquisa e filtros contra o MongoDB real do perfil test. */
@SpringBootTest
@ActiveProfiles("test")
class CatalogSearchIntegrationTest {
  @Autowired CatalogSearchService service;
  @Autowired CardRepository repository;
  private final List<Card> saved = new ArrayList<>();
  private final String run = UUID.randomUUID().toString().substring(0, 8);
  private String set;

  @BeforeEach void seed() {
    set = "Edition set-" + run;
    pokemon("Pikachu " + run, "set-" + run, "Common", PokemonCardType.POKEMON);
    pokemon("Raichu " + run, "set-" + run, "Rare", PokemonCardType.POKEMON);
    pokemon("Professor " + run, "set-" + run, "Common", PokemonCardType.TRAINER);
    pokemon("Pikachu " + run + " V", "other-" + run, "Rare", PokemonCardType.POKEMON);
    save(new YugiohCard("Mirror " + run, "Legend", "yg-" + run, null, "Common", null,
        "yg-" + run, null, null, null, null, null, null, null, YugiohCardType.TRAP, List.of()));
    save(new MagicCard("Bear " + run, "Alpha", "mtg-" + run, "1", "common", null,
        "mtg-" + run, "{1}{G}", 2.0, List.of(MagicColor.GREEN),
        List.of(MagicCardType.ARTIFACT, MagicCardType.CREATURE), "2", "2", List.of(), List.of(), null));
  }

  @AfterEach void cleanup() { repository.deleteAll(saved); }

  @Test void shouldCombineNameSetRarityAndTypeOnTheStoredCatalog() {
    assertEquals(List.of("Pikachu " + run, "Pikachu " + run + " V"),
        names(search("pokemon", "pikachu " + run, null, null, null, null, null)));
    assertEquals(List.of("Pikachu " + run, "Professor " + run, "Raichu " + run),
        names(search("pokemon", run, set, null, null, null, null)));
    assertEquals(List.of("Pikachu " + run, "Professor " + run),
        names(search("pokemon", run, set, "Common", null, null, null)));
    assertEquals(List.of("Pikachu " + run),
        names(search("pokemon", run, set, "Common", "POKEMON", null, null)));
    assertEquals(List.of("Mirror " + run), names(search("yugioh", run, null, null, "TRAP", null, null)));
    assertEquals(List.of("Bear " + run), names(search("magic", run, null, null, "CREATURE", null, null)));
    assertTrue(names(search("pokemon", run, set, null, "ENERGY", null, null)).isEmpty());
  }

  @Test void shouldPaginateAndSortTheFilteredResults() {
    var first = service.search("pokemon", new CatalogSearchRequest(run, set, null, null, 0, 2, "name-desc"));
    var second = service.search("pokemon", new CatalogSearchRequest(run, set, null, null, 1, 2, "name-desc"));

    assertEquals(List.of("Raichu " + run, "Professor " + run), names(first.content()));
    assertEquals(List.of("Pikachu " + run), names(second.content()));
    assertEquals(3, first.totalElements());
    assertEquals(2, first.totalPages());
  }

  @Test void shouldListTheFilterOptionsStoredForTheGame() {
    var pokemon = service.filters("pokemon");
    assertTrue(pokemon.sets().contains(set));
    assertTrue(pokemon.rarities().containsAll(List.of("Common", "Rare")));
    assertTrue(pokemon.types().containsAll(List.of("POKEMON", "TRAINER")));

    var magic = service.filters("magic");
    assertTrue(magic.sets().contains("Alpha"));
    assertTrue(magic.types().containsAll(List.of("ARTIFACT", "CREATURE")));
  }

  private List<CatalogCardResponse> search(String game, String q, String set, String rarity,
      String type, Integer page, Integer size) {
    return service.search(game, new CatalogSearchRequest(q, set, rarity, type, page, size, null)).content();
  }

  private static List<String> names(List<CatalogCardResponse> cards) {
    return cards.stream().map(CatalogCardResponse::name).toList();
  }

  private void pokemon(String name, String set, String rarity, PokemonCardType type) {
    boolean pokemon = type == PokemonCardType.POKEMON;
    save(new PokemonCard(name, "Edition " + set, set, "1", rarity, null, name, pokemon ? 60 : null, type,
        pokemon ? List.of(PokemonEnergyType.LIGHTNING) : List.of(), null, null, List.of()));
  }

  private void save(Card card) { saved.add(repository.save(card)); }
}
