package com.studyroom.dto;

import java.math.BigDecimal;
import java.time.LocalTime;

public record ShopView(Long id, Long canteenId, String shopName, String locationDesc,
                       LocalTime openTime, LocalTime closeTime, String cuisine,
                       BigDecimal avgPrice, String coverUrl, String description,
                       Integer status, Integer sortOrder, Integer queueHeat) {
}
