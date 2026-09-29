package com.studyroom.controller;

import com.studyroom.common.ApiResponse;
import com.studyroom.dto.AssistantAnswer;
import com.studyroom.dto.AssistantRequest;
import com.studyroom.dto.DiscoveryResult;
import com.studyroom.service.AssistantService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/assistant")
public class AssistantController {
    private final AssistantService assistantService;

    public AssistantController(AssistantService assistantService) {
        this.assistantService = assistantService;
    }

    @PostMapping("/search")
    public ApiResponse<DiscoveryResult> search(@RequestBody @Valid AssistantRequest request) {
        return ApiResponse.success(assistantService.search(request.message(),
                request.page() == null ? 1 : request.page()));
    }

    @PostMapping("/ask")
    public ApiResponse<AssistantAnswer> ask(@RequestBody @Valid AssistantRequest request) {
        return ApiResponse.success(assistantService.ask(request.message()));
    }
}
