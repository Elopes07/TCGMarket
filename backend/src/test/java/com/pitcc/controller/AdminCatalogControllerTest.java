package com.pitcc.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pitcc.dto.AdminCatalogCardResponse;
import com.pitcc.dto.AdminCatalogSearchRequest;
import com.pitcc.dto.AdminCatalogSummaryResponse;
import com.pitcc.dto.AdminCatalogSummaryResponse.GameSummary;
import com.pitcc.dto.CatalogPageResponse;
import com.pitcc.exception.CatalogCardNotFoundException;
import com.pitcc.exception.GlobalExceptionHandler;
import com.pitcc.model.CardGame;
import com.pitcc.model.CardSource;
import com.pitcc.model.User;
import com.pitcc.model.UserRole;
import com.pitcc.repository.UserRepository;
import com.pitcc.security.ApiErrorWriter;
import com.pitcc.security.JwtService;
import com.pitcc.security.SecurityConfig;
import com.pitcc.service.AdminCatalogService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminCatalogController.class)
@Import({SecurityConfig.class, JwtService.class, ApiErrorWriter.class, GlobalExceptionHandler.class})
@TestPropertySource(
    properties = {
      "security.jwt.secret=test-secret-key-must-be-at-least-32-bytes!",
      "security.jwt.expiration=1h"
    })
class AdminCatalogControllerTest {

  private static final String REASON = "Imagem da carta trocada na API.";

  @Autowired private MockMvc mockMvc;

  @Autowired private JwtService jwtService;

  @MockitoBean private AdminCatalogService adminCatalogService;

  @MockitoBean private UserRepository userRepository;

  @Test
  void shouldRequireAuthenticationForTheAdminCatalog() throws Exception {
    mockMvc
        .perform(get("/api/admin/catalog/cards"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value(SecurityConfig.AUTHENTICATION_REQUIRED));

    verify(adminCatalogService, never()).search(any());
  }

  @Test
  void shouldForbidUserFromEveryAdminCatalogOperation() throws Exception {
    String token = tokenFor("user1", UserRole.USER);

    mockMvc
        .perform(get("/api/admin/catalog/cards").header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.message").value(SecurityConfig.ACCESS_DENIED));
    mockMvc
        .perform(get("/api/admin/catalog/summary").header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(hide("card1", token, REASON))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(
            delete("/api/admin/catalog/cards/card1/hidden")
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isForbidden());

    verify(adminCatalogService, never()).search(any());
    verify(adminCatalogService, never()).summary();
    verify(adminCatalogService, never()).hide(anyString(), anyString(), anyString());
    verify(adminCatalogService, never()).unhide(anyString());
  }

  @Test
  void shouldSearchTheCatalogWithTheGivenFilters() throws Exception {
    String token = tokenFor("admin1", UserRole.ADMIN);
    when(adminCatalogService.search(new AdminCatalogSearchRequest(
            "pokemon", "chari", "Base", "Rare Holo", "POKEMON", "hidden", 1, 10, "name-desc")))
        .thenReturn(CatalogPageResponse.of(List.of(card(true)), 1, 10, 11));

    mockMvc
        .perform(
            get("/api/admin/catalog/cards")
                .param("game", "pokemon")
                .param("q", "chari")
                .param("set", "Base")
                .param("rarity", "Rare Holo")
                .param("type", "POKEMON")
                .param("visibility", "hidden")
                .param("page", "1")
                .param("size", "10")
                .param("sort", "name-desc")
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].id").value("card1"))
        .andExpect(jsonPath("$.content[0].source").value("POKEMON_TCG_API"))
        .andExpect(jsonPath("$.content[0].externalId").value("base1-4"))
        .andExpect(jsonPath("$.content[0].hidden").value(true))
        .andExpect(jsonPath("$.content[0].hiddenReason").value(REASON))
        .andExpect(jsonPath("$.totalElements").value(11))
        .andExpect(jsonPath("$.totalPages").value(2));
  }

  @Test
  void shouldReturnTheCatalogSummary() throws Exception {
    String token = tokenFor("admin1", UserRole.ADMIN);
    when(adminCatalogService.summary())
        .thenReturn(new AdminCatalogSummaryResponse(5, 1, List.of(
            new GameSummary(CardGame.POKEMON, 3, 1),
            new GameSummary(CardGame.YUGIOH, 2, 0),
            new GameSummary(CardGame.MAGIC_THE_GATHERING, 0, 0))));

    mockMvc
        .perform(get("/api/admin/catalog/summary").header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalCards").value(5))
        .andExpect(jsonPath("$.hiddenCards").value(1))
        .andExpect(jsonPath("$.games[0].game").value("POKEMON"))
        .andExpect(jsonPath("$.games[2].totalCards").value(0));
  }

  @Test
  void shouldHideTheCardUsingTheAdminIdFromTheTokenAndTheTrimmedReason() throws Exception {
    String token = tokenFor("admin1", UserRole.ADMIN);
    when(adminCatalogService.hide("admin1", "card1", REASON)).thenReturn(card(true));

    mockMvc
        .perform(hide("card1", token, "   " + REASON + "  "))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.hidden").value(true));

    verify(adminCatalogService).hide("admin1", "card1", REASON);
  }

  @Test
  void shouldRejectAMissingOrShortReason() throws Exception {
    String token = tokenFor("admin1", UserRole.ADMIN);

    mockMvc
        .perform(hide("card1", token, "   curto   "))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Motivo inválido."))
        .andExpect(jsonPath("$.fields[0].field").value("reason"));
    mockMvc
        .perform(
            put("/api/admin/catalog/cards/card1/hidden")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fields[0].field").value("reason"));
    mockMvc
        .perform(hide("card1", token, "x".repeat(301)))
        .andExpect(status().isBadRequest());

    verify(adminCatalogService, never()).hide(anyString(), anyString(), anyString());
  }

  @Test
  void shouldUnhideTheCard() throws Exception {
    String token = tokenFor("admin1", UserRole.ADMIN);
    when(adminCatalogService.unhide("card1")).thenReturn(card(false));

    mockMvc
        .perform(
            delete("/api/admin/catalog/cards/card1/hidden")
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.hidden").value(false))
        .andExpect(jsonPath("$.hiddenReason").doesNotExist());
  }

  @Test
  void shouldReturnNotFoundForUnknownCard() throws Exception {
    String token = tokenFor("admin1", UserRole.ADMIN);
    when(adminCatalogService.hide("admin1", "missing", REASON))
        .thenThrow(new CatalogCardNotFoundException());

    mockMvc
        .perform(hide("missing", token, REASON))
        .andExpect(status().isNotFound());
  }

  private static org.springframework.test.web.servlet.RequestBuilder hide(
      String id, String token, String reason) {
    return put("/api/admin/catalog/cards/" + id + "/hidden")
        .header(HttpHeaders.AUTHORIZATION, bearer(token))
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"reason\": \"" + reason + "\"}");
  }

  private String tokenFor(String id, UserRole role) {
    User user = new User();
    user.setId(id);
    user.setRole(role);
    when(userRepository.findById(id)).thenReturn(Optional.of(user));
    return jwtService.generate(id, role);
  }

  private static String bearer(String token) {
    return "Bearer " + token;
  }

  private static AdminCatalogCardResponse card(boolean hidden) {
    return new AdminCatalogCardResponse("card1", "base1-4", CardSource.POKEMON_TCG_API,
        CardGame.POKEMON, "Charizard", "Base", "base1", "4", "Rare Holo", null, hidden,
        hidden ? REASON : null, hidden ? Instant.parse("2026-10-13T12:00:00Z") : null);
  }
}
