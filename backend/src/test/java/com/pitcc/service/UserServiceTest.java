package com.pitcc.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pitcc.dto.CreateUserRequest;
import com.pitcc.dto.UserResponse;
import com.pitcc.exception.EmailAlreadyRegisteredException;
import com.pitcc.model.User;
import com.pitcc.model.UserRole;
import com.pitcc.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @Mock private UserRepository userRepository;
  @Mock private UsernameGenerator usernameGenerator;

  private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
  private UserService userService;

  @BeforeEach
  void setUp() {
    userService = new UserService(userRepository, passwordEncoder, usernameGenerator);
  }

  @Test
  void shouldPersistNormalizedUserWithHashedPasswordAndUserRole() {
    when(userRepository.existsByEmail("joao@email.com")).thenReturn(false);
    when(usernameGenerator.generate("João Silva")).thenReturn("joao.silva");
    when(userRepository.save(any(User.class)))
        .thenAnswer(
            invocation -> {
              User user = invocation.getArgument(0);
              user.setId("abc123");
              return user;
            });

    UserResponse response =
        userService.register(new CreateUserRequest(" João Silva ", "Joao@Email.com", "Senha@123"));

    ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(captor.capture());
    User saved = captor.getValue();

    assertEquals("João Silva", saved.getName());
    assertEquals("joao@email.com", saved.getEmail());
    assertNotEquals("Senha@123", saved.getPasswordHash());
    assertTrue(passwordEncoder.matches("Senha@123", saved.getPasswordHash()));
    assertEquals(UserRole.USER, saved.getRole());
    assertEquals("joao.silva", saved.getUsername());
    assertEquals("abc123", response.id());
    assertEquals("joao.silva", response.username());
    assertEquals("joao@email.com", response.email());
    assertEquals(UserRole.USER, response.role());
  }

  @Test
  void shouldRejectDuplicateEmailBeforeSaving() {
    when(userRepository.existsByEmail("joao@email.com")).thenReturn(true);

    assertThrows(
        EmailAlreadyRegisteredException.class,
        () -> userService.register(new CreateUserRequest("João Silva", "joao@email.com", "Senha@123")));

    verify(userRepository, never()).save(any());
  }

  @Test
  void shouldTranslateConcurrentDuplicateKeyIntoEmailConflict() {
    when(userRepository.existsByEmail("joao@email.com")).thenReturn(false, true);
    when(usernameGenerator.generate("João Silva")).thenReturn("joao.silva");
    when(userRepository.save(any(User.class))).thenThrow(new DuplicateKeyException("email"));

    assertThrows(
        EmailAlreadyRegisteredException.class,
        () -> userService.register(new CreateUserRequest("João Silva", "joao@email.com", "Senha@123")));
  }

  @Test
  void shouldRetryWithAnotherUsernameWhenConcurrentSignupTookIt() {
    when(userRepository.existsByEmail("joao@email.com")).thenReturn(false);
    when(usernameGenerator.generate("João Silva")).thenReturn("joao.silva", "joao.silva2");
    when(userRepository.save(any(User.class)))
        .thenThrow(new DuplicateKeyException("username"))
        .thenAnswer(invocation -> invocation.getArgument(0));

    UserResponse response =
        userService.register(new CreateUserRequest("João Silva", "joao@email.com", "Senha@123"));

    assertEquals("joao.silva2", response.username());
  }
}
