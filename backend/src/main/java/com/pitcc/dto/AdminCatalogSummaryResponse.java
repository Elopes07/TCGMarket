package com.pitcc.dto;

import com.pitcc.model.CardGame;
import java.util.List;

/** Totais do catálogo para a visão geral; {@code games} sempre traz os três TCGs, na ordem do enum. */
public record AdminCatalogSummaryResponse(
    long totalCards, long hiddenCards, List<GameSummary> games) {

  public record GameSummary(CardGame game, long totalCards, long hiddenCards) {}
}
