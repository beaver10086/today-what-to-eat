package com.studyroom.dto;

import com.studyroom.common.PageResult;

public record DiscoveryResult(SearchIntent conditions, PageResult<DishView> result, String notice) {
}
