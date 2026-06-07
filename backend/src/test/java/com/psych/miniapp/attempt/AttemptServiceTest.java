package com.psych.miniapp.attempt;

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
import com.psych.miniapp.attempt.service.AttemptService;
import com.psych.miniapp.attempt.service.ScoringService;
import com.psych.miniapp.attempt.service.model.ScoringResult;
import com.psych.miniapp.common.constant.AppConstants;
import com.psych.miniapp.common.constant.ErrorCode;
import com.psych.miniapp.common.exception.BizException;
import com.psych.miniapp.quiz.entity.Option;
import com.psych.miniapp.quiz.entity.Question;
import com.psych.miniapp.quiz.entity.Quiz;
import com.psych.miniapp.quiz.entity.ResultRule;
import com.psych.miniapp.quiz.mapper.OptionMapper;
import com.psych.miniapp.quiz.mapper.QuestionMapper;
import com.psych.miniapp.quiz.service.QuizService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttemptServiceTest {

    @Mock
    private QuizService quizService;

    @Mock
    private QuestionMapper questionMapper;

    @Mock
    private OptionMapper optionMapper;

    @Mock
    private TestAttemptMapper testAttemptMapper;

    @Mock
    private AnswerMapper answerMapper;

    @Mock
    private ScoringService scoringService;

    @Spy
    private AttemptConverter attemptConverter = new AttemptConverter();

    @InjectMocks
    private AttemptService attemptService;

    @Test
    void submit_persistsSnapshotAndReturnsResult() {
        Quiz quiz = buildQuiz();
        Question question1 = buildQuestion(101L, 1);
        Question question2 = buildQuestion(102L, 2);
        Option option1 = buildOption(1001L, 101L, 0);
        Option option2 = buildOption(1005L, 102L, 1);
        ResultRule rule = buildRule();

        SubmitAttemptRequest request = new SubmitAttemptRequest(
                1L,
                List.of(
                        new AnswerItemRequest(101L, 1001L),
                        new AnswerItemRequest(102L, 1005L)));

        when(quizService.requirePublishedQuiz(1L)).thenReturn(quiz);
        when(questionMapper.selectList(any())).thenReturn(List.of(question1, question2));
        when(optionMapper.selectBatchIds(List.of(1001L, 1005L))).thenReturn(List.of(option1, option2));

        Answer answer1 = new Answer();
        answer1.setQuestionId(101L);
        answer1.setOptionId(1001L);
        answer1.setScore(0);
        Answer answer2 = new Answer();
        answer2.setQuestionId(102L);
        answer2.setOptionId(1005L);
        answer2.setScore(1);
        ScoringResult scoringResult = new ScoringResult(1, rule, List.of(answer1, answer2));
        when(scoringService.scoreAndMatch(any())).thenReturn(scoringResult);

        when(testAttemptMapper.insert(any(TestAttempt.class))).thenAnswer(invocation -> {
            TestAttempt attempt = invocation.getArgument(0);
            attempt.setId(5001L);
            return 1;
        });

        AttemptResultResponse result = attemptService.submit(request, 1L);

        assertThat(result.getAttemptId()).isEqualTo(5001L);
        assertThat(result.getTotalScore()).isEqualTo(1);
        assertThat(result.getQuizTitle()).isEqualTo("压力自测");
        assertThat(result.getResultTitle()).isEqualTo("压力较低");
        assertThat(result.getDisclaimer()).isEqualTo(AppConstants.DISCLAIMER);

        ArgumentCaptor<TestAttempt> attemptCaptor = ArgumentCaptor.forClass(TestAttempt.class);
        verify(testAttemptMapper).insert(attemptCaptor.capture());
        TestAttempt savedAttempt = attemptCaptor.getValue();
        assertThat(savedAttempt.getUserId()).isEqualTo(1L);
        assertThat(savedAttempt.getQuizTitle()).isEqualTo("压力自测");
        assertThat(savedAttempt.getResultTitle()).isEqualTo("压力较低");
        assertThat(savedAttempt.getResultDescription()).isEqualTo("描述");
        assertThat(savedAttempt.getResultSuggestion()).isEqualTo("建议");
        assertThat(savedAttempt.getTotalScore()).isEqualTo(1);

        verify(answerMapper, times(2)).insert(any(Answer.class));
    }

    @Test
    void submit_rejectsIncompleteAnswers() {
        Quiz quiz = buildQuiz();
        Question question1 = buildQuestion(101L, 1);
        Question question2 = buildQuestion(102L, 2);

        SubmitAttemptRequest request = new SubmitAttemptRequest(
                1L,
                List.of(new AnswerItemRequest(101L, 1001L)));

        when(quizService.requirePublishedQuiz(1L)).thenReturn(quiz);
        when(questionMapper.selectList(any())).thenReturn(List.of(question1, question2));

        assertThatThrownBy(() -> attemptService.submit(request, 1L))
                .isInstanceOf(BizException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.UNPROCESSABLE.getCode());
    }

    @Test
    void listByUser_returnsSnapshotFieldsFromTestAttempt() {
        LocalDateTime completedAt = LocalDateTime.of(2026, 6, 6, 14, 30);
        TestAttempt attempt = buildTestAttempt(5001L, 1L, completedAt);

        when(testAttemptMapper.selectList(any())).thenReturn(List.of(attempt));

        List<AttemptListItemResponse> result = attemptService.listByUser(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAttemptId()).isEqualTo(5001L);
        assertThat(result.get(0).getQuizId()).isEqualTo(1L);
        assertThat(result.get(0).getQuizTitle()).isEqualTo("压力自测");
        assertThat(result.get(0).getResultTitle()).isEqualTo("压力适中");
        assertThat(result.get(0).getTotalScore()).isEqualTo(2);
        assertThat(result.get(0).getCompletedAt()).isEqualTo(completedAt);
        verifyNoInteractions(scoringService);
    }

    @Test
    void getDetail_returnsSnapshotScoresAndCurrentQuestionOptionContent() {
        LocalDateTime completedAt = LocalDateTime.of(2026, 6, 6, 14, 30);
        TestAttempt attempt = buildTestAttempt(5001L, 1L, completedAt);

        Answer answer1 = new Answer();
        answer1.setQuestionId(101L);
        answer1.setOptionId(1002L);
        answer1.setScore(1);
        Answer answer2 = new Answer();
        answer2.setQuestionId(102L);
        answer2.setOptionId(1005L);
        answer2.setScore(2);

        Question question1 = buildQuestion(101L, 1);
        question1.setContent("题干一");
        Question question2 = buildQuestion(102L, 2);
        question2.setContent("题干二");
        Option option1 = buildOption(1002L, 101L, 99);
        option1.setContent("选项一");
        Option option2 = buildOption(1005L, 102L, 99);
        option2.setContent("选项二");

        when(testAttemptMapper.selectById(5001L)).thenReturn(attempt);
        when(answerMapper.selectList(any())).thenReturn(List.of(answer2, answer1));
        when(questionMapper.selectBatchIds(any())).thenReturn(List.of(question1, question2));
        when(optionMapper.selectBatchIds(any())).thenReturn(List.of(option1, option2));

        AttemptDetailResponse result = attemptService.getDetail(5001L, 1L);

        assertThat(result.getAttemptId()).isEqualTo(5001L);
        assertThat(result.getQuizTitle()).isEqualTo("压力自测");
        assertThat(result.getResultTitle()).isEqualTo("压力适中");
        assertThat(result.getTotalScore()).isEqualTo(2);
        assertThat(result.getDisclaimer()).isEqualTo(AppConstants.DISCLAIMER);
        assertThat(result.getAnswers()).hasSize(2);
        assertThat(result.getAnswers().get(0).getQuestionContent()).isEqualTo("题干一");
        assertThat(result.getAnswers().get(0).getOptionContent()).isEqualTo("选项一");
        assertThat(result.getAnswers().get(0).getScore()).isEqualTo(1);
        assertThat(result.getAnswers().get(1).getQuestionContent()).isEqualTo("题干二");
        assertThat(result.getAnswers().get(1).getScore()).isEqualTo(2);
        verifyNoInteractions(scoringService);
    }

    @Test
    void getDetail_returnsNotFoundWhenAttemptMissing() {
        when(testAttemptMapper.selectById(99999L)).thenReturn(null);

        assertThatThrownBy(() -> attemptService.getDetail(99999L, 1L))
                .isInstanceOf(BizException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.NOT_FOUND.getCode());

        verifyNoInteractions(scoringService);
    }

    @Test
    void getDetail_returnsNotFoundWhenAttemptBelongsToAnotherUser() {
        TestAttempt attempt = buildTestAttempt(5001L, 2L, LocalDateTime.now());
        when(testAttemptMapper.selectById(5001L)).thenReturn(attempt);

        assertThatThrownBy(() -> attemptService.getDetail(5001L, 1L))
                .isInstanceOf(BizException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.NOT_FOUND.getCode());

        verifyNoInteractions(scoringService);
    }

    @Test
    void submit_rejectsOptionNotBelongingToQuestion() {
        Quiz quiz = buildQuiz();
        Question question1 = buildQuestion(101L, 1);
        Question question2 = buildQuestion(102L, 2);
        Option wrongOption = buildOption(1005L, 102L, 1);

        SubmitAttemptRequest request = new SubmitAttemptRequest(
                1L,
                List.of(
                        new AnswerItemRequest(101L, 1005L),
                        new AnswerItemRequest(102L, 1005L)));

        when(quizService.requirePublishedQuiz(1L)).thenReturn(quiz);
        when(questionMapper.selectList(any())).thenReturn(List.of(question1, question2));
        when(optionMapper.selectBatchIds(List.of(1005L, 1005L))).thenReturn(List.of(wrongOption));

        assertThatThrownBy(() -> attemptService.submit(request, 1L))
                .isInstanceOf(BizException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.UNPROCESSABLE.getCode());
    }

    private Quiz buildQuiz() {
        Quiz quiz = new Quiz();
        quiz.setId(1L);
        quiz.setTitle("压力自测");
        quiz.setStatus("published");
        return quiz;
    }

    private Question buildQuestion(Long id, int sortOrder) {
        Question question = new Question();
        question.setId(id);
        question.setQuizId(1L);
        question.setSortOrder(sortOrder);
        question.setType("single_choice");
        return question;
    }

    private Option buildOption(Long id, Long questionId, int score) {
        Option option = new Option();
        option.setId(id);
        option.setQuestionId(questionId);
        option.setScore(score);
        return option;
    }

    private ResultRule buildRule() {
        ResultRule rule = new ResultRule();
        rule.setId(201L);
        rule.setQuizId(1L);
        rule.setTitle("压力较低");
        rule.setDescription("描述");
        rule.setSuggestion("建议");
        return rule;
    }

    private TestAttempt buildTestAttempt(Long id, Long userId, LocalDateTime completedAt) {
        TestAttempt attempt = new TestAttempt();
        attempt.setId(id);
        attempt.setUserId(userId);
        attempt.setQuizId(1L);
        attempt.setTotalScore(2);
        attempt.setQuizTitle("压力自测");
        attempt.setResultTitle("压力适中");
        attempt.setResultDescription("描述快照");
        attempt.setResultSuggestion("建议快照");
        attempt.setCompletedAt(completedAt);
        return attempt;
    }
}
