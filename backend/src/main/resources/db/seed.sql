-- M3/M4 base seed data (admin, test user, quiz, questions, options, result_rules)
-- Prerequisite: run init.sql first
-- History records (test_attempt / answer) are created via POST /api/attempts during联调

USE psych_miniapp;

-- ---------------------------------------------------------------------------
-- admin_user (M6 login; password: admin123)
-- ---------------------------------------------------------------------------
DELETE FROM admin_user WHERE username = 'admin';

INSERT INTO admin_user (username, password_hash, created_at)
VALUES ('admin', '$2b$10$L9Z26.fPPDzB587jNFBRKOyyqix7lXYa8r8ykBFK2NKpZz/6fmNdu', NOW());

-- ---------------------------------------------------------------------------
-- C端联调固定用户 (M4/M5: user_id = 1)
-- ---------------------------------------------------------------------------
INSERT INTO user (id, openid, created_at)
VALUES (1, 'mock-openid', NOW())
ON DUPLICATE KEY UPDATE openid = 'mock-openid';

-- ---------------------------------------------------------------------------
-- 压力自测：清理旧数据后重建（可重复执行）
-- ---------------------------------------------------------------------------
DELETE a FROM answer a
INNER JOIN test_attempt ta ON a.attempt_id = ta.id
INNER JOIN quiz q ON ta.quiz_id = q.id
WHERE q.title = '压力自测';

DELETE ta FROM test_attempt ta
INNER JOIN quiz q ON ta.quiz_id = q.id
WHERE q.title = '压力自测';

DELETE rr FROM result_rule rr
INNER JOIN quiz q ON rr.quiz_id = q.id
WHERE q.title = '压力自测';

DELETE o FROM `option` o
INNER JOIN question qu ON o.question_id = qu.id
INNER JOIN quiz q ON qu.quiz_id = q.id
WHERE q.title = '压力自测';

DELETE qu FROM question qu
INNER JOIN quiz q ON qu.quiz_id = q.id
WHERE q.title = '压力自测';

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
    2,
    5,
    'published',
    1,
    NULL,
    NOW(),
    NOW()
);

INSERT INTO question (quiz_id, content, type, sort_order, created_at, updated_at)
SELECT q.id, '过去一周，你感到紧张或焦虑的频率是？', 'single_choice', 1, NOW(), NOW()
FROM quiz q WHERE q.title = '压力自测' AND q.deleted_at IS NULL
UNION ALL
SELECT q.id, '过去一周，你的睡眠质量如何？', 'single_choice', 2, NOW(), NOW()
FROM quiz q WHERE q.title = '压力自测' AND q.deleted_at IS NULL;

INSERT INTO `option` (question_id, content, score, sort_order, created_at, updated_at)
SELECT qu.id, '几乎没有', 0, 1, NOW(), NOW()
FROM question qu
INNER JOIN quiz q ON qu.quiz_id = q.id
WHERE q.title = '压力自测' AND qu.sort_order = 1
UNION ALL
SELECT qu.id, '偶尔', 1, 2, NOW(), NOW()
FROM question qu
INNER JOIN quiz q ON qu.quiz_id = q.id
WHERE q.title = '压力自测' AND qu.sort_order = 1
UNION ALL
SELECT qu.id, '经常', 2, 3, NOW(), NOW()
FROM question qu
INNER JOIN quiz q ON qu.quiz_id = q.id
WHERE q.title = '压力自测' AND qu.sort_order = 1
UNION ALL
SELECT qu.id, '很好', 0, 1, NOW(), NOW()
FROM question qu
INNER JOIN quiz q ON qu.quiz_id = q.id
WHERE q.title = '压力自测' AND qu.sort_order = 2
UNION ALL
SELECT qu.id, '一般', 1, 2, NOW(), NOW()
FROM question qu
INNER JOIN quiz q ON qu.quiz_id = q.id
WHERE q.title = '压力自测' AND qu.sort_order = 2
UNION ALL
SELECT qu.id, '很差', 2, 3, NOW(), NOW()
FROM question qu
INNER JOIN quiz q ON qu.quiz_id = q.id
WHERE q.title = '压力自测' AND qu.sort_order = 2;

INSERT INTO result_rule (quiz_id, min_score, max_score, title, description, suggestion, sort_order, created_at, updated_at)
SELECT q.id, 0, 1, '压力较低', '你目前压力水平较低，整体状态较为放松。', '保持现有作息，适当运动有助于维持好状态。', 1, NOW(), NOW()
FROM quiz q WHERE q.title = '压力自测' AND q.deleted_at IS NULL
UNION ALL
SELECT q.id, 2, 3, '压力适中', '你目前处于适度的压力水平，偶有紧张属正常现象。', '试着每天留出 10 分钟散步或深呼吸，给自己一点放松的时间。', 2, NOW(), NOW()
FROM quiz q WHERE q.title = '压力自测' AND q.deleted_at IS NULL
UNION ALL
SELECT q.id, 4, 4, '压力偏高', '你近期的压力水平偏高，建议关注休息与情绪调节。', '如果持续感到难以承受，可以和信任的人聊聊，或寻求专业支持。', 3, NOW(), NOW()
FROM quiz q WHERE q.title = '压力自测' AND q.deleted_at IS NULL;

UPDATE quiz q
SET question_count = (
    SELECT COUNT(*) FROM question qu WHERE qu.quiz_id = q.id
),
updated_at = NOW()
WHERE q.title = '压力自测' AND q.deleted_at IS NULL;
