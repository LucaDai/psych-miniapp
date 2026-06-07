# REST API 接口设计

> 版本：v1.2  
> 状态：已确认  
> 更新日期：2026-06-06

本文档定义 MVP 第一版所有 REST API。不包含实现代码。

---

## 1. 通用约定

### 1.1 基础路径

| 端 | 基础路径 |
|----|----------|
| C 端 | `/api` |
| 管理端 | `/api/admin` |

### 1.2 请求头

| Header | 说明 | 适用 |
|--------|------|------|
| `Authorization` | `Bearer {token}` | 需鉴权接口 |
| `Content-Type` | `application/json` | POST / PUT 请求 |

### 1.3 认证说明

MVP 使用**简单 Token**（服务端生成的 opaque token），不使用 JWT。

- 登录成功后返回 `token` 字符串
- 客户端在后续请求中通过 `Authorization: Bearer {token}` 携带
- 服务端在内存中维护 token 与用户/管理员的映射关系
- Token 过期后需重新登录

### 1.4 统一响应结构

**成功响应：**

```json
{
  "code": 0,
  "message": "ok",
  "data": {}
}
```

**失败响应：**

```json
{
  "code": 40001,
  "message": "错误描述",
  "data": null
}
```

### 1.5 通用错误码

| code | 说明 |
|------|------|
| 0 | 成功 |
| 40001 | 请求参数错误 |
| 40101 | 未登录或 Token 无效 |
| 40301 | 无权限 |
| 40401 | 资源不存在 |
| 40901 | 业务冲突（如分数区间重叠） |
| 42201 | 业务校验失败（如上架条件不满足） |
| 50001 | 服务器内部错误 |

### 1.6 分页结构

**请求参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | int | 否 | 页码，从 1 开始，默认 1 |
| pageSize | int | 否 | 每页条数，默认 20 |

**响应 data 结构：**

```json
{
  "list": [],
  "total": 0,
  "page": 1,
  "pageSize": 20
}
```

### 1.7 结果对象（AttemptResult）

提交与历史详情共用的结果数据结构：

```json
{
  "attemptId": 5001,
  "quizId": 1,
  "quizTitle": "压力自测",
  "totalScore": 23,
  "resultTitle": "压力适中",
  "resultDescription": "你目前处于适度的压力水平...",
  "resultSuggestion": "试着每天留出 10 分钟散步或深呼吸，给自己一点放松的时间。",
  "completedAt": "2026-06-06T14:30:00",
  "disclaimer": "本测试结果仅供参考，不构成医疗建议。如有心理困扰，请寻求专业机构帮助。"
}
```

**字段说明：**
- `resultTitle` / `resultDescription` / `resultSuggestion`：结果页核心文案，`resultSuggestion` 为更有人情味的行动建议
- `resultSuggestion` 可为 `null`（规则未配置建议时）

**数据来源约定：**
- 提交接口（`POST /api/attempts`）：计分并写入快照后，从 `test_attempt` 组装返回
- 历史详情（`GET /api/attempts/{attemptId}`）：**只读** `test_attempt` 快照字段，不重新计分、不匹配规则

---

## 2. C 端接口

### 2.1 微信登录

**POST** `/api/auth/wechat/login`

无需鉴权。

**请求参数（Body）：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| code | string | 是 | `wx.login` 返回的 code |

**返回 data：**

```json
{
  "token": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "expiresIn": 604800
}
```

---

### 2.2 测试列表

**GET** `/api/quizzes`

需 C 端 Token。

**返回 data：**

```json
{
  "list": [
    {
      "id": 1,
      "title": "压力自测",
      "description": "评估你近期的压力水平...",
      "coverImageUrl": "https://example.com/cover.jpg",
      "questionCount": 10,
      "estimatedMinutes": 5
    }
  ]
}
```

**说明：** 仅返回 `status = published` 且 `deleted_at IS NULL` 的测试。

---

### 2.3 测试详情

**GET** `/api/quizzes/{quizId}`

需 C 端 Token。

**路径参数：**

| 参数 | 类型 | 说明 |
|------|------|------|
| quizId | long | 测试 ID |

**返回 data：**

```json
{
  "id": 1,
  "title": "压力自测",
  "description": "完整简介文本...",
  "coverImageUrl": "https://example.com/cover.jpg",
  "questionCount": 10,
  "estimatedMinutes": 5
}
```

---

### 2.4 获取答题题目

**GET** `/api/quizzes/{quizId}/questions`

需 C 端 Token。

**返回 data：**

```json
{
  "quizId": 1,
  "title": "压力自测",
  "questions": [
    {
      "id": 101,
      "content": "过去一周，你感到紧张或焦虑的频率是？",
      "sortOrder": 1,
      "type": "single_choice",
      "options": [
        { "id": 1001, "content": "几乎没有", "sortOrder": 1 },
        { "id": 1002, "content": "偶尔", "sortOrder": 2 }
      ]
    }
  ]
}
```

**说明：** 选项不返回 `score` 字段。

---

### 2.5 提交答案

**POST** `/api/attempts`

需 C 端 Token。

**请求参数（Body）：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| quizId | long | 是 | 测试 ID |
| answers | array | 是 | 作答列表 |
| answers[].questionId | long | 是 | 题目 ID |
| answers[].optionId | long | 是 | 所选选项 ID |

**请求示例：**

```json
{
  "quizId": 1,
  "answers": [
    { "questionId": 101, "optionId": 1001 },
    { "questionId": 102, "optionId": 1005 }
  ]
}
```

**返回 data：** 直接返回 `AttemptResult` 对象（见 §1.7）

```json
{
  "attemptId": 5001,
  "quizId": 1,
  "quizTitle": "压力自测",
  "totalScore": 23,
  "resultTitle": "压力适中",
  "resultDescription": "你目前处于适度的压力水平...",
  "resultSuggestion": "试着每天留出 10 分钟散步或深呼吸，给自己一点放松的时间。",
  "completedAt": "2026-06-06T14:30:00",
  "disclaimer": "本测试结果仅供参考，不构成医疗建议。如有心理困扰，请寻求专业机构帮助。"
}
```

**后端处理流程：**
1. 校验答案完整性
2. 累加选项分值得到 `total_score`（总分区间计分）
3. 命中唯一 `result_rule`
4. 写入 `test_attempt`（含 `quiz_title`、`result_title`、`result_description`、`result_suggestion` 快照）及 `answer` 记录
5. 从 `test_attempt` 组装 `AttemptResult` 返回（含 `resultSuggestion`）

**业务校验失败（42201）：** 答案不完整、选项非法、无法命中唯一规则。

---

### 2.6 历史记录列表

**GET** `/api/attempts`

需 C 端 Token。

**请求参数（Query）：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | int | 否 | 页码，默认 1 |
| pageSize | int | 否 | 每页条数，默认 20 |

**返回 data：**

```json
{
  "list": [
    {
      "attemptId": 5001,
      "quizId": 1,
      "quizTitle": "压力自测",
      "resultTitle": "压力适中",
      "totalScore": 23,
      "completedAt": "2026-06-06T14:30:00"
    }
  ],
  "total": 5,
  "page": 1,
  "pageSize": 20
}
```

**说明：** `quizTitle`、`resultTitle`、`totalScore` 均取自 `test_attempt` 快照字段。

---

### 2.7 历史记录详情

**GET** `/api/attempts/{attemptId}`

需 C 端 Token。

**路径参数：**

| 参数 | 类型 | 说明 |
|------|------|------|
| attemptId | long | 答题记录 ID |

**返回 data：**

```json
{
  "attemptId": 5001,
  "quizId": 1,
  "quizTitle": "压力自测",
  "totalScore": 23,
  "resultTitle": "压力适中",
  "resultDescription": "你目前处于适度的压力水平...",
  "resultSuggestion": "试着每天留出 10 分钟散步或深呼吸，给自己一点放松的时间。",
  "completedAt": "2026-06-06T14:30:00",
  "disclaimer": "本测试结果仅供参考，不构成医疗建议。如有心理困扰，请寻求专业机构帮助。",
  "answers": [
    {
      "questionId": 101,
      "questionContent": "过去一周，你感到紧张或焦虑的频率是？",
      "optionId": 1001,
      "optionContent": "几乎没有",
      "score": 1
    }
  ]
}
```

**数据来源（禁止重新计算）：**

| 字段 | 来源 |
|------|------|
| quizTitle | `test_attempt.quiz_title` |
| totalScore | `test_attempt.total_score` |
| resultTitle | `test_attempt.result_title` |
| resultDescription | `test_attempt.result_description` |
| resultSuggestion | `test_attempt.result_suggestion` |
| completedAt | `test_attempt.completed_at` |
| answers[].score | `answer.score` |
| answers[].questionContent | 关联 `question.content`（仅展示用） |
| answers[].optionContent | 关联 `option.content`（仅展示用） |

**权限：** 仅可查看自己的记录，否则返回 40301。

---

## 3. 管理端接口

### 3.1 管理员登录

**POST** `/api/admin/auth/login`

无需鉴权。

**请求参数（Body）：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| username | string | 是 | 用户名 |
| password | string | 是 | 密码 |

**返回 data：**

```json
{
  "token": "admin-a1b2c3d4-e5f6-7890",
  "expiresIn": 86400
}
```

**说明：** 校验 `admin_user` 表中唯一管理员账号，返回简单 Token（非 JWT）。

---

### 3.2 测试列表

**GET** `/api/admin/quizzes`

需管理端 Token。

**请求参数（Query）：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | int | 否 | 页码，默认 1 |
| pageSize | int | 否 | 每页条数，默认 20 |
| status | string | 否 | 过滤：`draft` / `published` / `archived` |
| includeDeleted | boolean | 否 | 是否包含软删除，默认 true |

**返回 data：** 分页结构，`list` 项含 `coverImageUrl`、`deletedAt` 等字段。

---

### 3.3 创建测试

**POST** `/api/admin/quizzes`

**请求参数（Body）：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| title | string | 是 | 标题 |
| description | string | 是 | 简介 |
| coverImageUrl | string | 否 | 封面图 URL |
| estimatedMinutes | int | 否 | 预计时长，默认 5 |
| sortOrder | int | 否 | 排序，默认 0 |

**返回 data：** 新建测试对象，`status` 为 `draft`。

---

### 3.4 测试详情（含题目、选项、规则）

**GET** `/api/admin/quizzes/{quizId}`

**返回 data：** 测试基本信息 + `questions`（含 `options` 及 `score`）+ `resultRules` + `scoreRange`。

```json
{
  "scoreRange": {
    "minPossible": 2,
    "maxPossible": 20
  }
}
```

---

### 3.5 更新测试基本信息

**PUT** `/api/admin/quizzes/{quizId}`

**请求参数（Body）：** `title`、`description`、`coverImageUrl`、`estimatedMinutes`、`sortOrder`（均可选）。

---

### 3.6 软删除测试

**DELETE** `/api/admin/quizzes/{quizId}`

**返回 data：**

```json
{
  "id": 1,
  "deletedAt": "2026-06-06T15:00:00"
}
```

---

### 3.7 上架测试

**PUT** `/api/admin/quizzes/{quizId}/publish`

**返回 data：** `{ "id": 1, "status": "published" }`

---

### 3.8 下架测试

**PUT** `/api/admin/quizzes/{quizId}/unpublish`

**返回 data：** `{ "id": 1, "status": "archived" }`

---

### 3.9 – 3.17 题目 / 选项 / 结果规则 CRUD

与 v1.0 一致，路径前缀均为 `/api/admin/quizzes/{quizId}/...`：

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `.../questions` | 创建题目 |
| PUT | `.../questions/{questionId}` | 更新题目 |
| DELETE | `.../questions/{questionId}` | 删除题目 |
| POST | `.../questions/{questionId}/options` | 创建选项 |
| PUT | `.../questions/{questionId}/options/{optionId}` | 更新选项 |
| DELETE | `.../questions/{questionId}/options/{optionId}` | 删除选项 |
| POST | `.../result-rules` | 创建结果规则 |
| PUT | `.../result-rules/{ruleId}` | 更新结果规则 |
| DELETE | `.../result-rules/{ruleId}` | 删除结果规则 |

结果规则字段：`minScore`、`maxScore`、`title`、`description`、`suggestion`、`sortOrder`。

**创建 / 更新结果规则示例（Body）：**

```json
{
  "minScore": 0,
  "maxScore": 10,
  "title": "压力较低",
  "description": "你目前的压力水平较低，整体状态比较平稳。",
  "suggestion": "保持现有节奏就很好，可以继续安排一些让你放松的小活动。",
  "sortOrder": 1
}
```

**结果规则响应示例：**

```json
{
  "id": 201,
  "quizId": 1,
  "minScore": 0,
  "maxScore": 10,
  "title": "压力较低",
  "description": "你目前的压力水平较低，整体状态比较平稳。",
  "suggestion": "保持现有节奏就很好，可以继续安排一些让你放松的小活动。",
  "sortOrder": 1
}
```

`suggestion` 可选，用于结果页展示行动建议；提交答题时会快照至 `test_attempt.result_suggestion`。

---

## 4. 接口总览

### 4.1 C 端（7 个）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/auth/wechat/login` | 微信登录 |
| GET | `/api/quizzes` | 测试列表 |
| GET | `/api/quizzes/{quizId}` | 测试详情 |
| GET | `/api/quizzes/{quizId}/questions` | 获取答题题目 |
| POST | `/api/attempts` | 提交答案，直接返回结果对象 |
| GET | `/api/attempts` | 历史记录列表 |
| GET | `/api/attempts/{attemptId}` | 历史详情（读快照，不重算） |

### 4.2 管理端（17 个）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/admin/auth/login` | 管理员登录 |
| GET | `/api/admin/quizzes` | 测试列表 |
| POST | `/api/admin/quizzes` | 创建测试 |
| GET | `/api/admin/quizzes/{quizId}` | 测试详情 |
| PUT | `/api/admin/quizzes/{quizId}` | 更新基本信息 |
| DELETE | `/api/admin/quizzes/{quizId}` | 软删除 |
| PUT | `/api/admin/quizzes/{quizId}/publish` | 上架 |
| PUT | `/api/admin/quizzes/{quizId}/unpublish` | 下架 |
| POST/PUT/DELETE | `.../questions...` | 题目 CRUD |
| POST/PUT/DELETE | `.../options...` | 选项 CRUD |
| POST/PUT/DELETE | `.../result-rules...` | 结果规则 CRUD |

---

## 5. 免责声明常量

字段名 `disclaimer`，文案：

```
本测试结果仅供参考，不构成医疗建议。如有心理困扰，请寻求专业机构帮助。
```

适用接口：
- `POST /api/attempts`
- `GET /api/attempts/{attemptId}`
