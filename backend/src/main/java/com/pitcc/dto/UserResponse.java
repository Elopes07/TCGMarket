package com.pitcc.dto;

import com.pitcc.model.UserRole;
import java.time.Instant;

public record UserResponse(
    String id,
    String name,
    String username,
    String email,
    UserRole role,
    Instant registrationDate) {}
