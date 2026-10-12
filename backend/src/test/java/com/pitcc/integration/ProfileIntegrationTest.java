package com.pitcc.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.pitcc.dto.CreateUserRequest;
import com.pitcc.dto.UpdateProfileRequest;
import com.pitcc.exception.UsernameAlreadyTakenException;
import com.pitcc.model.User;
import com.pitcc.model.UserRole;
import com.pitcc.repository.UserRepository;
import com.pitcc.service.ProfileService;
import com.pitcc.service.UserService;
import com.pitcc.service.UsernameBackfill;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class ProfileIntegrationTest {

  private static final String DOMAIN = "@perfil-integracao.test";

  @Autowired private UserService userService;
  @Autowired private ProfileService profileService;
  @Autowired private UsernameBackfill usernameBackfill;
  @Autowired private UserRepository userRepository;
  @Autowired private MongoTemplate mongoTemplate;

  @BeforeEach
  @AfterEach
  void cleanUp() {
    mongoTemplate.remove(
        Query.query(Criteria.where("email").regex("perfil-integracao\\.test$")), User.class);
  }

  @Test
  void shouldGenerateDistinctUsernamesForHomonyms() {
    String first = userService.register(signup("primeiro")).username();
    String second = userService.register(signup("segundo")).username();

    assertEquals("quixote.integracao", first);
    assertEquals("quixote.integracao2", second);
  }

  @Test
  void shouldEnforceUniqueUsernameInTheDatabase() {
    userService.register(signup("primeiro"));
    User clone = legacyUser("clone");
    clone.setUsername("quixote.integracao");

    assertThrows(DuplicateKeyException.class, () -> userRepository.save(clone));
  }

  @Test
  void shouldBackfillUsernameForLegacyAccounts() {
    User legacy = userRepository.save(legacyUser("antigo"));

    usernameBackfill.run(new DefaultApplicationArguments());

    assertEquals(
        "quixote.integracao", userRepository.findById(legacy.getId()).orElseThrow().getUsername());
  }

  @Test
  void shouldRejectTakingAnotherPlayersUsername() {
    String taken = userService.register(signup("primeiro")).username();
    String otherId = userService.register(signup("segundo")).id();

    assertThrows(
        UsernameAlreadyTakenException.class,
        () ->
            profileService.updateOwnProfile(
                otherId,
                new UpdateProfileRequest("Quixote", taken, null, null, null, null, null)));
  }

  private CreateUserRequest signup(String alias) {
    return new CreateUserRequest("Quixote de la Integracao", alias + DOMAIN, "Senha@123");
  }

  private User legacyUser(String alias) {
    User user = new User();
    user.setName("Quixote Integração");
    user.setEmail(alias + DOMAIN);
    user.setPasswordHash("hash");
    user.setRole(UserRole.USER);
    user.setRegistrationDate(Instant.now());
    return user;
  }
}
