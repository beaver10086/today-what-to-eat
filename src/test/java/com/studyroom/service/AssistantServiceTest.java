package com.studyroom.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.studyroom.ai.OpenAiCompatibleClient;
import com.studyroom.common.PageResult;
import com.studyroom.dto.AssistantAnswer;
import com.studyroom.dto.CanteenView;
import com.studyroom.dto.DiscoveryResult;
import com.studyroom.dto.DishQuery;
import com.studyroom.dto.DishView;
import com.studyroom.dto.ShopView;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for the rule-based and model-assisted natural language search flow. */
@ExtendWith(MockitoExtension.class)
class AssistantServiceTest {
    @Mock
    private DishService dishService;
    @Mock
    private CanteenService canteenService;
    @Mock
    private ShopService shopService;
    @Mock
    private OpenAiCompatibleClient model;

    private AssistantService service;

    @BeforeEach
    void setUp() {
        service = new AssistantService(dishService, canteenService, shopService, model,
                new com.fasterxml.jackson.databind.ObjectMapper());
    }

    @Test
    void rulesOnlyParseExtractsBudgetSpiceMealAndNearbyWithoutModel() {
        stubDirectory();
        when(model.enabled()).thenReturn(false);
        when(dishService.search(any(DishQuery.class))).thenReturn(new PageResult<>(List.of(), 0, 1, 12));

        DiscoveryResult result = service.search("第一食堂 不辣的午餐 10块钱以内 附近的推荐", 2);

        assertEquals("已按价格和辣度筛选；当前食堂资料没有宿舍坐标，暂不能按实际步行距离排序。",
                result.notice());
        ArgumentCaptor<DishQuery> captor = ArgumentCaptor.forClass(DishQuery.class);
        verify(dishService).search(captor.capture());
        DishQuery query = captor.getValue();
        assertEquals(2, query.getPage());
        assertEquals(new BigDecimal("10"), query.getMaxPrice());
        assertEquals(0, query.getSpiceLevel());
        assertEquals(2, query.getMealType());
        assertEquals(1L, query.getCanteenId());
    }

    @Test
    void rulesOnlyParseExtractsCategoryShopAndKeyword() {
        stubDirectory();
        when(model.enabled()).thenReturn(false);
        when(dishService.search(any(DishQuery.class))).thenReturn(new PageResult<>(List.of(), 0, 1, 12));

        service.search("第二门店 有什么小吃 帮我看看");

        ArgumentCaptor<DishQuery> captor = ArgumentCaptor.forClass(DishQuery.class);
        verify(dishService).search(captor.capture());
        DishQuery query = captor.getValue();
        assertEquals(5, query.getCategory());
        assertEquals(2L, query.getShopId());
        assertEquals("小吃", query.getKeyword());
    }

    @Test
    void askFallsBackToCatalogSummaryWhenModelUnavailable() {
        stubDirectory();
        when(model.enabled()).thenReturn(true);
        when(model.complete(anyString(), anyString(), anyBoolean())).thenReturn("");
        DishView dish = dish(9L, "红烧牛肉饭", "第一食堂", "大众快餐");
        when(dishService.search(any(DishQuery.class)))
                .thenReturn(new PageResult<>(List.of(dish), 1, 1, 12));

        AssistantAnswer answer = service.ask("第一食堂 推荐个午餐");

        assertTrue(answer.answer().contains("红烧牛肉饭"));
        assertEquals(1, answer.dishes().size());
        verify(model, times(2)).complete(anyString(), anyString(), anyBoolean());
    }

    @Test
    void askExplainsWhenNothingMatches() {
        stubDirectory();
        when(model.enabled()).thenReturn(true);
        when(model.complete(anyString(), anyString(), anyBoolean())).thenReturn(null);
        when(dishService.search(any(DishQuery.class))).thenReturn(new PageResult<>(List.of(), 0, 1, 12));

        AssistantAnswer answer = service.ask("随便看看");

        assertEquals("我暂时没有在菜品库里找到符合条件的条目。可以换个菜名、食堂名或放宽条件再问。",
                answer.answer());
    }

    @Test
    void modelIntentIsMergedWithRuleBasedExtraction() {
        stubDirectory();
        when(model.enabled()).thenReturn(true);
        when(model.complete(anyString(), anyString(), anyBoolean())).thenReturn(
                "{\"budgetMax\":15,\"maxSpiceLevel\":2,\"mealType\":4,\"category\":2,"
                        + "\"canteenKeyword\":\"第一食堂\",\"shopKeyword\":\"第二门店\","
                        + "\"keyword\":\"牛肉\",\"nearby\":true}");
        when(dishService.search(any(DishQuery.class))).thenReturn(new PageResult<>(List.of(), 0, 1, 12));

        service.search("第一食堂 微微辣的牛肉 帮我看看");

        ArgumentCaptor<DishQuery> captor = ArgumentCaptor.forClass(DishQuery.class);
        verify(dishService).search(captor.capture());
        DishQuery query = captor.getValue();
        assertEquals(new BigDecimal("15"), query.getMaxPrice());
        assertEquals(2, query.getSpiceLevel());
        assertEquals(4, query.getMealType());
        assertEquals(2, query.getCategory());
        assertEquals(1L, query.getCanteenId());
        assertEquals(2L, query.getShopId());
        assertEquals("牛肉", query.getKeyword());
    }

    @Test
    void modelIntentFallsBackToRulesWhenPayloadIsNotUsable() {
        stubDirectory();
        when(model.enabled()).thenReturn(true);
        when(model.complete(anyString(), anyString(), anyBoolean())).thenReturn("这不是JSON");
        when(dishService.search(any(DishQuery.class))).thenReturn(new PageResult<>(List.of(), 0, 1, 12));

        DiscoveryResult result = service.search("第一食堂 不辣 5块钱");

        assertNotNull(result.conditions());
        assertEquals(new BigDecimal("5"), result.conditions().budgetMax());
        assertEquals(0, result.conditions().maxSpiceLevel());
    }

    @Test
    void modelIntentIgnoresOutOfRangeValuesAndUnknownPlaces() {
        stubDirectory();
        when(model.enabled()).thenReturn(true);
        when(model.complete(anyString(), anyString(), anyBoolean())).thenReturn(
                "{\"budgetMax\":900,\"maxSpiceLevel\":9,\"mealType\":3,\"category\":99,"
                        + "\"canteenKeyword\":\"不存在的食堂\",\"shopKeyword\":\"不存在的门店\","
                        + "\"keyword\":\"鸡\",\"nearby\":false}");
        when(dishService.search(any(DishQuery.class))).thenReturn(new PageResult<>(List.of(), 0, 1, 12));

        service.search("鸡 帮我看看");

        ArgumentCaptor<DishQuery> captor = ArgumentCaptor.forClass(DishQuery.class);
        verify(dishService).search(captor.capture());
        DishQuery query = captor.getValue();
        assertNull(query.getMaxPrice());
        assertNull(query.getSpiceLevel());
        assertNull(query.getMealType());
        assertNull(query.getCategory());
        assertEquals("鸡", query.getKeyword());
    }

    @Test
    void defaultPageStartsAtOneWhenUnspecified() {
        stubDirectory();
        when(model.enabled()).thenReturn(false);
        when(dishService.search(any(DishQuery.class))).thenReturn(new PageResult<>(List.of(), 0, 1, 12));

        service.search("米饭");

        ArgumentCaptor<DishQuery> captor = ArgumentCaptor.forClass(DishQuery.class);
        verify(dishService).search(captor.capture());
        assertEquals(1, captor.getValue().getPage());
    }

    private void stubDirectory() {
        lenient().when(canteenService.page(1, 100)).thenReturn(new PageResult<>(List.of(
                new CanteenView(1L, "第一食堂", "主校区", "教工路1号", null, null, "老牌食堂",
                        null, 1, 1)), 1, 1, 10));
        lenient().when(shopService.page(1, 100)).thenReturn(new PageResult<>(List.of(
                new ShopView(2L, 1L, "第二门店", "一楼", null, null, "中式快餐",
                        new BigDecimal("12"), null, "实惠", 1, 1, 3)), 1, 1, 10));
    }

    private static DishView dish(Long id, String name, String canteenName, String shopName) {
        return new DishView(id, 2L, shopName, 1L, canteenName, name, new BigDecimal("12.5"),
                1, 2, 0, 500, "招牌", null, 1, 1, new BigDecimal("4.5"), 2, "seed");
    }

}
