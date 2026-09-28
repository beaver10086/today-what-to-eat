package com.studyroom.service;

import com.studyroom.common.BizException;
import com.studyroom.common.PageResult;
import com.studyroom.dto.ShopRequest;
import com.studyroom.dto.ShopView;
import com.studyroom.mapper.DishMapper;
import com.studyroom.mapper.ShopMapper;
import com.studyroom.model.Shop;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShopService {
    private final ShopMapper shopMapper;
    private final DishMapper dishMapper;
    private final CanteenService canteenService;

    public ShopService(ShopMapper shopMapper, DishMapper dishMapper, CanteenService canteenService) {
        this.shopMapper = shopMapper;
        this.dishMapper = dishMapper;
        this.canteenService = canteenService;
    }

    @Transactional(readOnly = true)
    public PageResult<ShopView> page(int page, int size) {
        validatePage(page, size);
        List<ShopView> rows = shopMapper.selectPage((long) (page - 1) * size, size)
                .stream().map(ShopService::view).toList();
        return new PageResult<>(rows, shopMapper.count(), page, size);
    }

    @Transactional(readOnly = true)
    public ShopView get(Long id) { return view(require(id)); }

    @Transactional
    public ShopView create(ShopRequest request) {
        canteenService.require(request.canteenId());
        Shop shop = new Shop();
        apply(shop, request);
        shopMapper.insert(shop);
        return view(shop);
    }

    @Transactional
    public ShopView update(Long id, ShopRequest request) {
        canteenService.require(request.canteenId());
        Shop shop = require(id);
        apply(shop, request);
        shopMapper.update(shop);
        return view(require(id));
    }

    @Transactional
    public void delete(Long id) {
        require(id);
        if (dishMapper.countByShopId(id) > 0) {
            throw new BizException(409, "档口下仍有菜品，不能删除");
        }
        shopMapper.deleteById(id);
    }

    public Shop require(Long id) {
        Shop shop = id == null ? null : shopMapper.selectById(id);
        if (shop == null) {
            throw new BizException(404, "档口不存在");
        }
        return shop;
    }

    private static void apply(Shop shop, ShopRequest request) {
        shop.setCanteenId(request.canteenId());
        shop.setShopName(request.shopName().trim());
        shop.setLocationDesc(request.locationDesc());
        shop.setOpenTime(request.openTime());
        shop.setCloseTime(request.closeTime());
        shop.setCuisine(request.cuisine());
        shop.setAvgPrice(request.avgPrice());
        shop.setCoverUrl(request.coverUrl());
        shop.setDescription(request.description());
        shop.setStatus(request.status() == null ? 1 : request.status());
        shop.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
    }

    private static ShopView view(Shop shop) {
        return new ShopView(shop.getId(), shop.getCanteenId(), shop.getShopName(), shop.getLocationDesc(),
                shop.getOpenTime(), shop.getCloseTime(), shop.getCuisine(), shop.getAvgPrice(),
                shop.getCoverUrl(), shop.getDescription(), shop.getStatus(), shop.getSortOrder());
    }

    private static void validatePage(int page, int size) {
        if (page < 1 || size < 1 || size > 100) throw new BizException(400, "分页参数范围无效");
    }
}
