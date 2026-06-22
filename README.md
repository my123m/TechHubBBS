# TechHub 技术社区论坛

一个前后端分离的轻量级技术社区论坛，集成 AI 辅助阅读、智能推荐、神评机制、草稿保护、细粒度可见权限与版块公告管理。

## 项目定位

TechHub 为技术爱好者提供内容创作、交流互动、知识管理的综合平台。系统整合《数据库原理与应用》《Java程序设计（Spring Boot）》《Web程序设计（Vue 3）》三门课程的核心知识与工程实践，覆盖从需求分析、数据库建模到全栈开发与部署的完整工程流程。

## 系统角色

| 角色   | 核心权限                                                                                   |
| ------ | ------------------------------------------------------------------------------------------ |
| 游客   | 浏览公开帖子与公告，不可互动，不可使用 AI                                                  |
| 普通用户 | 发帖、回帖、点赞、收藏、关注、AI 总结与问答、草稿保存、推荐神评（注册 ≥ 7 天）           |
| 版主   | 管理指定版块（加精、置顶、锁定、删除），管理版块公告，撤销神评                             |
| 管理员 | 全局用户管理、版块管理、系统配置、数据统计、神评强制干预、全站公告管理                     |

## 功能模块

```
├── 用户模块
│   ├── 注册/登录/退出/修改密码（BCrypt 加密）
│   ├── 个人信息编辑（头像、简介）
│   └── 个人主页（发帖、收藏、关注/粉丝列表）
├── 版块模块
│   ├── 版块 CRUD、排序、启用/禁用
│   └── 公告管理（须知/活动类型，置顶优先，Markdown）
├── 帖子模块
│   ├── 发布/编辑/删除（Markdown，软删除）
│   ├── 四级可见权限（公开/登录可见/粉丝可见/私密）
│   ├── 草稿自动保存与恢复
│   └── 标题关键词搜索（结果过滤可见性）
├── 互动模块
│   ├── 回复帖子
│   ├── 神评（精华评论）推荐与撤销
│   ├── 点赞、收藏、关注
│   └── 通知系统（实时未读数）
├── AI 辅助模块
│   ├── 帖子 AI 摘要生成
│   ├── 基于帖子原文的私有问答
│   └── 异步处理，超时兜底
├── 推荐模块
│   ├── 热门帖子（浏览量+回复+点赞+收藏权重）
│   ├── 基于标签/关键词的相似推荐
│   └── 基于用户行为的协同过滤
└── 管理后台
    ├── 数据统计仪表盘（ECharts 可视化）
    ├── 用户/帖子/版块/公告管理
    └── 系统配置
```

## 技术栈

| 层级     | 技术                                                         |
| -------- | ------------------------------------------------------------ |
| **前端** | Vue 3 + TypeScript + Vite + Element Plus + Pinia + Vue Router 4 |
| HTTP     | ofetch（拦截器统一处理 Token 与错误）                        |
| 内容渲染 | markdown-it + DOMPurify（XSS 防护）+ highlight.js（代码高亮）|
| 图表     | ECharts（管理后台）                                          |
| **后端** | Spring Boot 3.5.x + Maven                                    |
| 安全     | Spring Security + JWT（无状态认证）+ BCrypt（cost=10）       |
| ORM      | MyBatis-Plus 3.5.9（雪花主键、逻辑删除、分页插件）          |
| 数据库   | MySQL 8.0（InnoDB, utf8mb4）+ HikariCP 连接池                |
| 缓存     | Redis 7.x（Spring Data Redis, Lettuce）                      |
| AI      | OpenAI / 国内大模型接口（可配置，异步调用，超时兜底）        |
| 分词     | jieba-analysis / ansj_seg（中文分词，关键词提取）            |
| 文件存储 | dromara/x-file-storage（本地存储，易扩展至 OSS）             |
| API 文档 | Knife4j (Swagger)                                            |

## 项目结构

```
TechHubBBS/
├── backend/                    # Spring Boot 后端
│   ├── docker-compose.yml      # MySQL + Redis 容器编排
│   └── src/
│       └── main/
│           └── resources/
│               └── application.yml   # 应用配置
├── frontend/                   # Vue 3 前端（待初始化）
├── docs/                       # 项目文档
│   ├── TechHub 技术社区论坛 —— 综合课程设计方案.md
│   ├── TechHub 技术方案文档.md
│   ├── TechHub 后端开发文档.md
│   ├── TechHub API开发文档.md
│   ├── TechHub 前端开发文档.md
│   └── TechHub 前端页面设计规范.md
└── README.md
```

## 快速开始

### 环境要求

| 工具       | 最低版本                                   |
| ---------- | ------------------------------------------ |
| JDK        | 17+                                        |
| Maven      | 3.6+                                       |
| Node.js    | ^20.19.0 或 >=22.12.0                      |
| pnpm       | latest                                     |
| Docker     | 20.10+（运行 MySQL / Redis）               |

### 1. 启动基础设施

```bash
cd backend
docker-compose up -d
```

这将启动 MySQL 8.0（端口 3306）和 Redis 7（端口 6379）。

> **全栈部署**：如需一并启动后端应用容器，使用 `docker compose --profile full up -d`。

### 2. 启动后端

```bash
cd backend
mvn spring-boot:run
```

后端运行在 `http://localhost:8080`，API 文档地址 `http://localhost:8080/swagger-ui.html`。

### 3. 启动前端

```bash
cd frontend
pnpm install
pnpm dev
```

前端开发服务器运行在 `http://localhost:5173`。

### 环境变量

| 变量            | 默认值                               | 说明             |
| --------------- | ------------------------------------ | ---------------- |
| `DB_PASSWORD`   | `root`                               | MySQL 密码       |
| `REDIS_PASSWORD`| (空)                                 | Redis 密码       |
| `JWT_SECRET`    | (内置默认值)                         | JWT 签名密钥     |
| `AI_PROVIDER`   | `openai`                             | AI 服务提供商    |
| `AI_API_KEY`    | (空)                                 | AI API 密钥      |
| `AI_API_URL`    | `https://api.openai.com/v1`         | AI API 地址      |
| `AI_MODEL`      | `gpt-3.5-turbo`                     | AI 模型名称      |

## 安全规范

- 密码使用 BCrypt（cost=10）不可逆哈希存储
- 接口采用 JWT 无状态认证，Token 有效期 24 小时
- 前端 Markdown 渲染使用 DOMPurify 防 XSS
- 后端统一 JSON 响应格式，全局异常处理
- 数据库使用 InnoDB 引擎，utf8mb4 字符集，雪花算法主键
- 逻辑删除（软删除），数据不可逆删除

## 文档

详细设计文档见 `docs/` 目录：

- **[综合课程设计方案](docs/TechHub%20技术社区论坛%20——%20综合课程设计方案.md)** — 项目概述、功能模块树、业务规则
- **[技术方案文档](docs/TechHub%20技术方案文档.md)** — 系统架构、数据库设计、安全设计、性能优化
- **[后端开发文档](docs/TechHub%20后端开发文档.md)** — 包结构、配置、安全实现、各业务模块实现细节
- **[API 开发文档](docs/TechHub%20API开发文档.md)** — 全局前缀 `/api/v1`，各模块接口详情
- **[前端开发文档](docs/TechHub%20前端开发文档.md)** — 目录结构、路由、状态管理、组件清单
- **[前端页面设计规范](docs/TechHub%20前端页面设计规范.md)** — 页面布局、色彩体系、组件设计

## 性能目标

| 指标           | 目标值          |
| -------------- | --------------- |
| 接口响应时间   | P95 < 500ms     |
| AI 接口超时    | < 30s（异步）   |
| 并发支持       | ≥ 200 QPS       |
| 可用性         | 99.5%           |
| 数据备份       | 每日            |

## License

本项目为课程设计项目。
