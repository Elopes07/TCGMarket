package com.pitcc.dto;

/**
 * Parâmetros de pesquisa do catálogo. Todos são opcionais; {@code page} começa em 0.
 * {@code set} é o nome da coleção ({@code setName}) e {@code type} o tipo de carta do jogo.
 */
public record CatalogSearchRequest(
    String q,
    String set,
    String rarity,
    String type,
    Integer page,
    Integer size,
    String sort) {}
