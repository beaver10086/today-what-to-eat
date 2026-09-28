package com.studyroom.dto;

import java.math.BigDecimal;

public record TasteProfileView(Long tagId, String tagName, Integer tagType,
                               BigDecimal weight, Integer source) {
}
