# TechHub 技术社区论坛 —— 技术方案文档

> **版本**: v1.0
> **日期**: 2026-05-25
> **项目**: TechHub 技术社区论坛
> **关联文档**:
> - [综合课程设计方案](./TechHub%20技术社区论坛%20——%20综合课程设计方案.md)
> - [后端开发文档](./TechHub%20后端开发文档.md)
> - [API 开发文档](./TechHub%20API开发文档.md)
> - [前端页面设计规范](./TechHub%20前端页面设计规范.md)
> - [前端开发文档](./TechHub%20前端开发文档.md)

---

## 目录

1. [项目背景与目标](#1-项目背景与目标)
2. [技术选型](#2-技术选型)
3. [系统架构设计](#3-系统架构设计)
4. [数据库设计概要](#4-数据库设计概要)
5. [接口设计概述](#5-接口设计概述)
6. [核心模块详细设计](#6-核心模块详细设计)
7. [安全设计](#7-安全设计)
8. [性能优化策略](#8-性能优化策略)
9. [开发与部署方案](#9-开发与部署方案)
10. [风险评估与应对](#10-风险评估与应对)

---

## 1. 项目背景与目标

### 1.1 项目定位

TechHub 是一个**前后端分离的轻量级技术社区论坛**，旨在为技术爱好者提供内容创作、交流互动、知识管理的综合平台。本系统用于系统整合《数据库原理与应用》、《Java程序设计（Spring Boot）》、《Web程序设计（Vue 3）》三门课程的核心知识与工程实践，覆盖从需求分析、数据库建模到全栈开发与部署的完整工程流程。

### 1.2 核心目标

- 实现完整的用户、内容、互动、通知、管理功能闭环
- 引入**个性化推荐算法**提升内容分发效率
- 构建**神评（精华评论）机制**激励高质量互动
- 集成基于帖子原文的**私有 AI 总结与问答**功能
- 实现**帖子草稿自动保存**，提升写作体验
- 实现**帖子可见权限控制**（公开 / 登录可见 / 粉丝可见 / 私密四级）
- 新增**版块公告管理**，支持管理员 / 版主发布版块须知与技术活动通知
- 严格执行安全规范（密码加密、防 XSS、统一响应）
- 践行数据库规范（InnoDB、utf8mb4、雪花主键）

### 1.3 系统角色

| 角色 | 英文标识 | 核心权限 |
|------|----------|----------|
| 游客 | GUEST | 浏览公开帖子与公告，不可互动，不可使用 AI |
| 普通用户 | USER | 发帖、回帖、点赞、收藏、关注、AI 总结与问答、草稿保存、推荐神评（注册 ≥ 7 天） |
| 版主 | MODERATOR | 管理指定版块（加精、置顶、锁定、删除），管理管辖版块公告，撤销神评 |
| 管理员 | ADMIN | 全局用户管理、版块管理、系统配置、数据统计、神评强制干预、全站公告管理 |

- **管理员内容管理**：管理员可编辑和删除任意用户的帖子和评论，操作记录通过 `update_time` 字段追踪。实现代码在 `PostServiceImpl` 中通过 `isAdmin()` 短路求值豁免作者身份校验，`CommentServiceImpl` 采用相同模式。

### 1.4 非功能性需求

| 指标 | 目标值 | 说明 |
|------|--------|------|
| 接口响应时间 | P95 < 500ms | 普通 CRUD 接口 |
| AI 接口超时 | < 30s | 异步处理，前端轮询 |
| 并发支持 | ≥ 200 QPS | 基于 HikariCP + Redis 缓存 |
| 可用性 | 99.5% | 单点部署，允许计划内维护 |
| 数据持久性 | 每日备份 | MySQL dump + 文件系统备份 |
| 密码安全 | BCrypt（cost=10） | 不可逆哈希 |

---

## 2. 技术选型

### 2.1 技术栈总览

| 层级 | 技术 | 版本 | 选型理由 |
|------|------|------|----------|
| **前端框架** | Vue 3 + TypeScript | 3.5+ | 组合式 API，类型安全，生态成熟 |
| **构建工具** | Vite | 6.x | 极速 HMR，原生 ESM，生产构建优化 |
| **UI 组件库** | Element Plus | 2.x | Vue 3 原生支持，组件丰富，支持暗色主题 |
| **状态管理** | Pinia | 2.x | Vue 3 官方推荐，模块化，TypeScript 友好 |
| **路由** | Vue Router | 4.x | 动态路由懒加载，导航守卫权限控制 |
| **HTTP 客户端** | ofetch | 1.x | 基于 Fetch API，拦截器统一 Token 与错误处理 |
| **Markdown** | markdown-it + DOMPurify + highlight.js | — | 安全渲染，防 XSS，代码语法高亮 |
| **图表** | ECharts (vue-echarts) | 5.x | 管理后台数据可视化 |
| **后端框架** | Spring Boot | 3.5.x | 自动配置，内嵌服务器，生态完善 |
| **安全框架** | Spring Security + JWT + BCrypt | — | 无状态认证，密码强哈希，方法级权限控制 |
| **ORM** | MyBatis-Plus | 3.5.9 | 雪花主键生成，条件构造器，分页插件，逻辑删除 |
| **数据库** | MySQL | 8.0 | InnoDB 引擎，utf8mb4 字符集，ACID 事务 |
| **缓存** | Redis | 7.x | 热点数据缓存，推荐数据加速，高频查询优化 |
| **AI 集成** | OpenAI / 国产大模型 API | — | 可配置切换，异步调用，超时兜底 |
| **中文分词** | jieba-analysis / ansj_seg | — | 帖子内容分词，TF-IDF 关键词提取 |
| **文件存储** | dromara/x-file-storage | 2.2.0 | 统一文件上传 API，本地存储，可切换 OSS |
| **API 文档** | Knife4j (springdoc-openapi) | 4.x | 在线调试，增强 Swagger UI |
| **运行环境** | JDK 17+ / Node.js ^20.19.0 \|\| >=22.12.0 | — | LTS 版本保障稳定性 |

### 2.2 关键选型决策

**为什么选择 MyBatis-Plus 而非 JPA？**
- 雪花算法主键内置支持，无需额外配置
- 条件构造器对复杂动态查询（如可见性过滤）的支持远优于 JPA Criteria API
- MyBatis-Plus 分页插件与逻辑删除对论坛场景高度契合
- SQL 可控性强，便于性能调优

**为什么选择 ofetch 而非 Axios？**
- 基于原生 Fetch API，包体更小，无 XMLHttpRequest 历史包袱
- 拦截器 API 设计简洁，支持请求/响应双向拦截
- Node.js 与浏览器端统一 API，便于 SSR 扩展

**为什么选择暗色模式优先？**
- 技术社区用户群体习惯（IDE 终端环境）
- 减少眼部疲劳，提升长时间阅读体验
- Element Plus 2.x 原生支持暗色主题变量覆盖

---

## 3. 系统架构设计

### 3.1 总体架构

TechHub 采用经典的 **B/S 三层架构**，前端为 Vue 3 SPA，后端为 Spring Boot REST API，数据层为 MySQL + Redis。前后端通过 HTTPS 通信，采用 JWT 无状态认证。

```
┌─────────────────────────────────────────────────────────────┐
│                     Client (Browser)                         │
│              Vue 3 SPA — Vite + Element Plus                │
│     ┌─────────────────────────────────────────────────┐     │
│     │  Pinia Stores (user / notification / draft)     │     │
│     │  Vue Router 4 (导航守卫 + 懒加载)                │     │
│     │  ofetch HTTP Client (JWT Bearer Token)          │     │
│     └─────────────────────────────────────────────────┘     │
└────────────────────────┬────────────────────────────────────┘
                         │ HTTPS (RESTful JSON)
                         │ Authorization: Bearer <JWT>
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                   Nginx (Reverse Proxy)                      │
│          Static Assets (/assets/*) + API Proxy (/api/*)      │
│          TLS Termination  ·  Gzip  ·  Rate Limiting         │
└───────────┬──────────────────────────────────┬──────────────┘
            │ /api/v1/*                         │ /*
            ▼                                   ▼
┌───────────────────────┐           ┌───────────────────────┐
│   Spring Boot 3.5     │           │   Static Files        │
│   (Embedded Tomcat)   │           │   (Vite dist/)        │
│                       │           └───────────────────────┘
│  ┌──────────────────┐ │
│  │ Security Layer   │ │  JwtAuthenticationFilter
│  │ JWT + BCrypt     │ │  → SecurityFilterChain
│  └────────┬─────────┘ │  → @PreAuthorize (方法级)
│           │            │
│  ┌────────┴─────────┐ │
│  │ Controller Layer │ │  参数校验 @Valid
│  │ (REST Endpoints) │ │  统一响应 R<T>
│  └────────┬─────────┘ │
│           │            │
│  ┌────────┴─────────┐ │
│  │ Service Layer    │ │  业务逻辑 + 事务管理
│  │ (@Transactional) │ │  可见性过滤 + AI 调用
│  └────────┬─────────┘ │
│           │            │
│  ┌────────┴─────────┐ │
│  │ Mapper Layer     │ │  MyBatis-Plus BaseMapper
│  │ (Data Access)    │ │  条件构造器 + 分页插件
│  └────────┬─────────┘ │
└───────────┼───────────┘
            │
   ┌────────┴────────┐
   ▼                 ▼
┌──────────┐   ┌──────────┐   ┌──────────────┐
│ MySQL 8  │   │ Redis 7  │   │ Local Storage │
│ InnoDB   │   │ Cache    │   │ (File Uploads)│
│ utf8mb4  │   │ Session  │   │ /data/uploads │
└──────────┘   └──────────┘   └──────────────┘
```

### 3.2 后端分层架构

| 层 | 职责 | 关键技术 |
|----|------|----------|
| **Controller** | 接收 HTTP 请求，参数校验，调用 Service，返回 `R<T>` | `@RestController`, `@Valid`, `@PreAuthorize` |
| **Service** | 业务逻辑实现，事务管理，权限过滤，AI 调用 | `@Transactional`, `@Service` |
| **Mapper** | 数据访问，MyBatis-Plus CRUD | `BaseMapper<T>`, `LambdaQueryWrapper` |
| **Entity** | 数据库表映射，POJO 模型 | `@TableName`, `@TableId(type = ASSIGN_ID)` |

### 3.3 请求处理流程

```
Client Request
    │
    ▼
JwtAuthenticationFilter — 提取 Token → 解析用户身份 → 设置 SecurityContext
    │
    ▼
DispatcherServlet — 路由到 Controller
    │
    ▼
Controller — @Valid 参数校验 → 调用 Service
    │
    ▼
Service — 业务逻辑 → 权限判断 → 调用 Mapper
    │
    ▼
Mapper — MyBatis-Plus SQL → MySQL / Redis
    │
    ▼
Service → Controller → R<T> → Jackson 序列化 JSON
    │
    ▼
Client Response — { code, message, data }
```

### 3.4 前端组件架构

```
App.vue
├── AppHeader (全局导航 + 通知铃铛 + 用户菜单)
├── <RouterView>
│   ├── DefaultLayout.vue
│   │   ├── HomePage → PostList + NoticeCarousel
│   │   ├── CategoryPage → PostList + NoticeBanner
│   │   ├── PostDetailPage → MdViewer + DivineComments + AiPanel + Comments
│   │   ├── PostCreatePage → MdEditor + VisibilitySelector + DraftAutoSave
│   │   ├── UserProfilePage → UserInfo + PostTabs
│   │   ├── NotificationPage → NotificationList
│   │   └── DraftPage → DraftList
│   ├── AdminLayout.vue
│   │   ├── AppSidebar (管理菜单)
│   │   ├── DashboardPage → StatChart (ECharts)
│   │   ├── UserManagePage / PostManagePage / CategoryManagePage
│   │   ├── NoticeManagePage / DivineManagePage
│   └── AuthLayout.vue → LoginPage / RegisterPage
└── AppFooter
```

---

## 4. 数据库设计概要

### 4.1 设计规范（强制）

| 规范项 | 要求 |
|--------|------|
| 存储引擎 | 所有表强制使用 **InnoDB** |
| 字符集 | 统一 **utf8mb4**，排序规则 **utf8mb4_unicode_ci** |
| 主键 | 全部使用 **BIGINT**，应用层雪花算法生成 |
| 外键 | 合理定义以维护参照完整性，关键表设置 ON DELETE CASCADE |
| 时间字段 | 统一 `DATETIME`，默认 `CURRENT_TIMESTAMP` |
| 索引 | 为高频查询条件建立联合索引；唯一约束用于防重 |
| 范式 | 满足第三范式（3NF） |

### 4.2 核心实体关系

```
User ──< Post ──< Comment ──< CommentRecommend (神评推荐)
  │       │
  │       ├── PostDraft (草稿)
  │       ├── PostKeyword (分词关键词)
  │       └── PostSimilarity (帖子相似度)
  │
  ├── UserLike (点赞: POST / COMMENT)
  ├── Favorite (收藏)
  ├── Follow (关注)
  ├── Notification (通知: REPLY / LIKE / FOLLOW / DIVINE / SYSTEM)
  ├── UserProfile (用户画像: JSON 关键词权重)
  ├── AiSummary (AI 总结: (user_id, post_id) UNIQUE, 用户隔离)
  └── AiQaHistory (AI 问答历史: 用户隔离)

Category ──< Post
Category ──< CategoryNotice (版块公告: 须知 / 活动)

FileDetail (x-file-storage 文件记录)
```

### 4.3 核心表说明

| 表名 | 核心字段 | 设计要点 |
|------|----------|----------|
| `user` | id, username, password(BCrypt), role, status | 唯一约束 username/email |
| `post` | id, title, content(TEXT), visibility(0-3), type(0-2), status(0-2), deleted(逻辑删除) | 索引 (category_id, create_time)；可见性由 PostVisibilityService 动态过滤 |
| `comment` | id, content, post_id, user_id, parent_id, is_divine, recommend_count, like_count | 神评双维度判定（点赞 ≥ 10 且推荐 ≥ 5） |
| `comment_recommend` | comment_id, user_id | 唯一约束防重复推荐 |
| `post_draft` | user_id, post_id(NULLABLE), title, content(LONGTEXT) | (user_id, post_id) UNIQUE；Upsert 逻辑 |
| `ai_summary` | user_id, post_id, content, status(0/1/2) | (user_id, post_id) UNIQUE；用户级数据隔离 |
| `category_notice` | category_id, title, content, type(0/1), is_pinned | 索引 (category_id, status, is_pinned, create_time) |
| `user_profile` | user_id, keyword_weights(JSON) | 推荐系统用户画像 |
| `post_keyword` | post_id, keyword, tfidf_weight | 推荐系统关键词矩阵 |

完整 DDL 详见[《后端开发文档》第5节](./TechHub%20后端开发文档.md#5-数据库实现)。

---

## 5. 接口设计概述

### 5.1 设计原则

- **RESTful 风格**：资源 URL 使用名词复数，HTTP 方法语义化（GET/POST/PATCH/DELETE）
- **全局前缀**：`/api/v1`
- **统一响应体**：`{ code, message, data }`，code 与 HTTP 状态码一致
- **分页规范**：查询参数 `page`（默认 1）/ `size`（默认 10 或 20），响应含 `records`/`total`/`pages`
- **排序规范**：`sort=new`（默认，时间倒序）/ `sort=hot`（热度倒序）
- **雪花 ID 序列化**：BIGINT → JSON String，避免 JavaScript 精度丢失

### 5.2 认证流程

```
POST /api/v1/auth/register  → 创建用户（BCrypt 加密密码）
POST /api/v1/auth/login     → 返回 JWT Token (24h 有效期)
                               {
                                 "token": "eyJ...",
                                 "userId": "123...",
                                 "username": "...",
                                 "role": "USER"
                               }
后续请求 Header: Authorization: Bearer <token>
```

### 5.3 接口分组

| 分组 | 端点前缀 | 认证要求 | 说明 |
|------|----------|----------|------|
| 认证 | `/auth` | 无需登录 | 注册、登录 |
| 用户 | `/users` | 部分公开 | 个人资料、关注、用户帖子列表 |
| 版块 | `/categories` | 管理员写 | CRUD，版块公告 |
| 帖子 | `/posts` | 动态过滤 | CRUD + 可见性校验 + 点赞/收藏 |
| 回复与神评 | `/comments` | 需要登录 | CRUD + 推荐/取消推荐 |
| 互动 | `/posts/{id}/likes` 等 | 需要登录 | 点赞、收藏、关注 |
| 通知 | `/notifications` | 需要登录 | 列表、已读、未读数 |
| 推荐 | `/recommendations` | 需要登录 | 个性化推荐、"猜你喜欢" |
| AI | `/posts/{id}/ai` | 需要登录 | 总结生成、问答（私有数据） |
| 草稿 | `/drafts` | 需要登录 | Upsert、检查、恢复、删除 |
| 管理后台 | `/admin` | ADMIN/MODERATOR | 用户管理、帖子管理、数据统计 |
| 文件 | `/files` | 需要登录 | 图片上传（头像、帖子内嵌） |

完整 API 规范（含请求/响应示例和错误码）详见[《API 开发文档》](./TechHub%20API开发文档.md)。

---

## 6. 核心模块详细设计

### 6.1 用户认证与权限管理

采用 **Spring Security + JWT + BCrypt** 的无状态认证方案：

1. **注册**：BCrypt（cost=10）加密密码存储，用户名/邮箱唯一性校验
2. **登录**：验证凭据 → 签发 JWT（含 userId/username/role，24h 过期）
3. **请求认证**：`JwtAuthenticationFilter` 从 Authorization Header 提取 Token → 解析身份 → 设置 SecurityContext
4. **权限控制**：
   - **路由级**：SecurityFilterChain 配置公开/需登录/需角色的 URL 模式
   - **方法级**：`@PreAuthorize("hasRole('ADMIN')")` 细粒度控制
   - **前端按钮级**：`v-permission` 自定义指令基于用户角色显隐
   - **业务级**：PostVisibilityService 动态判断帖子是否可见

### 6.2 帖子可见权限系统（核心机制）

这是 TechHub **最具架构意义的特性**。帖子通过 `visibility` 字段控制四级可见性：

| visibility | 名称 | 谁可见 | 前端图标 |
|------------|------|--------|----------|
| 0 | 公开 | 所有人（含游客） | 👁 View |
| 1 | 登录可见 | 所有已登录用户 | 🔒 Lock |
| 2 | 粉丝可见 | 帖子作者的粉丝 | 👤 User |
| 3 | 私密 | 仅帖子作者本人 | ⭐ Star |

**后端过滤逻辑（PostVisibilityService）**：
```
if (未登录) → 仅返回 visibility = 0
if (已登录 && 是管理员) → 不过滤（查看所有）
if (已登录 && 是作者) → 全部可见
if (已登录 && visibility = 1) → 可见
if (已登录 && visibility = 2 && 是作者粉丝) → 可见
if (已登录 && visibility = 3) → 仅作者可见
```

**安全策略**：无权限帖子返回 **404 而非 403**，避免信息泄露（攻击者无法通过错误码判断帖子是否存在）。

### 6.3 草稿自动保存机制

采用**前端定时触发 + 后端 Upsert** 模式：

```
┌──────────────────┐         ┌──────────────────┐
│   Vue Frontend   │         │  Spring Backend  │
│                  │         │                  │
│ onMounted()      │         │                  │
│   → checkDraft() ├────────► GET /drafts/check │
│   ← 提示恢复      │◄────────┤ 返回草稿 or null  │
│                  │         │                  │
│ startAutoSave()  │         │                  │
│   (30s interval) │         │                  │
│   → saveDraft()  ├────────► POST /drafts      │
│                  │  Upsert │ (userId, postId) │
│                  │         │ UNIQUE constraint│
│   ← 更新时间      │◄────────┤                  │
│                  │         │                  │
│ handleSubmit()   │         │                  │
│   → createPost() ├────────► POST /posts       │
│                  │         │ → 自动删除草稿    │
│   ← 跳转详情页    │◄────────┤                  │
└──────────────────┘         └──────────────────┘
```

- 新帖草稿 `postId` 为 NULL，每用户仅一条（业务层控制）
- 编辑已有帖子时 `(userId, postId)` 唯一确定一条草稿
- 离开页面（`onUnmounted`）自动保存一次
- 发布成功后后端自动清理对应草稿

### 6.4 神评（社区治理）机制

神评是通过社区投票机制将高质量评论提升至帖子醒目位置的治理工具。

**触发条件与算法**：

```
1. 帖子资格: post.comment_count ≥ 10 → eligible_for_divine = 1
2. 用户门槛: 注册 ≥ 7 天
3. 双维度达标: like_count ≥ 10 && recommend_count ≥ 5
4. 自动加冕: 达标 → is_divine = 1, divine_time = NOW()
5. 动态撤销: 定时任务每 5 分钟检查 → 低于阈值 → is_divine = 0
6. 管理员干预: 可强制设置/撤销，不受阈值约束
```

**数据流**：用户推荐 → 写入 comment_recommend（唯一约束防重）→ 缓存计数更新 → 阈值检查 → 触发神评通知 → 帖子 divine_comment_count 更新。

### 6.5 AI 总结与问答

**核心原则**：数据完全私有，用户隔离，管理员无权访问用户 AI 数据。

```
┌────────────────────────────────────────────────────────┐
│                    AI 服务集成架构                       │
│                                                        │
│  用户 → POST /ai/summary                                │
│    │                                                   │
│    ├─ 内容长度检查 (≥ 50 chars)                          │
│    │   ├─ 不满足 → 422 "内容过短"                       │
│    │   └─ 满足 ↓                                       │
│    │                                                   │
│    ├─ Upsert ai_summary (status = 0 生成中)             │
│    │                                                   │
│    ├─ 异步调用 LLM API                                  │
│    │   ├─ Prompt: "基于以下帖子总结..."                  │
│    │   ├─ 超时 10s → 重试 2 次                         │
│    │   ├─ 成功 → status = 1, content 写入               │
│    │   └─ 失败 → status = 2, error_message 写入        │
│    │                                                   │
│  用户 → POST /ai/qa                                     │
│    ├─ 前置: 必须已有 status=1 的摘要（否则 403）         │
│    ├─ Prompt: 帖子原文 + 问题 → LLM                     │
│    ├─ 防注入: Prompt 限定"仅基于帖子原文回答"             │
│    └─ 无关问题: 统一回复 "请基于帖子内容提问"             │
│                                                        │
│  数据隔离: (user_id, post_id) UNIQUE                    │
│  管理员: 无权查看任何用户 AI 数据                         │
└────────────────────────────────────────────────────────┘
```

AI 对接设计要点详见[《后端开发文档》第 8.10 节](./TechHub%20后端开发文档.md#810-ai-辅助模块)。

### 6.6 个性化推荐系统

采用**混合推荐策略**，整合基于内容与协同过滤，解决冷启动问题。

```
┌──────────────────────────────────────────────────────┐
│                 推荐系统 Pipeline                      │
│                                                      │
│  离线计算（定时任务）                                   │
│  ┌─────────────────────────────────────────────┐     │
│  │ 帖子分词 (jieba) → TF-IDF 关键词 → post_keyword │     │
│  │ 用户互动记录 → 关键词累积 → user_profile (JSON)   │     │
│  │ 关键词矩阵 × 余弦相似度 → post_similarity 表     │     │
│  └─────────────────────────────────────────────┘     │
│                      ↓                                │
│  在线推荐（请求时）                                     │
│  ┌─────────────────────────────────────────────┐     │
│  │ 1. 读取 Redis 用户画像缓存 (TTL 30min)        │     │
│  │ 2. 未命中 → 从 user_profile 构建临时画像       │     │
│  │ 3. 画像匹配相似度矩阵 → 候选帖子集              │     │
│  │ 4. PostVisibilityService 过滤不可见帖子        │     │
│  │ 5. 去重 + 排序 → 分页返回                      │     │
│  │                                                │     │
│  │ 冷启动降级: 画像为空 → 热门帖子排行             │     │
│  └─────────────────────────────────────────────┘     │
└──────────────────────────────────────────────────────┘
```

相关帖子推荐（`GET /posts/{id}/related`）直接基于 `post_similarity` 表查询，按 `similarity_score` 降序。

### 6.7 版块公告系统

- 管理员和版主可发布公告（须知 / 活动两种类型）
- 置顶公告在版块顶部 `el-carousel` 轮播展示
- 非置顶公告折叠在列表中，点击"查看全部"展开
- 版主仅能管理管辖版块的公告，管理员全局管理

### 6.8 管理后台

采用 **AdminLayout** 独立布局（左侧菜单 + 右侧内容区），主要功能：

| 模块 | 功能 | 图表/组件 |
|------|------|-----------|
| 仪表盘 | 用户增长、每日发帖量、活跃度、推荐效果 | ECharts (LineChart, BarChart, PieChart) |
| 用户管理 | 搜索、封禁/解封、角色分配 | el-table + el-pagination |
| 帖子管理 | 全站帖子（无视可见性），设置精华/置顶/锁定 | el-table + 批量操作 |
| 版块管理 | CRUD，排序，启用/禁用 | el-form + el-table |
| 公告管理 | 全局公告管理 | el-table |
| 神评管理 | 推荐记录查看，强制撤销 | el-table + el-dialog |

### 6.9 文件上传

使用 **dromara/x-file-storage** 统一处理文件上传，配置基于本地文件系统，保留扩展至云存储（OSS/MinIO）的能力。

- 启动类添加 `@EnableFileStorage`
- 配置本地存储路径 `/data/uploads/`，访问域名 `http://localhost:8080/file/`
- 支持用户头像上传、帖子 Markdown 内嵌图片上传
- 格式限制：JPG / PNG / GIF / WebP，单文件 ≤ 5MB

### 6.10 通知系统

提供 5 种通知类型，自动触发，实时推送：

| 类型 | 枚举值 | 触发场景 |
|------|--------|----------|
| 回复通知 | REPLY | 有人回复了你的帖子或评论 |
| 点赞通知 | LIKE | 有人点赞了你的帖子或评论 |
| 关注通知 | FOLLOW | 有人关注了你 |
| 神评通知 | DIVINE | 你的评论被选为神评 |
| 系统通知 | SYSTEM | 系统公告或管理通知 |

前端实现 30 秒轮询未读数，通知铃铛 `el-badge` 显示，新通知到达时触发铃铛摆动动画（CSS keyframes），支持全部标记已读。

---

## 7. 安全设计

### 7.1 认证安全

| 措施 | 实现 |
|------|------|
| 密码加密 | BCrypt（cost=10），不可逆哈希，禁止明文存储 |
| Token 管理 | JWT 24h 过期，用户封禁后 Token 即时失效 |
| 无状态会话 | `SessionCreationPolicy.STATELESS`，不依赖 Session |
| 传输安全 | HTTPS 强制（生产环境 Nginx TLS 终止） |

### 7.2 授权安全

```
Spring Security Filter Chain
    │
    ├── /api/v1/auth/**              → permitAll
    ├── /api/v1/posts (GET)          → permitAll (后端可见性过滤)
    ├── /api/v1/admin/**             → hasAnyRole(ADMIN, MODERATOR)
    │   └── @PreAuthorize("hasRole('ADMIN')")  → 细粒度方法级控制
    ├── 其他接口                      → authenticated
    └── 前端 v-permission 指令        → 按钮级显隐
```

### 7.3 数据安全

| 风险 | 防护措施 |
|------|----------|
| XSS（跨站脚本） | markdown-it 禁用原始 HTML（`html: false`）；DOMPurify 白名单清洗 |
| SQL 注入 | MyBatis-Plus 参数化查询（`#{param}`），BlockAttackInnerInterceptor 防全表操作 |
| CSRF | REST API 无状态，`csrf().disable()`（不依赖 Cookie） |
| ID 精度丢失 | Jackson 雪花 ID → String 序列化，避免 JavaScript Number 精度问题 |
| AI 数据隐私 | (user_id, post_id) UNIQUE 隔离，管理员无权访问，AI Prompt 防注入 |
| 信息泄露 | 不可见帖子返回 404（非 403），不暴露帖子存在信息 |

### 7.4 环境安全

- 敏感配置（DB 密码、JWT Secret、CORS 白名单域名、AI API Key）通过环境变量注入，不入库
- Docker Compose 统一管理容器网络，MySQL/Redis 不暴露公网端口
- 定时任务：神评检查、推荐模型更新、热门帖子刷新

---

## 8. 性能优化策略

### 8.1 数据库层

| 策略 | 实现 |
|------|------|
| 索引优化 | 联合索引覆盖高频查询：`(category_id, create_time)`、`(user_id, is_read, create_time)` |
| 连接池 | HikariCP：min-idle=5，max-pool=20，idle-timeout=300s |
| 逻辑删除 | MyBatis-Plus `@TableLogic`，`deleted=1` 自动过滤 |
| 计数缓存 | `post.like_count`、`comment_count` 等高频字段直接维护在表上，避免实时 COUNT |

### 8.2 缓存层

| 缓存对象 | 策略 | TTL |
|----------|------|-----|
| 热门帖子列表 | Redis String (JSON) | 5 min |
| 推荐结果 | Redis String (JSON) | 10 min |
| 用户画像 | Redis Hash | 30 min |
| 版块列表 | Redis String (JSON) | 30 min |
| 未读通知数 | Redis String | 30s 轮询更新 |

### 8.3 前端层

| 策略 | 实现 |
|------|------|
| 路由懒加载 | `() => import()` 动态导入，首屏仅加载必需 JS |
| 组件按需导入 | `unplugin-vue-components` + Element Plus resolver |
| 骨架屏 | Skeleton 占位，200ms 淡出 → 内容 200ms 淡入 |
| 资源缓存 | Vite 构建产物带 hash，静态资源 `Cache-Control: max-age=31536000 immutable` |
| 列表优化 | 首页使用无限滚动（`useInfiniteScroll`），其他列表使用分页 |

### 8.4 异步化

- AI 总结生成 → 异步线程池调用 LLM，接口即时返回，前端通过 `status` 轮询
- 关键词提取 → 异步处理，不阻塞发帖请求
- 通知推送 → 异步创建，避免阻塞核心业务

---

## 9. 开发与部署方案

### 9.1 开发环境

**本地基础设施**（Docker Compose 一键启动）：
```yaml
services:
  mysql:     image: mysql:8.0    (port 3306, db: techhub)
  redis:     image: redis:7      (port 6379)
```

**启动方式**：
- 后端：IDE 运行 `TechHubApplication`（dev profile）+ Docker Compose
- 前端：`pnpm dev`（Vite HMR + API 代理到 localhost:8080）

### 9.2 实施阶段

| 阶段 | 内容 | 预计时间 |
|------|------|----------|
| 一 | 需求与设计：用例图、E-R 图、API 文档、前端组件树 | 2 天 |
| 二 | 数据库实现：建表、索引、种子数据 | 1.5 天 |
| 三 | 后端基础框架：项目初始化、安全框架、ORM、统一响应、注册登录 | 2 天 |
| 四 | 后端核心业务：所有业务模块开发、单元测试 | 8 天 |
| 五 | 前端基础与组件：项目搭建、公共组件、Markdown 编辑器、路由与状态 | 2 天 |
| 六 | 前端业务页面：所有页面开发、AI 面板、草稿恢复、权限控制 | 6.5 天 |
| 七 | 联调测试与优化：全功能联调、权限测试、性能优化、部署 | 2.5 天 |
| **合计** | | **~24.5 天** |

### 9.3 部署架构

```
Production:

  Internet
     │
     ▼
  Nginx (:443) ──── TLS Termination
     │                Gzip
     ├── /api/* ──► Spring Boot (:8080)
     │                ├── MySQL (容器内部网络)
     │                ├── Redis (容器内部网络)
     │                └── /data/uploads (文件存储)
     │
     └── /* ──────► Static Files (dist/)
```

**构建产物**：
- 后端：`mvn package -Pprod` → `target/techhub-backend.jar`
- 前端：`pnpm build` → `dist/`
- 容器化：Dockerfile 多阶段构建，最终镜像仅含 JRE + jar + dist

---

## 10. 风险评估与应对

| 风险项 | 概率 | 影响 | 应对措施 |
|--------|------|------|----------|
| **AI 服务不可用** | 中 | 高 | 异步调用 + 重试机制（2 次），前端显示友好错误 + 重试按钮，503 状态码降级 |
| **推荐冷启动** | 高 | 中 | 默认降级为热门帖子排行，新用户展示热门内容引导互动，积累数据后切换个性化推荐 |
| **雪花 ID 时钟回拨** | 低 | 高 | 服务器 NTP 时间同步，MyBatis-Plus ASSIGN_ID 内置容错，极端情况回退 UUID |
| **CORS 白名单通配** | 低 | 严重 | 禁止 `allowedOriginPatterns("*")`，CORS 白名单通过环境变量 `CORS_ORIGINS` 注入；`WebMvcConfig` 使用 `allowedOrigins(精确列表)` + `allowCredentials(true)`；生产缺失 `CORS_ORIGINS` 即 fail-fast |
| **大文件上传性能** | 中 | 低 | 前端限制 5MB + 格式白名单，Nginx `client_max_body_size` 限制，后续可扩展分片上传 |
| **JWT Secret 泄露** | 低 | 严重 | Secret 仅通过环境变量注入 `JWT_SECRET`，配置与代码均**无默认回退**，缺失即 fail-fast 拒绝启动；不写入配置文件或仓库；建立定期轮换机制 |
| **XSS via Markdown** | 中 | 高 | `markdown-it` 禁用原始 HTML + DOMPurify 白名单清洗，服务端可增加二次清洗 |
| **高并发下 DB 压力** | 中 | 中 | Redis 缓存热点数据（热门帖子、推荐结果），HikariCP 连接池调优，读写分离后续扩展 |
| **可见权限逻辑复杂** | 低 | 中 | PostVisibilityService 集中管理，充分的单元测试覆盖所有角色 × 可见性组合，404 策略防信息泄露 |
| **前后端联调延期** | 中 | 中 | API 文档先行（Knife4j），前端使用 MSW (Mock Service Worker) 并行开发，提前对齐接口契约 |

---

## 附录

### A. 关联文档索引

| 文档 | 用途 | 关键内容 |
|------|------|----------|
| [综合课程设计方案](./TechHub%20技术社区论坛%20——%20综合课程设计方案.md) | 项目总览 | 功能模块树、课程知识点覆盖矩阵、实施阶段 |
| [后端开发文档](./TechHub%20后端开发文档.md) | 后端实现参考 | 完整 DDL、项目包结构、所有业务模块代码示例 |
| [API 开发文档](./TechHub%20API开发文档.md) | 接口规范 | 全部端点请求/响应示例、错误码、业务逻辑 |
| [前端页面设计规范](./TechHub%20前端页面设计规范.md) | 设计系统 | 色彩、字体、间距、动效、组件级视觉规范 |
| [前端开发文档](./TechHub%20前端开发文档.md) | 前端实现参考 | 路由表、状态管理、所有核心功能代码示例 |

### B. 术语对照

| 中文 | 英文 | 说明 |
|------|------|------|
| 神评 | Divine Comment | 由社区投票选出的高质量评论 |
| 可见权限 | Visibility | 帖子的四级可见性控制 |
| 版块公告 | Category Notice | 版块级别的须知或活动通知 |
| 草稿 | Draft | 帖子编辑过程中的自动保存内容 |
| 用户画像 | User Profile | 推荐系统中用户兴趣的关键词权重 |

---

> **文档状态**: 已完成
> **下次评审**: 实施阶段开始前
> **变更记录**: v1.0 - 初始版本
