package com.studyroom.controller;

import com.studyroom.common.ApiResponse;
import com.studyroom.common.AuditAction;
import com.studyroom.common.PageResult;
import com.studyroom.dto.TagRequest;
import com.studyroom.dto.TagView;
import com.studyroom.service.TagService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/tags")
public class TagController {
    private final TagService tagService;

    public TagController(TagService tagService) { this.tagService = tagService; }

    @GetMapping
    public ApiResponse<PageResult<TagView>> page(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "100") @Min(1) @Max(100) int size) {
        return ApiResponse.success(tagService.page(page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<TagView> get(@PathVariable @Min(1) Long id) {
        return ApiResponse.success(tagService.get(id));
    }

    @PostMapping
    @AuditAction(module = "tag", action = 1)
    public ApiResponse<TagView> create(@RequestBody @Valid TagRequest request) {
        return ApiResponse.success(tagService.create(request));
    }

    @PutMapping("/{id}")
    @AuditAction(module = "tag", action = 2)
    public ApiResponse<TagView> update(@PathVariable @Min(1) Long id,
                                       @RequestBody @Valid TagRequest request) {
        return ApiResponse.success(tagService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @AuditAction(module = "tag", action = 3)
    public ApiResponse<Void> delete(@PathVariable @Min(1) Long id) {
        tagService.delete(id);
        return ApiResponse.success(null);
    }
}
