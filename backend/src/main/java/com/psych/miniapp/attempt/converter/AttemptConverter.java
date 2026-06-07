package com.psych.miniapp.attempt.converter;

import com.psych.miniapp.attempt.dto.AttemptResultResponse;
import com.psych.miniapp.attempt.entity.TestAttempt;
import com.psych.miniapp.common.constant.AppConstants;
import org.springframework.stereotype.Component;

@Component
public class AttemptConverter {

    public AttemptResultResponse toResultResponse(TestAttempt attempt) {
        return AttemptResultResponse.builder()
                .attemptId(attempt.getId())
                .quizId(attempt.getQuizId())
                .quizTitle(attempt.getQuizTitle())
                .totalScore(attempt.getTotalScore())
                .resultTitle(attempt.getResultTitle())
                .resultDescription(attempt.getResultDescription())
                .resultSuggestion(attempt.getResultSuggestion())
                .completedAt(attempt.getCompletedAt())
                .disclaimer(AppConstants.DISCLAIMER)
                .build();
    }
}
