package com.studyroom.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.studyroom.common.BizException;
import com.studyroom.dto.ShopRequest;
import com.studyroom.mapper.DishMapper;
import com.studyroom.mapper.ShopMapper;
import com.studyroom.model.Shop;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShopServiceTest {
    @Mock private ShopMapper shopMapper;
    @Mock private DishMapper dishMapper;
    @Mock private CanteenService canteenService;
    private ShopService service;

    @BeforeEach
    void setUp() {
        service = new ShopService(shopMapper, dishMapper, canteenService);
    }

    @Test
    void pagesRowsAndRejectsInvalidPageArguments() {
        when(shopMapper.selectPage(0, 20)).thenReturn(List.of(shop(2L)));
        when(shopMapper.count()).thenReturn(1L);

        assertThat(service.page(1, 20).records()).hasSize(1);
        assertThatThrownBy(() -> service.page(1, 101)).isInstanceOf(BizException.class);
        assertThatThrownBy(() -> service.page(0, 20)).isInstanceOf(BizException.class);
    }

    @Test
    void createsAndUpdatesAfterValidatingParentCanteen() {
        when(shopMapper.insert(any(Shop.class))).thenAnswer(invocation -> {
            Shop saved = invocation.getArgument(0);
            saved.setId(2L);
            return 1;
        });
        when(shopMapper.selectById(2L)).thenReturn(shop(2L));

        assertThat(service.create(request(null, null)).shopName()).isEqualTo("面食档");
        assertThat(service.update(2L, request(5, 0)).status()).isZero();
        verify(canteenService, org.mockito.Mockito.times(2)).require(1L);
        verify(shopMapper).update(any(Shop.class));
    }

    @Test
    void preventsDeletingShopsThatStillContainDishes() {
        when(shopMapper.selectById(2L)).thenReturn(shop(2L));
        when(dishMapper.countByShopId(2L)).thenReturn(1L, 0L);

        assertThatThrownBy(() -> service.delete(2L)).isInstanceOf(BizException.class)
                .hasMessageContaining("仍有菜品");
        service.delete(2L);
        verify(shopMapper).deleteById(2L);
        verify(shopMapper, never()).deleteById(99L);
    }

    @Test
    void requireAndGetRejectMissingShop() {
        when(shopMapper.selectById(99L)).thenReturn(null);

        assertThatThrownBy(() -> service.require(99L)).isInstanceOf(BizException.class);
        assertThatThrownBy(() -> service.get(99L)).isInstanceOf(BizException.class);
    }

    private static ShopRequest request(Integer sortOrder, Integer status) {
        return new ShopRequest(1L, " 面食档 ", "一层", null, null, "面食",
                new BigDecimal("15.00"), null, null, null, status, sortOrder);
    }

    private static Shop shop(Long id) {
        Shop shop = new Shop();
        shop.setId(id);
        shop.setCanteenId(1L);
        shop.setShopName("示例档口");
        shop.setStatus(1);
        shop.setIsDeleted(0);
        return shop;
    }
}
