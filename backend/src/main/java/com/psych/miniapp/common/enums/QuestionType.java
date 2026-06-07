package com.psych.miniapp.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum QuestionType {

    SINGLE_CHOICE("single_choice");

    private final String value;
}
