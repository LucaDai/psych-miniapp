package com.psych.miniapp.attempt.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubmitAttemptRequest {

    @NotNull
    private Long quizId;

    @NotEmpty
    @Valid
    private List<AnswerItemRequest> answers;
}
