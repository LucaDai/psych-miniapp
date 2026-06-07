package com.psych.miniapp.attempt.converter;

import com.psych.miniapp.attempt.dto.AnswerDetailResponse;
import com.psych.miniapp.attempt.dto.AttemptDetailResponse;
import com.psych.miniapp.attempt.dto.AttemptListItemResponse;
import com.psych.miniapp.attempt.dto.AttemptResultResponse;
import com.psych.miniapp.attempt.entity.Answer;
import com.psych.miniapp.attempt.entity.TestAttempt;
import com.psych.miniapp.common.constant.AppConstants;
import com.psych.miniapp.quiz.entity.Option;
import com.psych.miniapp.quiz.entity.Question;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

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

    public AttemptListItemResponse toListItemResponse(TestAttempt attempt) {
        return AttemptListItemResponse.builder()
                .attemptId(attempt.getId())
                .quizId(attempt.getQuizId())
                .quizTitle(attempt.getQuizTitle())
                .resultTitle(attempt.getResultTitle())
                .totalScore(attempt.getTotalScore())
                .completedAt(attempt.getCompletedAt())
                .build();
    }

    public AttemptDetailResponse toDetailResponse(
            TestAttempt attempt,
            List<Answer> answers,
            Map<Long, Question> questionMap,
            Map<Long, Option> optionMap) {
        AttemptResultResponse result = toResultResponse(attempt);
        List<AnswerDetailResponse> answerDetails = answers.stream()
                .sorted(Comparator.comparingInt(answer -> questionSortOrder(answer, questionMap)))
                .map(answer -> toAnswerDetailResponse(answer, questionMap, optionMap))
                .toList();

        return AttemptDetailResponse.builder()
                .attemptId(result.getAttemptId())
                .quizId(result.getQuizId())
                .quizTitle(result.getQuizTitle())
                .totalScore(result.getTotalScore())
                .resultTitle(result.getResultTitle())
                .resultDescription(result.getResultDescription())
                .resultSuggestion(result.getResultSuggestion())
                .completedAt(result.getCompletedAt())
                .disclaimer(result.getDisclaimer())
                .answers(answerDetails)
                .build();
    }

    private AnswerDetailResponse toAnswerDetailResponse(
            Answer answer,
            Map<Long, Question> questionMap,
            Map<Long, Option> optionMap) {
        Question question = questionMap.get(answer.getQuestionId());
        Option option = optionMap.get(answer.getOptionId());
        return AnswerDetailResponse.builder()
                .questionId(answer.getQuestionId())
                .questionContent(question != null ? question.getContent() : null)
                .optionId(answer.getOptionId())
                .optionContent(option != null ? option.getContent() : null)
                .score(answer.getScore())
                .build();
    }

    private int questionSortOrder(Answer answer, Map<Long, Question> questionMap) {
        Question question = questionMap.get(answer.getQuestionId());
        return question != null ? question.getSortOrder() : Integer.MAX_VALUE;
    }
}
