package com.pitcc.dto;

import com.pitcc.model.User;
import com.pitcc.model.UserRole;
import java.util.List;

public final class UserResponses {

  private UserResponses() {}

  public static UserResponse toUserResponse(User user) {
    return new UserResponse(
        user.getId(),
        user.getName(),
        user.getUsername(),
        user.getEmail(),
        user.getRole(),
        user.getRegistrationDate());
  }

  public static ProfileResponse toProfileResponse(User user) {
    return new ProfileResponse(
        user.getId(),
        user.getName(),
        user.getUsername(),
        user.getEmail(),
        user.getRole(),
        user.getRegistrationDate(),
        user.getBio(),
        user.getCity(),
        user.getState(),
        orEmpty(user.getFavoriteGames()),
        orEmpty(user.getLinks()));
  }

  public static PublicProfileResponse toPublicProfileResponse(User user) {
    return new PublicProfileResponse(
        user.getName(),
        user.getUsername(),
        user.getRole() == UserRole.ADMIN,
        user.getRegistrationDate(),
        user.getBio(),
        user.getCity(),
        user.getState(),
        orEmpty(user.getFavoriteGames()),
        orEmpty(user.getLinks()));
  }

  private static <T> List<T> orEmpty(List<T> values) {
    return values == null ? List.of() : List.copyOf(values);
  }
}
