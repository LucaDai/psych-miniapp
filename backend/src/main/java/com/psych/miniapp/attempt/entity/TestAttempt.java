package com.psych.miniapp.attempt.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("test_attempt")
public class TestAttempt {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long quizId;

    private Long resultRuleId;

    private Integer totalScore;

    private String quizTitle;

    private String resultTitle;

    private String resultDescription;

    /** 结果建议快照，提交时从 result_rule.suggestion 写入 */
    private String resultSuggestion;

    private LocalDateTime completedAt;
}
