package com.pitcc.integration.catalog.pokemon.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PokemonCardsResponse(
        List<PokemonCardDto> data,
        Integer page,
        Integer pageSize,
        Integer count,
        Integer totalCount
) {

    // Mantém compatibilidade com os testes que criam apenas a lista.
    public PokemonCardsResponse(List<PokemonCardDto> data) {
        this(data, null, null, null, null);
    }
}