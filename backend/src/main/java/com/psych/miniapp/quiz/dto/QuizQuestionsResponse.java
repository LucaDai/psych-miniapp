package com.psych.miniapp.quiz.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizQuestionsResponse {

    private Long quizId;
    private String title;
    private List<QuestionItemResponse> questions;
}
