package com.psych.miniapp.quiz.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizDetailResponse {

    private Long id;
    private String title;
    private String description;
    private Integer questionCount;
    private Integer estimatedMinutes;
    private String coverImageUrl;
}
