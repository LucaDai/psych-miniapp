package com.psych.miniapp.quiz.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionItemResponse {

    private Long id;
    private String content;
    private Integer sortOrder;
    private String type;
    private List<OptionItemResponse> options;
}
