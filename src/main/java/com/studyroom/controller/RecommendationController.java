package com.studyroom.controller;

import com.studyroom.common.ApiResponse;
import com.studyroom.common.AuthUser;
import com.studyroom.dto.FeedbackRequest;
import com.studyroom.dto.RecommendationRequest;
import com.studyroom.dto.RecommendationView;
import com.studyroom.service.RecommendationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {
    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @PostMapping
    public ApiResponse<RecommendationView> recommend(@RequestBody @Valid RecommendationRequest body,
                                                      HttpServletRequest request) {
        return ApiResponse.success(recommendationService.recommend(currentUser(request).id(), body));
    }

    @PostMapping("/{id}/feedback")
    public ApiResponse<Void> feedback(@PathVariable Long id,
                                      @RequestBody @Valid FeedbackRequest body,
                                      HttpServletRequest request) {
        recommendationService.feedback(currentUser(request).id(), id, body);
        return ApiResponse.success(null);
    }

    @PostMapping("/{id}/next")
    public ApiResponse<RecommendationView> next(@PathVariable Long id,
                                                 @RequestBody @Valid FeedbackRequest body,
                                                 HttpServletRequest request) {
        return ApiResponse.success(recommendationService.next(currentUser(request).id(), id, body));
    }

    private static AuthUser currentUser(HttpServletRequest request) {
        return (AuthUser) request.getAttribute(AuthUser.REQUEST_ATTRIBUTE);
    }
}
