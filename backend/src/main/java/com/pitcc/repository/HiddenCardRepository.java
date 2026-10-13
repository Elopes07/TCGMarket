package com.pitcc.repository;

import com.pitcc.model.CardSource;
import com.pitcc.model.HiddenCard;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface HiddenCardRepository extends MongoRepository<HiddenCard, String> {

  List<HiddenCard> findBySource(CardSource source);

  List<HiddenCard> findByExternalIdIn(Collection<String> externalIds);

  Optional<HiddenCard> findBySourceAndExternalId(CardSource source, String externalId);

  void deleteBySourceAndExternalId(CardSource source, String externalId);
}
