package com.psych.miniapp.attempt.service.model;

import com.psych.miniapp.attempt.entity.Answer;
import com.psych.miniapp.quiz.entity.ResultRule;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class ScoringResult {

    private final int totalScore;
    private final ResultRule matchedRule;
    private final List<Answer> answerRecords;
}
