package com.pitcc.service;

import com.pitcc.dto.CreateUserRequest;
import com.pitcc.dto.UserResponse;
import com.pitcc.dto.UserResponses;
import com.pitcc.exception.EmailAlreadyRegisteredException;
import com.pitcc.model.User;
import com.pitcc.model.UserRole;
import com.pitcc.repository.UserRepository;
import java.time.Instant;
import java.util.Locale;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

  private static final int USERNAME_ATTEMPTS = 3;

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final UsernameGenerator usernameGenerator;

  public UserService(
      UserRepository userRepository,
      PasswordEncoder passwordEncoder,
      UsernameGenerator usernameGenerator) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.usernameGenerator = usernameGenerator;
  }

  public UserResponse register(CreateUserRequest request) {
    String email = normalizeEmail(request.email());
    if (userRepository.existsByEmail(email)) {
      throw new EmailAlreadyRegisteredException(email);
    }

    User user = new User();
    user.setName(request.name().trim());
    user.setEmail(email);
    user.setPasswordHash(passwordEncoder.encode(request.password()));
    user.setRole(UserRole.USER);
    user.setRegistrationDate(Instant.now());

    for (int attempt = 1; ; attempt++) {
      user.setUsername(usernameGenerator.generate(user.getName()));
      try {
        return UserResponses.toUserResponse(userRepository.save(user));
      } catch (DuplicateKeyException exception) {
        // A colisão pode ser do e-mail ou do username gerado por um cadastro simultâneo.
        if (userRepository.existsByEmail(email)) {
          throw new EmailAlreadyRegisteredException(email);
        }
        if (attempt == USERNAME_ATTEMPTS) {
          throw exception;
        }
      }
    }
  }

  private String normalizeEmail(String email) {
    return email.trim().toLowerCase(Locale.ROOT);
  }
}
