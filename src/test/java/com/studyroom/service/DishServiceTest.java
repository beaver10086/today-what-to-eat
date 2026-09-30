package com.studyroom.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.studyroom.common.BizException;
import com.studyroom.dto.DishQuery;
import com.studyroom.mapper.DishMapper;
import com.studyroom.mapper.DishTagMapper;
import com.studyroom.mapper.OperationLogMapper;
import com.studyroom.model.DishTag;
import com.studyroom.model.Dish;
import com.studyroom.model.Shop;
import com.studyroom.model.Canteen;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DishServiceTest {
    @Mock private DishMapper dishMapper;
    @Mock private ShopService shopService;
    @Mock private CanteenService canteenService;
    @Mock private TagService tagService;
    @Mock private DishTagMapper dishTagMapper;
    @Mock private OperationLogMapper operationLogMapper;

    private DishService dishService;

    @BeforeEach
    void setUp() {
        dishService = new DishService(dishMapper, shopService, canteenService, tagService,
                dishTagMapper, operationLogMapper);
    }

    @Test
    void priceRangeWithNoMatchesReturnsAnEmptyPage() {
        DishQuery query = new DishQuery();
        query.setMinPrice(new java.math.BigDecimal("100.00"));
        query.setMaxPrice(new java.math.BigDecimal("120.00"));
        when(dishMapper.search(eq(query), eq(0L), eq(20))).thenReturn(List.of());
        when(dishMapper.countMatches(query)).thenReturn(0L);

        var result = dishService.search(query);

        assertThat(result.records()).isEmpty();
        assertThat(result.total()).isZero();
    }

    @Test
    void invertedPriceRangeIsRejectedBeforeMapperAccess() {
        DishQuery query = new DishQuery();
        query.setMinPrice(new java.math.BigDecimal("30.00"));
        query.setMaxPrice(new java.math.BigDecimal("20.00"));

        assertThatThrownBy(() -> dishService.search(query))
                .isInstanceOf(BizException.class)
                .hasMessage("最低价格不能高于最高价格");
        verify(dishMapper, never()).search(any(), eq(0L), eq(20));
    }

    @Test
    void randomDishRespectsFiltersAndSelectsOneResult() {
        DishQuery query = new DishQuery();
        query.setCanteenId(3L);
        query.setCategory(2);
        var expected = new com.studyroom.dto.DishView(91L, 4L, "档口", 3L, "食堂", "随机菜",
                new java.math.BigDecimal("12.00"), 2, 15, 0, 400, "菜品介绍", null, 0, 1,
                new java.math.BigDecimal("4.5"), null, null);
        when(dishMapper.countMatches(query)).thenReturn(3L);
        when(dishMapper.search(eq(query), any(Long.class), eq(1))).thenReturn(List.of(expected));

        var selected = dishService.random(query);

        assertThat(selected).isEqualTo(expected);
        assertThat(query.getSize()).isEqualTo(1);
        verify(dishMapper, times(2)).search(eq(query), any(Long.class), eq(1));
    }

    @Test
    void randomDishReportsWhenFiltersMatchNothing() {
        DishQuery query = new DishQuery();
        when(dishMapper.countMatches(query)).thenReturn(0L);
        when(dishMapper.search(eq(query), eq(0L), eq(1))).thenReturn(List.of());

        assertThatThrownBy(() -> dishService.random(query))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("没有可推荐");
    }

    @Test
    void replacingTagsRestoresOldLinkAndInsertsOnlyNewTag() {
        DishTag deletedLink = new DishTag();
        deletedLink.setId(71L);
        deletedLink.setDishId(9L);
        deletedLink.setTagId(4L);
        deletedLink.setIsDeleted(1);
        when(dishMapper.selectById(9L)).thenReturn(new com.studyroom.model.Dish());
        when(dishTagMapper.selectAllByDishId(9L)).thenReturn(List.of(deletedLink));
        when(tagService.require(4L)).thenReturn(new com.studyroom.model.Tag());
        when(tagService.require(5L)).thenReturn(new com.studyroom.model.Tag());

        List<Long> result = dishService.replaceTags(9L, List.of(4L, 5L, 5L));

        assertThat(result).containsExactly(4L, 5L);
        verify(dishTagMapper).setDeleted(71L, 0);
        ArgumentCaptor<DishTag> inserted = ArgumentCaptor.forClass(DishTag.class);
        verify(dishTagMapper).insert(inserted.capture());
        assertThat(inserted.getValue().getDishId()).isEqualTo(9L);
        assertThat(inserted.getValue().getTagId()).isEqualTo(5L);
    }

    @Test
    void csvImportValidatesWholeFileBeforeWriting() {
        String header = "shopId,dishName,price,category,mealType,spiceLevel,"
                + "takeoutSuitability,dataSource,calorie,description,imageUrl,isAvailable\n";
        String rows = "4,番茄面,12.50,1,2,0,2,实地核对,300,, ,1\n"
                + "4,错误价格,-1,1,2,0,2,实地核对,300,,,1\n";
        when(dishMapper.findActiveIdByShopAndName(4L, "番茄面")).thenReturn(null);

        assertThatThrownBy(() -> dishService.importCsv((header + rows)
                .getBytes(StandardCharsets.UTF_8), 7L, "127.0.0.1"))
                .isInstanceOf(BizException.class).hasMessageContaining("第 3 行");
        verify(dishMapper, never()).insert(any());
    }

    @Test
    void csvImportAcceptsQuotedComma() {
        String header = "shopId,dishName,price,category,mealType,spiceLevel,"
                + "takeoutSuitability,dataSource,calorie,description,imageUrl,isAvailable\n";
        String row = "4,番茄面,12.50,1,2,0,2,\"实地,核对\",300,,,1\n";
        Shop shop = new Shop();
        shop.setCanteenId(3L);
        shop.setShopName("面食档");
        Canteen canteen = new Canteen();
        canteen.setId(3L);
        canteen.setCanteenName("东区食堂");
        when(shopService.require(4L)).thenReturn(shop);
        when(dishMapper.findActiveIdByShopAndName(4L, "番茄面")).thenReturn(null);
        when(canteenService.require(3L)).thenReturn(canteen);
        when(dishMapper.insert(any(Dish.class))).thenAnswer(invocation -> {
            ((Dish) invocation.getArgument(0)).setId(91L);
            return 1;
        });

        assertThat(dishService.importCsv((header + row).getBytes(StandardCharsets.UTF_8),
                7L, "127.0.0.1").imported()).isEqualTo(1);
        ArgumentCaptor<Dish> inserted = ArgumentCaptor.forClass(Dish.class);
        verify(dishMapper).insert(inserted.capture());
        assertThat(inserted.getValue().getDataSource()).isEqualTo("实地,核对");
        assertThat(inserted.getValue().getTakeoutSuitability()).isEqualTo(2);
        verify(operationLogMapper).insert(any());
    }

    @Test
    void csvImportRejectsExistingDishWithoutWriting() {
        String csv = "shopId,dishName,price,category,mealType,spiceLevel,"
                + "takeoutSuitability,dataSource,calorie,description,imageUrl,isAvailable\n"
                + "4,番茄面,12.50,1,2,0,2,实地核对,300,,,1\n";
        when(shopService.require(4L)).thenReturn(new Shop());
        when(dishMapper.findActiveIdByShopAndName(4L, "番茄面")).thenReturn(100L);

        assertThatThrownBy(() -> dishService.importCsv(csv.getBytes(StandardCharsets.UTF_8),
                7L, "127.0.0.1")).isInstanceOf(BizException.class)
                .hasMessageContaining("菜品已存在");
        verify(dishMapper, never()).insert(any());
    }

    @Test
    void csvImportRejectsDuplicateNamesInFileWithoutWriting() {
        String csv = "shopId,dishName,price,category,mealType,spiceLevel,"
                + "takeoutSuitability,dataSource,calorie,description,imageUrl,isAvailable\n"
                + "4,Tomato Rice,12.50,1,2,0,2,checked,300,,,1\n"
                + "4,tomato rice,12.50,1,2,0,2,checked,300,,,1\n";
        when(shopService.require(4L)).thenReturn(new Shop());
        when(dishMapper.findActiveIdByShopAndName(4L, "Tomato Rice")).thenReturn(null);

        assertThatThrownBy(() -> dishService.importCsv(csv.getBytes(StandardCharsets.UTF_8),
                7L, "127.0.0.1")).isInstanceOf(BizException.class)
                .hasMessageContaining("第 3 行菜品已存在");
        verify(dishMapper, never()).insert(any());
    }
}
