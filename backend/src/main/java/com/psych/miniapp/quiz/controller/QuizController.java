package com.psych.miniapp.quiz.controller;

import com.psych.miniapp.common.response.Result;
import com.psych.miniapp.quiz.dto.QuizDetailResponse;
import com.psych.miniapp.quiz.dto.QuizListResponse;
import com.psych.miniapp.quiz.service.QuizService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/quizzes")
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;

    @GetMapping
    public Result<QuizListResponse> list() {
        return Result.ok(quizService.listPublished());
    }

    @GetMapping("/{quizId}")
    public Result<QuizDetailResponse> detail(@PathVariable Long quizId) {
        return Result.ok(quizService.getPublishedById(quizId));
    }
}
