package com.pitcc.model;

import com.pitcc.util.TextNormalizer;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;


@Document(collection = "cards")
@CompoundIndexes({
        @CompoundIndex(
                name = "source_external_id_unique",
                def = "{'source': 1, 'externalId': 1}",
                unique = true
        ),
        // Outras impressões da mesma carta na tela de detalhes.
        @CompoundIndex(
                name = "game_conceptual_id",
                def = "{'game': 1, 'conceptualId': 1}"
        )
})
public abstract class Card {

    @Id
    private String id;

    private String name;
    private CardGame game;
    private String edition;
    private String codeCollection;
    private String cardNumber;
    private String officialRarity;
    private String imageUrl;
    private String externalId;
    private String conceptualId;
    private CardSource source;

    protected Card(){
    }

    protected Card(String name, CardGame game, String edition,
                   String codeCollection, String cardNumber, String officialRarity,
                   String imageUrl, String externalId, CardSource source) {

        // =======================  VALIDAÇÕES  ========================

        //validação name
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("O Nome da carta é um atributo obrigatório.");
        }

        //validação game
        if (game == null) {
            throw new IllegalArgumentException("O jogo da carta é obrigatório.");
        }

        //validação source
        if (source == null) {
            throw new IllegalArgumentException("A origem da carta é obrigatória.");
        }

        //validação externalId
        if (externalId == null || externalId.isBlank()) {
            throw new IllegalArgumentException("O identificador externo da carta é obrigatório.");
        }

        //validação edition
        if (edition == null || edition.isBlank()) {
            throw new IllegalArgumentException("A edição/set da carta é um atributo obrigatório.");
        }

        //validação codeCollection
        if (codeCollection == null || codeCollection.isBlank()) {
            throw new IllegalArgumentException("O código da coleção da carta é obrigatório.");
        }


        // =============================================================

        this.name = name.strip();
        this.game = game;
        this.edition = edition.strip();
        this.codeCollection = codeCollection.strip();
        this.cardNumber = TextNormalizer.normalizeOptionalText(cardNumber);
        this.officialRarity = TextNormalizer.normalizeOptionalText(officialRarity);
        this.imageUrl = TextNormalizer.normalizeOptionalText(imageUrl);
        this.externalId = externalId.strip();
        this.source = source;
    }


    public String getConceptualId() {
        return conceptualId;
    }

    public void setConceptualId(String conceptualId) {
        this.conceptualId = TextNormalizer.normalizeOptionalText(conceptualId);
    }

    //getters
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public CardGame getGame() {

        return game;
    }

    public String getEdition() {
        return edition;
    }

    public String getCodeCollection() {
        return codeCollection;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public String getOfficialRarity() {
        return officialRarity;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getExternalId() {
        return externalId;
    }

    public CardSource getSource() {
        return source;
    }


}
