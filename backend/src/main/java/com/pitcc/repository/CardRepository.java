
package com.pitcc.repository;

import com.pitcc.model.Card;
import com.pitcc.model.CardGame;
import com.pitcc.model.CardSource;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface CardRepository extends MongoRepository<Card, String> {

    long countBySourceAndExternalId(CardSource source, String externalId);

    Optional<Card> findBySourceAndExternalId(
            CardSource source,
            String externalId
    );

    boolean existsBySourceAndExternalId(
            CardSource source,
            String externalId
    );

    List<Card> findTop20ByGameAndConceptualIdAndIdNotOrderByEditionAsc(
            CardGame game,
            String conceptualId,
            String id
    );
}
