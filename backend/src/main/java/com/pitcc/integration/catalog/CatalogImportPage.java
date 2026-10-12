package com.pitcc.integration.catalog;

import java.util.Objects;

/**
 * Resultado de uma busca paginada. 'fetched' conta os DTOs retornados pela API,
 * mesmo que alguns nao possam ser convertidos em Card.
 */
public record CatalogImportPage(ImportBatch batch, int fetched, boolean hasNext) {

    public CatalogImportPage {
        Objects.requireNonNull(batch, "batch");
        if (fetched < 0) {
            throw new IllegalArgumentException("fetched cannot be negative");
        }
    }
}
