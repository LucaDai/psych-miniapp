package com.psych.miniapp.quiz.converter;

import com.psych.miniapp.quiz.dto.QuizDetailResponse;
import com.psych.miniapp.quiz.dto.QuizListItemResponse;
import com.psych.miniapp.quiz.entity.Quiz;
import org.springframework.stereotype.Component;

@Component
public class QuizConverter {

    public QuizListItemResponse toListItem(Quiz quiz) {
        return QuizListItemResponse.builder()
                .id(quiz.getId())
                .title(quiz.getTitle())
                .description(quiz.getDescription())
                .questionCount(quiz.getQuestionCount())
                .estimatedMinutes(quiz.getEstimatedMinutes())
                .coverImageUrl(quiz.getCoverImageUrl())
                .build();
    }

    public QuizDetailResponse toDetail(Quiz quiz) {
        return QuizDetailResponse.builder()
                .id(quiz.getId())
                .title(quiz.getTitle())
                .description(quiz.getDescription())
                .questionCount(quiz.getQuestionCount())
                .estimatedMinutes(quiz.getEstimatedMinutes())
                .coverImageUrl(quiz.getCoverImageUrl())
                .build();
    }
}
