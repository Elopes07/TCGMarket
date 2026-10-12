package com.pitcc.dto;

import com.pitcc.model.CardGame;
import com.pitcc.model.UserRole;
import java.time.Instant;
import java.util.List;

public record ProfileResponse(
    String id,
    String name,
    String username,
    String email,
    UserRole role,
    Instant registrationDate,
    String bio,
    String city,
    String state,
    List<CardGame> favoriteGames,
    List<String> links) {}
