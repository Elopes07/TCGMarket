package com.pitcc.dto;

import com.pitcc.model.CardGame;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public record UpdateProfileRequest(
    @NotBlank(message = "O nome é obrigatório.")
        @Size(min = 2, max = 120, message = "O nome deve ter entre 2 e 120 caracteres.")
        String name,
    @NotBlank(message = "O nome de usuário é obrigatório.")
        @Pattern(
            regexp = "^[a-z0-9_.]{3,20}$",
            message =
                "O nome de usuário deve ter de 3 a 20 caracteres: letras minúsculas, números, \"_\" e \".\".")
        String username,
    @Size(max = 280, message = "A bio deve ter no máximo 280 caracteres.") String bio,
    @Size(max = 60, message = "A cidade deve ter no máximo 60 caracteres.") String city,
    @Pattern(
            regexp =
                "^(AC|AL|AP|AM|BA|CE|DF|ES|GO|MA|MT|MS|MG|PA|PB|PR|PE|PI|RJ|RN|RS|RO|RR|SC|SP|SE|TO)$",
            message = "Informe uma UF válida.")
        String state,
    @Size(max = 3, message = "Escolha no máximo 3 TCGs.") List<@NotNull CardGame> favoriteGames,
    @Size(max = 3, message = "Informe no máximo 3 links.")
        List<
                @NotBlank(message = "O link não pode ficar vazio.")
                @Size(max = 200, message = "Cada link deve ter no máximo 200 caracteres.")
                @Pattern(
                    regexp = "^https://[^\\s/?#]+\\.[^\\s/?#]+(?:[/?#]\\S*)?$",
                    message = "Informe um link completo começando com https://")
                String>
            links) {}
