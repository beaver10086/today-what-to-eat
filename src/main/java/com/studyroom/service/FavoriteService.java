package com.studyroom.service;

import com.studyroom.common.BizException;
import com.studyroom.common.PageResult;
import com.studyroom.dto.FavoriteRequest;
import com.studyroom.dto.FavoriteView;
import com.studyroom.mapper.FavoriteMapper;
import com.studyroom.model.Favorite;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FavoriteService {
    private final FavoriteMapper favoriteMapper;
    private final DishService dishService;
    private final ShopService shopService;

    public FavoriteService(FavoriteMapper favoriteMapper, DishService dishService, ShopService shopService) {
        this.favoriteMapper = favoriteMapper;
        this.dishService = dishService;
        this.shopService = shopService;
    }

    @Transactional
    public void add(Long userId, FavoriteRequest request) {
        requireTarget(request.targetType(), request.targetId());
        Favorite existing = favoriteMapper.selectByUserAndTarget(userId,
                request.targetType(), request.targetId());
        if (existing == null) {
            Favorite favorite = new Favorite();
            favorite.setUserId(userId);
            favorite.setTargetType(request.targetType());
            favorite.setTargetId(request.targetId());
            favoriteMapper.insert(favorite);
        } else if (existing.getIsDeleted() != null && existing.getIsDeleted() == 1) {
            favoriteMapper.setDeleted(existing.getId(), 0);
        }
    }

    @Transactional
    public void remove(Long userId, int targetType, Long targetId) {
        validateTargetType(targetType);
        Favorite favorite = favoriteMapper.selectByUserAndTarget(userId, targetType, targetId);
        if (favorite != null && (favorite.getIsDeleted() == null || favorite.getIsDeleted() == 0)) {
            favoriteMapper.setDeleted(favorite.getId(), 1);
        }
    }

    @Transactional(readOnly = true)
    public PageResult<FavoriteView> page(Long userId, Integer targetType, int page, int size) {
        validateTargetType(targetType);
        if (page < 1 || size < 1 || size > 100) {
            throw new BizException(400, "分页参数范围无效");
        }
        long offset = (long) (page - 1) * size;
        return new PageResult<>(favoriteMapper.selectFavoriteViews(userId, targetType, offset, size),
                favoriteMapper.countByUser(userId, targetType), page, size);
    }

    @Transactional(readOnly = true)
    public List<Long> ids(Long userId, int targetType) {
        validateTargetType(targetType);
        return favoriteMapper.selectTargetIds(userId, targetType);
    }

    private void requireTarget(int targetType, Long targetId) {
        validateTargetType(targetType);
        if (targetId == null || targetId < 1) {
            throw new BizException(400, "收藏目标无效");
        }
        if (targetType == 1) {
            dishService.require(targetId);
        } else {
            shopService.require(targetId);
        }
    }

    private void validateTargetType(Integer targetType) {
        if (targetType != null && targetType != 1 && targetType != 2) {
            throw new BizException(400, "收藏类型仅支持菜品或档口");
        }
    }
}
