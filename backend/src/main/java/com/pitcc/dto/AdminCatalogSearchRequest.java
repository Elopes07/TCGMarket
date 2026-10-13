package com.pitcc.dto;

/**
 * Pesquisa administrativa do catálogo. Todos os parâmetros são opcionais; sem {@code game}, a
 * pesquisa cobre os três TCGs. {@code visibility} aceita {@code all}, {@code visible} ou
 * {@code hidden}.
 */
public record AdminCatalogSearchRequest(
    String game,
    String q,
    String set,
    String rarity,
    String type,
    String visibility,
    Integer page,
    Integer size,
    String sort) {}
