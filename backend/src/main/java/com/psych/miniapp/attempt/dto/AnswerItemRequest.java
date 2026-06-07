package com.psych.miniapp.attempt.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnswerItemRequest {

    @NotNull
    private Long questionId;

    @NotNull
    private Long optionId;
}
