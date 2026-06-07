package com.psych.miniapp.attempt.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.psych.miniapp.attempt.converter.AttemptConverter;
import com.psych.miniapp.attempt.dto.AnswerItemRequest;
import com.psych.miniapp.attempt.dto.AttemptDetailResponse;
import com.psych.miniapp.attempt.dto.AttemptListItemResponse;
import com.psych.miniapp.attempt.dto.AttemptResultResponse;
import com.psych.miniapp.attempt.dto.SubmitAttemptRequest;
import com.psych.miniapp.attempt.entity.Answer;
import com.psych.miniapp.attempt.entity.TestAttempt;
import com.psych.miniapp.attempt.mapper.AnswerMapper;
import com.psych.miniapp.attempt.mapper.TestAttemptMapper;
import com.psych.miniapp.attempt.service.model.ScoringInput;
import com.psych.miniapp.attempt.service.model.ScoringResult;
import com.psych.miniapp.common.constant.ErrorCode;
import com.psych.miniapp.common.exception.BizException;
import com.psych.miniapp.quiz.entity.Option;
import com.psych.miniapp.quiz.entity.Question;
import com.psych.miniapp.quiz.entity.Quiz;
import com.psych.miniapp.quiz.mapper.OptionMapper;
import com.psych.miniapp.quiz.mapper.QuestionMapper;
import com.psych.miniapp.quiz.service.QuizService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AttemptService {

    private final QuizService quizService;
    private final QuestionMapper questionMapper;
    private final OptionMapper optionMapper;
    private final TestAttemptMapper testAttemptMapper;
    private final AnswerMapper answerMapper;
    private final ScoringService scoringService;
    private final AttemptConverter attemptConverter;

    @Transactional
    public AttemptResultResponse submit(SubmitAttemptRequest request, Long userId) {
        Quiz quiz = quizService.requirePublishedQuiz(request.getQuizId());
        List<Question> questions = listQuestionsByQuizId(quiz.getId());
        validateAnswersCompleteness(questions, request.getAnswers());

        List<Long> optionIds = request.getAnswers().stream()
                .map(AnswerItemRequest::getOptionId)
                .toList();
        Map<Long, Option> optionMap = loadOptionMap(optionIds);
        Set<Long> questionIds = questions.stream().map(Question::getId).collect(Collectors.toSet());
        validateOptionOwnership(request.getAnswers(), optionMap, questionIds);

        ScoringResult scoringResult = scoringService.scoreAndMatch(
                new ScoringInput(quiz, questions, optionMap, request.getAnswers()));

        LocalDateTime completedAt = LocalDateTime.now();
        TestAttempt attempt = buildTestAttempt(userId, quiz, scoringResult, completedAt);
        testAttemptMapper.insert(attempt);

        for (Answer answer : scoringResult.getAnswerRecords()) {
            answer.setAttemptId(attempt.getId());
            answerMapper.insert(answer);
        }

        return attemptConverter.toResultResponse(attempt);
    }

    public List<AttemptListItemResponse> listByUser(Long userId) {
        List<TestAttempt> attempts = testAttemptMapper.selectList(new LambdaQueryWrapper<TestAttempt>()
                .eq(TestAttempt::getUserId, userId)
                .orderByDesc(TestAttempt::getCompletedAt));
        return attempts.stream()
                .map(attemptConverter::toListItemResponse)
                .toList();
    }

    public AttemptDetailResponse getDetail(Long attemptId, Long userId) {
        TestAttempt attempt = requireOwnedAttempt(attemptId, userId);
        List<Answer> answers = answerMapper.selectList(new LambdaQueryWrapper<Answer>()
                .eq(Answer::getAttemptId, attemptId));

        List<Long> questionIds = answers.stream().map(Answer::getQuestionId).distinct().toList();
        List<Long> optionIds = answers.stream().map(Answer::getOptionId).distinct().toList();

        Map<Long, Question> questionMap = questionIds.isEmpty()
                ? Map.of()
                : questionMapper.selectBatchIds(questionIds).stream()
                        .collect(Collectors.toMap(Question::getId, Function.identity()));
        Map<Long, Option> optionMap = optionIds.isEmpty()
                ? Map.of()
                : optionMapper.selectBatchIds(optionIds).stream()
                        .collect(Collectors.toMap(Option::getId, Function.identity()));

        return attemptConverter.toDetailResponse(attempt, answers, questionMap, optionMap);
    }

    private TestAttempt requireOwnedAttempt(Long attemptId, Long userId) {
        TestAttempt attempt = testAttemptMapper.selectById(attemptId);
        if (attempt == null || !attempt.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
        return attempt;
    }

    private List<Question> listQuestionsByQuizId(Long quizId) {
        return questionMapper.selectList(new LambdaQueryWrapper<Question>()
                .eq(Question::getQuizId, quizId)
                .orderByAsc(Question::getSortOrder));
    }

    private void validateAnswersCompleteness(List<Question> questions, List<AnswerItemRequest> answers) {
        if (answers.size() != questions.size()) {
            throw new BizException(ErrorCode.UNPROCESSABLE);
        }

        Set<Long> questionIds = new HashSet<>();
        for (AnswerItemRequest answer : answers) {
            if (!questionIds.add(answer.getQuestionId())) {
                throw new BizException(ErrorCode.UNPROCESSABLE);
            }
        }

        Set<Long> expectedQuestionIds = questions.stream()
                .map(Question::getId)
                .collect(Collectors.toSet());
        if (!questionIds.equals(expectedQuestionIds)) {
            throw new BizException(ErrorCode.UNPROCESSABLE);
        }
    }

    private Map<Long, Option> loadOptionMap(List<Long> optionIds) {
        List<Option> options = optionMapper.selectBatchIds(optionIds);
        if (options.size() != optionIds.size()) {
            throw new BizException(ErrorCode.UNPROCESSABLE);
        }
        return options.stream().collect(Collectors.toMap(Option::getId, Function.identity()));
    }

    private void validateOptionOwnership(
            List<AnswerItemRequest> answers,
            Map<Long, Option> optionMap,
            Set<Long> questionIds) {
        for (AnswerItemRequest answer : answers) {
            Option option = optionMap.get(answer.getOptionId());
            if (!questionIds.contains(answer.getQuestionId())) {
                throw new BizException(ErrorCode.UNPROCESSABLE);
            }
            if (!option.getQuestionId().equals(answer.getQuestionId())) {
                throw new BizException(ErrorCode.UNPROCESSABLE);
            }
        }
    }

    private TestAttempt buildTestAttempt(
            Long userId,
            Quiz quiz,
            ScoringResult scoringResult,
            LocalDateTime completedAt) {
        TestAttempt attempt = new TestAttempt();
        attempt.setUserId(userId);
        attempt.setQuizId(quiz.getId());
        attempt.setResultRuleId(scoringResult.getMatchedRule().getId());
        attempt.setTotalScore(scoringResult.getTotalScore());
        attempt.setQuizTitle(quiz.getTitle());
        attempt.setResultTitle(scoringResult.getMatchedRule().getTitle());
        attempt.setResultDescription(scoringResult.getMatchedRule().getDescription());
        attempt.setResultSuggestion(scoringResult.getMatchedRule().getSuggestion());
        attempt.setCompletedAt(completedAt);
        return attempt;
    }
}
