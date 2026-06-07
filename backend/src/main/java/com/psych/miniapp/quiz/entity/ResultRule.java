package com.psych.miniapp.quiz.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("result_rule")
public class ResultRule {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long quizId;

    private Integer minScore;

    private Integer maxScore;

    private String title;

    private String description;

    /** 结果建议，用于结果页展示更有人情味的行动提示 */
    private String suggestion;

    private Integer sortOrder;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
