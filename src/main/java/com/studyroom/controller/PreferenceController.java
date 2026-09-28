package com.studyroom.controller;

import com.studyroom.common.ApiResponse;
import com.studyroom.common.AuthUser;
import com.studyroom.dto.PreferenceRequest;
import com.studyroom.dto.PreferenceView;
import com.studyroom.service.PreferenceService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/preferences/me")
public class PreferenceController {
    private final PreferenceService preferenceService;

    public PreferenceController(PreferenceService preferenceService) {
        this.preferenceService = preferenceService;
    }

    @GetMapping
    public ApiResponse<PreferenceView> get(HttpServletRequest request) {
        return ApiResponse.success(preferenceService.get(currentUser(request).id()));
    }

    @PutMapping
    public ApiResponse<PreferenceView> save(@RequestBody @Valid PreferenceRequest body,
                                            HttpServletRequest request) {
        return ApiResponse.success(preferenceService.save(currentUser(request).id(), body));
    }

    @GetMapping("/profile")
    public ApiResponse<List<com.studyroom.dto.TasteProfileView>> profile(HttpServletRequest request) {
        return ApiResponse.success(preferenceService.profile(currentUser(request).id()));
    }

    private static AuthUser currentUser(HttpServletRequest request) {
        return (AuthUser) request.getAttribute(AuthUser.REQUEST_ATTRIBUTE);
    }
}
