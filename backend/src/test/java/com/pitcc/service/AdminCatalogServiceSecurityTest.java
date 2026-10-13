package com.pitcc.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.pitcc.model.UserRole;
import com.pitcc.repository.CardRepository;
import com.pitcc.repository.HiddenCardRepository;
import com.pitcc.security.AuthenticatedUser;
import java.util.List;
import org.bson.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(AdminCatalogServiceSecurityTest.MethodSecurity.class)
class AdminCatalogServiceSecurityTest {

  @Configuration
  @EnableMethodSecurity
  @Import(AdminCatalogService.class)
  static class MethodSecurity {}

  @Autowired private AdminCatalogService adminCatalogService;

  @MockitoBean private CardRepository cardRepository;

  @MockitoBean private HiddenCardRepository hiddenCardRepository;

  @MockitoBean private MongoTemplate mongoTemplate;

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void shouldBlockUserEvenIfTheServiceIsCalledDirectly() {
    authenticateAs(UserRole.USER);

    assertThrows(AccessDeniedException.class, () -> adminCatalogService.summary());
    assertThrows(
        AccessDeniedException.class, () -> adminCatalogService.hide("user1", "card1", "motivo"));
    assertThrows(AccessDeniedException.class, () -> adminCatalogService.unhide("card1"));
  }

  @Test
  void shouldBlockAnonymousCalls() {
    assertThrows(
        AuthenticationCredentialsNotFoundException.class, () -> adminCatalogService.summary());
  }

  @Test
  void shouldAllowAdmin() {
    authenticateAs(UserRole.ADMIN);
    when(mongoTemplate.aggregate(any(Aggregation.class), anyString(), eq(Document.class)))
        .thenReturn(new AggregationResults<>(List.of(), new Document()));

    assertDoesNotThrow(() -> adminCatalogService.summary());
  }

  private void authenticateAs(UserRole role) {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser("abc123", role),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))));
  }
}
