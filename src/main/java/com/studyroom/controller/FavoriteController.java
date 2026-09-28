package com.studyroom.controller;

import com.studyroom.common.ApiResponse;
import com.studyroom.common.AuthUser;
import com.studyroom.common.PageResult;
import com.studyroom.dto.FavoriteRequest;
import com.studyroom.dto.FavoriteView;
import com.studyroom.service.FavoriteService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/favorites")
public class FavoriteController {
    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @PostMapping
    public ApiResponse<Void> add(@RequestBody @Valid FavoriteRequest body, HttpServletRequest request) {
        favoriteService.add(currentUser(request).id(), body);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/{targetType}/{targetId}")
    public ApiResponse<Void> remove(@PathVariable @Min(1) @Max(2) int targetType,
                                    @PathVariable @Min(1) Long targetId,
                                    HttpServletRequest request) {
        favoriteService.remove(currentUser(request).id(), targetType, targetId);
        return ApiResponse.success(null);
    }

    @GetMapping
    public ApiResponse<PageResult<FavoriteView>> page(
            @RequestParam(required = false) @Min(1) @Max(2) Integer targetType,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) int size,
            HttpServletRequest request) {
        return ApiResponse.success(favoriteService.page(currentUser(request).id(), targetType, page, size));
    }

    @GetMapping("/ids")
    public ApiResponse<List<Long>> ids(@RequestParam @Min(1) @Max(2) int targetType,
                                       HttpServletRequest request) {
        return ApiResponse.success(favoriteService.ids(currentUser(request).id(), targetType));
    }

    private static AuthUser currentUser(HttpServletRequest request) {
        return (AuthUser) request.getAttribute(AuthUser.REQUEST_ATTRIBUTE);
    }
}
