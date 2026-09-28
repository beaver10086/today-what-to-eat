package com.studyroom.controller;

import com.studyroom.common.ApiResponse;
import com.studyroom.common.AuditAction;
import com.studyroom.common.PageResult;
import com.studyroom.dto.CanteenRequest;
import com.studyroom.dto.CanteenView;
import com.studyroom.service.CanteenService;
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
@RequestMapping("/api/canteens")
public class CanteenController {
    private final CanteenService canteenService;

    public CanteenController(CanteenService canteenService) { this.canteenService = canteenService; }

    @GetMapping
    public ApiResponse<PageResult<CanteenView>> page(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.success(canteenService.page(page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<CanteenView> get(@PathVariable @Min(1) Long id) {
        return ApiResponse.success(canteenService.get(id));
    }

    @PostMapping
    @AuditAction(module = "canteen", action = 1)
    public ApiResponse<CanteenView> create(@RequestBody @Valid CanteenRequest request) {
        return ApiResponse.success(canteenService.create(request));
    }

    @PutMapping("/{id}")
    @AuditAction(module = "canteen", action = 2)
    public ApiResponse<CanteenView> update(@PathVariable @Min(1) Long id,
                                           @RequestBody @Valid CanteenRequest request) {
        return ApiResponse.success(canteenService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @AuditAction(module = "canteen", action = 3)
    public ApiResponse<Void> delete(@PathVariable @Min(1) Long id) {
        canteenService.delete(id);
        return ApiResponse.success(null);
    }
}
