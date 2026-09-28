package com.studyroom.controller;

import com.studyroom.common.ApiResponse;
import com.studyroom.common.AuditAction;
import com.studyroom.common.PageResult;
import com.studyroom.dto.DishQuery;
import com.studyroom.dto.DishRequest;
import com.studyroom.dto.DishView;
import com.studyroom.service.DishService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/dishes")
public class DishController {
    private final DishService dishService;

    public DishController(DishService dishService) { this.dishService = dishService; }

    @GetMapping
    public ApiResponse<PageResult<DishView>> search(@Valid @org.springframework.web.bind.annotation.ModelAttribute DishQuery query) {
        return ApiResponse.success(dishService.search(query));
    }

    @GetMapping("/{id}")
    public ApiResponse<DishView> get(@PathVariable @Min(1) Long id) {
        return ApiResponse.success(dishService.get(id));
    }

    @PostMapping
    @AuditAction(module = "dish", action = 1)
    public ApiResponse<DishView> create(@RequestBody @Valid DishRequest request) {
        return ApiResponse.success(dishService.create(request));
    }

    @PutMapping("/{id}")
    @AuditAction(module = "dish", action = 2)
    public ApiResponse<DishView> update(@PathVariable @Min(1) Long id,
                                       @RequestBody @Valid DishRequest request) {
        return ApiResponse.success(dishService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @AuditAction(module = "dish", action = 3)
    public ApiResponse<Void> delete(@PathVariable @Min(1) Long id) {
        dishService.delete(id);
        return ApiResponse.success(null);
    }

    @GetMapping("/{id}/tags")
    public ApiResponse<List<Long>> tags(@PathVariable @Min(1) Long id) {
        return ApiResponse.success(dishService.tagIds(id));
    }

    @PutMapping("/{id}/tags")
    @AuditAction(module = "dish", action = 2)
    public ApiResponse<List<Long>> replaceTags(@PathVariable @Min(1) Long id,
            @RequestBody List<@NotNull @Min(1) Long> tagIds) {
        return ApiResponse.success(dishService.replaceTags(id, tagIds));
    }
}
