package com.studyroom.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.studyroom.common.BizException;
import com.studyroom.dto.FeedbackRequest;
import com.studyroom.dto.RecommendationRequest;
import com.studyroom.dto.RecommendationView;
import com.studyroom.mapper.DishTagMapper;
import com.studyroom.mapper.FeedbackRecordMapper;
import com.studyroom.mapper.PreferenceMapper;
import com.studyroom.mapper.RecommendationItemMapper;
import com.studyroom.mapper.RecommendationMapper;
import com.studyroom.mapper.UserTasteProfileMapper;
import com.studyroom.model.DishTag;
import com.studyroom.model.Preference;
import com.studyroom.model.Recommendation;
import com.studyroom.model.RecommendationItem;
import com.studyroom.model.UserTasteProfile;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceFlowTest {
    @Mock private RecommendationMapper recommendationMapper;
    @Mock private RecommendationItemMapper itemMapper;
    @Mock private FeedbackRecordMapper feedbackMapper;
    @Mock private PreferenceMapper preferenceMapper;
    @Mock private UserTasteProfileMapper profileMapper;
    @Mock private DishTagMapper dishTagMapper;

    private RecommendationService service;

    @BeforeEach
    void setUp() {
        service = new RecommendationService(recommendationMapper, itemMapper, feedbackMapper,
                preferenceMapper, profileMapper, dishTagMapper);
        Preference preference = new Preference();
        preference.setUserId(7L);
        preference.setMaxSpiceLevel(2);
        preference.setBudgetMin(BigDecimal.ZERO);
        preference.setBudgetMax(new BigDecimal("30.00"));
        preference.setDietType(0);
        preference.setDislikeTagIds("[]");
        lenient().when(preferenceMapper.selectByUserId(7L)).thenReturn(preference);
        lenient().when(profileMapper.selectByUserId(7L)).thenReturn(List.of());
        lenient().when(recommendationMapper.selectCandidates(anyInt(), anyInt(), any(), any(), anyInt(),
                anyList(), anyList())).thenReturn(List.of(dish(101L), dish(102L)));
        lenient().when(dishTagMapper.selectTagsByDishIds(anyList(), eq(7L))).thenReturn(List.of());
        lenient().when(recommendationMapper.insert(any(Recommendation.class))).thenAnswer(invocation -> {
            ((Recommendation) invocation.getArgument(0)).setId(501L);
            return 1;
        });
    }

    @Test
    void reportsMissingQuestionnaireAndEmptyCandidateSet() {
        when(preferenceMapper.selectByUserId(8L)).thenReturn(null);
        assertThatThrownBy(() -> service.recommend(8L, new RecommendationRequest(2, 5, null, null)))
                .isInstanceOf(BizException.class).hasMessageContaining("问卷");

        when(recommendationMapper.selectCandidates(anyInt(), anyInt(), any(), any(), anyInt(),
                anyList(), anyList())).thenReturn(List.of());
        assertThatThrownBy(() -> service.recommend(7L, new RecommendationRequest(2, 5, null, null)))
                .isInstanceOf(BizException.class).hasMessageContaining("预算");
    }

    @Test
    void feedbackPersistsRecordAndCreatesTasteWeight() {
        when(recommendationMapper.selectById(501L)).thenReturn(ownedRecommendation());
        when(itemMapper.selectByRecommendationId(501L)).thenReturn(List.of(item(101L)));
        when(profileMapper.selectAllByUserId(7L)).thenReturn(List.of());
        DishTag tag = new DishTag();
        tag.setTagId(31L);
        when(dishTagMapper.selectByDishId(101L)).thenReturn(List.of(tag));

        service.feedback(7L, 501L, new FeedbackRequest(101L, 1, "喜欢", "不错"));

        verify(feedbackMapper).insert(any());
        ArgumentCaptor<UserTasteProfile> profile = ArgumentCaptor.forClass(UserTasteProfile.class);
        verify(profileMapper).insert(profile.capture());
        assertThat(profile.getValue().getWeight()).isEqualByComparingTo("1");
        assertThat(profile.getValue().getSource()).isEqualTo(2);
    }

    @Test
    void feedbackRejectsForeignDishAndNextFeedbackOnFeedbackEndpoint() {
        when(recommendationMapper.selectById(501L)).thenReturn(ownedRecommendation());
        when(itemMapper.selectByRecommendationId(501L)).thenReturn(List.of(item(101L)));
        assertThatThrownBy(() -> service.feedback(7L, 501L,
                new FeedbackRequest(999L, 1, null, null))).isInstanceOf(BizException.class);
        assertThatThrownBy(() -> service.feedback(7L, 501L,
                new FeedbackRequest(101L, 3, null, null))).isInstanceOf(BizException.class)
                .hasMessageContaining("next");
    }

    @Test
    void nextRecordsSkipAndExcludesPreviousDishes() {
        when(recommendationMapper.selectById(501L)).thenReturn(ownedRecommendation());
        when(itemMapper.selectByRecommendationId(501L)).thenReturn(List.of(item(101L)));

        RecommendationView next = service.next(7L, 501L,
                new FeedbackRequest(101L, 1, "不想吃", "换一组"));

        assertThat(next.id()).isEqualTo(501L);
        verify(feedbackMapper).insert(any());
        ArgumentCaptor<List<Long>> excluded = ArgumentCaptor.forClass(List.class);
        verify(recommendationMapper).selectCandidates(anyInt(), anyInt(), any(), any(), anyInt(),
                excluded.capture(), anyList());
        assertThat(excluded.getValue()).contains(101L);
    }

    @Test
    void feedbackCannotAccessAnotherUsersRecommendation() {
        Recommendation foreign = ownedRecommendation();
        foreign.setUserId(88L);
        when(recommendationMapper.selectById(501L)).thenReturn(foreign);
        assertThatThrownBy(() -> service.feedback(7L, 501L,
                new FeedbackRequest(101L, 1, null, null))).isInstanceOf(BizException.class);
    }

    private static Recommendation ownedRecommendation() {
        Recommendation recommendation = new Recommendation();
        recommendation.setId(501L);
        recommendation.setUserId(7L);
        recommendation.setMealType(2);
        recommendation.setBudgetMax(new BigDecimal("25.00"));
        return recommendation;
    }

    private static RecommendationItem item(Long dishId) {
        RecommendationItem item = new RecommendationItem();
        item.setDishId(dishId);
        return item;
    }

    private static com.studyroom.dto.DishView dish(Long id) {
        return new com.studyroom.dto.DishView(id, 2L, "档口", 3L, "食堂", "菜" + id,
                new BigDecimal("10.00"), 2, 2, 0, 300, null, null, 0, 1, new BigDecimal("4.0"));
    }
}
