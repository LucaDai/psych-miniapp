package com.psych.miniapp.attempt.controller;

import com.psych.miniapp.attempt.dto.AttemptDetailResponse;
import com.psych.miniapp.attempt.dto.AttemptListItemResponse;
import com.psych.miniapp.attempt.dto.AttemptResultResponse;
import com.psych.miniapp.attempt.dto.SubmitAttemptRequest;
import com.psych.miniapp.attempt.service.AttemptService;
import com.psych.miniapp.common.response.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/attempts")
@RequiredArgsConstructor
public class AttemptController {

    private static final long TEST_USER_ID = 1L;

    private final AttemptService attemptService;

    @GetMapping
    public Result<List<AttemptListItemResponse>> list() {
        return Result.ok(attemptService.listByUser(TEST_USER_ID));
    }

    @GetMapping("/{attemptId}")
    public Result<AttemptDetailResponse> detail(@PathVariable Long attemptId) {
        return Result.ok(attemptService.getDetail(attemptId, TEST_USER_ID));
    }

    @PostMapping
    public Result<AttemptResultResponse> submit(@Valid @RequestBody SubmitAttemptRequest request) {
        return Result.ok(attemptService.submit(request, TEST_USER_ID));
    }
}
