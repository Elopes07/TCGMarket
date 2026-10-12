package com.pitcc.integration.catalog;

import com.pitcc.model.CardGame;
import java.util.List;
import java.util.Optional;

public interface CardCatalogProvider {

  CardGame getCardGame();

  List<ExternalCard> searchCards(String query);

  Optional<ExternalCard> findByExternalId(String externalId);
}