package com.studyroom.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.studyroom.dto.DishTagWeight;
import com.studyroom.dto.DishView;
import com.studyroom.dto.RecommendationRequest;
import com.studyroom.dto.RecommendationView;
import com.studyroom.mapper.DishTagMapper;
import com.studyroom.mapper.FeedbackRecordMapper;
import com.studyroom.mapper.PreferenceMapper;
import com.studyroom.mapper.RecommendationItemMapper;
import com.studyroom.mapper.RecommendationMapper;
import com.studyroom.mapper.UserTasteProfileMapper;
import com.studyroom.model.Preference;
import com.studyroom.model.Recommendation;
import com.studyroom.model.UserTasteProfile;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {
    @Mock
    private RecommendationMapper recommendationMapper;
    @Mock
    private RecommendationItemMapper itemMapper;
    @Mock
    private FeedbackRecordMapper feedbackMapper;
    @Mock
    private PreferenceMapper preferenceMapper;
    @Mock
    private UserTasteProfileMapper profileMapper;
    @Mock
    private DishTagMapper dishTagMapper;

    private RecommendationService service;

    @BeforeEach
    void setUp() {
        service = new RecommendationService(recommendationMapper, itemMapper, feedbackMapper,
                preferenceMapper, profileMapper, dishTagMapper);
        Preference preference = new Preference();
        preference.setUserId(7L);
        preference.setMaxSpiceLevel(2);
        preference.setBudgetMin(new BigDecimal("5.00"));
        preference.setBudgetMax(new BigDecimal("30.00"));
        preference.setDietType(0);
        preference.setDislikeTagIds("[]");
        when(preferenceMapper.selectByUserId(7L)).thenReturn(preference);
        when(profileMapper.selectByUserId(7L)).thenReturn(List.of(profile(11L, "5.00")));
        when(recommendationMapper.selectCandidates(anyInt(), anyInt(), any(), any(), anyInt(),
                anyList(), anyList())).thenReturn(List.of(dish(101L, "普通菜", "4.0"),
                dish(102L, "偏好菜", "4.0")));
        when(dishTagMapper.selectTagsByDishIds(anyList(), eq(7L))).thenReturn(List.of(
                new DishTagWeight(101L, 12L, "清淡", BigDecimal.ZERO),
                new DishTagWeight(102L, 11L, "酸甜", new BigDecimal("5.00"))));
        when(recommendationMapper.insert(any(Recommendation.class))).thenAnswer(invocation -> {
            Recommendation saved = invocation.getArgument(0);
            saved.setId(501L);
            return 1;
        });
    }

    @Test
    void ranksDishesByTasteProfileAndPersistsRecommendationItems() {
        RecommendationView result = service.recommend(7L, new RecommendationRequest(2, 2, null, null));

        assertThat(result.id()).isEqualTo(501L);
        assertThat(result.source()).isEqualTo(2);
        assertThat(result.items()).extracting(item -> item.dish().id()).containsExactly(102L, 101L);
        assertThat(result.items().get(0).reason()).contains("酸甜");
    }

    private static UserTasteProfile profile(Long tagId, String weight) {
        UserTasteProfile profile = new UserTasteProfile();
        profile.setTagId(tagId);
        profile.setWeight(new BigDecimal(weight));
        profile.setIsDeleted(0);
        return profile;
    }

    private static DishView dish(Long id, String name, String rating) {
        return new DishView(id, 2L, "一号档口", 3L, "东区食堂", name,
                new BigDecimal("12.00"), 2, 2, 0, 500, "", null, 0, 1,
                new BigDecimal(rating));
    }
}
