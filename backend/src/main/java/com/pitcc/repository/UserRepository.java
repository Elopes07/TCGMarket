package com.pitcc.repository;

import com.pitcc.model.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserRepository extends MongoRepository<User, String> {

  Optional<User> findByEmail(String email);

  boolean existsByEmail(String email);

  Optional<User> findByUsername(String username);

  boolean existsByUsername(String username);

  List<User> findByUsernameIsNull();
}
