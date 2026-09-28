package com.studyroom.dto;

import java.time.LocalTime;

public record CanteenView(Long id, String canteenName, String campus, String location,
                          LocalTime openTime, LocalTime closeTime, String description,
                          String coverUrl, Integer sortOrder, Integer status) {
}
