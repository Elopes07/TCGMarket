package com.pitcc.service;

import static org.junit.jupiter.api.Assertions.*;

import com.pitcc.dto.AdminCatalogCardResponse;
import com.pitcc.dto.AdminCatalogSearchRequest;
import com.pitcc.dto.AdminCatalogSummaryResponse;
import com.pitcc.dto.CatalogCardResponse;
import com.pitcc.dto.CatalogSearchRequest;
import com.pitcc.exception.InvalidCatalogRequestException;
import com.pitcc.model.*;
import com.pitcc.repository.CardRepository;
import com.pitcc.repository.HiddenCardRepository;
import com.pitcc.security.AuthenticatedUser;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

/** Ocultação de cartas contra o MongoDB real do perfil test. */
@SpringBootTest
@ActiveProfiles("test")
class AdminCatalogIntegrationTest {
  private static final String REASON = "Imagem da carta trocada na API.";

  @Autowired AdminCatalogService admin;
  @Autowired CatalogSearchService catalog;
  @Autowired CardImportService importer;
  @Autowired CardRepository cards;
  @Autowired HiddenCardRepository hiddenCards;
  private final List<Card> saved = new ArrayList<>();
  private final String run = UUID.randomUUID().toString().substring(0, 8);
  private Card charizard;
  private Card dark;

  @BeforeEach void seed() {
    SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
        new AuthenticatedUser("admin1", UserRole.ADMIN), null,
        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    charizard = save(pokemon("Charizard " + run));
    save(pokemon("Pikachu " + run));
    dark = save(new YugiohCard("Dark " + run, "Starter", "yg-" + run, null, "Ultra Rare", null,
        "yg-" + run, null, null, null, null, null, null, null, YugiohCardType.TRAP, List.of()));
  }

  @AfterEach void cleanup() {
    saved.forEach(card ->
        hiddenCards.deleteBySourceAndExternalId(card.getSource(), card.getExternalId()));
    cards.deleteAll(saved);
    SecurityContextHolder.clearContext();
  }

  @Test void shouldHideTheCardFromThePublicCatalogAndKeepItHiddenAfterReimport() {
    AdminCatalogCardResponse hidden = admin.hide("admin1", charizard.getId(), REASON);

    assertTrue(hidden.hidden());
    assertEquals(REASON, hidden.hiddenReason());
    assertNotNull(hidden.hiddenAt());
    assertEquals(List.of("Pikachu " + run), publicNames());
    assertEquals("Charizard " + run, catalog.findByExternalId("pokemon", "Charizard " + run).name(),
        "a carta oculta continua acessível para coleções e anúncios que a referenciam");

    Card reimported = importer.upsert(pokemon("Charizard " + run));

    assertEquals(charizard.getId(), reimported.getId());
    assertEquals(List.of("Pikachu " + run), publicNames());
    AdminCatalogCardResponse afterImport =
        search(null, "hidden").stream().filter(card -> card.id().equals(charizard.getId()))
            .findFirst().orElseThrow();
    assertEquals(CardSource.POKEMON_TCG_API, afterImport.source());
    assertEquals("Charizard " + run, afterImport.externalId());

    admin.unhide(charizard.getId());

    assertEquals(List.of("Charizard " + run, "Pikachu " + run), publicNames());
  }

  @Test void shouldFilterTheAdminCatalogByVisibilityAcrossGames() {
    admin.hide("admin1", charizard.getId(), REASON);
    admin.hide("admin1", dark.getId(), REASON);

    assertEquals(List.of("Charizard " + run, "Dark " + run), names(search(null, "hidden")));
    assertEquals(List.of("Pikachu " + run), names(search(null, "visible")));
    assertEquals(List.of("Charizard " + run, "Dark " + run, "Pikachu " + run),
        names(search(null, null)));
    assertEquals(List.of("Charizard " + run), names(search("pokemon", "hidden")));
    assertEquals(List.of(true, false), search("pokemon", "all").stream()
        .map(AdminCatalogCardResponse::hidden).toList());
  }

  @Test void shouldCountHiddenCardsPerGameInTheSummary() {
    AdminCatalogSummaryResponse before = admin.summary();

    admin.hide("admin1", charizard.getId(), REASON);
    admin.hide("admin1", charizard.getId(), "Outro motivo para a mesma carta.");
    AdminCatalogSummaryResponse after = admin.summary();

    assertEquals(List.of(CardGame.POKEMON, CardGame.YUGIOH, CardGame.MAGIC_THE_GATHERING),
        after.games().stream().map(AdminCatalogSummaryResponse.GameSummary::game).toList());
    assertEquals(before.totalCards(), after.totalCards());
    assertEquals(before.hiddenCards() + 1, after.hiddenCards());
    assertEquals(before.games().getFirst().hiddenCards() + 1, after.games().getFirst().hiddenCards());
    assertEquals(after.totalCards(),
        after.games().stream().mapToLong(AdminCatalogSummaryResponse.GameSummary::totalCards).sum());
  }

  @Test void shouldRequireAGameToFilterByType() {
    assertThrows(InvalidCatalogRequestException.class, () -> admin.search(
        new AdminCatalogSearchRequest(null, run, null, null, "TRAP", null, null, null, null)));
    assertThrows(InvalidCatalogRequestException.class, () -> admin.search(
        new AdminCatalogSearchRequest(null, run, null, null, null, "deleted", null, null, null)));
  }

  private List<AdminCatalogCardResponse> search(String game, String visibility) {
    return admin.search(new AdminCatalogSearchRequest(
        game, run, null, null, null, visibility, null, null, null)).content();
  }

  private List<String> publicNames() {
    return catalog.search("pokemon", new CatalogSearchRequest(run, null, null, null, null, null, null))
        .content().stream().map(CatalogCardResponse::name).toList();
  }

  private static List<String> names(List<AdminCatalogCardResponse> found) {
    return found.stream().map(AdminCatalogCardResponse::name).toList();
  }

  private PokemonCard pokemon(String name) {
    return new PokemonCard(name, "Base " + run, "base-" + run, "4", "Rare Holo", null, name, 120,
        PokemonCardType.POKEMON, List.of(PokemonEnergyType.FIRE), null, null, List.of());
  }

  private Card save(Card card) {
    Card persisted = cards.save(card);
    saved.add(persisted);
    return persisted;
  }
}
