package com.psych.miniapp.attempt.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnswerDetailResponse {

    private Long questionId;
    private String questionContent;
    private Long optionId;
    private String optionContent;
    private Integer score;
}
