package com.pitcc.integration.catalog;

import com.pitcc.model.CardGame;
import com.pitcc.model.CardSource;

public interface CatalogImportProvider {

  CardGame getCardGame();

  CardSource getSource();

  ImportBatch fetchForImport(String query);

  default ImportBatch fetchAllForImport() {
    throw new UnsupportedOperationException(
            "Importação completa ainda não implementada para este provedor."
    );
  }
}