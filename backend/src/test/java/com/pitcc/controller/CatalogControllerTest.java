package com.pitcc.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pitcc.config.PermitAllSecurityConfig;
import com.pitcc.dto.CatalogCardResponse;
import com.pitcc.dto.CatalogFiltersResponse;
import com.pitcc.dto.CatalogPageResponse;
import com.pitcc.dto.CatalogSearchRequest;
import com.pitcc.exception.CatalogCardNotFoundException;
import com.pitcc.exception.GlobalExceptionHandler;
import com.pitcc.exception.InvalidCatalogRequestException;
import com.pitcc.integration.catalog.ExternalApiErrorType;
import com.pitcc.integration.catalog.ExternalApiException;
import com.pitcc.model.CardGame;
import com.pitcc.service.CatalogSearchService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CatalogController.class)
@Import({GlobalExceptionHandler.class, PermitAllSecurityConfig.class})
class CatalogControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private CatalogSearchService catalogSearchService;

  @Test
  void shouldReturnAPageOfCardsFromTheRequestedCatalog() throws Exception {
    CatalogSearchRequest request =
        new CatalogSearchRequest("charizard", "Base", "Rare Holo", "POKEMON", 1, 10, "name-desc");
    when(catalogSearchService.search("pokemon", request))
        .thenReturn(CatalogPageResponse.of(List.of(new CatalogCardResponse(
            "base1-4",
            null,
            "Charizard",
            CardGame.POKEMON,
            "Base",
            "base1",
            "4",
            "Rare Holo",
            "https://img.example/charizard.png")), 1, 10, 11));

    mockMvc.perform(get("/api/catalog/pokemon/cards")
            .param("q", "charizard")
            .param("set", "Base")
            .param("rarity", "Rare Holo")
            .param("type", "POKEMON")
            .param("page", "1")
            .param("size", "10")
            .param("sort", "name-desc"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].externalId").value("base1-4"))
        .andExpect(jsonPath("$.content[0].name").value("Charizard"))
        .andExpect(jsonPath("$.content[0].cardGame").value("POKEMON"))
        .andExpect(jsonPath("$.content[0].setCode").value("base1"))
        .andExpect(jsonPath("$.page").value(1))
        .andExpect(jsonPath("$.size").value(10))
        .andExpect(jsonPath("$.totalElements").value(11))
        .andExpect(jsonPath("$.totalPages").value(2));
  }

  @Test
  void shouldAllowListingTheCatalogWithoutParameters() throws Exception {
    when(catalogSearchService.search(eq("yugioh"), any()))
        .thenReturn(CatalogPageResponse.of(List.of(), 0, 20, 0));

    mockMvc.perform(get("/api/catalog/yugioh/cards"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isEmpty())
        .andExpect(jsonPath("$.totalPages").value(0));

    verify(catalogSearchService).search(
        "yugioh", new CatalogSearchRequest(null, null, null, null, null, null, null));
  }

  @Test
  void shouldReturnTheFilterOptionsOfTheCatalog() throws Exception {
    when(catalogSearchService.filters("magic"))
        .thenReturn(new CatalogFiltersResponse(
            List.of("Limited Edition Alpha"),
            List.of("rare"),
            List.of("CREATURE")));

    mockMvc.perform(get("/api/catalog/magic/filters"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.sets[0]").value("Limited Edition Alpha"))
        .andExpect(jsonPath("$.rarities[0]").value("rare"))
        .andExpect(jsonPath("$.types[0]").value("CREATURE"));
  }

  @Test
  void shouldReturnOneCardByExternalId() throws Exception {
    when(catalogSearchService.findByExternalId("magic", "alpha"))
        .thenReturn(new CatalogCardResponse(
            "alpha", "oracle", "Black Lotus", CardGame.MAGIC_THE_GATHERING, null, "lea", "232", "rare", null));

    mockMvc.perform(get("/api/catalog/magic/cards/by-external-id").param("externalId", "alpha"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.externalId").value("alpha"))
        .andExpect(jsonPath("$.name").value("Black Lotus"));
  }

  @Test
  void shouldTranslateCatalogErrors() throws Exception {
    when(catalogSearchService.search(eq("digimon"), any()))
        .thenThrow(new InvalidCatalogRequestException("Jogo desconhecido: digimon. Use pokemon, yugioh ou magic."));
    when(catalogSearchService.findByExternalId("yugioh", "missing"))
        .thenThrow(new CatalogCardNotFoundException());
    when(catalogSearchService.search(eq("pokemon"), any()))
        .thenThrow(new ExternalApiException(
            CardGame.POKEMON, ExternalApiErrorType.UNAVAILABLE, null, "Catalog unavailable for POKEMON", null));

    mockMvc.perform(get("/api/catalog/digimon/cards").param("q", "x"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Jogo desconhecido: digimon. Use pokemon, yugioh ou magic."));

    mockMvc.perform(get("/api/catalog/yugioh/cards/by-external-id").param("externalId", "missing"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Nenhuma carta encontrada para esse identificador."));

    mockMvc.perform(get("/api/catalog/pokemon/cards").param("q", "pikachu"))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.message").value("O catálogo externo está indisponível."));

    mockMvc.perform(get("/api/catalog/magic/cards").param("page", "abc"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Parâmetro inválido: page."));
  }
}
