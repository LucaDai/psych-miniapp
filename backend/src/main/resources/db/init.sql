-- psych-miniapp MVP schema (MySQL 8)
-- Source: docs/schema.md v1.2

CREATE DATABASE IF NOT EXISTS psych_miniapp
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE psych_miniapp;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `answer`;
DROP TABLE IF EXISTS `test_attempt`;
DROP TABLE IF EXISTS `result_rule`;
DROP TABLE IF EXISTS `option`;
DROP TABLE IF EXISTS `question`;
DROP TABLE IF EXISTS `quiz`;
DROP TABLE IF EXISTS `admin_user`;
DROP TABLE IF EXISTS `user`;

SET FOREIGN_KEY_CHECKS = 1;

-- ---------------------------------------------------------------------------
-- user
-- ---------------------------------------------------------------------------
CREATE TABLE `user` (
    `id`         BIGINT       NOT NULL AUTO_INCREMENT,
    `openid`     VARCHAR(64)  NOT NULL,
    `created_at` DATETIME     NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_openid` (`openid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- admin_user
-- ---------------------------------------------------------------------------
CREATE TABLE `admin_user` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT,
    `username`      VARCHAR(50)  NOT NULL,
    `password_hash` VARCHAR(255) NOT NULL,
    `created_at`    DATETIME     NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_admin_user_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- quiz
-- ---------------------------------------------------------------------------
CREATE TABLE `quiz` (
    `id`                BIGINT        NOT NULL AUTO_INCREMENT,
    `title`             VARCHAR(100)  NOT NULL,
    `description`       TEXT          NOT NULL,
    `cover_image_url`   VARCHAR(500)  NULL,
    `question_count`    INT           NOT NULL DEFAULT 0,
    `estimated_minutes` INT           NOT NULL DEFAULT 5,
    `status`            VARCHAR(20)   NOT NULL,
    `sort_order`        INT           NOT NULL DEFAULT 0,
    `deleted_at`        DATETIME      NULL,
    `created_at`        DATETIME      NOT NULL,
    `updated_at`        DATETIME      NOT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_quiz_status_deleted_sort` (`status`, `deleted_at`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- question
-- ---------------------------------------------------------------------------
CREATE TABLE `question` (
    `id`         BIGINT      NOT NULL AUTO_INCREMENT,
    `quiz_id`    BIGINT      NOT NULL,
    `content`    TEXT        NOT NULL,
    `type`       VARCHAR(20) NOT NULL,
    `sort_order` INT         NOT NULL DEFAULT 0,
    `created_at` DATETIME    NOT NULL,
    `updated_at` DATETIME    NOT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_question_quiz_sort` (`quiz_id`, `sort_order`),
    CONSTRAINT `fk_question_quiz`
        FOREIGN KEY (`quiz_id`) REFERENCES `quiz` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- option (reserved word)
-- ---------------------------------------------------------------------------
CREATE TABLE `option` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `question_id` BIGINT       NOT NULL,
    `content`     VARCHAR(500) NOT NULL,
    `score`       INT          NOT NULL DEFAULT 0,
    `sort_order`  INT          NOT NULL DEFAULT 0,
    `created_at`  DATETIME     NOT NULL,
    `updated_at`  DATETIME     NOT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_option_question_sort` (`question_id`, `sort_order`),
    CONSTRAINT `fk_option_question`
        FOREIGN KEY (`question_id`) REFERENCES `question` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- result_rule
-- ---------------------------------------------------------------------------
CREATE TABLE `result_rule` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `quiz_id`     BIGINT       NOT NULL,
    `min_score`   INT          NOT NULL,
    `max_score`   INT          NOT NULL,
    `title`       VARCHAR(100) NOT NULL,
    `description` TEXT         NOT NULL,
    `suggestion`  TEXT         NULL,
    `sort_order`  INT          NOT NULL DEFAULT 0,
    `created_at`  DATETIME     NOT NULL,
    `updated_at`  DATETIME     NOT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_result_rule_quiz_score` (`quiz_id`, `min_score`, `max_score`),
    CONSTRAINT `fk_result_rule_quiz`
        FOREIGN KEY (`quiz_id`) REFERENCES `quiz` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- test_attempt
-- ---------------------------------------------------------------------------
CREATE TABLE `test_attempt` (
    `id`                  BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`             BIGINT       NOT NULL,
    `quiz_id`             BIGINT       NOT NULL,
    `result_rule_id`      BIGINT       NULL,
    `total_score`         INT          NOT NULL,
    `quiz_title`          VARCHAR(100) NOT NULL,
    `result_title`        VARCHAR(100) NOT NULL,
    `result_description`  TEXT         NOT NULL,
    `result_suggestion`   TEXT         NULL,
    `completed_at`        DATETIME     NOT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_test_attempt_user_completed` (`user_id`, `completed_at` DESC),
    CONSTRAINT `fk_test_attempt_user`
        FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
    CONSTRAINT `fk_test_attempt_quiz`
        FOREIGN KEY (`quiz_id`) REFERENCES `quiz` (`id`),
    CONSTRAINT `fk_test_attempt_result_rule`
        FOREIGN KEY (`result_rule_id`) REFERENCES `result_rule` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- answer
-- ---------------------------------------------------------------------------
CREATE TABLE `answer` (
    `id`          BIGINT NOT NULL AUTO_INCREMENT,
    `attempt_id`  BIGINT NOT NULL,
    `question_id` BIGINT NOT NULL,
    `option_id`   BIGINT NOT NULL,
    `score`       INT    NOT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_answer_attempt` (`attempt_id`),
    UNIQUE KEY `uk_answer_attempt_question` (`attempt_id`, `question_id`),
    CONSTRAINT `fk_answer_attempt`
        FOREIGN KEY (`attempt_id`) REFERENCES `test_attempt` (`id`),
    CONSTRAINT `fk_answer_question`
        FOREIGN KEY (`question_id`) REFERENCES `question` (`id`),
    CONSTRAINT `fk_answer_option`
        FOREIGN KEY (`option_id`) REFERENCES `option` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
