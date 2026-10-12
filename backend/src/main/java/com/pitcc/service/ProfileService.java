package com.pitcc.service;

import com.pitcc.dto.ProfileResponse;
import com.pitcc.dto.PublicProfileResponse;
import com.pitcc.dto.UpdateProfileRequest;
import com.pitcc.dto.UserResponses;
import com.pitcc.exception.InvalidAuthenticationException;
import com.pitcc.exception.UserNotFoundException;
import com.pitcc.exception.UsernameAlreadyTakenException;
import com.pitcc.model.CardGame;
import com.pitcc.model.User;
import com.pitcc.repository.UserRepository;
import java.util.List;
import java.util.Locale;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

@Service
public class ProfileService {

  private final UserRepository userRepository;

  public ProfileService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  public ProfileResponse ownProfile(String userId) {
    return UserResponses.toProfileResponse(currentUser(userId));
  }

  /** Atualiza apenas os campos do perfil; e-mail, senha e papel nunca passam por aqui. */
  public ProfileResponse updateOwnProfile(String userId, UpdateProfileRequest request) {
    User user = currentUser(userId);
    String username = request.username().trim().toLowerCase(Locale.ROOT);
    if (!username.equals(user.getUsername()) && userRepository.existsByUsername(username)) {
      throw new UsernameAlreadyTakenException();
    }

    user.setName(request.name().trim());
    user.setUsername(username);
    user.setBio(blankToNull(request.bio()));
    user.setCity(blankToNull(request.city()));
    user.setState(blankToNull(request.state()));
    user.setFavoriteGames(favoriteGames(request.favoriteGames()));
    user.setLinks(links(request.links()));

    try {
      return UserResponses.toProfileResponse(userRepository.save(user));
    } catch (DuplicateKeyException exception) {
      throw new UsernameAlreadyTakenException();
    }
  }

  public PublicProfileResponse publicProfile(String username) {
    return userRepository
        .findByUsername(username.trim().toLowerCase(Locale.ROOT))
        .map(UserResponses::toPublicProfileResponse)
        .orElseThrow(UserNotFoundException::new);
  }

  private User currentUser(String userId) {
    return userRepository.findById(userId).orElseThrow(InvalidAuthenticationException::new);
  }

  private static String blankToNull(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  private static List<CardGame> favoriteGames(List<CardGame> games) {
    return games == null ? List.of() : games.stream().distinct().toList();
  }

  private static List<String> links(List<String> links) {
    return links == null ? List.of() : links.stream().map(String::trim).distinct().toList();
  }
}
