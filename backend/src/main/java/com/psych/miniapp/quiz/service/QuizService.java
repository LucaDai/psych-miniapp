package com.psych.miniapp.quiz.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.psych.miniapp.common.constant.ErrorCode;
import com.psych.miniapp.common.enums.QuizStatus;
import com.psych.miniapp.common.exception.BizException;
import com.psych.miniapp.quiz.converter.QuizConverter;
import com.psych.miniapp.quiz.dto.QuizDetailResponse;
import com.psych.miniapp.quiz.dto.QuizListItemResponse;
import com.psych.miniapp.quiz.dto.QuizListResponse;
import com.psych.miniapp.quiz.entity.Quiz;
import com.psych.miniapp.quiz.mapper.QuizMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class QuizService {

    private final QuizMapper quizMapper;
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
        Quiz quiz = quizMapper.selectById(quizId);
        if (!isVisibleToClient(quiz)) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
        return quizConverter.toDetail(quiz);
    }

    private boolean isVisibleToClient(Quiz quiz) {
        return quiz != null
                && quiz.getDeletedAt() == null
                && QuizStatus.PUBLISHED.getValue().equals(quiz.getStatus());
    }
}
