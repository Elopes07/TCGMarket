package com.pitcc.dto;

import com.pitcc.model.Card;
import com.pitcc.model.CardGame;
import com.pitcc.model.CardSource;
import com.pitcc.model.HiddenCard;
import java.time.Instant;

public record AdminCatalogCardResponse(
    String id,
    String externalId,
    CardSource source,
    CardGame cardGame,
    String name,
    String setName,
    String setCode,
    String cardNumber,
    String rarity,
    String imageUrl,
    boolean hidden,
    String hiddenReason,
    Instant hiddenAt) {

  public static AdminCatalogCardResponse from(Card card, HiddenCard hidden) {
    return new AdminCatalogCardResponse(card.getId(), card.getExternalId(), card.getSource(),
        card.getGame(), card.getName(), card.getEdition(), card.getCodeCollection(),
        card.getCardNumber(), card.getOfficialRarity(), card.getImageUrl(), hidden != null,
        hidden == null ? null : hidden.getReason(), hidden == null ? null : hidden.getHiddenAt());
  }
}
