# 后端架构设计

> 版本：v1.1  
> 状态：M4 后端已实现  
> 更新日期：2026-06-06

本文档描述心理自测小程序 MVP 后端架构。技术栈：**Spring Boot 3、Java 17、Maven、MySQL、MyBatis-Plus**。

设计与命名约定；§8.3 起补充 M4 已落地调用链（与代码一致）。

---

## 1. 项目目录结构

```
psych-miniapp/
├── docs/                          # 设计文档
├── miniapp/                       # 微信小程序（独立目录）
├── admin/                         # React 管理端（独立目录）
└── backend/                       # Spring Boot 后端
    ├── pom.xml
    ├── src/
    │   ├── main/
    │   │   ├── java/
    │   │   │   └── com/psych/miniapp/
    │   │   │       ├── PsychMiniappApplication.java
    │   │   │       ├── config/                # 配置类
    │   │   │       ├── common/                # 通用组件
    │   │   │       ├── auth/                  # 认证模块
    │   │   │       ├── quiz/                  # 测试内容模块
    │   │   │       ├── attempt/               # 答题与历史模块
    │   │   │       └── admin/                 # 管理端专用
    │   │   └── resources/
    │   │       ├── application.yml
    │   │       ├── application-local.yml
    │   │       └── mapper/                    # MyBatis XML（若选用 MyBatis）
    │   └── test/
    │       └── java/
    │           └── com/psych/miniapp/
    └── sql/                                   # DDL 与种子数据（M1 产出，非代码）
        ├── schema/
        └── seed/
```

**说明：**
- 后端、小程序、管理端三端同仓，平级目录
- 业务按领域分包（`quiz`、`attempt`、`auth`），不按技术层拆顶层包
- 每个领域包内部再分 `controller` / `service` / `repository`（或 `mapper`）

---

## 2. 分层设计

### 2.1 整体分层

```
┌─────────────────────────────────────────────┐
│  Controller 层                               │
│  接收 HTTP 请求、参数校验、调用 Service、返回 DTO  │
├─────────────────────────────────────────────┤
│  Service 层                                  │
│  业务逻辑、事务、计分、快照写入、权限校验        │
├─────────────────────────────────────────────┤
│  Repository 层                               │
│  数据库 CRUD，Entity ↔ 数据库映射             │
├─────────────────────────────────────────────┤
│  MySQL                                       │
└─────────────────────────────────────────────┘
```

### 2.2 各层职责

| 层 | 职责 | 禁止 |
|----|------|------|
| Controller | 路由映射、请求参数绑定、`@Valid` 校验、调用 Service、包装 `Result<T>` | 不写业务逻辑、不直接访问 Repository |
| Service | 业务规则、事务边界、计分与规则匹配、快照组装、权限判断 | 不处理 HTTP 细节 |
| Repository | 单表或简单联表查询、Entity 持久化 | 不写业务规则 |

### 2.3 领域模块与分层对应

| 领域包 | Controller | Service | Repository |
|--------|------------|---------|------------|
| `auth` | `AuthController`、`AdminAuthController` | `AuthService`、`AdminAuthService`、`TokenService` | `UserRepository`、`AdminUserRepository` |
| `quiz` | `QuizController`（C 端）、`AdminQuizController` | `QuizService`、`QuestionService`、`OptionService`、`ResultRuleService` | `QuizRepository`、`QuestionRepository`、`OptionRepository`、`ResultRuleRepository` |
| `attempt` | `AttemptController` | `AttemptService`、`ScoringService` | `TestAttemptRepository`、`AnswerRepository` |

### 2.4 跨层调用规则

- Controller → Service：单向
- Service → Repository：单向
- Service → Service：允许同领域或跨领域调用（如 `AttemptService` 调用 `QuizService` 校验测试状态）
- Repository 之间：禁止互调，联表查询放在单一 Repository 或 Service 编排

### 2.5 事务边界

| 操作 | 事务范围 |
|------|----------|
| 提交答案 `POST /api/attempts` | 计分 → 写 `test_attempt` → 写 `answer` 列表，同一事务 |
| 删除题目 | 删选项 → 删题目 → 更新 `quiz.question_count`，同一事务 |
| 软删除测试 | 写 `deleted_at` + 改 `status`，同一事务 |
| 查询类接口 | 只读，无事务或只读事务 |

---

## 3. Entity 设计

Entity 与数据库表一一对应，放在各自领域包的 `entity` 子包（或统一 `entity` 包）。

| Entity 类名 | 对应表 | 所在包 |
|-------------|--------|--------|
| `User` | `user` | `auth.entity` |
| `AdminUser` | `admin_user` | `auth.entity` |
| `Quiz` | `quiz` | `quiz.entity` |
| `Question` | `question` | `quiz.entity` |
| `Option` | `option` | `quiz.entity` |
| `ResultRule` | `result_rule` | `quiz.entity` |
| `TestAttempt` | `test_attempt` | `attempt.entity` |
| `Answer` | `answer` | `attempt.entity` |

### 3.1 Entity 设计原则

- 字段名与数据库列名映射：`cover_image_url` → `coverImageUrl`（驼峰）
- 时间字段统一 `LocalDateTime`
- 状态字段用 `String` 或 Java `enum`（如 `QuizStatus`：`DRAFT`、`PUBLISHED`、`ARCHIVED`）
- Entity 不暴露给 C 端 API，仅内部使用
- 软删除字段 `deletedAt`：NULL 表示未删除

### 3.2 关键 Entity 字段分组

**`Quiz`**
- 基本信息：`id`、`title`、`description`、`coverImageUrl`
- 展示：`questionCount`、`estimatedMinutes`、`sortOrder`
- 状态：`status`、`deletedAt`
- 审计：`createdAt`、`updatedAt`

**`TestAttempt`（快照核心）**
- 关联：`id`、`userId`、`quizId`、`resultRuleId`
- 计分：`totalScore`
- 快照：`quizTitle`、`resultTitle`、`resultDescription`、`resultSuggestion`
- 时间：`completedAt`

---

## 4. DTO 设计

DTO 分三类：**Request**（入参）、**Response**（出参）、**内部传输**（Service 间）。

### 4.1 通用 DTO

| 类名 | 用途 |
|------|------|
| `Result<T>` | 统一响应包装：`code`、`message`、`data` |
| `PageResult<T>` | 分页：`list`、`total`、`page`、`pageSize`（管理端预留；M5 历史列表 MVP 直接返回 `List`） |

### 4.2 C 端 DTO

| 类名 | 类型 | 对应接口 |
|------|------|----------|
| `WechatLoginRequest` | Request | `POST /api/auth/wechat/login` |
| `LoginResponse` | Response | C 端 / 管理端登录共用 |
| `QuizListItemResponse` | Response | `GET /api/quizzes` |
| `QuizDetailResponse` | Response | `GET /api/quizzes/{id}` |
| `QuizQuestionsResponse` | Response | `GET /api/quizzes/{id}/questions` |
| `QuestionItemResponse` | Response | 题目子项 |
| `OptionItemResponse` | Response | 选项子项（C 端无 score） |
| `SubmitAttemptRequest` | Request | `POST /api/attempts` |
| `AnswerItemRequest` | Request | 作答子项 |
| `AttemptResultResponse` | Response | 提交结果 & 历史详情核心 |
| `AttemptListItemResponse` | Response | `GET /api/attempts` 列表项（MVP 返回 `List`，无分页） |
| `AttemptDetailResponse` | Response | `GET /api/attempts/{id}`，结果区快照 + `answers` |
| `AnswerDetailResponse` | Response | 历史详情作答明细（`score` 快照；文案读当前 `question`/`option`） |

### 4.3 管理端 DTO

| 类名 | 类型 | 对应接口 |
|------|------|----------|
| `AdminLoginRequest` | Request | `POST /api/admin/auth/login` |
| `QuizAdminListItemResponse` | Response | 管理端列表项（含 status、deletedAt） |
| `QuizAdminDetailResponse` | Response | 管理端详情（含 questions、resultRules、scoreRange） |
| `CreateQuizRequest` | Request | 创建测试 |
| `UpdateQuizRequest` | Request | 更新基本信息 |
| `CreateQuestionRequest` | Request | 创建题目 |
| `UpdateQuestionRequest` | Request | 更新题目 |
| `CreateOptionRequest` | Request | 创建选项（含 score） |
| `UpdateOptionRequest` | Request | 更新选项 |
| `OptionAdminResponse` | Response | 管理端选项（含 score） |
| `CreateResultRuleRequest` | Request | 创建规则 |
| `UpdateResultRuleRequest` | Request | 更新规则 |
| `ResultRuleResponse` | Response | 规则 |
| `ScoreRangeResponse` | Response | `minPossible`、`maxPossible` |

### 4.4 DTO 与 Entity 转换

- 转换逻辑放在各领域的 `converter` 或 `assembler` 类中（如 `QuizConverter`、`AttemptConverter`）
- Controller 只接触 DTO，Service 入出参优先 DTO，内部可使用 Entity
- `AttemptResultResponse` 从 `TestAttempt` Entity 组装，保证提交与历史读取结构一致

### 4.5 快照相关 DTO 映射

| Response 字段 | Entity 来源 |
|---------------|-------------|
| `quizTitle` | `TestAttempt.quizTitle` |
| `totalScore` | `TestAttempt.totalScore` |
| `resultTitle` | `TestAttempt.resultTitle` |
| `resultDescription` | `TestAttempt.resultDescription` |
| `resultSuggestion` | `TestAttempt.resultSuggestion` |
| `completedAt` | `TestAttempt.completedAt` |
| `disclaimer` | `AppConstants.DISCLAIMER`（不落库） |

历史详情 Service **禁止**从 `ResultRule` 重新取标题/描述。

---

## 5. Exception 设计

### 5.1 异常体系

```
BizException（业务异常基类）
├── BadRequestException        → 40001
├── UnauthorizedException      → 40101
├── ForbiddenException         → 40301
├── NotFoundException          → 40401
├── ConflictException          → 40901
├── UnprocessableException     → 42201
└── InternalException          → 50001
```

### 5.2 异常类设计

| 类名 | code | 典型场景 |
|------|------|----------|
| `BadRequestException` | 40001 | 参数缺失、格式错误 |
| `UnauthorizedException` | 40101 | Token 缺失或无效 |
| `ForbiddenException` | 40301 | 访问他人答题记录 |
| `NotFoundException` | 40401 | 测试/记录不存在 |
| `ConflictException` | 40901 | 结果规则区间重叠 |
| `UnprocessableException` | 42201 | 上架校验失败、提交答案不完整、无法命中规则 |
| `InternalException` | 50001 | 未预期错误 |

### 5.3 全局异常处理

`GlobalExceptionHandler`（`@RestControllerAdvice`）统一捕获：

| 异常类型 | 处理方式 |
|----------|----------|
| `BizException` 子类 | 返回对应 `code` + `message` |
| `MethodArgumentNotValidException` | 40001，取首个校验错误信息 |
| `Exception` | 50001，记录日志，返回通用错误 |

### 5.4 业务异常示例

| 场景 | 异常 |
|------|------|
| 测试未上架 | `NotFoundException`（C 端视为不存在） |
| 提交答案数量不对 | `UnprocessableException` |
| 总分无法命中规则 | `UnprocessableException` |
| 区间重叠 | `ConflictException` |
| 查看他人记录 | `ForbiddenException` |

---

## 6. Auth 模块设计

### 6.1 设计原则

- **不使用 JWT**
- 使用服务端生成的 **opaque token**（如 UUID）
- Token 存储在服务端内存（`ConcurrentHashMap` 或 `TokenStore`）
- C 端与管理端 Token 分池存储，互不通用
- MVP 单管理员，无角色权限

### 6.2 模块组件

| 组件 | 职责 |
|------|------|
| `AuthController` | C 端微信登录入口 |
| `AdminAuthController` | 管理端登录入口 |
| `AuthService` | 微信 code2session（或 mock）、用户自动注册 |
| `AdminAuthService` | 校验单一管理员账号密码 |
| `TokenService` | 生成 token、存储映射、校验、过期清理 |
| `AuthInterceptor` | 拦截请求，解析 `Authorization` Header |
| `UserContext` / `AdminContext` | 线程上下文存放当前 userId / adminId |

### 6.3 Token 存储结构

**C 端 Token：**

| 字段 | 说明 |
|------|------|
| token | UUID 字符串 |
| userId | 关联 `user.id` |
| createdAt | 创建时间 |
| expiresAt | 过期时间 |

**管理端 Token：**

| 字段 | 说明 |
|------|------|
| token | UUID 字符串，前缀可与 C 端区分（如 `admin-`） |
| adminId | 固定为唯一管理员 ID |
| expiresAt | 过期时间 |

### 6.4 认证流程

**C 端：**

```
wx.login() → code
  → POST /api/auth/wechat/login
  → AuthService: code2session → openid
  → 查找或创建 User
  → TokenService.createClientToken(userId)
  → 返回 token
```

**管理端：**

```
username + password
  → POST /api/admin/auth/login
  → AdminAuthService: 查 admin_user → BCrypt 校验
  → TokenService.createAdminToken(adminId)
  → 返回 token
```

**请求鉴权：**

```
Authorization: Bearer {token}
  → AuthInterceptor
  → TokenService.validate(token)
  → 写入 UserContext / AdminContext
  → 放行
```

### 6.5 拦截器路由规则

| 路径 | 鉴权 |
|------|------|
| `POST /api/auth/wechat/login` | 放行 |
| `POST /api/admin/auth/login` | 放行 |
| `/api/admin/**` | 管理端 Token |
| `/api/**` | C 端 Token |

### 6.6 微信本地 Mock

`application-local.yml` 配置：

| 配置项 | 说明 |
|--------|------|
| `wechat.mock-enabled` | `true` 时 code 直接映射为固定 openid |
| `wechat.mock-openid` | 本地开发用固定 openid |

---

## 7. 配置文件设计

### 7.1 文件划分

| 文件 | 用途 |
|------|------|
| `application.yml` | 公共配置、默认 profile |
| `application-local.yml` | 本地开发覆盖配置 |

### 7.2 配置项清单

**`application.yml`**

| 配置项 | 说明 | 示例 |
|--------|------|------|
| `spring.application.name` | 应用名 | `psych-miniapp` |
| `spring.profiles.active` | 激活 profile | `local` |
| `server.port` | 端口 | `8080` |
| `spring.datasource.url` | 数据库连接 | `jdbc:mysql://localhost:3306/psych_miniapp` |
| `spring.datasource.username` | 数据库用户 | `root` |
| `spring.datasource.password` | 数据库密码 | — |
| `auth.client-token-expire-seconds` | C 端 Token 有效期 | `604800`（7 天） |
| `auth.admin-token-expire-seconds` | 管理端 Token 有效期 | `86400`（1 天） |

**`application-local.yml`**

| 配置项 | 说明 |
|--------|------|
| `wechat.app-id` | 微信小程序 AppID |
| `wechat.app-secret` | AppSecret |
| `wechat.mock-enabled` | 本地 mock 开关 |
| `wechat.mock-openid` | mock 用 openid |
| `cors.allowed-origins` | 管理端本地地址，如 `http://localhost:5173` |

### 7.3 常量配置

免责声明等固定文案放在 `common.constant.AppConstants` 中，避免硬编码分散。

---

## 8. 包结构命名建议

根包：`com.psych.miniapp`

```
com.psych.miniapp
├── PsychMiniappApplication
│
├── config
│   ├── WebMvcConfig              # CORS、拦截器注册
│   ├── MybatisConfig             # 若用 MyBatis
│   └── JacksonConfig             # 日期格式等
│
├── common
│   ├── constant
│   │   └── AppConstants          # 免责声明、状态枚举值
│   ├── enums
│   │   ├── QuizStatus
│   │   └── QuestionType
│   ├── response
│   │   ├── Result
│   │   └── PageResult
│   ├── exception
│   │   ├── BizException
│   │   ├── NotFoundException
│   │   └── ...
│   └── handler
│       └── GlobalExceptionHandler
│
├── auth
│   ├── controller
│   │   ├── AuthController
│   │   └── AdminAuthController
│   ├── service
│   │   ├── AuthService
│   │   ├── AdminAuthService
│   │   └── TokenService
│   ├── interceptor
│   │   └── AuthInterceptor
│   ├── context
│   │   ├── UserContext
│   │   └── AdminContext
│   ├── entity
│   │   ├── User
│   │   └── AdminUser
│   ├── repository
│   │   ├── UserRepository
│   │   └── AdminUserRepository
│   └── dto
│       ├── WechatLoginRequest
│       └── LoginResponse
│
├── quiz
│   ├── controller
│   │   ├── QuizController          # C 端
│   │   └── AdminQuizController     # 管理端
│   ├── service
│   │   ├── QuizService
│   │   ├── QuestionService
│   │   ├── OptionService
│   │   └── ResultRuleService
│   ├── entity
│   │   ├── Quiz
│   │   ├── Question
│   │   ├── Option
│   │   └── ResultRule
│   ├── repository
│   │   └── ...
│   ├── dto
│   │   └── ...
│   └── converter
│       └── QuizConverter
│
├── attempt
│   ├── controller
│   │   └── AttemptController
│   ├── service
│   │   ├── AttemptService          # 提交、历史查询
│   │   ├── ScoringService          # 总分计分、规则匹配（仅提交时调用）
│   │   └── model
│   │       ├── ScoringInput        # Service 内部传输对象
│   │       └── ScoringResult
│   ├── entity
│   │   ├── TestAttempt
│   │   └── Answer
│   ├── repository
│   │   ├── TestAttemptRepository
│   │   └── AnswerRepository
│   ├── dto
│   │   ├── SubmitAttemptRequest
│   │   ├── AttemptResultResponse
│   │   └── ...
│   └── converter
│       └── AttemptConverter
│
└── admin                              # 可选：管理端专属组合逻辑
    └── validator
        └── QuizPublishValidator       # 上架校验
```

### 8.1 命名约定

| 类型 | 命名规则 | 示例 |
|------|----------|------|
| Controller | `{Domain}Controller` / `Admin{Domain}Controller` | `QuizController` |
| Service | `{Domain}Service` | `AttemptService` |
| Repository | `{Entity}Repository` | `TestAttemptRepository` |
| Entity | 单数名词 | `TestAttempt` |
| Request DTO | `{Action}{Domain}Request` | `SubmitAttemptRequest` |
| Response DTO | `{Domain}Response` | `AttemptResultResponse` |
| Converter | `{Domain}Converter` | `AttemptConverter` |

### 8.2 核心类职责补充

**`ScoringService`**
- 仅被 `AttemptService.submit()` 调用
- 入参：`ScoringInput`（quiz、questions、optionMap、answers）
- 出参：`ScoringResult`（totalScore、matchedRule、answerRecords）
- 规则匹配：`result_rule` 闭区间唯一命中，否则 `42201`
- 不被历史查询路径调用

**`AttemptService`**
- `submit(SubmitAttemptRequest, userId)` → `@Transactional`：校验 → `ScoringService` → 写 `test_attempt` + `answer` → 返回 `AttemptResultResponse`
- `listByUser(userId)` → 直读 `test_attempt` 快照，返回 `List<AttemptListItemResponse>`（不调用 `ScoringService`）
- `getDetail(attemptId, userId)` → 直读 `test_attempt` + `answer` 快照，关联 `question`/`option` 取展示文案（不调用 `ScoringService`）

**`QuizService`（M4 扩展）**
- `requirePublishedQuiz(quizId)` → 已上架且未软删，否则 `40401`（`AttemptService` 与拉题共用）
- `getPublishedQuestions(quizId)` → 题目 + 选项（不含 score）

**`QuizPublishValidator`**
- 校验题目数、选项数、规则区间覆盖与重叠
- 上架接口调用（待 M6）

### 8.3 M4 已落地调用链

#### GET `/api/quizzes/{quizId}/questions`

```
QuizController.questions(quizId)
  └─ QuizService.getPublishedQuestions(quizId)
       ├─ requirePublishedQuiz(quizId)          → QuizMapper.selectById
       ├─ QuestionMapper.selectList             → quiz_id, ORDER BY sort_order
       ├─ OptionMapper.selectList               → question_id IN (...), ORDER BY sort_order
       └─ QuizConverter.toQuestionsResponse     → 选项映射丢弃 score
```

#### POST `/api/attempts`

```
AttemptController.submit(@Valid SubmitAttemptRequest)
  │  userId = 1L（M4 固定测试用户，待鉴权接入后改为 UserContext）
  └─ AttemptService.submit(request, userId)     → @Transactional
       ├─ QuizService.requirePublishedQuiz
       ├─ QuestionMapper.selectList             → 完整性校验（42201）
       ├─ OptionMapper.selectBatchIds           → 选项归属校验（42201）
       ├─ ScoringService.scoreAndMatch(ScoringInput)
       │    ├─ totalScore = Σ option.score
       │    └─ ResultRuleMapper.selectList      → 闭区间唯一匹配（42201）
       ├─ TestAttemptMapper.insert              → 快照含 resultSuggestion
       ├─ AnswerMapper.insert × N               → 同事务
       └─ AttemptConverter.toResultResponse     → 填充 disclaimer
```

#### M4 事务边界（已实现）

| 步骤 | 操作 | 失败 |
|------|------|------|
| 1–5 | 只读查询 + 内存计分/匹配 | 抛 `BizException`，无写库 |
| 6 | `INSERT test_attempt` | 整体回滚 |
| 7 | `INSERT answer` × 题目数 | 整体回滚 |

#### GET `/api/attempts`（M5 已实现）

```
AttemptController.list()
  │  userId = 1L
  └─ AttemptService.listByUser(userId)
       ├─ TestAttemptMapper.selectList     → user_id = ?, ORDER BY completed_at DESC
       └─ AttemptConverter.toListItemResponse(each)
```

#### GET `/api/attempts/{attemptId}`（M5 已实现）

```
AttemptController.detail(attemptId)
  │  userId = 1L
  └─ AttemptService.getDetail(attemptId, userId)
       ├─ TestAttemptMapper.selectById       → 不存在或非本人 → 40401
       ├─ AnswerMapper.selectList          → attempt_id = ?
       ├─ QuestionMapper.selectBatchIds    → 仅取 content / sortOrder（展示）
       ├─ OptionMapper.selectBatchIds      → 仅取 content（展示）
       └─ AttemptConverter.toDetailResponse
            ├─ 结果区：test_attempt 快照
            └─ answers：score 来自 answer；文案来自 question/option 当前值
```

> 历史查询路径**禁止**调用 `ScoringService`、`ResultRuleMapper`。

#### M4/M5 临时联调约定

| 项 | 当前实现 | 目标（后续） |
|----|----------|--------------|
| 用户身份 | `AttemptController` 常量 `TEST_USER_ID = 1L` | `UserContext` + C 端 Token |
| 鉴权 | 拉题/提交/历史接口无拦截器 | `AuthInterceptor` 校验 Bearer Token |
| 归属错误 | 不存在或非本人均 `40401` | 接入鉴权后可区分 `40301` |
| 测试数据 | `seed.sql`：`user.id=1`；历史由 `POST /api/attempts` 生成 | 微信登录自动创建用户 |

---

## 9. 技术选型说明

| 项目 | 选型 | 说明 |
|------|------|------|
| 框架 | Spring Boot 3 | 与 Java 17 配套 |
| 构建 | Maven | 单模块即可 |
| 数据库 | MySQL 8 | 本地开发 |
| 持久层 | MyBatis-Plus | 已选用；`BaseMapper` + `LambdaQueryWrapper`，M4 无 XML |
| 密码哈希 | BCrypt | `spring-security-crypto` 即可，不引入完整 Security 链 |
| JSON | Jackson | Spring Boot 默认 |
| 校验 | Jakarta Validation | `@Valid`、`@NotNull` 等 |

---

## 10. 关联文档

| 文档 | 关系 |
|------|------|
| `requirements.md` | 功能需求来源 |
| `schema.md` | Entity 字段依据 |
| `api.md` | Controller 与 DTO 依据 |
| `milestone.md` | 实施顺序 |
