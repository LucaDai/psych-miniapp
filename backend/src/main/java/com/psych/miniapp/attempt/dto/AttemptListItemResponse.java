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
public class AttemptListItemResponse {

    private Long attemptId;
    private Long quizId;
    private String quizTitle;
    private String resultTitle;
    private Integer totalScore;
    private LocalDateTime completedAt;
}
