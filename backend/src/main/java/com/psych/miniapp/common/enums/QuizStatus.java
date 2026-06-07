package com.psych.miniapp.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum QuizStatus {

    DRAFT("draft"),
    PUBLISHED("published"),
    ARCHIVED("archived");

    private final String value;
}
