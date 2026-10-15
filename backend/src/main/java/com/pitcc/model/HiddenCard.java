package com.pitcc.model;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Carta ocultada do catálogo público por um administrador. Fica fora de {@code cards} porque a
 * importação substitui o documento inteiro da carta; a identidade é a mesma da importação.
 */
@Document(collection = "hidden_cards")
@CompoundIndex(
    name = "hidden_source_external_id_unique",
    def = "{'source': 1, 'externalId': 1}",
    unique = true)
public class HiddenCard {

  @Id private String id;

  private CardSource source;

  private String externalId;

  private CardGame game;

  private String reason;

  private String hiddenBy;

  private Instant hiddenAt;

  public HiddenCard() {}

  public String getId() {
    return id;
  }

  public CardSource getSource() {
    return source;
  }

  public void setSource(CardSource source) {
    this.source = source;
  }

  public String getExternalId() {
    return externalId;
  }

  public void setExternalId(String externalId) {
    this.externalId = externalId;
  }

  public CardGame getGame() {
    return game;
  }

  public void setGame(CardGame game) {
    this.game = game;
  }

  public String getReason() {
    return reason;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }

  public String getHiddenBy() {
    return hiddenBy;
  }

  public void setHiddenBy(String hiddenBy) {
    this.hiddenBy = hiddenBy;
  }

  public Instant getHiddenAt() {
    return hiddenAt;
  }

  public void setHiddenAt(Instant hiddenAt) {
    this.hiddenAt = hiddenAt;
  }
}
