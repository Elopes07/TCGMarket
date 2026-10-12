package com.pitcc.integration.catalog.mtg;

import com.pitcc.integration.catalog.CardCatalogProvider;
import com.pitcc.integration.catalog.CatalogImportPage;
import com.pitcc.integration.catalog.ExternalCard;
import com.pitcc.integration.catalog.ImportBatch;
import com.pitcc.integration.catalog.PagedCatalogImportProvider;
import com.pitcc.integration.catalog.mtg.dto.MtgCardDto;
import com.pitcc.model.CardGame;
import com.pitcc.model.CardSource;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class MtgProvider implements CardCatalogProvider, PagedCatalogImportProvider {

  private static final Logger log = LoggerFactory.getLogger(MtgProvider.class);
  static final int SEARCH_LIMIT = 20;
  private final MtgClient client;

  public MtgProvider(MtgClient client) {
    this.client = client;
  }

  @Override
  public CardGame getCardGame() {
    return CardGame.MAGIC_THE_GATHERING;
  }

  @Override
  public CardSource getSource() {
    return CardSource.SCRYFALL;
  }

  @Override
  public ImportBatch fetchForImport(String query) {
    return map(client.search(query));
  }

  // Necessario por compatibilidade com CatalogImportProvider.
  // O importAll() usa fetchPageForImport quando o provider e paginado.
  @Override
  public ImportBatch fetchAllForImport() {
    throw new UnsupportedOperationException(
            "Importacao Magic completa exige processamento incremental por Bulk Data");
  }

  @Override
  public CatalogImportPage fetchPageForImport(int page) {
    MtgClient.BulkPage bulk = client.fetchBulkPage(page);
    return new CatalogImportPage(map(bulk.cards()), bulk.cards().size(), bulk.hasNext());
  }

  private ImportBatch map(List<MtgCardDto> cards) {
    return ImportBatch.map(cards, getSource(),
            dto -> dto.id(),
            dto -> MtgCardMapper.toMagicCard(dto).stream().toList());
  }

  @Override
  public List<ExternalCard> searchCards(String query) {
    return client.search(query).stream()
            .map(this::toCard)
            .flatMap(Optional::stream)
            .limit(SEARCH_LIMIT)
            .toList();
  }

  @Override
  public Optional<ExternalCard> findByExternalId(String externalId) {
    return client.findById(externalId).flatMap(this::toCard);
  }

  private Optional<ExternalCard> toCard(MtgCardDto dto) {
    Optional<ExternalCard> card = MtgCardMapper.toExternalCard(dto);
    if (card.isEmpty()) {
      log.warn("Ignorando carta do catalogo Magic sem id ou nome");
    }
    return card;
  }
}
