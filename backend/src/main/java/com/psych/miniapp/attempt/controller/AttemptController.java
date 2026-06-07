package com.psych.miniapp.attempt.controller;

import com.psych.miniapp.attempt.dto.AttemptResultResponse;
import com.psych.miniapp.attempt.dto.SubmitAttemptRequest;
import com.psych.miniapp.attempt.service.AttemptService;
import com.psych.miniapp.common.response.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/attempts")
@RequiredArgsConstructor
public class AttemptController {

    private static final long TEST_USER_ID = 1L;

    private final AttemptService attemptService;

    @PostMapping
    public Result<AttemptResultResponse> submit(@Valid @RequestBody SubmitAttemptRequest request) {
        return Result.ok(attemptService.submit(request, TEST_USER_ID));
    }
}
