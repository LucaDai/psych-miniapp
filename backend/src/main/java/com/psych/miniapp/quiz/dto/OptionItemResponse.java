package com.psych.miniapp.quiz.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OptionItemResponse {

    private Long id;
    private String content;
    private Integer sortOrder;
}
