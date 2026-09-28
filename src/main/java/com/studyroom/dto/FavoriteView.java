package com.studyroom.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record FavoriteView(Long favoriteId, Integer targetType, Long targetId, String targetName,
                           String subtitle, String imageUrl, BigDecimal price, Long shopId,
                           Long canteenId, LocalDateTime createTime) {
}
