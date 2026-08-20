package cc.shturl.wa.demo.service.impl;

import cc.shturl.wa.demo.entity.Cards;
import cc.shturl.wa.demo.entity.User;
import cc.shturl.wa.demo.entity.UserCardPools;
import cc.shturl.wa.demo.mapper.CardsMapper;
import cc.shturl.wa.demo.mapper.UserCardPoolsMapper;
import cc.shturl.wa.demo.mapper.UserMapper;
import cc.shturl.wa.demo.service.CardCollectionService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CardCollectionServiceImpl implements CardCollectionService {
    private static final String FULL_COLLECTION_USERNAME = "ethan";

    private final CardsMapper cardsMapper;
    private final UserCardPoolsMapper userCardPoolsMapper;
    private final UserMapper userMapper;

    @Override
    public Set<Long> listPlayableCardIds(Long userId) {
        List<Cards> enabled = cardsMapper.selectList(Wrappers.<Cards>lambdaQuery()
                .eq(Cards::getStatus, 1));
        if (hasFullCollection(userId)) {
            return enabled.stream().map(Cards::getId).collect(Collectors.toCollection(HashSet::new));
        }
        Set<Long> owned = listOwnedCollectibleIds(userId);
        Set<Long> playable = new HashSet<>();
        for (Cards card : enabled) {
            if (isStarter(card) || owned.contains(card.getId())) {
                playable.add(card.getId());
            }
        }
        return playable;
    }

    @Override
    public boolean isUnlocked(Long userId, Cards card) {
        if (card == null) {
            return false;
        }
        if (isStarter(card) || hasFullCollection(userId)) {
            return true;
        }
        if (userId == null) {
            return false;
        }
        return userCardPoolsMapper.selectCount(Wrappers.<UserCardPools>lambdaQuery()
                .eq(UserCardPools::getUserId, userId)
                .eq(UserCardPools::getCardId, card.getId())
                .eq(UserCardPools::getUnlockedStatus, 1)) > 0;
    }

    @Override
    @Transactional
    public Cards unlockRandomCollectible(Long userId) {
        if (userId == null || userMapper.selectById(userId) == null || hasFullCollection(userId)) {
            return null;
        }
        List<Cards> locked = cardsMapper.selectList(Wrappers.<Cards>lambdaQuery()
                .eq(Cards::getStatus, 1)
                .eq(Cards::getRequireUnlock, 1)
                .orderByAsc(Cards::getId));
        Set<Long> owned = listOwnedCollectibleIds(userId);
        List<Cards> candidates = locked.stream().filter(card -> !owned.contains(card.getId())).collect(Collectors.toList());
        if (candidates.isEmpty()) {
            return null;
        }
        Cards picked = candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
        UserCardPools row = new UserCardPools();
        row.setUserId(userId);
        row.setCardId(picked.getId());
        row.setOwnedCount(1);
        row.setUnlockedStatus(1);
        row.setLevel(1);
        try {
            userCardPoolsMapper.insert(row);
        } catch (DuplicateKeyException ignored) {
            return picked;
        }
        return picked;
    }

    private boolean hasFullCollection(Long userId) {
        if (userId == null) {
            return false;
        }
        User user = userMapper.selectById(userId);
        return user != null && user.getUsername() != null
                && FULL_COLLECTION_USERNAME.equalsIgnoreCase(user.getUsername().trim());
    }

    private Set<Long> listOwnedCollectibleIds(Long userId) {
        if (userId == null) {
            return Set.of();
        }
        List<UserCardPools> rows = userCardPoolsMapper.selectList(Wrappers.<UserCardPools>lambdaQuery()
                .eq(UserCardPools::getUserId, userId)
                .eq(UserCardPools::getUnlockedStatus, 1));
        if (rows == null || rows.isEmpty()) {
            return Set.of();
        }
        return rows.stream().map(UserCardPools::getCardId).collect(Collectors.toCollection(HashSet::new));
    }

    private boolean isStarter(Cards card) {
        return card.getRequireUnlock() == null || card.getRequireUnlock() == 0;
    }
}
