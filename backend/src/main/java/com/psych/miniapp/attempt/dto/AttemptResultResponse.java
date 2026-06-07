package com.psych.miniapp.attempt.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttemptResultResponse {

    private Long attemptId;
    private Long quizId;
    private String quizTitle;
    private Integer totalScore;
    private String resultTitle;
    private String resultDescription;
    private String resultSuggestion;
    private LocalDateTime completedAt;
    private String disclaimer;
}
