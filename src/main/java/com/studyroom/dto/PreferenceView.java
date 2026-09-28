package com.studyroom.dto;

import java.math.BigDecimal;
import java.util.List;

public record PreferenceView(Integer maxSpiceLevel, BigDecimal budgetMin, BigDecimal budgetMax,
                             List<Long> likeTagIds, List<Long> dislikeTagIds, Integer dietType,
                             String profileSummary, String questionnaireVersion) {
}
