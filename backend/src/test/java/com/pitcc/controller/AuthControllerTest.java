package com.pitcc.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pitcc.dto.LoginRequest;
import com.pitcc.dto.LoginResponse;
import com.pitcc.dto.UserResponse;
import com.pitcc.exception.GlobalExceptionHandler;
import com.pitcc.exception.InvalidCredentialsException;
import com.pitcc.model.User;
import com.pitcc.model.UserRole;
import com.pitcc.repository.UserRepository;
import com.pitcc.security.ApiErrorWriter;
import com.pitcc.security.JwtService;
import com.pitcc.security.SecurityConfig;
import com.pitcc.service.AuthService;
import java.time.Instant;
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

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtService.class, ApiErrorWriter.class, GlobalExceptionHandler.class})
@TestPropertySource(
    properties = {
      "security.jwt.secret=test-secret-key-must-be-at-least-32-bytes!",
      "security.jwt.expiration=1h"
    })
class AuthControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private JwtService jwtService;

  @MockitoBean private AuthService authService;

  @MockitoBean private UserRepository userRepository;

  @Test
  void shouldLoginWithoutReturningPassword() throws Exception {
    when(authService.login(any(LoginRequest.class)))
        .thenReturn(
            new LoginResponse(
                "token-assinado",
                new UserResponse(
                    "abc123",
                    "João Silva",
                    "joao.silva",
                    "joao@email.com",
                    UserRole.USER,
                    Instant.parse("2026-10-02T18:00:00Z"))));

    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "email": "joao@email.com",
                      "password": "Senha@123"
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").value("token-assinado"))
        .andExpect(jsonPath("$.user.id").value("abc123"))
        .andExpect(jsonPath("$.user.email").value("joao@email.com"))
        .andExpect(jsonPath("$.user.role").value("USER"))
        .andExpect(jsonPath("$.password").doesNotExist())
        .andExpect(jsonPath("$.passwordHash").doesNotExist())
        .andExpect(jsonPath("$.user.password").doesNotExist())
        .andExpect(jsonPath("$.user.passwordHash").doesNotExist());
  }

  @Test
  void shouldRejectInvalidCredentialsWithoutRevealingTheAccount() throws Exception {
    when(authService.login(any(LoginRequest.class))).thenThrow(new InvalidCredentialsException());

    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "email": "joao@email.com",
                      "password": "Errada@123"
                    }
                    """))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("E-mail ou senha inválidos."))
        .andExpect(jsonPath("$.password").doesNotExist())
        .andExpect(jsonPath("$.passwordHash").doesNotExist());
  }

  @Test
  void shouldRejectIncompleteLogin() throws Exception {
    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "email": "invalido",
                      "password": ""
                    }
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Dados de login inválidos."));
  }

  @Test
  void shouldRequireAuthenticationForCurrentUser() throws Exception {
    mockMvc
        .perform(get("/api/auth/me"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("Autenticação necessária."));
  }

  @Test
  void shouldIdentifyCurrentUserFromTheToken() throws Exception {
    when(authService.currentUser())
        .thenReturn(
            new UserResponse(
                "abc123",
                "João Silva",
                "joao.silva",
                "joao@email.com",
                UserRole.ADMIN,
                Instant.parse("2026-10-02T18:00:00Z")));
    User admin = new User();
    admin.setId("abc123");
    admin.setRole(UserRole.ADMIN);
    when(userRepository.findById("abc123")).thenReturn(Optional.of(admin));
    String token = jwtService.generate("abc123", UserRole.ADMIN);

    mockMvc
        .perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value("abc123"))
        .andExpect(jsonPath("$.role").value("ADMIN"))
        .andExpect(jsonPath("$.password").doesNotExist())
        .andExpect(jsonPath("$.passwordHash").doesNotExist());
  }

  @Test
  void shouldIgnoreInvalidTokenOnCurrentUser() throws Exception {
    mockMvc
        .perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer token-invalido"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("Autenticação necessária."));
  }
}
