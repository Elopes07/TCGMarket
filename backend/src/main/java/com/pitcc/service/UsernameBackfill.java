package com.pitcc.service;

import com.pitcc.model.User;
import com.pitcc.repository.UserRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

@Component
public class UsernameBackfill implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(UsernameBackfill.class);
  private static final int ATTEMPTS = 3;

  private final UserRepository userRepository;
  private final UsernameGenerator usernameGenerator;

  public UsernameBackfill(UserRepository userRepository, UsernameGenerator usernameGenerator) {
    this.userRepository = userRepository;
    this.usernameGenerator = usernameGenerator;
  }

  @Override
  public void run(ApplicationArguments args) {
    List<User> pending = userRepository.findByUsernameIsNull();
    if (pending.isEmpty()) {
      return;
    }
    int assigned = 0;
    for (User user : pending) {
      if (assign(user)) {
        assigned++;
      }
    }
    log.info("Username gerado para {} de {} contas sem username.", assigned, pending.size());
  }

  private boolean assign(User user) {
    for (int attempt = 1; attempt <= ATTEMPTS; attempt++) {
      user.setUsername(usernameGenerator.generate(user.getName()));
      try {
        userRepository.save(user);
        return true;
      } catch (DuplicateKeyException exception) {
        log.debug("Username {} ocupado durante a geração; tentando outro.", user.getUsername());
      }
    }
    log.warn("Não foi possível gerar username para a conta {}.", user.getId());
    return false;
  }
}
