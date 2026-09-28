package com.studyroom.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyroom.common.BizException;
import com.studyroom.dto.PreferenceRequest;
import com.studyroom.dto.PreferenceView;
import com.studyroom.dto.TasteProfileView;
import com.studyroom.mapper.PreferenceMapper;
import com.studyroom.mapper.UserTasteProfileMapper;
import com.studyroom.model.Preference;
import com.studyroom.model.Tag;
import com.studyroom.model.UserTasteProfile;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PreferenceService {
    private static final TypeReference<List<Long>> LONG_LIST = new TypeReference<>() { };
    private final PreferenceMapper preferenceMapper;
    private final UserTasteProfileMapper profileMapper;
    private final TagService tagService;
    private final ObjectMapper objectMapper;

    public PreferenceService(PreferenceMapper preferenceMapper, UserTasteProfileMapper profileMapper,
                             TagService tagService, ObjectMapper objectMapper) {
        this.preferenceMapper = preferenceMapper;
        this.profileMapper = profileMapper;
        this.tagService = tagService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public PreferenceView get(Long userId) {
        Preference preference = preferenceMapper.selectByUserId(userId);
        if (preference == null) {
            throw new BizException(404, "还没有填写口味问卷");
        }
        return view(preference);
    }

    @Transactional
    public PreferenceView save(Long userId, PreferenceRequest request) {
        if (request.budgetMin() != null && request.budgetMax() != null
                && request.budgetMin().compareTo(request.budgetMax()) > 0) {
            throw new BizException(400, "预算下限不能高于上限");
        }
        Set<Long> likes = new LinkedHashSet<>(request.likeTagIds());
        Set<Long> dislikes = new LinkedHashSet<>(request.dislikeTagIds());
        if (!java.util.Collections.disjoint(likes, dislikes)) {
            throw new BizException(400, "喜欢和忌口标签不能重复");
        }
        Map<Long, Tag> tags = new LinkedHashMap<>();
        likes.forEach(id -> tags.put(id, tagService.require(id)));
        dislikes.forEach(id -> tags.put(id, tagService.require(id)));

        Preference preference = preferenceMapper.selectByUserId(userId);
        boolean create = preference == null;
        if (create) {
            preference = new Preference();
            preference.setUserId(userId);
        }
        preference.setMaxSpiceLevel(request.maxSpiceLevel());
        preference.setBudgetMin(request.budgetMin());
        preference.setBudgetMax(request.budgetMax());
        preference.setLikeTagIds(toJson(new ArrayList<>(likes)));
        preference.setDislikeTagIds(toJson(new ArrayList<>(dislikes)));
        preference.setDietType(request.dietType());
        preference.setProfileSummary(summary(request, likes, dislikes, tags));
        preference.setQuestionnaireVersion("v1");
        if (create) {
            preferenceMapper.insert(preference);
        } else {
            preferenceMapper.update(preference);
        }
        refreshProfile(userId, likes, dislikes);
        return view(preference);
    }

    @Transactional(readOnly = true)
    public List<TasteProfileView> profile(Long userId) {
        List<TasteProfileView> views = new ArrayList<>();
        for (UserTasteProfile row : profileMapper.selectByUserId(userId)) {
            Tag tag = tagService.require(row.getTagId());
            views.add(new TasteProfileView(tag.getId(), tag.getTagName(), tag.getTagType(),
                    row.getWeight(), row.getSource()));
        }
        return views;
    }

    private void refreshProfile(Long userId, Set<Long> likes, Set<Long> dislikes) {
        List<UserTasteProfile> existing = profileMapper.selectAllByUserId(userId);
        Map<Long, UserTasteProfile> byTag = new LinkedHashMap<>();
        for (UserTasteProfile row : existing) {
            byTag.put(row.getTagId(), row);
            if (!likes.contains(row.getTagId()) && !dislikes.contains(row.getTagId())
                    && row.getIsDeleted() == 0) {
                profileMapper.setDeleted(row.getId(), 1);
            }
        }
        for (Long tagId : union(likes, dislikes)) {
            BigDecimal weight = likes.contains(tagId) ? new BigDecimal("5.00") : new BigDecimal("-10.00");
            UserTasteProfile row = byTag.get(tagId);
            if (row == null) {
                row = new UserTasteProfile();
                row.setUserId(userId);
                row.setTagId(tagId);
                row.setWeight(weight);
                row.setSource(1);
                profileMapper.insert(row);
            } else {
                row.setWeight(weight);
                row.setSource(1);
                row.setIsDeleted(0);
                profileMapper.update(row);
            }
        }
    }

    private String summary(PreferenceRequest request, Set<Long> likes, Set<Long> dislikes,
                           Map<Long, Tag> tags) {
        List<String> likedNames = likes.stream().map(tags::get).map(Tag::getTagName).toList();
        List<String> dislikedNames = dislikes.stream().map(tags::get).map(Tag::getTagName).toList();
        String budget = request.budgetMax() == null ? "预算不限"
                : "单餐预算 " + (request.budgetMin() == null ? "0" : request.budgetMin().toPlainString())
                + " 至 " + request.budgetMax().toPlainString() + " 元";
        String diet = switch (request.dietType()) {
            case 1 -> "偏好素食";
            case 2 -> "清真饮食";
            case 3 -> "有其他饮食要求";
            default -> "饮食类型不限";
        };
        return "可接受辣度至 " + request.maxSpiceLevel() + " 级；" + budget + "；" + diet
                + "；喜欢：" + (likedNames.isEmpty() ? "未指定" : String.join("、", likedNames))
                + "；忌口：" + (dislikedNames.isEmpty() ? "无" : String.join("、", dislikedNames));
    }

    private PreferenceView view(Preference preference) {
        return new PreferenceView(preference.getMaxSpiceLevel(), preference.getBudgetMin(),
                preference.getBudgetMax(), fromJson(preference.getLikeTagIds()),
                fromJson(preference.getDislikeTagIds()), preference.getDietType(),
                preference.getProfileSummary(), preference.getQuestionnaireVersion());
    }

    private String toJson(List<Long> values) {
        try {
            return objectMapper.writeValueAsString(values);
        } catch (JsonProcessingException exception) {
            throw new BizException(500, "无法保存问卷数据");
        }
    }

    private List<Long> fromJson(String value) {
        try {
            return value == null ? List.of() : objectMapper.readValue(value, LONG_LIST);
        } catch (JsonProcessingException exception) {
            throw new BizException(500, "问卷数据格式异常");
        }
    }

    private static Set<Long> union(Set<Long> first, Set<Long> second) {
        Set<Long> combined = new LinkedHashSet<>(first);
        combined.addAll(second);
        return combined;
    }
}
