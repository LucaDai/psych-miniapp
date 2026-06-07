package com.psych.miniapp.attempt.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("answer")
public class Answer {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long attemptId;

    private Long questionId;

    private Long optionId;

    private Integer score;
}
