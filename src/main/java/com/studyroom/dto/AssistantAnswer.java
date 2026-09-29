package com.studyroom.dto;

import java.util.List;

public record AssistantAnswer(String answer, SearchIntent conditions, List<DishView> dishes) {
}
