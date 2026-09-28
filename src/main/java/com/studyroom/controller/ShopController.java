package com.studyroom.controller;

import com.studyroom.common.ApiResponse;
import com.studyroom.common.AuditAction;
import com.studyroom.common.PageResult;
import com.studyroom.dto.ShopRequest;
import com.studyroom.dto.ShopView;
import com.studyroom.service.ShopService;
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
@RequestMapping("/api/shops")
public class ShopController {
    private final ShopService shopService;

    public ShopController(ShopService shopService) { this.shopService = shopService; }

    @GetMapping
    public ApiResponse<PageResult<ShopView>> page(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.success(shopService.page(page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<ShopView> get(@PathVariable @Min(1) Long id) {
        return ApiResponse.success(shopService.get(id));
    }

    @PostMapping
    @AuditAction(module = "shop", action = 1)
    public ApiResponse<ShopView> create(@RequestBody @Valid ShopRequest request) {
        return ApiResponse.success(shopService.create(request));
    }

    @PutMapping("/{id}")
    @AuditAction(module = "shop", action = 2)
    public ApiResponse<ShopView> update(@PathVariable @Min(1) Long id,
                                        @RequestBody @Valid ShopRequest request) {
        return ApiResponse.success(shopService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @AuditAction(module = "shop", action = 3)
    public ApiResponse<Void> delete(@PathVariable @Min(1) Long id) {
        shopService.delete(id);
        return ApiResponse.success(null);
    }
}
