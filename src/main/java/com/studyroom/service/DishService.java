package com.studyroom.service;

import com.studyroom.common.BizException;
import com.studyroom.common.PageResult;
import com.studyroom.dto.DishQuery;
import com.studyroom.dto.DishRequest;
import com.studyroom.dto.DishView;
import com.studyroom.mapper.DishMapper;
import com.studyroom.mapper.DishTagMapper;
import com.studyroom.model.Dish;
import com.studyroom.model.DishTag;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DishService {
    private final DishMapper dishMapper;
    private final ShopService shopService;
    private final CanteenService canteenService;
    private final TagService tagService;
    private final DishTagMapper dishTagMapper;

    public DishService(DishMapper dishMapper, ShopService shopService, CanteenService canteenService,
                       TagService tagService, DishTagMapper dishTagMapper) {
        this.dishMapper = dishMapper;
        this.shopService = shopService;
        this.canteenService = canteenService;
        this.tagService = tagService;
        this.dishTagMapper = dishTagMapper;
    }

    @Transactional(readOnly = true)
    public PageResult<DishView> search(DishQuery query) {
        if (query.getPage() < 1 || query.getSize() < 1 || query.getSize() > 100) {
            throw new BizException(400, "分页参数范围无效");
        }
        if (query.getMinPrice() != null && query.getMaxPrice() != null
                && query.getMinPrice().compareTo(query.getMaxPrice()) > 0) {
            throw new BizException(400, "最低价格不能高于最高价格");
        }
        long offset = (long) (query.getPage() - 1) * query.getSize();
        return new PageResult<>(dishMapper.search(query, offset, query.getSize()),
                dishMapper.countMatches(query), query.getPage(), query.getSize());
    }

    @Transactional(readOnly = true)
    public DishView get(Long id) {
        return view(require(id));
    }

    @Transactional
    public DishView create(DishRequest request) {
        shopService.require(request.shopId());
        Dish dish = new Dish();
        apply(dish, request);
        dishMapper.insert(dish);
        return view(dish);
    }

    @Transactional
    public DishView update(Long id, DishRequest request) {
        shopService.require(request.shopId());
        Dish dish = require(id);
        apply(dish, request);
        dishMapper.update(dish);
        return view(require(id));
    }

    @Transactional
    public void delete(Long id) {
        require(id);
        dishMapper.deleteById(id);
        dishTagMapper.deleteActiveByDishId(id);
    }

    @Transactional
    public List<Long> replaceTags(Long dishId, List<Long> requestedTagIds) {
        require(dishId);
        Set<Long> desired = new LinkedHashSet<>(requestedTagIds == null ? List.of() : requestedTagIds);
        desired.forEach(tagService::require);
        List<DishTag> existing = dishTagMapper.selectAllByDishId(dishId);
        Set<Long> existingTagIds = new LinkedHashSet<>();
        for (DishTag link : existing) {
            existingTagIds.add(link.getTagId());
            int deleted = desired.contains(link.getTagId()) ? 0 : 1;
            if (link.getIsDeleted() != deleted) {
                dishTagMapper.setDeleted(link.getId(), deleted);
            }
        }
        for (Long tagId : desired) {
            if (!existingTagIds.contains(tagId)) {
                DishTag link = new DishTag();
                link.setDishId(dishId);
                link.setTagId(tagId);
                dishTagMapper.insert(link);
            }
        }
        return new ArrayList<>(desired);
    }

    @Transactional(readOnly = true)
    public List<Long> tagIds(Long dishId) {
        require(dishId);
        return dishTagMapper.selectByDishId(dishId).stream().map(DishTag::getTagId).toList();
    }

    public Dish require(Long id) {
        Dish dish = id == null ? null : dishMapper.selectById(id);
        if (dish == null) {
            throw new BizException(404, "菜品不存在");
        }
        return dish;
    }

    private void apply(Dish dish, DishRequest request) {
        dish.setShopId(request.shopId());
        dish.setDishName(request.dishName().trim());
        dish.setPrice(request.price());
        dish.setCategory(request.category());
        dish.setMealType(request.mealType() == null ? 15 : request.mealType());
        dish.setSpiceLevel(request.spiceLevel() == null ? 0 : request.spiceLevel());
        dish.setCalorie(request.calorie());
        dish.setDescription(request.description());
        dish.setImageUrl(request.imageUrl());
        dish.setIsSignature(request.isSignature() == null ? 0 : request.isSignature());
        dish.setIsAvailable(request.isAvailable() == null ? 1 : request.isAvailable());
        dish.setRating(BigDecimal.ZERO);
        dish.setRatingCount(0);
        dish.setLikeCount(0);
        dish.setDislikeCount(0);
        dish.setFavoriteCount(0);
        dish.setRecommendCount(0);
    }

    private DishView view(Dish dish) {
        var shop = shopService.require(dish.getShopId());
        var canteen = canteenService.require(shop.getCanteenId());
        return new DishView(dish.getId(), dish.getShopId(), shop.getShopName(), canteen.getId(),
                canteen.getCanteenName(), dish.getDishName(), dish.getPrice(), dish.getCategory(),
                dish.getMealType(), dish.getSpiceLevel(), dish.getCalorie(), dish.getDescription(),
                dish.getImageUrl(), dish.getIsSignature(), dish.getIsAvailable(), dish.getRating());
    }
}
