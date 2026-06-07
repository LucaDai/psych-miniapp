# 设计文档一致性审查报告

> 版本：v1.0  
> 审查日期：2026-06-06  
> 审查范围：`requirements.md`、`schema.md`、`api.md`、`architecture.md`、`milestone.md`  
> 参考遗留：`prd.md`（未纳入正式范围，仅标注）

---

## 1. 审查结论摘要

| 维度 | 结论 |
|------|------|
| 核心业务约束（5 项重点） | **基本一致**，无阻断性冲突 |
| 字段与 API 映射 | **基本一致**，快照字段对齐 |
| API 与 Schema | **基本一致**，1 处历史展示风险需实现时注意 |
| Architecture 与 Schema | **基本一致**，持久层命名与选型需更新 |
| Milestone 与需求 | **部分偏离**，M2 范围与用户最新执行指令不一致 |

**总体评价：** 五份主文档在 MVP 核心决策上已对齐，可以进入 M2 开发。建议在写代码前修复 **3 个中优先级文档问题**，其余为低优先级或实现期注意事项。

---

## 2. 重点约束核对（5 项）

| 约束 | requirements | schema | api | architecture | milestone | 结论 |
|------|-------------|--------|-----|--------------|-----------|------|
| 不使用 JWT | §1.2 明确排除 | — | §1.3 opaque token | §6.1 明确 | M2/MVP DoD 明确 | ✅ 一致 |
| 历史结果快照 | §5.2、§3.6 HI-02 | §3.7 快照字段 | §1.7、§2.7 禁止重算 | §4.5、§8.2 AttemptService | M4/M5 验收 | ✅ 一致 |
| quiz 软删除 `deleted_at` | §5.1、§4.2 QZ-04 | §3.3 `deleted_at` | §2.2、§3.6 | §2.5、§3.1 Quiz.deletedAt | M1/M3 验收 | ✅ 一致 |
| 用户仅 openid | §1.2、§3.1 U-02 | §3.1 仅 id/openid | 登录换 token | User 实体无昵称 | — | ✅ 一致 |
| 总分区间计分 | §4.4 RR-01 | §3.6 区间规则 | §2.5 累加分值 | ScoringService 总分 | M4 验收 | ✅ 一致 |

---

## 3. 文档冲突清单

### 3.1 【高】持久层技术选型不一致

| 文档 | 描述 |
|------|------|
| `architecture.md` §1、§9 | 「MyBatis 或 Spring Data JPA，择一，MVP 推荐 JPA」 |
| `milestone.md` M2 | 「MyBatis 或 JPA，择一」 |
| **已确认实施** | **MyBatis Plus**（用户 M2 指令） |

**影响：** 包结构、配置类命名、数据访问层叫法（Repository vs Mapper）会与 architecture 不符。

**修改建议：**
- 更新 `architecture.md` §9 为固定 **MyBatis Plus**
- 将 §2.3、§8 中 `*Repository` 统一改为 `*Mapper`（继承 `BaseMapper`）
- 将 `MybatisConfig` 改为 `MybatisPlusConfig`，补充分页插件说明
- 同步更新 `milestone.md` M2 任务描述

---

### 3.2 【中】M2 里程碑范围 vs 最新执行指令

| 文档 | M2 范围 |
|------|---------|
| `milestone.md` M2 | 含微信登录、管理端登录、简单 Token 鉴权、CORS、mock |
| **用户最新 M2 指令** | **暂不实现** Controller、Service 业务、微信登录、Token 鉴权、CRUD；仅骨架 + Entity + Mapper + 能 `mvn spring-boot:run` |

**影响：** 若严格按 `milestone.md` 做 M2，会超出用户当前要求；若按用户指令做，M2 验收标准需临时调整。

**修改建议：**
- 将 `milestone.md` M2 拆为两阶段：
  - **M2a（当前）**：项目骨架、配置、Entity、Mapper、Result、异常处理，可启动
  - **M2b（后续）**：登录、Token 鉴权、CORS、微信 mock
- 或在 M2 任务清单中标注「本阶段跳过」项，避免与执行指令冲突

---

### 3.3 【中】软删除时是否同步修改 `status`

| 文档 | 描述 |
|------|------|
| `architecture.md` §2.5 | 软删除事务：「写 `deleted_at` + 改 `status`」 |
| `requirements.md` QZ-04 | 仅写「写入 `deleted_at`」，未要求改 status |
| `api.md` §3.6 | 返回 `deletedAt`，未说明是否改 status |
| C 端过滤 | `published` 且 `deleted_at IS NULL`（api §2.2） |

**影响：** 仅写 `deleted_at` 已足够使 C 端不可见；额外改 `status` 为实现细节，文档未统一。

**修改建议（二选一）：**
- **方案 A（推荐）：** 软删除只写 `deleted_at`，管理端以 `deletedAt != null` 标识「已删除」；更新 `architecture.md` §2.5
- **方案 B：** 软删除同时置 `status = archived`；在 `requirements.md` QZ-04 和 `api.md` §3.6 补充说明

---

### 3.4 【中】Java 枚举 vs 数据库/API 字面值

| 文档 | 题型 | 测试状态 |
|------|------|----------|
| `architecture.md` §8 | `QuestionType.SINGLE_CHOICE` | `QuizStatus.DRAFT` 等（大写） |
| `schema.md` | `single_choice`（小写 snake） | `draft` / `published` / `archived` |
| `api.md` | 返回 `"single_choice"` | 返回 `"draft"` 等 |

**影响：** 实现时需做 enum ↔ DB 字符串转换，文档未约定映射规则。

**修改建议：**
- 在 `architecture.md` §3.1 或 `schema.md` §3.4 增加约定：「DB/API 存小写 snake；Java enum 名大写，持久化值与 DB 一致」
- 或 Entity 直接用 `String` 存 status/type（MVP 更简单）

---

### 3.5 【低】`prd.md` 与 `requirements.md` 并存

| 状况 |
|------|
| `prd.md` 为 v1.0，内容与 `requirements.md` v1.1 高度重叠 |
| 其他文档已引用 `requirements.md`，未引用 `prd.md` |

**修改建议：**
- 在 `prd.md` 顶部标注「已废弃，见 requirements.md」，或删除 `prd.md`，避免后续误读

---

### 3.6 【低】MVP 完成定义措辞歧义

| 文档 | 内容 |
|------|------|
| `milestone.md` DoD | 「约束：无删除、**无草稿**、无昵称头像」 |

**问题：** 「无草稿」易被理解为测试不能有 `draft` 状态，与 `quiz.status = draft` 及需求矛盾。

**修改建议：** 改为「无答题草稿（中途退出丢弃）」

---

### 3.7 【低】`api.md` 管理端 CRUD 章节缩水

| 状况 |
|------|
| §3.9–§3.17 写「与 v1.0 一致」，缺少完整请求/响应示例 |

**影响：** 不构成逻辑冲突，但 M6 开发时需回看旧版或补全。

**修改建议：** M6 前将 §3.9–§3.17 恢复为完整接口定义

---

### 3.8 【低】`architecture.md` 版本滞后

| 状况 |
|------|
| `architecture.md` 标注 v1.0，其余主文档为 v1.1 |
| 未反映 `POST /api/attempts`、MyBatis Plus 等 v1.1 决策 |

**修改建议：** 升版至 v1.1，同步 API 路径与持久层选型

---

## 4. 字段一致性核对

### 4.1 `quiz` 表

| 字段 | schema | api 响应 | architecture Entity | requirements | 结论 |
|------|--------|----------|----------------------|--------------|------|
| cover_image_url | ✅ | coverImageUrl | coverImageUrl | ✅ | ✅ |
| deleted_at | ✅ | deletedAt（管理端） | deletedAt | ✅ | ✅ |
| question_count | ✅ | questionCount | questionCount | ✅ | ✅ |
| estimated_minutes | ✅ | estimatedMinutes | estimatedMinutes | ✅ | ✅ |
| sort_order | ✅ | sortOrder（管理端） | sortOrder | ✅ | ✅ |
| status | draft/published/archived | 同左 | status/enum | ✅ | ✅ |

### 4.2 `test_attempt` 快照

| 字段 | schema | api AttemptResult | api 历史列表 | api 历史详情 | architecture | 结论 |
|------|--------|-------------------|-------------|-------------|--------------|------|
| quiz_title | ✅ | quizTitle | quizTitle | quizTitle | quizTitle | ✅ |
| result_title | ✅ | resultTitle | resultTitle | resultTitle | resultTitle | ✅ |
| result_description | ✅ | resultDescription | — | resultDescription | resultDescription | ✅ |
| total_score | ✅ | totalScore | totalScore | totalScore | totalScore | ✅ |
| result_rule_id | ✅ | 未暴露 API | — | — | resultRuleId | ✅ 仅追溯 |

### 4.3 `user` 表

| 字段 | schema | requirements | 结论 |
|------|--------|--------------|------|
| openid | ✅ | ✅ 唯一标识 | ✅ |
| nickname/avatar | 无 | 明确不存 | ✅ |

**说明：** requirements U-03 写「按 openid 关联」，schema 用 `user_id` 外键。此为合理实现映射（登录后 openid → user.id），**非冲突**。

### 4.4 `answer` 表

| 字段 | schema | api 历史详情 | 结论 |
|------|--------|-------------|------|
| score | ✅ 提交时写入 | answers[].score 直读 | ✅ |
| questionContent | — | 关联 question.content | ⚠️ 见 §5.3 |

---

## 5. API 与 Schema 交叉验证

### 5.1 提交接口

| 项 | api | schema | 结论 |
|----|-----|--------|------|
| 路径 | `POST /api/attempts` | — | ✅ 与 requirements A-05 一致 |
| 请求含 quizId | ✅ | test_attempt.quiz_id | ✅ |
| 计分 | 累加 option.score | option.score | ✅ 总分区间 |
| 快照写入 | quiz_title/result_title/result_description | test_attempt 字段 | ✅ |
| 响应 | AttemptResult 直出 | 从 test_attempt 组装 | ✅ |

### 5.2 历史接口

| 项 | api | schema | 结论 |
|----|-----|--------|------|
| 列表字段来源 | test_attempt 快照 | 同 | ✅ |
| 详情禁止重算 | §2.7 明确 | §3.7 明确 | ✅ |
| 无删除接口 | 无 | 无 | ✅ |

### 5.3 【风险】历史详情关联题干/选项文本

| 项 | 说明 |
|----|------|
| api §2.7 | `questionContent`、`optionContent` 来自关联 `question` / `option` 表 |
| schema §3.8 | 「MVP 采用关联查询」 |
| 风险 | 题目/选项被管理端删除或修改后，历史详情的题干/选项文案可能变化或缺失 |

**判定：** 与快照策略**不完全矛盾**（结果标题/描述/总分仍是快照），但**答题明细展示**未快照。

**修改建议（可选，不阻断 M2）：**
- MVP 维持关联查询，在 `requirements.md` 或 `api.md` 注明「答题明细文案以当前题目/选项为准，结果解读以快照为准」
- 二期可将 questionContent/optionContent 写入 `answer` 快照字段

### 5.4 C 端测试可见性

| 条件 | requirements | api | schema | 结论 |
|------|-------------|-----|--------|------|
| 上架 | published | status=published | published | ✅ |
| 未软删 | deleted_at IS NULL | 同 | deleted_at NULL | ✅ |

### 5.5 认证

| 项 | 各文档 | 结论 |
|----|--------|------|
| Token 类型 | opaque，非 JWT | ✅ |
| 存储 | 服务端内存 | ✅ |
| C/管理端分离 | 是 | ✅ |
| Header | Authorization: Bearer | ✅ |

---

## 6. Architecture 与 Schema 交叉验证

| 项 | architecture | schema | 结论 |
|----|--------------|--------|------|
| 8 张表 / 8 Entity | ✅ | ✅ | ✅ |
| Entity 分包 | auth/quiz/attempt | — | ✅ |
| TestAttempt 快照字段 | §3.2 完整 | §3.7 完整 | ✅ |
| Quiz 软删除字段 | deletedAt | deleted_at | ✅ |
| ScoringService 仅提交时调用 | §8.2 | §3.7 历史不重算 | ✅ |
| 事务：提交答案 | POST /api/attempts | test_attempt + answer | ✅ |
| 持久层 | Repository（JPA/MyBatis） | — | ❌ 需改为 Mapper + MyBatis Plus |

---

## 7. Milestone 与需求符合度

| Milestone | 符合需求 | 问题 |
|-----------|----------|------|
| M1 | ✅ | 字段与 schema v1.1 对齐 |
| M2 | ⚠️ | 文档含登录/鉴权，与用户当前「仅骨架」指令冲突（见 §3.2） |
| M3 | ✅ | 首页、详情、软删除、coverImageUrl |
| M4 | ✅ | POST /api/attempts、一题一屏、快照、总分计分 |
| M5 | ✅ | 历史快照、不可删除 |
| M6 | ✅ | 单管理员、详情页内管理、3 个测试 |

**M2 与已确认技术栈：**
- `milestone.md` 写「MyBatis 或 JPA」→ 应改为 **MyBatis Plus**（与用户指令一致）

---

## 8. 修改建议优先级汇总

### 必须在 M2 写代码前明确（中优先级）

| # | 问题 | 建议 |
|---|------|------|
| 1 | 持久层选型 | 以 **MyBatis Plus** 为准，更新 architecture + milestone |
| 2 | M2 范围 | 拆分 M2a/M2b，或标注登录/鉴权延后 |
| 3 | 软删除是否改 status | 选定方案 A 或 B 并同步三份文档 |

### 建议在 M3 前完成（低优先级）

| # | 问题 | 建议 |
|---|------|------|
| 4 | enum 与 DB 字面值 | 补充映射约定 |
| 5 | prd.md 冗余 | 废弃或删除 |
| 6 | milestone DoD「无草稿」 | 改为「无答题草稿」 |
| 7 | architecture 升版 v1.1 | 同步 API 路径与 Mapper 命名 |

### 实现期注意（可不改文档）

| # | 问题 | 建议 |
|---|------|------|
| 8 | 历史详情题干关联查询 | 实现时处理题目删除后的展示降级 |
| 9 | `option` 表保留字 | Entity 使用 `@TableName("`option`")` |
| 10 | 服务重启 Token 失效 | milestone 已记录，MVP 可接受 |

---

## 9. 可开始 M2 的判定

| 条件 | 状态 |
|------|------|
| 核心 MVP 约束五件套无冲突 | ✅ |
| 快照字段 API/Schema 对齐 | ✅ |
| Entity 字段可从 schema 直接生成 | ✅ |
| 持久层按 MyBatis Plus 执行（文档待补） | ✅ 以用户指令为准 |
| M1 数据库已就绪 | ⚠️ 依赖本地 MySQL（启动前提） |

**结论：可以开始 M2 代码生成。** 建议采用用户已确认的 **MyBatis Plus + 骨架优先（不含登录/鉴权）** 方案；文档修补可与 M2 并行或紧随其后。

---

## 10. 审查文件版本快照

| 文件 | 版本 | 状态 |
|------|------|------|
| requirements.md | v1.1 | 主需求文档 |
| schema.md | v1.1 | 一致 |
| api.md | v1.1 | 一致 |
| architecture.md | v1.0 | 需升版 |
| milestone.md | v1.1 | M2 范围需调整 |
| prd.md | v1.0 | 建议废弃 |
