package com.studyroom.service;

import com.studyroom.common.BizException;
import com.studyroom.dto.DishTagWeight;
import com.studyroom.dto.DishView;
import com.studyroom.dto.FeedbackRequest;
import com.studyroom.dto.RecommendationRequest;
import com.studyroom.dto.RecommendationView;
import com.studyroom.dto.RecommendedDishView;
import com.studyroom.mapper.DishTagMapper;
import com.studyroom.mapper.FeedbackRecordMapper;
import com.studyroom.mapper.PreferenceMapper;
import com.studyroom.mapper.RecommendationItemMapper;
import com.studyroom.mapper.RecommendationMapper;
import com.studyroom.mapper.UserTasteProfileMapper;
import com.studyroom.model.FeedbackRecord;
import com.studyroom.model.DishTag;
import com.studyroom.model.Preference;
import com.studyroom.model.Recommendation;
import com.studyroom.model.RecommendationItem;
import com.studyroom.model.UserTasteProfile;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecommendationService {
    private static final BigDecimal MAX_WEIGHT = new BigDecimal("10.00");
    private static final BigDecimal MIN_WEIGHT = new BigDecimal("-10.00");
    private final RecommendationMapper recommendationMapper;
    private final RecommendationItemMapper itemMapper;
    private final FeedbackRecordMapper feedbackMapper;
    private final PreferenceMapper preferenceMapper;
    private final UserTasteProfileMapper profileMapper;
    private final DishTagMapper dishTagMapper;

    public RecommendationService(RecommendationMapper recommendationMapper,
                                 RecommendationItemMapper itemMapper,
                                 FeedbackRecordMapper feedbackMapper,
                                 PreferenceMapper preferenceMapper,
                                 UserTasteProfileMapper profileMapper,
                                 DishTagMapper dishTagMapper) {
        this.recommendationMapper = recommendationMapper;
        this.itemMapper = itemMapper;
        this.feedbackMapper = feedbackMapper;
        this.preferenceMapper = preferenceMapper;
        this.profileMapper = profileMapper;
        this.dishTagMapper = dishTagMapper;
    }

    @Transactional
    public RecommendationView recommend(Long userId, RecommendationRequest request) {
        Preference preference = requirePreference(userId);
        Set<Long> excluded = new HashSet<>();
        if (request.excludeRecommendationId() != null) {
            Recommendation previous = requireOwnedRecommendation(request.excludeRecommendationId(), userId);
            for (RecommendationItem item : itemMapper.selectByRecommendationId(previous.getId())) {
                excluded.add(item.getDishId());
            }
        }
        return generate(userId, request, preference, excluded);
    }

    @Transactional
    public void feedback(Long userId, Long recommendationId, FeedbackRequest request) {
        Recommendation recommendation = requireOwnedRecommendation(recommendationId, userId);
        boolean wasRecommended = itemMapper.selectByRecommendationId(recommendationId).stream()
                .anyMatch(item -> item.getDishId().equals(request.dishId()));
        if (!wasRecommended) {
            throw new BizException(400, "该菜品不属于本次推荐");
        }
        if (request.feedbackType() == 3) {
            throw new BizException(400, "换一组请使用 next 接口");
        }
        recordFeedback(userId, recommendationId, request);
        if (request.feedbackType() == 1 || request.feedbackType() == 2) {
            adjustProfile(userId, request.dishId(), request.feedbackType() == 1
                    ? BigDecimal.ONE : new BigDecimal("-2.00"));
        }
    }

    @Transactional
    public RecommendationView next(Long userId, Long recommendationId, FeedbackRequest request) {
        Recommendation previous = requireOwnedRecommendation(recommendationId, userId);
        boolean wasRecommended = itemMapper.selectByRecommendationId(recommendationId).stream()
                .anyMatch(item -> item.getDishId().equals(request.dishId()));
        if (!wasRecommended) {
            throw new BizException(400, "该菜品不属于本次推荐");
        }
        FeedbackRequest nextFeedback = new FeedbackRequest(request.dishId(), 3,
                request.reasonTag(), request.comment());
        recordFeedback(userId, recommendationId, nextFeedback);
        Preference preference = requirePreference(userId);
        RecommendationRequest nextRequest = new RecommendationRequest(previous.getMealType(),
                itemMapper.selectByRecommendationId(recommendationId).size(), recommendationId,
                previous.getBudgetMax());
        Set<Long> excluded = new HashSet<>();
        itemMapper.selectByRecommendationId(recommendationId).forEach(item -> excluded.add(item.getDishId()));
        return generate(userId, nextRequest, preference, excluded);
    }

    private RecommendationView generate(Long userId, RecommendationRequest request,
                                        Preference preference, Set<Long> excluded) {
        long started = System.nanoTime();
        BigDecimal budgetMax = minNullable(preference.getBudgetMax(), request.budgetMax());
        List<Long> dislikeIds = parseIds(preference.getDislikeTagIds());
        List<DishView> candidates = recommendationMapper.selectCandidates(
                1 << (request.mealType() - 1), preference.getMaxSpiceLevel(), preference.getBudgetMin(),
                budgetMax, preference.getDietType(), new ArrayList<>(excluded), dislikeIds);
        if (candidates.isEmpty()) {
            throw new BizException(404, "没有符合条件的菜品，请调整预算或口味问卷");
        }
        Map<Long, List<DishTagWeight>> tagsByDish = new HashMap<>();
        Map<Long, BigDecimal> weights = new HashMap<>();
        profileMapper.selectByUserId(userId).forEach(row -> weights.put(row.getTagId(), row.getWeight()));
        List<Long> ids = candidates.stream().map(DishView::id).toList();
        for (DishTagWeight row : dishTagMapper.selectTagsByDishIds(ids, userId)) {
            tagsByDish.computeIfAbsent(row.dishId(), ignored -> new ArrayList<>()).add(row);
        }
        Set<Long> negative = new HashSet<>();
        weights.forEach((tagId, weight) -> {
            if (weight.signum() < 0) {
                negative.add(tagId);
            }
        });
        List<ScoredDish> scored = candidates.stream()
                .filter(dish -> tagsByDish.getOrDefault(dish.id(), List.of()).stream()
                        .noneMatch(tag -> negative.contains(tag.tagId())))
                .map(dish -> score(dish, tagsByDish.getOrDefault(dish.id(), List.of()), weights))
                .sorted(Comparator.comparing(ScoredDish::score).reversed()
                        .thenComparing(item -> item.dish().id()))
                .toList();
        if (scored.isEmpty()) {
            throw new BizException(404, "没有符合当前忌口和画像的菜品");
        }
        int limit = request.limit() == null ? 5 : request.limit();
        List<ScoredDish> selected = scored.stream().limit(limit).toList();
        Recommendation recommendation = new Recommendation();
        recommendation.setRequestNo(UUID.randomUUID().toString().replace("-", ""));
        recommendation.setUserId(userId);
        recommendation.setMealType(request.mealType());
        recommendation.setBudgetMax(budgetMax);
        recommendation.setSource(2);
        recommendation.setIsFallback(0);
        recommendation.setCostMs((int) Math.max(0, (System.nanoTime() - started) / 1_000_000));
        recommendationMapper.insert(recommendation);
        List<RecommendedDishView> views = new ArrayList<>();
        int rank = 1;
        for (ScoredDish item : selected) {
            RecommendationItem row = new RecommendationItem();
            row.setRecommendationId(recommendation.getId());
            row.setDishId(item.dish().id());
            row.setShopId(item.dish().shopId());
            row.setRankNo(rank++);
            row.setScore(item.score());
            row.setReason(item.reason());
            itemMapper.insert(row);
            views.add(new RecommendedDishView(item.dish(), item.score(), item.reason()));
        }
        return new RecommendationView(recommendation.getId(), recommendation.getRequestNo(),
                recommendation.getMealType(), recommendation.getBudgetMax(), recommendation.getSource(),
                recommendation.getIsFallback(), recommendation.getCostMs(), views);
    }

    private ScoredDish score(DishView dish, List<DishTagWeight> tags, Map<Long, BigDecimal> weights) {
        BigDecimal score = BigDecimal.ZERO;
        List<String> matched = new ArrayList<>();
        for (DishTagWeight tag : tags) {
            BigDecimal weight = weights.getOrDefault(tag.tagId(), BigDecimal.ZERO);
            score = score.add(weight);
            if (weight.signum() > 0) {
                matched.add(tag.tagName());
            }
        }
        score = score.add(dish.rating() == null ? BigDecimal.ZERO
                : dish.rating().divide(BigDecimal.TEN, 2, RoundingMode.HALF_UP));
        String reason = matched.isEmpty() ? "符合预算、餐次和口味限制，综合评分较高"
                : "符合你对" + String.join("、", matched) + "的偏好";
        return new ScoredDish(dish, score.setScale(2, RoundingMode.HALF_UP), reason);
    }

    private void adjustProfile(Long userId, Long dishId, BigDecimal delta) {
        Map<Long, UserTasteProfile> existing = new LinkedHashMap<>();
        profileMapper.selectAllByUserId(userId).forEach(row -> existing.put(row.getTagId(), row));
        for (DishTag tag : dishTagMapper.selectByDishId(dishId)) {
            UserTasteProfile row = existing.get(tag.getTagId());
            BigDecimal old = row == null || row.getIsDeleted() == 1 ? BigDecimal.ZERO : row.getWeight();
            BigDecimal changed = old.add(delta).max(MIN_WEIGHT).min(MAX_WEIGHT);
            if (row == null) {
                row = new UserTasteProfile();
                row.setUserId(userId);
                row.setTagId(tag.getTagId());
                row.setWeight(changed);
                row.setSource(2);
                profileMapper.insert(row);
            } else {
                if (row.getIsDeleted() != null && row.getIsDeleted() == 1) {
                    profileMapper.setDeleted(row.getId(), 0);
                }
                row.setWeight(changed);
                row.setSource(2);
                row.setIsDeleted(0);
                profileMapper.update(row);
            }
        }
    }

    private void recordFeedback(Long userId, Long recommendationId, FeedbackRequest request) {
        FeedbackRecord record = new FeedbackRecord();
        record.setUserId(userId);
        record.setRecommendationId(recommendationId);
        record.setDishId(request.dishId());
        record.setFeedbackType(request.feedbackType());
        record.setReasonTag(request.reasonTag());
        record.setComment(request.comment());
        feedbackMapper.insert(record);
    }

    private Preference requirePreference(Long userId) {
        Preference preference = preferenceMapper.selectByUserId(userId);
        if (preference == null) {
            throw new BizException(400, "请先完成口味问卷，再使用个性化推荐");
        }
        return preference;
    }

    private Recommendation requireOwnedRecommendation(Long id, Long userId) {
        Recommendation recommendation = recommendationMapper.selectById(id);
        if (recommendation == null || !recommendation.getUserId().equals(userId)) {
            throw new BizException(404, "推荐记录不存在");
        }
        return recommendation;
    }

    private List<Long> parseIds(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().readValue(json,
                    new com.fasterxml.jackson.core.type.TypeReference<>() { });
        } catch (com.fasterxml.jackson.core.JsonProcessingException exception) {
            throw new BizException(500, "问卷忌口数据格式异常");
        }
    }

    private BigDecimal minNullable(BigDecimal first, BigDecimal second) {
        if (first == null) {
            return second;
        }
        if (second == null) {
            return first;
        }
        return first.min(second);
    }

    private record ScoredDish(DishView dish, BigDecimal score, String reason) {
    }
}
