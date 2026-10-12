package com.pitcc.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pitcc.dto.ProfileResponse;
import com.pitcc.dto.PublicProfileResponse;
import com.pitcc.dto.UpdateProfileRequest;
import com.pitcc.exception.UserNotFoundException;
import com.pitcc.exception.UsernameAlreadyTakenException;
import com.pitcc.model.CardGame;
import com.pitcc.model.User;
import com.pitcc.model.UserRole;
import com.pitcc.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

  @Mock private UserRepository userRepository;

  private ProfileService profileService;
  private User user;

  @BeforeEach
  void setUp() {
    profileService = new ProfileService(userRepository);
    user = new User();
    user.setId("u1");
    user.setName("Lucas Martins");
    user.setUsername("lucas.martins");
    user.setEmail("lucas@email.com");
    user.setPasswordHash("hash");
    user.setRole(UserRole.USER);
    user.setRegistrationDate(Instant.parse("2025-05-12T10:00:00Z"));
  }

  @Test
  void shouldUpdateOnlyProfileFieldsAndNormalizeThem() {
    when(userRepository.findById("u1")).thenReturn(Optional.of(user));
    when(userRepository.existsByUsername("lucas.tcg")).thenReturn(false);
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

    ProfileResponse response =
        profileService.updateOwnProfile(
            "u1",
            new UpdateProfileRequest(
                "  Lucas M. ",
                "lucas.tcg",
                "  Coleciono Pokémon.  ",
                "   ",
                "SP",
                List.of(CardGame.POKEMON, CardGame.POKEMON, CardGame.MAGIC_THE_GATHERING),
                List.of(" https://instagram.com/lucas ", "https://instagram.com/lucas")));

    assertEquals("Lucas M.", response.name());
    assertEquals("lucas.tcg", response.username());
    assertEquals("Coleciono Pokémon.", response.bio());
    assertNull(response.city());
    assertEquals("SP", response.state());
    assertEquals(List.of(CardGame.POKEMON, CardGame.MAGIC_THE_GATHERING), response.favoriteGames());
    assertEquals(List.of("https://instagram.com/lucas"), response.links());
    assertEquals("lucas@email.com", user.getEmail());
    assertEquals(UserRole.USER, user.getRole());
    assertEquals("hash", user.getPasswordHash());
  }

  @Test
  void shouldKeepOwnUsernameWithoutCheckingAvailability() {
    when(userRepository.findById("u1")).thenReturn(Optional.of(user));
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

    profileService.updateOwnProfile("u1", request("lucas.martins"));

    verify(userRepository, never()).existsByUsername(any());
  }

  @Test
  void shouldRejectUsernameAlreadyInUse() {
    when(userRepository.findById("u1")).thenReturn(Optional.of(user));
    when(userRepository.existsByUsername("maria.cardz")).thenReturn(true);

    assertThrows(
        UsernameAlreadyTakenException.class,
        () -> profileService.updateOwnProfile("u1", request("maria.cardz")));
    verify(userRepository, never()).save(any());
  }

  @Test
  void shouldTranslateConcurrentUsernameCollision() {
    when(userRepository.findById("u1")).thenReturn(Optional.of(user));
    when(userRepository.existsByUsername("lucas.tcg")).thenReturn(false);
    when(userRepository.save(any(User.class))).thenThrow(new DuplicateKeyException("username"));

    assertThrows(
        UsernameAlreadyTakenException.class,
        () -> profileService.updateOwnProfile("u1", request("lucas.tcg")));
  }

  @Test
  void shouldExposePublicProfileWithoutPrivateData() {
    user.setRole(UserRole.ADMIN);
    when(userRepository.findByUsername("lucas.martins")).thenReturn(Optional.of(user));

    PublicProfileResponse response = profileService.publicProfile(" Lucas.Martins ");

    assertEquals("lucas.martins", response.username());
    assertTrue(response.administrator());
    assertTrue(response.favoriteGames().isEmpty());
    assertFalse(response.toString().contains("lucas@email.com"));
  }

  @Test
  void shouldReturnNotFoundForUnknownUsername() {
    when(userRepository.findByUsername("ninguem")).thenReturn(Optional.empty());

    assertThrows(UserNotFoundException.class, () -> profileService.publicProfile("ninguem"));
  }

  private UpdateProfileRequest request(String username) {
    return new UpdateProfileRequest("Lucas Martins", username, null, null, null, null, null);
  }
}
