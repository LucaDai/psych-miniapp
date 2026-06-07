package com.psych.miniapp.attempt.service.model;

import com.psych.miniapp.attempt.dto.AnswerItemRequest;
import com.psych.miniapp.quiz.entity.Option;
import com.psych.miniapp.quiz.entity.Question;
import com.psych.miniapp.quiz.entity.Quiz;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@AllArgsConstructor
public class ScoringInput {

    private final Quiz quiz;
    private final List<Question> questions;
    private final Map<Long, Option> optionMap;
    private final List<AnswerItemRequest> answers;
}
