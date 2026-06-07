package com.psych.miniapp.quiz.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.psych.miniapp.common.constant.ErrorCode;
import com.psych.miniapp.common.enums.QuizStatus;
import com.psych.miniapp.common.exception.BizException;
import com.psych.miniapp.quiz.converter.QuizConverter;
import com.psych.miniapp.quiz.dto.QuizDetailResponse;
import com.psych.miniapp.quiz.dto.QuizListItemResponse;
import com.psych.miniapp.quiz.dto.QuizListResponse;
import com.psych.miniapp.quiz.dto.QuizQuestionsResponse;
import com.psych.miniapp.quiz.entity.Option;
import com.psych.miniapp.quiz.entity.Question;
import com.psych.miniapp.quiz.entity.Quiz;
import com.psych.miniapp.quiz.mapper.OptionMapper;
import com.psych.miniapp.quiz.mapper.QuestionMapper;
import com.psych.miniapp.quiz.mapper.QuizMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuizService {

    private final QuizMapper quizMapper;
    private final QuestionMapper questionMapper;
    private final OptionMapper optionMapper;
    private final QuizConverter quizConverter;

    public QuizListResponse listPublished() {
        LambdaQueryWrapper<Quiz> wrapper = new LambdaQueryWrapper<Quiz>()
                .eq(Quiz::getStatus, QuizStatus.PUBLISHED.getValue())
                .isNull(Quiz::getDeletedAt)
                .orderByAsc(Quiz::getSortOrder);

        List<QuizListItemResponse> list = quizMapper.selectList(wrapper).stream()
                .map(quizConverter::toListItem)
                .toList();

        return new QuizListResponse(list);
    }

    public QuizDetailResponse getPublishedById(Long quizId) {
        return quizConverter.toDetail(requirePublishedQuiz(quizId));
    }

    public QuizQuestionsResponse getPublishedQuestions(Long quizId) {
        Quiz quiz = requirePublishedQuiz(quizId);

        List<Question> questions = questionMapper.selectList(new LambdaQueryWrapper<Question>()
                .eq(Question::getQuizId, quizId)
                .orderByAsc(Question::getSortOrder));

        List<Long> questionIds = questions.stream().map(Question::getId).toList();
        Map<Long, List<Option>> optionsByQuestion = questionIds.isEmpty()
                ? Collections.emptyMap()
                : optionMapper.selectList(new LambdaQueryWrapper<Option>()
                                .in(Option::getQuestionId, questionIds)
                                .orderByAsc(Option::getSortOrder))
                        .stream()
                        .collect(Collectors.groupingBy(Option::getQuestionId));

        return quizConverter.toQuestionsResponse(quiz, questions, optionsByQuestion);
    }

    public Quiz requirePublishedQuiz(Long quizId) {
        Quiz quiz = quizMapper.selectById(quizId);
        if (!isVisibleToClient(quiz)) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
        return quiz;
    }

    private boolean isVisibleToClient(Quiz quiz) {
        return quiz != null
                && quiz.getDeletedAt() == null
                && QuizStatus.PUBLISHED.getValue().equals(quiz.getStatus());
    }
}
