package com.pitcc.dto;

import com.pitcc.model.CardGame;
import java.time.Instant;
import java.util.List;

public record PublicProfileResponse(
    String name,
    String username,
    boolean administrator,
    Instant registrationDate,
    String bio,
    String city,
    String state,
    List<CardGame> favoriteGames,
    List<String> links) {}
