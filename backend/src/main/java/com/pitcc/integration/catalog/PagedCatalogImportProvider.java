package com.pitcc.integration.catalog;

/**
 * Implementacao opcional para provedores que oferecem importacao incremental.
 * Outros provedores continuam usando CatalogImportProvider sem mudancas.
 */
public interface PagedCatalogImportProvider extends CatalogImportProvider {

    CatalogImportPage fetchPageForImport(int page);
}
