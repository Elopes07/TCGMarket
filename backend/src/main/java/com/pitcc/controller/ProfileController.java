package com.pitcc.controller;

import com.pitcc.dto.ProfileResponse;
import com.pitcc.dto.PublicProfileResponse;
import com.pitcc.dto.UpdateProfileRequest;
import com.pitcc.exception.InvalidAuthenticationException;
import com.pitcc.security.AuthenticatedUser;
import com.pitcc.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class ProfileController {

  private final ProfileService profileService;

  public ProfileController(ProfileService profileService) {
    this.profileService = profileService;
  }

  @GetMapping("/me/profile")
  public ProfileResponse ownProfile(@AuthenticationPrincipal AuthenticatedUser current) {
    return profileService.ownProfile(requireId(current));
  }

  @PutMapping("/me/profile")
  public ProfileResponse updateOwnProfile(
      @AuthenticationPrincipal AuthenticatedUser current,
      @Valid @RequestBody UpdateProfileRequest request) {
    return profileService.updateOwnProfile(requireId(current), request);
  }

  @GetMapping("/{username}/profile")
  public PublicProfileResponse publicProfile(@PathVariable String username) {
    return profileService.publicProfile(username);
  }

  private static String requireId(AuthenticatedUser current) {
    if (current == null) {
      throw new InvalidAuthenticationException();
    }
    return current.id();
  }
}
