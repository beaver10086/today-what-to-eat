package com.studyroom.dto;

import java.math.BigDecimal;

public record DishTagWeight(Long dishId, Long tagId, String tagName, BigDecimal weight) {
}
