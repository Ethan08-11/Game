package cc.shturl.wa.demo.service;

import cc.shturl.wa.demo.entity.Cards;

import java.util.Set;

public interface CardCollectionService {
    String LOCKED_IMAGE_URL = "/images/cards/Card_Locked.webp";

    Set<Long> listPlayableCardIds(Long userId);

    boolean isUnlocked(Long userId, Cards card);

    Cards unlockRandomCollectible(Long userId);
}
