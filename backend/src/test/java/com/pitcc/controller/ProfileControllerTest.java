package com.pitcc.controller;

import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pitcc.dto.ProfileResponse;
import com.pitcc.dto.PublicProfileResponse;
import com.pitcc.dto.UpdateProfileRequest;
import com.pitcc.exception.GlobalExceptionHandler;
import com.pitcc.exception.UserNotFoundException;
import com.pitcc.exception.UsernameAlreadyTakenException;
import com.pitcc.model.CardGame;
import com.pitcc.model.User;
import com.pitcc.model.UserRole;
import com.pitcc.repository.UserRepository;
import com.pitcc.security.ApiErrorWriter;
import com.pitcc.security.JwtService;
import com.pitcc.security.SecurityConfig;
import com.pitcc.service.ProfileService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProfileController.class)
@Import({SecurityConfig.class, JwtService.class, ApiErrorWriter.class, GlobalExceptionHandler.class})
@TestPropertySource(
    properties = {
      "security.jwt.secret=test-secret-key-must-be-at-least-32-bytes!",
      "security.jwt.expiration=1h"
    })
class ProfileControllerTest {

  private static final String VALID_BODY =
      """
      {
        "name": "Lucas Martins",
        "username": "lucas.tcg",
        "bio": "Coleciono Pokémon.",
        "city": "Araras",
        "state": "SP",
        "favoriteGames": ["POKEMON"],
        "links": ["https://instagram.com/lucas.tcg"]
      }
      """;

  @Autowired private MockMvc mockMvc;

  @Autowired private JwtService jwtService;

  @MockitoBean private ProfileService profileService;

  @MockitoBean private UserRepository userRepository;

  private String token;

  @BeforeEach
  void authenticate() {
    User user = new User();
    user.setId("u1");
    user.setRole(UserRole.USER);
    when(userRepository.findById("u1")).thenReturn(Optional.of(user));
    token = "Bearer " + jwtService.generate("u1", UserRole.USER);
  }

  @Test
  void shouldRequireAuthenticationForOwnProfile() throws Exception {
    mockMvc
        .perform(get("/api/users/me/profile"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("Autenticação necessária."));

    mockMvc
        .perform(put("/api/users/me/profile").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
        .andExpect(status().isUnauthorized());
    verify(profileService, never()).updateOwnProfile(any(), any());
  }

  @Test
  void shouldReturnOwnProfileOfAuthenticatedUser() throws Exception {
    when(profileService.ownProfile("u1")).thenReturn(profile());

    mockMvc
        .perform(get("/api/users/me/profile").header(HttpHeaders.AUTHORIZATION, token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.username").value("lucas.tcg"))
        .andExpect(jsonPath("$.email").value("lucas@email.com"))
        .andExpect(jsonPath("$.passwordHash").doesNotExist());
  }

  @Test
  void shouldIgnoreFieldsOutsideTheProfileContract() throws Exception {
    when(profileService.updateOwnProfile(eq("u1"), any(UpdateProfileRequest.class)))
        .thenReturn(profile());

    mockMvc
        .perform(
            put("/api/users/me/profile")
                .header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "name": "Lucas Martins",
                      "username": "lucas.tcg",
                      "role": "ADMIN",
                      "email": "outro@email.com",
                      "passwordHash": "x",
                      "id": "outro"
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.role").value("USER"));

    ArgumentCaptor<UpdateProfileRequest> captor = ArgumentCaptor.forClass(UpdateProfileRequest.class);
    verify(profileService).updateOwnProfile(eq("u1"), captor.capture());
    assertEquals("lucas.tcg", captor.getValue().username());
  }

  @Test
  void shouldRejectInvalidProfileFields() throws Exception {
    mockMvc
        .perform(
            put("/api/users/me/profile")
                .header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "name": "L",
                      "username": "Lucas TCG",
                      "bio": "%s",
                      "state": "XX",
                      "links": ["javascript:alert(1)", "https://ok.com", "https://a.com", "https://b.com"]
                    }
                    """
                        .formatted("a".repeat(281))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Dados do perfil inválidos."))
        .andExpect(jsonPath("$.fields[*].field").value(hasItem("name")))
        .andExpect(jsonPath("$.fields[*].field").value(hasItem("username")))
        .andExpect(jsonPath("$.fields[*].field").value(hasItem("bio")))
        .andExpect(jsonPath("$.fields[*].field").value(hasItem("state")))
        .andExpect(jsonPath("$.fields[*].field").value(hasItem("links")))
        .andExpect(jsonPath("$.fields[*].field").value(hasItem("links[0]")));
    verify(profileService, never()).updateOwnProfile(any(), any());
  }

  @Test
  void shouldRejectUnknownGame() throws Exception {
    mockMvc
        .perform(
            put("/api/users/me/profile")
                .header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    { "name": "Lucas", "username": "lucas", "favoriteGames": ["DIGIMON"] }
                    """))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldReportUsernameConflictOnTheField() throws Exception {
    when(profileService.updateOwnProfile(eq("u1"), any(UpdateProfileRequest.class)))
        .thenThrow(new UsernameAlreadyTakenException());

    mockMvc
        .perform(
            put("/api/users/me/profile")
                .header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BODY))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.fields[0].field").value("username"))
        .andExpect(jsonPath("$.fields[0].message").value("Este nome de usuário já está em uso."));
  }

  @Test
  void shouldServePublicProfileWithoutAuthentication() throws Exception {
    when(profileService.publicProfile("maria.cardz"))
        .thenReturn(
            new PublicProfileResponse(
                "Maria Cardoso",
                "maria.cardz",
                false,
                Instant.parse("2025-08-01T10:00:00Z"),
                "Duelista.",
                "Campinas",
                "SP",
                List.of(CardGame.YUGIOH),
                List.of()));

    mockMvc
        .perform(get("/api/users/maria.cardz/profile"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.username").value("maria.cardz"))
        .andExpect(jsonPath("$.email").doesNotExist())
        .andExpect(jsonPath("$.role").doesNotExist())
        .andExpect(jsonPath("$.id").doesNotExist());
  }

  @Test
  void shouldReturnNotFoundForUnknownPublicProfile() throws Exception {
    when(profileService.publicProfile("ninguem")).thenThrow(new UserNotFoundException());

    mockMvc
        .perform(get("/api/users/ninguem/profile"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Usuário não encontrado."));
  }

  private ProfileResponse profile() {
    return new ProfileResponse(
        "u1",
        "Lucas Martins",
        "lucas.tcg",
        "lucas@email.com",
        UserRole.USER,
        Instant.parse("2025-05-12T10:00:00Z"),
        "Coleciono Pokémon.",
        "Araras",
        "SP",
        List.of(CardGame.POKEMON),
        List.of("https://instagram.com/lucas.tcg"));
  }
}
