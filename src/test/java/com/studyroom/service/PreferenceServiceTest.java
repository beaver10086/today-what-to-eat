package com.studyroom.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyroom.common.BizException;
import com.studyroom.dto.PreferenceRequest;
import com.studyroom.mapper.PreferenceMapper;
import com.studyroom.mapper.UserTasteProfileMapper;
import com.studyroom.model.Preference;
import com.studyroom.model.Tag;
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
class PreferenceServiceTest {
    @Mock private PreferenceMapper preferenceMapper;
    @Mock private UserTasteProfileMapper profileMapper;
    @Mock private TagService tagService;
    private PreferenceService service;

    @BeforeEach
    void setUp() {
        service = new PreferenceService(preferenceMapper, profileMapper, tagService, new ObjectMapper());
        org.mockito.Mockito.lenient().when(tagService.require(10L)).thenReturn(tag(10L, "清淡"));
        org.mockito.Mockito.lenient().when(tagService.require(20L)).thenReturn(tag(20L, "忌辣"));
    }

    @Test
    void savesQuestionnaireAndCreatesProfileWeights() {
        when(preferenceMapper.selectByUserId(7L)).thenReturn(null);
        UserTasteProfile stale = profile(30L, 0);
        stale.setId(30L);
        when(profileMapper.selectAllByUserId(7L)).thenReturn(List.of(stale));

        var saved = service.save(7L, request(1, new BigDecimal("5.00"),
                new BigDecimal("25.00"), 1));

        assertThat(saved.likeTagIds()).containsExactly(10L);
        assertThat(saved.dislikeTagIds()).containsExactly(20L);
        assertThat(saved.profileSummary()).contains("偏好素食").contains("清淡").contains("忌辣");
        ArgumentCaptor<UserTasteProfile> captor = ArgumentCaptor.forClass(UserTasteProfile.class);
        verify(profileMapper, org.mockito.Mockito.times(2)).insert(captor.capture());
        assertThat(captor.getAllValues()).extracting(UserTasteProfile::getWeight)
                .containsExactly(new BigDecimal("5.00"), new BigDecimal("-10.00"));
        verify(profileMapper).setDeleted(30L, 1);
    }

    @Test
    void updatesExistingQuestionnaireAndProfileRows() {
        Preference preference = preference(7L);
        UserTasteProfile liked = profile(10L, 0);
        liked.setId(100L);
        UserTasteProfile stale = profile(30L, 0);
        stale.setId(101L);
        when(preferenceMapper.selectByUserId(7L)).thenReturn(preference);
        when(profileMapper.selectAllByUserId(7L)).thenReturn(List.of(liked, stale));

        service.save(7L, request(2, null, null, 0));

        verify(preferenceMapper).update(preference);
        verify(profileMapper).update(liked);
        verify(profileMapper).insert(any(UserTasteProfile.class));
        verify(profileMapper).setDeleted(101L, 1);
    }

    @Test
    void rejectsInvertedBudgetsAndConflictingTasteTags() {
        PreferenceRequest inverted = new PreferenceRequest(1, new BigDecimal("20"),
                new BigDecimal("10"), List.of(10L), List.of(), 0);
        PreferenceRequest overlap = new PreferenceRequest(1, null, null,
                List.of(10L), List.of(10L), 0);

        assertThatThrownBy(() -> service.save(7L, inverted)).isInstanceOf(BizException.class);
        assertThatThrownBy(() -> service.save(7L, overlap)).isInstanceOf(BizException.class);
    }

    @Test
    void getsSavedQuestionnaireAndProfilesWithTagNames() {
        Preference preference = preference(7L);
        preference.setLikeTagIds("[10]");
        preference.setDislikeTagIds("[20]");
        when(preferenceMapper.selectByUserId(7L)).thenReturn(preference);
        when(profileMapper.selectByUserId(7L)).thenReturn(List.of(profile(10L, 0)));

        assertThat(service.get(7L).likeTagIds()).containsExactly(10L);
        assertThat(service.profile(7L).get(0).tagName()).isEqualTo("清淡");
    }

    @Test
    void reportsMissingQuestionnaireAndMalformedJson() {
        when(preferenceMapper.selectByUserId(7L)).thenReturn(null, preference(7L));

        assertThatThrownBy(() -> service.get(7L)).isInstanceOf(BizException.class);
        Preference malformed = preference(7L);
        malformed.setLikeTagIds("not-json");
        when(preferenceMapper.selectByUserId(7L)).thenReturn(malformed);
        assertThatThrownBy(() -> service.get(7L)).isInstanceOf(BizException.class);
    }

    private static PreferenceRequest request(int dietType, BigDecimal min, BigDecimal max, int spice) {
        return new PreferenceRequest(spice, min, max, List.of(10L), List.of(20L), dietType);
    }

    private static Preference preference(Long userId) {
        Preference preference = new Preference();
        preference.setId(50L);
        preference.setUserId(userId);
        preference.setMaxSpiceLevel(2);
        preference.setBudgetMin(new BigDecimal("5"));
        preference.setBudgetMax(new BigDecimal("25"));
        preference.setLikeTagIds("[]");
        preference.setDislikeTagIds("[]");
        preference.setDietType(0);
        return preference;
    }

    private static UserTasteProfile profile(Long tagId, int deleted) {
        UserTasteProfile profile = new UserTasteProfile();
        profile.setUserId(7L);
        profile.setTagId(tagId);
        profile.setWeight(BigDecimal.ZERO);
        profile.setIsDeleted(deleted);
        return profile;
    }

    private static Tag tag(Long id, String name) {
        Tag tag = new Tag();
        tag.setId(id);
        tag.setTagName(name);
        tag.setTagType(1);
        return tag;
    }
}
