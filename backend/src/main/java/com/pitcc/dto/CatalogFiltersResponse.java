package com.pitcc.dto;

import java.util.List;

/** Opções de filtro que existem no catálogo interno de um jogo. */
public record CatalogFiltersResponse(List<String> sets, List<String> rarities, List<String> types) {}
