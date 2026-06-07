package com.psych.miniapp.quiz.converter;

import com.psych.miniapp.quiz.dto.OptionItemResponse;
import com.psych.miniapp.quiz.dto.QuestionItemResponse;
import com.psych.miniapp.quiz.dto.QuizDetailResponse;
import com.psych.miniapp.quiz.dto.QuizListItemResponse;
import com.psych.miniapp.quiz.dto.QuizQuestionsResponse;
import com.psych.miniapp.quiz.entity.Option;
import com.psych.miniapp.quiz.entity.Question;
import com.psych.miniapp.quiz.entity.Quiz;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

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

    public QuizQuestionsResponse toQuestionsResponse(
            Quiz quiz,
            List<Question> questions,
            Map<Long, List<Option>> optionsByQuestion) {
        List<QuestionItemResponse> questionItems = questions.stream()
                .map(question -> QuestionItemResponse.builder()
                        .id(question.getId())
                        .content(question.getContent())
                        .sortOrder(question.getSortOrder())
                        .type(question.getType())
                        .options(toOptionItems(optionsByQuestion.getOrDefault(question.getId(), Collections.emptyList())))
                        .build())
                .toList();

        return QuizQuestionsResponse.builder()
                .quizId(quiz.getId())
                .title(quiz.getTitle())
                .questions(questionItems)
                .build();
    }

    private List<OptionItemResponse> toOptionItems(List<Option> options) {
        return options.stream()
                .map(option -> OptionItemResponse.builder()
                        .id(option.getId())
                        .content(option.getContent())
                        .sortOrder(option.getSortOrder())
                        .build())
                .toList();
    }
}
