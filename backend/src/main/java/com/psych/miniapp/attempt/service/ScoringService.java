package com.psych.miniapp.attempt.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.psych.miniapp.attempt.dto.AnswerItemRequest;
import com.psych.miniapp.attempt.entity.Answer;
import com.psych.miniapp.attempt.service.model.ScoringInput;
import com.psych.miniapp.attempt.service.model.ScoringResult;
import com.psych.miniapp.common.constant.ErrorCode;
import com.psych.miniapp.common.exception.BizException;
import com.psych.miniapp.quiz.entity.Option;
import com.psych.miniapp.quiz.entity.ResultRule;
import com.psych.miniapp.quiz.mapper.ResultRuleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ScoringService {

    private final ResultRuleMapper resultRuleMapper;

    public ScoringResult scoreAndMatch(ScoringInput input) {
        int totalScore = 0;
        List<Answer> answerRecords = new ArrayList<>();

        for (AnswerItemRequest answerItem : input.getAnswers()) {
            Option option = input.getOptionMap().get(answerItem.getOptionId());
            totalScore += option.getScore();

            Answer answer = new Answer();
            answer.setQuestionId(answerItem.getQuestionId());
            answer.setOptionId(answerItem.getOptionId());
            answer.setScore(option.getScore());
            answerRecords.add(answer);
        }

        ResultRule matchedRule = matchUniqueRule(input.getQuiz().getId(), totalScore);
        return new ScoringResult(totalScore, matchedRule, answerRecords);
    }

    private ResultRule matchUniqueRule(Long quizId, int totalScore) {
        LambdaQueryWrapper<ResultRule> wrapper = new LambdaQueryWrapper<ResultRule>()
                .eq(ResultRule::getQuizId, quizId)
                .le(ResultRule::getMinScore, totalScore)
                .ge(ResultRule::getMaxScore, totalScore);

        List<ResultRule> rules = resultRuleMapper.selectList(wrapper);
        if (rules.size() != 1) {
            throw new BizException(ErrorCode.UNPROCESSABLE);
        }
        return rules.get(0);
    }
}
