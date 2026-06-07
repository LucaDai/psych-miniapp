-- M3 smoke test data: one published quiz for GET /api/quizzes and GET /api/quizzes/{quizId}
-- Prerequisite: run init.sql first

USE psych_miniapp;

DELETE FROM quiz WHERE title = '压力自测';

INSERT INTO quiz (
    title,
    description,
    cover_image_url,
    question_count,
    estimated_minutes,
    status,
    sort_order,
    deleted_at,
    created_at,
    updated_at
) VALUES (
    '压力自测',
    '评估你近期的压力水平，帮助你了解当前状态。本测试仅供参考，不构成医疗建议。',
    NULL,
    10,
    5,
    'published',
    1,
    NULL,
    NOW(),
    NOW()
);
