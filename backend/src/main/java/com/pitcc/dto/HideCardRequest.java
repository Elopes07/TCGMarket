package com.pitcc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record HideCardRequest(
    @NotBlank(message = "O motivo é obrigatório.")
        @Size(min = 10, max = 300, message = "O motivo deve ter entre 10 e 300 caracteres.")
        String reason) {

  public HideCardRequest {
    reason = reason == null ? null : reason.strip();
  }
}
