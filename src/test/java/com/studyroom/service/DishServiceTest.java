package com.studyroom.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.studyroom.common.BizException;
import com.studyroom.dto.DishQuery;
import com.studyroom.mapper.DishMapper;
import com.studyroom.mapper.DishTagMapper;
import com.studyroom.model.DishTag;
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

    private DishService dishService;

    @BeforeEach
    void setUp() {
        dishService = new DishService(dishMapper, shopService, canteenService, tagService, dishTagMapper);
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
}
