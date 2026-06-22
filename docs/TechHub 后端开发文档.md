# TechHub 后端开发文档

> 基于「TechHub 技术社区论坛 —— 综合课程设计方案」的后端工程实现文档
> 技术栈：Spring Boot 3.x + MyBatis-Plus + MySQL 8.0 + Redis + Spring Security + JWT + Knife4j + x-file-storage

---

## 目录

1. [技术栈与版本](#1-技术栈与版本)
2. [项目初始化](#2-项目初始化)
3. [项目包结构](#3-项目包结构)
4. [架构设计](#4-架构设计)
5. [数据库实现](#5-数据库实现)
6. [核心配置](#6-核心配置)
7. [安全实现](#7-安全实现)
8. [核心业务模块实现](#8-核心业务模块实现)
   - [8.1 统一响应与全局异常处理](#81-统一响应与全局异常处理)
   - [8.2 认证模块](#82-认证模块)
   - [8.3 用户模块](#83-用户模块)
   - [8.4 版块与公告模块](#84-版块与公告模块)
   - [8.5 帖子模块（含可见权限、草稿）](#85-帖子模块含可见权限草稿)
   - [8.6 回复与神评模块](#86-回复与神评模块)
   - [8.7 互动模块（点赞、收藏、关注）](#87-互动模块点赞收藏关注)
   - [8.8 通知模块](#88-通知模块)
   - [8.9 推荐模块](#89-推荐模块)
   - [8.10 AI 辅助模块](#810-ai-辅助模块)
   - [8.11 管理后台模块](#811-管理后台模块)
   - [8.12 文件上传模块](#812-文件上传模块)
9. [API 文档（Knife4j）](#9-api-文档knife4j)
10. [测试](#10-测试)
11. [构建与部署](#11-构建与部署)
12. [类清单](#12-类清单)

---

## 1. 技术栈与版本

| 类别       | 技术                               | 用途说明                                       |
| ---------- | ---------------------------------- | ---------------------------------------------- |
| 框架       | Spring Boot 3.x                    | 自动配置、内嵌服务器，起步依赖                  |
| 安全       | Spring Security + JWT + BCrypt     | 无状态认证，密码强哈希，接口授权               |
| ORM        | MyBatis-Plus 3.5.9                 | 雪花主键、BaseMapper、条件构造器、分页、逻辑删除 |
| 数据库     | MySQL 8.0 + HikariCP               | InnoDB 引擎，utf8mb4 字符集，连接池管理        |
| 缓存       | Redis 7.x (Spring Data Redis)      | 热点数据缓存，推荐数据加速，高频查询优化        |
| AI 服务集成| OpenAI / 国内大模型接口（可配置）   | AI 总结与问答，异步调用，超时兜底               |
| 中文分词   | jieba-analysis / ansj_seg          | 帖子内容分词，提取关键词                        |
| API 文档   | Knife4j 4.x (springdoc-openapi)    | 在线接口调试，增强 Swagger UI                   |
| JSON 序列化| Jackson (Spring Boot 内置)         | JSON 处理，雪花 ID 序列化为字符串               |
| 工具库     | Lombok, Apache Commons Lang3       | 简化 POJO，通用工具                            |
| 文件存储   | dromara/x-file-storage（本地存储）   | 统一文件上传，本地文件系统存储，易扩展至OSS      |
| 测试       | JUnit 5, MockMvc, Testcontainers   | 单元测试，接口测试，数据库集成测试              |

> **JDK 最低版本**：17（Spring Boot 3.x 要求）

---

## 2. 项目初始化

### 2.1 创建项目

使用 Spring Initializr 或手动创建 Maven 项目：

```xml
<!-- pom.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.5.14</version>
        <relativePath/>
    </parent>

    <groupId>com.techhub</groupId>
    <artifactId>techhub-backend</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>TechHub Backend</name>
    <description>TechHub 技术社区论坛后端服务</description>

    <properties>
        <java.version>17</java.version>
        <mybatis-plus.version>3.5.9</mybatis-plus.version>
        <knife4j.version>4.4.0</knife4j.version>
        <jjwt.version>0.12.6</jjwt.version>
    </properties>

    <dependencies>
        <!-- Spring Boot -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <!-- MyBatis-Plus -->
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
            <version>${mybatis-plus.version}</version>
        </dependency>

        <!-- MySQL -->
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!-- Redis -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>

        <!-- JWT -->
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
            <version>${jjwt.version}</version>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
            <version>${jjwt.version}</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId>
            <version>${jjwt.version}</version>
            <scope>runtime</scope>
        </dependency>

        <!-- Knife4j API 文档 -->
        <dependency>
            <groupId>com.github.xiaoymin</groupId>
            <artifactId>knife4j-openapi3-jakarta-spring-boot-starter</artifactId>
            <version>${knife4j.version}</version>
        </dependency>

        <!-- 工具库 -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.apache.commons</groupId>
            <artifactId>commons-lang3</artifactId>
        </dependency>

        <!-- 文件上传 -->
        <dependency>
            <groupId>org.dromara.x-file-storage</groupId>
            <artifactId>x-file-storage-spring</artifactId>
            <version>2.2.0</version>
        </dependency>

        <!-- JSON 处理 -->
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
        </dependency>

        <!-- 测试 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-test</artifactId>
            <scope>test</scope>
        </dependency>

        <!-- 若使用 jieba 分词，添加对应依赖 -->
        <!-- <dependency> -->
        <!--     <groupId>com.huaban</groupId> -->
        <!--     <artifactId>jieba-analysis</artifactId> -->
        <!--     <version>1.0.2</version> -->
        <!-- </dependency> -->
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

### 2.2 启动类

```java
// src/main/java/com/techhub/TechHubApplication.java
package com.techhub;

import org.dromara.x.file.storage.spring.EnableFileStorage;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.techhub.mapper")
@EnableScheduling   // 启用定时任务（推荐模型更新、神评检查）
@EnableFileStorage  // 启用文件存储服务
public class TechHubApplication {
    public static void main(String[] args) {
        SpringApplication.run(TechHubApplication.class, args);
    }
}
```

---

## 3. 项目包结构

```
techhub-backend/
├── src/
│   ├── main/
│   │   ├── java/com/techhub/
│   │   │   ├── TechHubApplication.java          # 启动类
│   │   │   ├── common/                           # 公共模块
│   │   │   │   ├── R.java                        # 统一响应体
│   │   │   │   ├── PageResult.java               # 分页结果
│   │   │   │   ├── ResultCode.java               # 状态码枚举
│   │   │   │   └── BusinessException.java        # 业务异常
│   │   │   ├── config/                           # 配置类
│   │   │   │   ├── SecurityConfig.java            # Spring Security 配置
│   │   │   │   ├── MybatisPlusConfig.java         # MyBatis-Plus 配置
│   │   │   │   ├── RedisConfig.java              # Redis 配置
│   │   │   │   ├── WebMvcConfig.java             # MVC 配置（CORS）
│   │   │   │   ├── JacksonConfig.java            # Jackson 配置（雪花ID序列化）
│   │   │   │   ├── AiConfig.java                 # AI 服务配置
│   │   │   │   └── FileRecorderConfig.java      # 文件上传记录器（FileRecorder注入）
│   │   │   ├── security/                         # 安全模块
│   │   │   │   ├── JwtTokenProvider.java         # JWT 生成与验证
│   │   │   │   ├── JwtAuthenticationFilter.java  # JWT 认证过滤器
│   │   │   │   ├── UserDetailsServiceImpl.java   # 用户认证服务
│   │   │   │   └── SecurityUtils.java            # 安全工具（获取当前用户）
│   │   │   ├── controller/                       # 控制器层
│   │   │   │   ├── AuthController.java
│   │   │   │   ├── UserController.java
│   │   │   │   ├── PostController.java
│   │   │   │   ├── CommentController.java
│   │   │   │   ├── CategoryController.java
│   │   │   │   ├── NoticeController.java
│   │   │   │   ├── NotificationController.java
│   │   │   │   ├── RecommendationController.java
│   │   │   │   ├── AiController.java
│   │   │   │   ├── DraftController.java
│   │   │   │   ├── InteractionController.java
│   │   │   │   ├── FollowController.java
│   │   │   │   ├── FileController.java           # 文件上传
│   │   │   │   └── admin/                        # 管理后台控制器
│   │   │   │       ├── AdminUserController.java
│   │   │   │       ├── AdminPostController.java
│   │   │   │       ├── AdminCategoryController.java
│   │   │   │       ├── AdminNoticeController.java
│   │   │   │       ├── AdminDivineController.java
│   │   │   │       └── AdminStatisticsController.java
│   │   │   ├── service/                          # 服务层接口
│   │   │   │   ├── UserService.java
│   │   │   │   ├── PostService.java
│   │   │   │   ├── PostVisibilityService.java    # 可见权限服务
│   │   │   │   ├── CommentService.java
│   │   │   │   ├── DivineCommentService.java     # 神评判定服务
│   │   │   │   ├── CategoryService.java
│   │   │   │   ├── NoticeService.java
│   │   │   │   ├── NotificationService.java
│   │   │   │   ├── RecommendationService.java    # 推荐算法服务
│   │   │   │   ├── AiService.java                # AI 集成服务
│   │   │   │   ├── DraftService.java
│   │   │   │   ├── InteractionService.java       # 点赞/收藏
│   │   │   │   ├── FollowService.java
│   │   │   │   └── impl/                         # 服务实现
│   │   │   │       ├── UserServiceImpl.java
│   │   │   │       ├── PostServiceImpl.java
│   │   │   │       ├── PostVisibilityServiceImpl.java
│   │   │   │       ├── CommentServiceImpl.java
│   │   │   │       ├── DivineCommentServiceImpl.java
│   │   │   │       ├── CategoryServiceImpl.java
│   │   │   │       ├── NoticeServiceImpl.java
│   │   │   │       ├── NotificationServiceImpl.java
│   │   │   │       ├── RecommendationServiceImpl.java
│   │   │   │       ├── AiServiceImpl.java
│   │   │   │       ├── DraftServiceImpl.java
│   │   │   │       ├── InteractionServiceImpl.java
│   │   │   │       └── FollowServiceImpl.java
│   │   │   ├── mapper/                           # MyBatis Mapper 接口
│   │   │   │   ├── UserMapper.java
│   │   │   │   ├── PostMapper.java
│   │   │   │   ├── CommentMapper.java
│   │   │   │   ├── CommentRecommendMapper.java
│   │   │   │   ├── CategoryMapper.java
│   │   │   │   ├── CategoryNoticeMapper.java
│   │   │   │   ├── UserLikeMapper.java
│   │   │   │   ├── FavoriteMapper.java
│   │   │   │   ├── FollowMapper.java
│   │   │   │   ├── NotificationMapper.java
│   │   │   │   ├── UserProfileMapper.java
│   │   │   │   ├── PostKeywordMapper.java
│   │   │   │   ├── PostSimilarityMapper.java
│   │   │   │   ├── AiSummaryMapper.java
│   │   │   │   ├── AiQaHistoryMapper.java
│   │   │   │   ├── PostDraftMapper.java
│   │   │   │   └── FileDetailMapper.java           # 文件记录（x-file-storage）
│   │   │   ├── entity/                           # 实体类（对应数据库表）
│   │   │   │   ├── User.java
│   │   │   │   ├── Post.java
│   │   │   │   ├── Comment.java
│   │   │   │   ├── CommentRecommend.java
│   │   │   │   ├── Category.java
│   │   │   │   ├── CategoryNotice.java
│   │   │   │   ├── UserLike.java
│   │   │   │   ├── Favorite.java
│   │   │   │   ├── Follow.java
│   │   │   │   ├── Notification.java
│   │   │   │   ├── UserProfile.java
│   │   │   │   ├── PostKeyword.java
│   │   │   │   ├── PostSimilarity.java
│   │   │   │   ├── AiSummary.java
│   │   │   │   ├── AiQaHistory.java
│   │   │   │   ├── PostDraft.java
│   │   │   │   ├── FileDetail.java               # 文件记录（x-file-storage）
│   │   │   ├── dto/                              # 数据传输对象
│   │   │   │   ├── auth/
│   │   │   │   │   ├── LoginRequest.java
│   │   │   │   │   ├── RegisterRequest.java
│   │   │   │   │   └── LoginResponse.java
│   │   │   │   ├── user/
│   │   │   │   │   ├── UserUpdateRequest.java
│   │   │   │   │   └── UserProfileVO.java
│   │   │   │   ├── post/
│   │   │   │   │   ├── PostCreateRequest.java
│   │   │   │   │   ├── PostUpdateRequest.java
│   │   │   │   │   ├── PostListQuery.java
│   │   │   │   │   └── PostVO.java
│   │   │   │   └── ...
│   │   │   ├── enums/                            # 枚举
│   │   │   │   ├── RoleEnum.java
│   │   │   │   ├── PostTypeEnum.java
│   │   │   │   ├── PostStatusEnum.java
│   │   │   │   ├── VisibilityEnum.java
│   │   │   │   ├── NoticeTypeEnum.java
│   │   │   │   └── NotificationTypeEnum.java
│   │   │   ├── handler/                          # 全局异常处理
│   │   │   │   └── GlobalExceptionHandler.java
│   │   │   ├── scheduler/                        # 定时任务
│   │   │   │   ├── DivineCommentScheduler.java   # 神评检查
│   │   │   │   ├── RecommendationScheduler.java  # 推荐模型更新
│   │   │   │   └── HotPostScheduler.java         # 热门帖子更新
│   │   │   └── util/                             # 工具类
│   │   │       ├── SnowflakeIdGenerator.java     # 雪花ID生成器
│   │   │       ├── KeywordExtractor.java         # 关键词提取
│   │   │       └── MarkdownUtils.java            # Markdown 工具
│   │   └── resources/
│   │       ├── application.yml                    # 主配置
│   │       ├── application-dev.yml                # 开发环境
│   │       ├── application-prod.yml               # 生产环境
│   │       └── db/
│   │           ├── schema.sql                     # DDL 建表脚本
│   │           └── data.sql                       # 种子数据
│   └── test/
│       └── java/com/techhub/
│           ├── controller/                        # 接口测试
│           ├── service/                           # 服务测试
│           └── TechHubApplicationTests.java
├── pom.xml
├── docker-compose.yml                             # MySQL + Redis 环境
└── Dockerfile
```

---

## 4. 架构设计

### 4.1 分层架构

```
┌─────────────────────────────────────────────────┐
│              Controller (控制器层)                │
│  接收 HTTP 请求，参数校验，调用 Service，返回 R<T> │
├─────────────────────────────────────────────────┤
│               Service (服务层)                    │
│  业务逻辑实现，事务管理，权限过滤，AI调用          │
├─────────────────────────────────────────────────┤
│              Mapper (数据访问层)                   │
│  MyBatis-Plus BaseMapper，条件构造，SQL映射       │
├─────────────────────────────────────────────────┤
│              Entity / DTO (数据模型)              │
│  表实体映射，请求/响应对象，VO视图对象             │
├─────────────────────────────────────────────────┤
│           Security (安全拦截层)                    │
│  JwtAuthenticationFilter → SecurityFilterChain    │
│  → @PreAuthorize 方法级授权                       │
└─────────────────────────────────────────────────┘
```

### 4.2 请求处理流程

```
Client Request
    │
    ▼
JwtAuthenticationFilter（提取 Token，解析用户身份）
    │
    ▼
DispatcherServlet（路由到 Controller）
    │
    ▼
Controller（参数校验 @Valid，调用 Service）
    │
    ▼
Service（业务逻辑，事务，权限判断）
    │
    ▼
Mapper（MyBatis-Plus 数据操作）
    │
    ▼
MySQL / Redis（数据读写）
    │
    ▼
Service → Controller → R<T> → Jackson 序列化 JSON
    │
    ▼
Client Response（统一格式 { code, message, data }）
```

### 4.3 核心设计原则

- **单一职责**：Controller 只做参数接收与响应，Service 承担所有业务逻辑
- **事务边界**：`@Transactional` 加在 Service 层，保证数据一致性
- **权限分层**：Filter 层校验登录态，`@PreAuthorize` 校验角色权限，Service 内部做可见权限业务判断
- **无状态**：Session 禁用，所有认证信息通过 JWT 传递
- **统一异常**：所有异常由 `GlobalExceptionHandler` 统一处理并返回 `R<T>` 格式

---

## 5. 数据库实现

### 5.1 设计规范（强制）

| 规范项     | 要求                                            |
| ---------- | ----------------------------------------------- |
| 存储引擎   | 全部表使用 **InnoDB**                           |
| 字符集     | 统一 **utf8mb4**，排序规则 **utf8mb4_unicode_ci** |
| 主键       | 全部使用 **BIGINT**，由应用层雪花算法生成       |
| 外键       | 合理定义以维护参照完整性                        |
| 时间字段   | 统一 `DATETIME`，根据需要设置默认值与自动更新   |
| 索引       | 联合索引覆盖高频查询条件，唯一约束用于防重复    |
| 范式       | 满足第三范式                                    |

### 5.2 完整建表 DDL

```sql
-- ==================== 用户表 ====================
CREATE TABLE `user` (
    `id`          BIGINT       NOT NULL COMMENT '雪花主键',
    `username`    VARCHAR(50)  NOT NULL COMMENT '用户名',
    `password`    VARCHAR(255) NOT NULL COMMENT 'BCrypt 加密密码',
    `email`       VARCHAR(100) NOT NULL COMMENT '邮箱',
    `avatar_url`  VARCHAR(500) DEFAULT NULL COMMENT '头像URL（由x-file-storage上传后返回）',
    `bio`         VARCHAR(500) DEFAULT NULL COMMENT '个人简介',
    `role`        VARCHAR(20)  NOT NULL DEFAULT 'USER' COMMENT '角色 USER/MODERATOR/ADMIN',
    `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1正常 0封禁',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ==================== 版块表 ====================
CREATE TABLE `category` (
    `id`          BIGINT       NOT NULL COMMENT '雪花主键',
    `name`        VARCHAR(50)  NOT NULL COMMENT '版块名称',
    `description` VARCHAR(255) DEFAULT NULL COMMENT '版块描述',
    `sort_order`  INT          NOT NULL DEFAULT 0 COMMENT '排序',
    `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1启用 0禁用',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='版块表';

-- ==================== 版块公告表 ====================
CREATE TABLE `category_notice` (
    `id`          BIGINT       NOT NULL COMMENT '雪花主键',
    `category_id` BIGINT       NOT NULL COMMENT '版块ID',
    `title`       VARCHAR(200) NOT NULL COMMENT '公告标题',
    `content`     TEXT         NOT NULL COMMENT '公告内容（Markdown）',
    `type`        TINYINT      NOT NULL DEFAULT 0 COMMENT '类型 0须知 1活动',
    `author_id`   BIGINT       NOT NULL COMMENT '发布者ID',
    `is_pinned`   TINYINT      NOT NULL DEFAULT 0 COMMENT '是否置顶 0否 1是',
    `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1已发布 0草稿',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_category_status_pinned_time` (`category_id`, `status`, `is_pinned`, `create_time`),
    KEY `idx_author_id` (`author_id`),
    CONSTRAINT `fk_notice_category` FOREIGN KEY (`category_id`) REFERENCES `category` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_notice_author` FOREIGN KEY (`author_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='版块公告表';

-- ==================== 帖子表 ====================
CREATE TABLE `post` (
    `id`                   BIGINT       NOT NULL COMMENT '雪花主键',
    `title`                VARCHAR(200) NOT NULL COMMENT '标题',
    `content`              TEXT         NOT NULL COMMENT '内容（Markdown原文，内嵌图片由x-file-storage上传后引用URL）',
    `category_id`          BIGINT       NOT NULL COMMENT '版块ID',
    `author_id`            BIGINT       NOT NULL COMMENT '作者ID',
    `type`                 TINYINT      NOT NULL DEFAULT 0 COMMENT '类型 0普通 1精华 2置顶',
    `status`               TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1正常 0锁定 2删除（逻辑删除）',
    `visibility`           TINYINT      NOT NULL DEFAULT 0 COMMENT '可见权限 0公开 1登录可见 2粉丝可见 3私密',
    `view_count`           INT          NOT NULL DEFAULT 0 COMMENT '浏览量',
    `like_count`           INT          NOT NULL DEFAULT 0 COMMENT '点赞数',
    `comment_count`        INT          NOT NULL DEFAULT 0 COMMENT '评论数',
    `divine_comment_count` INT          NOT NULL DEFAULT 0 COMMENT '神评数',
    `eligible_for_divine`  TINYINT      NOT NULL DEFAULT 0 COMMENT '是否可推荐神评（comment_count>=10）',
    `create_time`          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`              TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除 0正常 1已删除',
    PRIMARY KEY (`id`),
    KEY `idx_category_time` (`category_id`, `create_time`),
    KEY `idx_author_id` (`author_id`),
    KEY `idx_type_create_time` (`type`, `create_time`),
    CONSTRAINT `fk_post_category` FOREIGN KEY (`category_id`) REFERENCES `category` (`id`),
    CONSTRAINT `fk_post_author` FOREIGN KEY (`author_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='帖子表';

-- ==================== 回复表 ====================
CREATE TABLE `comment` (
    `id`               BIGINT   NOT NULL COMMENT '雪花主键',
    `content`          TEXT     NOT NULL COMMENT '回复内容（Markdown）',
    `post_id`          BIGINT   NOT NULL COMMENT '帖子ID',
    `user_id`          BIGINT   NOT NULL COMMENT '回复者ID',
    `parent_id`        BIGINT   DEFAULT NULL COMMENT '父评论ID（回复特定楼层）',
    `reply_to_user_id` BIGINT   DEFAULT NULL COMMENT '回复目标用户ID',
    `like_count`       INT      NOT NULL DEFAULT 0 COMMENT '点赞数',
    `recommend_count`  INT      NOT NULL DEFAULT 0 COMMENT '推荐数',
    `is_divine`        TINYINT  NOT NULL DEFAULT 0 COMMENT '是否神评 0否 1是',
    `divine_time`      DATETIME DEFAULT NULL COMMENT '神评认定时间',
    `create_time`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_post_create_time` (`post_id`, `create_time`),
    KEY `idx_user_id` (`user_id`),
    CONSTRAINT `fk_comment_post` FOREIGN KEY (`post_id`) REFERENCES `post` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_comment_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='回复表';

-- ==================== 点赞表 ====================
CREATE TABLE `user_like` (
    `id`          BIGINT   NOT NULL COMMENT '雪花主键',
    `user_id`     BIGINT   NOT NULL COMMENT '用户ID',
    `target_type` VARCHAR(10) NOT NULL COMMENT '目标类型 POST/COMMENT',
    `target_id`   BIGINT   NOT NULL COMMENT '目标ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_target` (`user_id`, `target_type`, `target_id`),
    CONSTRAINT `fk_like_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='点赞表';

-- ==================== 收藏表 ====================
CREATE TABLE `favorite` (
    `id`          BIGINT   NOT NULL COMMENT '雪花主键',
    `user_id`     BIGINT   NOT NULL COMMENT '用户ID',
    `post_id`     BIGINT   NOT NULL COMMENT '帖子ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_post` (`user_id`, `post_id`),
    CONSTRAINT `fk_fav_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_fav_post` FOREIGN KEY (`post_id`) REFERENCES `post` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='收藏表';

-- ==================== 关注表 ====================
CREATE TABLE `follow` (
    `id`          BIGINT   NOT NULL COMMENT '雪花主键',
    `follower_id` BIGINT   NOT NULL COMMENT '关注者ID',
    `followee_id` BIGINT   NOT NULL COMMENT '被关注者ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '关注时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_follower_followee` (`follower_id`, `followee_id`),
    KEY `idx_followee_id` (`followee_id`),
    CONSTRAINT `fk_follow_follower` FOREIGN KEY (`follower_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_follow_followee` FOREIGN KEY (`followee_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='关注表';

-- ==================== 通知表 ====================
CREATE TABLE `notification` (
    `id`          BIGINT       NOT NULL COMMENT '雪花主键',
    `user_id`     BIGINT       NOT NULL COMMENT '接收用户ID',
    `type`        VARCHAR(20)  NOT NULL COMMENT '通知类型 REPLY/LIKE/FOLLOW/DIVINE/SYSTEM',
    `source_id`   BIGINT       DEFAULT NULL COMMENT '来源ID（帖子/评论等）',
    `content`     VARCHAR(500) NOT NULL COMMENT '通知内容',
    `is_read`     TINYINT      NOT NULL DEFAULT 0 COMMENT '是否已读 0否 1是',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_read_time` (`user_id`, `is_read`, `create_time`),
    CONSTRAINT `fk_notif_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知表';

-- ==================== 神评推荐记录表 ====================
CREATE TABLE `comment_recommend` (
    `id`          BIGINT   NOT NULL COMMENT '雪花主键',
    `comment_id`  BIGINT   NOT NULL COMMENT '评论ID',
    `user_id`     BIGINT   NOT NULL COMMENT '推荐用户ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '推荐时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_comment_user` (`comment_id`, `user_id`),
    KEY `idx_comment_id` (`comment_id`),
    CONSTRAINT `fk_rec_comment` FOREIGN KEY (`comment_id`) REFERENCES `comment` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_rec_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='神评推荐记录表';

-- ==================== 用户画像表（推荐系统） ====================
CREATE TABLE `user_profile` (
    `user_id`          BIGINT NOT NULL COMMENT '用户ID',
    `keyword_weights`  JSON   DEFAULT NULL COMMENT '关键词权重 JSON',
    `last_update_time` DATETIME DEFAULT NULL COMMENT '最后更新时间',
    PRIMARY KEY (`user_id`),
    CONSTRAINT `fk_profile_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户画像表';

-- ==================== 帖子关键词表（推荐系统） ====================
CREATE TABLE `post_keyword` (
    `post_id`     BIGINT       NOT NULL COMMENT '帖子ID',
    `keyword`     VARCHAR(100) NOT NULL COMMENT '关键词',
    `tfidf_weight` DOUBLE      NOT NULL DEFAULT 0 COMMENT 'TF-IDF 权重',
    PRIMARY KEY (`post_id`, `keyword`),
    KEY `idx_keyword` (`keyword`),
    CONSTRAINT `fk_kw_post` FOREIGN KEY (`post_id`) REFERENCES `post` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='帖子关键词表';

-- ==================== 帖子相似度表（推荐系统） ====================
CREATE TABLE `post_similarity` (
    `post_id_a`       BIGINT NOT NULL COMMENT '帖子A的ID',
    `post_id_b`       BIGINT NOT NULL COMMENT '帖子B的ID',
    `similarity_score` DOUBLE NOT NULL DEFAULT 0 COMMENT '相似度评分',
    PRIMARY KEY (`post_id_a`, `post_id_b`),
    KEY `idx_post_b` (`post_id_b`),
    CONSTRAINT `fk_sim_post_a` FOREIGN KEY (`post_id_a`) REFERENCES `post` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_sim_post_b` FOREIGN KEY (`post_id_b`) REFERENCES `post` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='帖子相似度表';

-- ==================== AI 总结表 ====================
CREATE TABLE `ai_summary` (
    `id`            BIGINT   NOT NULL COMMENT '雪花主键',
    `user_id`       BIGINT   NOT NULL COMMENT '用户ID',
    `post_id`       BIGINT   NOT NULL COMMENT '帖子ID',
    `content`       TEXT     DEFAULT NULL COMMENT 'AI 总结内容',
    `status`        TINYINT  NOT NULL DEFAULT 0 COMMENT '状态 0生成中 1成功 2失败',
    `error_message` VARCHAR(500) DEFAULT NULL COMMENT '错误信息',
    `create_time`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_post` (`user_id`, `post_id`),
    CONSTRAINT `fk_ai_summary_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_ai_summary_post` FOREIGN KEY (`post_id`) REFERENCES `post` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI总结表';

-- ==================== AI 问答记录表 ====================
CREATE TABLE `ai_qa_history` (
    `id`          BIGINT   NOT NULL COMMENT '雪花主键',
    `user_id`     BIGINT   NOT NULL COMMENT '用户ID',
    `post_id`     BIGINT   NOT NULL COMMENT '帖子ID',
    `question`    TEXT     NOT NULL COMMENT '用户问题',
    `answer`      TEXT     NOT NULL COMMENT 'AI 回答',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提问时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_post` (`user_id`, `post_id`),
    CONSTRAINT `fk_qa_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_qa_post` FOREIGN KEY (`post_id`) REFERENCES `post` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI问答记录表';

-- ==================== 帖子草稿表 ====================
CREATE TABLE `post_draft` (
    `id`            BIGINT   NOT NULL COMMENT '雪花主键',
    `user_id`       BIGINT   NOT NULL COMMENT '用户ID',
    `post_id`       BIGINT   DEFAULT NULL COMMENT '帖子ID（编辑已有帖子时非空，新帖草稿为NULL）',
    `title`         VARCHAR(200) DEFAULT NULL COMMENT '标题',
    `content`       LONGTEXT DEFAULT NULL COMMENT '内容（Markdown）',
    `category_id`   BIGINT   DEFAULT NULL COMMENT '版块ID',
    `visibility`    TINYINT  DEFAULT 0 COMMENT '可见权限',
    `last_saved_at` DATETIME DEFAULT NULL COMMENT '最后保存时间',
    `create_time`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    UNIQUE KEY `uk_user_post_draft` (`user_id`, `post_id`),
    CONSTRAINT `fk_draft_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='帖子草稿表';

-- ==================== 文件记录表（x-file-storage 官方推荐） ====================
CREATE TABLE `file_detail` (
    `id`                VARCHAR(32)  NOT NULL COMMENT '文件ID（由x-file-storage生成）',
    `url`               VARCHAR(512) NOT NULL COMMENT '文件访问地址',
    `size`              BIGINT       DEFAULT NULL COMMENT '文件大小，单位字节',
    `filename`          VARCHAR(256) DEFAULT NULL COMMENT '存储文件名',
    `original_filename` VARCHAR(256) DEFAULT NULL COMMENT '原始文件名',
    `base_path`         VARCHAR(256) DEFAULT NULL COMMENT '基础存储路径',
    `path`              VARCHAR(256) DEFAULT NULL COMMENT '存储路径',
    `ext`               VARCHAR(32)  DEFAULT NULL COMMENT '文件扩展名',
    `content_type`      VARCHAR(128) DEFAULT NULL COMMENT 'MIME类型',
    `platform`          VARCHAR(32)  DEFAULT NULL COMMENT '存储平台（如 local-1）',
    `th_url`            VARCHAR(512) DEFAULT NULL COMMENT '缩略图访问路径',
    `th_filename`       VARCHAR(256) DEFAULT NULL COMMENT '缩略图名称',
    `th_size`           BIGINT       DEFAULT NULL COMMENT '缩略图大小，单位字节',
    `th_content_type`   VARCHAR(128) DEFAULT NULL COMMENT '缩略图MIME类型',
    `object_id`         VARCHAR(32)  DEFAULT NULL COMMENT '文件所属对象ID（帖子ID/用户ID等）',
    `object_type`       VARCHAR(32)  DEFAULT NULL COMMENT '文件所属对象类型（如 user_avatar, post_image）',
    `metadata`          TEXT         DEFAULT NULL COMMENT '文件元数据（JSON）',
    `user_metadata`     TEXT         DEFAULT NULL COMMENT '用户自定义元数据（JSON）',
    `th_metadata`       TEXT         DEFAULT NULL COMMENT '缩略图元数据（JSON）',
    `th_user_metadata`  TEXT         DEFAULT NULL COMMENT '缩略图用户自定义元数据（JSON）',
    `attr`              TEXT         DEFAULT NULL COMMENT '附加属性（JSON）',
    `file_acl`          VARCHAR(32)  DEFAULT NULL COMMENT '文件ACL',
    `th_file_acl`       VARCHAR(32)  DEFAULT NULL COMMENT '缩略图文件ACL',
    `hash_info`         TEXT         DEFAULT NULL COMMENT '哈希信息（JSON）',
    `upload_id`         VARCHAR(128) DEFAULT NULL COMMENT '上传ID（分片上传时使用）',
    `upload_status`     INT          DEFAULT NULL COMMENT '上传状态 1初始化完成 2上传完成（分片上传时使用）',
    `create_time`       DATETIME     DEFAULT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_object` (`object_type`, `object_id`),
    KEY `idx_url` (`url`(255))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文件记录表（x-file-storage）';

-- ==================== 文件分片信息表（分片上传场景，可选） ====================
CREATE TABLE `file_part_detail` (
    `id`          VARCHAR(32)  NOT NULL COMMENT '分片ID',
    `platform`    VARCHAR(32)  DEFAULT NULL COMMENT '存储平台',
    `upload_id`   VARCHAR(128) DEFAULT NULL COMMENT '上传ID，分片上传时使用',
    `e_tag`       VARCHAR(255) DEFAULT NULL COMMENT '分片ETag',
    `part_number` INT          DEFAULT NULL COMMENT '分片号（1~10000）',
    `part_size`   BIGINT       DEFAULT NULL COMMENT '分片大小，单位字节',
    `hash_info`   TEXT         DEFAULT NULL COMMENT '哈希信息（JSON）',
    `create_time` DATETIME     DEFAULT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文件分片信息表（x-file-storage）';
```

---

## 6. 核心配置

### 6.1 application.yml

```yaml
# src/main/resources/application.yml
server:
  port: 8080

spring:
  profiles:
    active: dev

  # 数据源配置
  datasource:
    url: jdbc:mysql://localhost:3306/techhub?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8mb4
    username: root
    password: ${DB_PASSWORD:root}
    driver-class-name: com.mysql.cj.jdbc.Driver
    type: com.zaxxer.hikari.HikariDataSource
    hikari:
      minimum-idle: 5
      maximum-pool-size: 20
      idle-timeout: 300000
      max-lifetime: 1200000

  # Redis 配置
  data:
    redis:
      host: localhost
      port: 6379
      password: ${REDIS_PASSWORD:}
      timeout: 5000ms
      lettuce:
        pool:
          max-active: 20
          max-idle: 10
          min-idle: 5

  # Jackson 配置
  jackson:
    date-format: yyyy-MM-dd HH:mm:ss
    time-zone: Asia/Shanghai
    default-property-inclusion: non_null
    serialization:
      write-dates-as-timestamps: false
    generator:
      write-bigdecimal-as-plain: true

# 文件存储配置（x-file-storage）
dromara:
  x-file-storage:
    default-platform: local-1
    local:
      - platform: local-1
        enable-storage: true
        base-path: /data/uploads/
        domain: http://localhost:8080/file/
```

### 6.2 MybatisPlusConfig

```java
// src/main/java/com/techhub/config/MybatisPlusConfig.java
package com.techhub.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.techhub.mapper")
public class MybatisPlusConfig {

    /**
     * MyBatis-Plus 插件配置
     * - 分页插件（MySQL）
     * - 防全表更新与删除插件
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 分页插件
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        // 防止全表更新或删除
        interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());
        return interceptor;
    }
}
```

```yaml
# application.yml 追加 MyBatis-Plus 配置
mybatis-plus:
  global-config:
    db-config:
      id-type: ASSIGN_ID                      # 雪花算法主键
      logic-delete-field: deleted             # 逻辑删除字段
      logic-delete-value: 1                   # 删除时的值
      logic-not-delete-value: 0               # 未删除时的值
  configuration:
    map-underscore-to-camel-case: true        # 下划线转驼峰
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl  # 开发环境打印SQL
  mapper-locations: classpath*:/mapper/**/*.xml   # 可选XML映射
```

### 6.3 SecurityConfig

```java
// src/main/java/com/techhub/config/SecurityConfig.java
package com.techhub.config;

import com.techhub.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity  // 启用 @PreAuthorize
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 禁用 CSRF（REST API 不需要）
            .csrf(csrf -> csrf.disable())
            // 无状态会话
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // 请求授权
            .authorizeHttpRequests(auth -> auth
                // 公开接口
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers(HttpMethod.GET,
                    "/api/v1/posts",
                    "/api/v1/posts/{id}",
                    "/api/v1/categories",
                    "/api/v1/categories/{categoryId}/notices",
                    "/api/v1/notices/{id}",
                    "/api/v1/users/{id}",
                    "/api/v1/users/{id}/posts"
                ).permitAll()
                // Knife4j / Swagger 文档
                .requestMatchers(
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/doc.html",
                    "/webjars/**"
                ).permitAll()
                // 管理后台需要管理员/版主（由 @PreAuthorize 细粒度控制）
                .requestMatchers("/api/v1/admin/**").hasAnyRole("ADMIN", "MODERATOR")
                // 其他接口需要登录
                .anyRequest().authenticated()
            )
            // JWT 过滤器
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
}
```

### 6.4 WebMvcConfig（CORS）

```java
// src/main/java/com/techhub/config/WebMvcConfig.java
package com.techhub.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
```

### 6.5 RedisConfig

```java
// src/main/java/com/techhub/config/RedisConfig.java
package com.techhub.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        // Key 使用 String 序列化
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);

        // Value 使用 JSON 序列化
        GenericJackson2JsonRedisSerializer jsonSerializer =
                new GenericJackson2JsonRedisSerializer();
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }
}
```

### 6.6 雪花 ID 序列化为字符串

```java
// src/main/java/com/techhub/config/JacksonConfig.java
package com.techhub.config;

import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    /**
     * 将 Long 类型主键序列化为 String，
     * 防止 JavaScript 精度丢失（JS 安全整数最大 2^53-1，雪花ID超过此范围）
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
        return builder -> {
            builder.serializerByType(Long.class, ToStringSerializer.instance);
            builder.serializerByType(Long.TYPE, ToStringSerializer.instance);
        };
    }
}
```

### 6.7 Docker Compose（本地开发环境）

```yaml
# docker-compose.yml
version: '3.8'
services:
  mysql:
    image: mysql:8.0
    container_name: techhub-mysql
    restart: unless-stopped
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: techhub
      MYSQL_CHARSET: utf8mb4
      MYSQL_COLLATION: utf8mb4_unicode_ci
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql
      - ./src/main/resources/db/schema.sql:/docker-entrypoint-initdb.d/01-schema.sql
      - ./src/main/resources/db/data.sql:/docker-entrypoint-initdb.d/02-data.sql
    command: --character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci

  redis:
    image: redis:7-alpine
    container_name: techhub-redis
    restart: unless-stopped
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data

volumes:
  mysql_data:
  redis_data:
```

**Profiles 机制说明：**

实际 `docker-compose.yml` 还包含 `app` 服务（配置了 `profiles: ["full"]`），因此：

- **本地开发**：`docker compose up -d` 默认只启动 mysql + redis（app 在 IDE 运行）
- **全栈部署**：`docker compose --profile full up -d` 启动 mysql + redis + app（前后端全部容器化）

---

## 7. 安全实现

### 7.1 JwtTokenProvider

```java
// src/main/java/com/techhub/security/JwtTokenProvider.java
package com.techhub.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtTokenProvider {

    @Value("${jwt.secret:TechHubSecretKeyForJWTTokenGenerationMustBeAtLeast256BitsLong}")
    private String jwtSecret;

    @Value("${jwt.expiration:86400000}") // 默认 24 小时
    private long jwtExpiration;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成 JWT Token
     * @param userId 用户ID
     * @param username 用户名
     * @param role 角色
     */
    public String generateToken(Long userId, String username, String role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpiration);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 从 Token 中提取用户ID
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        return Long.parseLong(claims.getSubject());
    }

    /**
     * 验证 Token 是否有效
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
```

### 7.2 JwtAuthenticationFilter

```java
// src/main/java/com/techhub/security/JwtAuthenticationFilter.java
package com.techhub.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String token = extractToken(request);

        if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
            Long userId = jwtTokenProvider.getUserIdFromToken(token);

            // 从 Token 提取角色信息（更完整的做法是查库，此处简化）
            Claims claims = Jwts.parser()
                    .verifyWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            String role = claims.get("role", String.class);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userId,
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + role))
                    );
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
```

> **重构提示**：JwtAuthenticationFilter 中重复解析 Token 两次（validate 一次，提取角色一次），生产代码应优化为解析一次并通过自定义 Authentication 对象传递 Claims。此处为简化教学示例。

### 7.3 SecurityUtils（获取当前用户）

```java
// src/main/java/com/techhub/security/SecurityUtils.java
package com.techhub.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityUtils {

    /**
     * 获取当前登录用户ID
     * @throws RuntimeException 未登录时抛出
     */
    public static Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new RuntimeException("用户未登录");
        }
        return (Long) auth.getPrincipal();
    }

    /**
     * 获取当前用户角色
     */
    public static String getCurrentUserRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getAuthorities().isEmpty()) {
            return "ANONYMOUS";
        }
        return auth.getAuthorities().iterator().next().getAuthority();
    }

    /**
     * 判断当前用户是否登录
     */
    public static boolean isAuthenticated() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated()
                && !"anonymousUser".equals(auth.getPrincipal());
    }
}
```

---

## 8. 核心业务模块实现

### 8.1 统一响应与全局异常处理

#### 8.1.1 统一响应体 `R<T>`

```java
// src/main/java/com/techhub/common/R.java
package com.techhub.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class R<T> {

    private int code;
    private String message;
    private T data;

    private R(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    // ---- 成功 ----
    public static <T> R<T> ok() {
        return new R<>(200, "success", null);
    }

    public static <T> R<T> ok(T data) {
        return new R<>(200, "success", data);
    }

    public static <T> R<T> ok(String message, T data) {
        return new R<>(200, message, data);
    }

    // ---- 失败 ----
    public static <T> R<T> fail(int code, String message) {
        return new R<>(code, message, null);
    }

    public static <T> R<T> fail(ResultCode resultCode) {
        return new R<>(resultCode.getCode(), resultCode.getMessage(), null);
    }

    public static <T> R<T> fail(ResultCode resultCode, String message) {
        return new R<>(resultCode.getCode(), message, null);
    }
}
```

#### 8.1.2 状态码枚举

```java
// src/main/java/com/techhub/common/ResultCode.java
package com.techhub.common;

import lombok.Getter;

@Getter
public enum ResultCode {

    SUCCESS(200, "操作成功"),

    // 客户端错误 4xx
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或Token已过期"),
    FORBIDDEN(403, "无权限访问"),
    NOT_FOUND(404, "资源不存在"),
    CONFLICT(409, "资源冲突"),
    CONTENT_TOO_SHORT(422, "内容过短"),

    // 服务端错误 5xx
    INTERNAL_ERROR(500, "服务器内部错误"),
    AI_SERVICE_ERROR(503, "AI 服务暂时不可用");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
```

#### 8.1.3 业务异常

```java
// src/main/java/com/techhub/common/BusinessException.java
package com.techhub.common;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    public BusinessException(ResultCode resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
}
```

#### 8.1.4 全局异常处理器

```java
// src/main/java/com/techhub/handler/GlobalExceptionHandler.java
package com.techhub.handler;

import com.techhub.common.BusinessException;
import com.techhub.common.R;
import com.techhub.common.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ---- 业务异常 ----
    @ExceptionHandler(BusinessException.class)
    public R<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常: code={}, message={}", e.getCode(), e.getMessage());
        return R.fail(e.getCode(), e.getMessage());
    }

    // ---- 参数校验异常 ----
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return R.fail(ResultCode.BAD_REQUEST, message);
    }

    // ---- 权限不足 ----
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public R<Void> handleAccessDeniedException(AccessDeniedException e) {
        return R.fail(ResultCode.FORBIDDEN);
    }

    // ---- 兜底异常 ----
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public R<Void> handleUnknownException(Exception e) {
        log.error("未知异常", e);
        return R.fail(ResultCode.INTERNAL_ERROR);
    }
}
```

### 8.2 认证模块

#### 8.2.1 RegisterRequest / LoginRequest

```java
// src/main/java/com/techhub/dto/auth/RegisterRequest.java
package com.techhub.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度3-50个字符")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 100, message = "密码长度6-100个字符")
    private String password;

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;
}
```

```java
// src/main/java/com/techhub/dto/auth/LoginRequest.java
package com.techhub.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;
}
```

```java
// src/main/java/com/techhub/dto/auth/LoginResponse.java
package com.techhub.dto.auth;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginResponse {
    private String token;
    private String tokenType;    // "Bearer"
    private Long userId;
    private String username;
    private String role;
}
```

#### 8.2.2 AuthController

```java
// src/main/java/com/techhub/controller/AuthController.java
package com.techhub.controller;

import com.techhub.common.BusinessException;
import com.techhub.common.R;
import com.techhub.common.ResultCode;
import com.techhub.dto.auth.LoginRequest;
import com.techhub.dto.auth.LoginResponse;
import com.techhub.dto.auth.RegisterRequest;
import com.techhub.entity.User;
import com.techhub.security.JwtTokenProvider;
import com.techhub.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public R<Void> register(@Valid @RequestBody RegisterRequest request) {
        // 检查用户名是否已存在
        if (userService.existsByUsername(request.getUsername())) {
            throw new BusinessException(ResultCode.CONFLICT, "用户名已存在");
        }
        // 检查邮箱是否已存在
        if (userService.existsByEmail(request.getEmail())) {
            throw new BusinessException(ResultCode.CONFLICT, "邮箱已注册");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setRole("USER");
        user.setStatus(1);
        userService.save(user);

        return R.ok("注册成功");
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public R<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        User user = userService.getByUsername(request.getUsername());

        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户名或密码错误");
        }
        if (user.getStatus() == 0) {
            throw new BusinessException(ResultCode.FORBIDDEN, "账号已被封禁");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户名或密码错误");
        }

        String token = jwtTokenProvider.generateToken(
                user.getId(), user.getUsername(), user.getRole());

        LoginResponse response = LoginResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(user.getId())
                .username(user.getUsername())
                .role(user.getRole())
                .build();

        return R.ok(response);
    }
}
```

### 8.3 用户模块

#### 8.3.1 User 实体

```java
// src/main/java/com/techhub/entity/User.java
package com.techhub.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user")
public class User {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String username;
    private String password;
    private String email;
    private String avatarUrl;
    private String bio;
    private String role;     // USER / MODERATOR / ADMIN
    private Integer status;  // 1正常 0封禁

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
```

#### 8.3.2 UserMapper

```java
// src/main/java/com/techhub/mapper/UserMapper.java
package com.techhub.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.techhub.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
    // BaseMapper 已提供所有基本 CRUD，复杂查询可在此扩展
}
```

#### 8.3.3 UserController

```java
// src/main/java/com/techhub/controller/UserController.java
package com.techhub.controller;

import com.techhub.common.R;
import com.techhub.entity.User;
import com.techhub.security.SecurityUtils;
import com.techhub.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 获取当前用户信息
     */
    @GetMapping("/me")
    public R<User> getCurrentUser() {
        Long userId = SecurityUtils.getCurrentUserId();
        User user = userService.getById(userId);
        // 脱敏：不返回密码
        user.setPassword(null);
        return R.ok(user);
    }

    /**
     * 更新个人信息
     */
    @PatchMapping("/me")
    public R<Void> updateProfile(@RequestBody User updateData) {
        Long userId = SecurityUtils.getCurrentUserId();
        // 仅允许更新头像、简介
        User user = new User();
        user.setId(userId);
        user.setAvatarUrl(updateData.getAvatarUrl());
        user.setBio(updateData.getBio());
        userService.updateById(user);
        return R.ok("更新成功");
    }

    /**
     * 获取用户公开信息
     */
    @GetMapping("/{id}")
    public R<User> getUserProfile(@PathVariable Long id) {
        User user = userService.getById(id);
        if (user == null) {
            return R.fail(404, "用户不存在");
        }
        // 脱敏
        user.setPassword(null);
        user.setEmail(null);
        return R.ok(user);
    }
}
```

### 8.4 版块与公告模块

#### 8.4.1 CategoryController

```java
// src/main/java/com/techhub/controller/CategoryController.java
package com.techhub.controller;

import com.techhub.common.R;
import com.techhub.entity.Category;
import com.techhub.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    /** 启用版块列表（公开） */
    @GetMapping
    public R<List<Category>> list() {
        return R.ok(categoryService.listEnabled());
    }

    /** 新建版块（管理员） */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public R<Category> create(@RequestBody Category category) {
        categoryService.save(category);
        return R.ok(category);
    }

    /** 编辑版块（管理员） */
    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> update(@PathVariable Long id, @RequestBody Category category) {
        category.setId(id);
        categoryService.updateById(category);
        return R.ok("更新成功");
    }

    /** 删除版块（管理员） */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> delete(@PathVariable Long id) {
        categoryService.removeById(id);
        return R.ok("删除成功");
    }
}
```

#### 8.4.2 NoticeController（版块公告）

```java
// src/main/java/com/techhub/controller/NoticeController.java
package com.techhub.controller;

import com.techhub.common.BusinessException;
import com.techhub.common.R;
import com.techhub.common.ResultCode;
import com.techhub.dto.notice.NoticeCreateRequest;
import com.techhub.entity.CategoryNotice;
import com.techhub.entity.User;
import com.techhub.security.SecurityUtils;
import com.techhub.service.NoticeService;
import com.techhub.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;
    private final UserService userService;

    /**
     * 版块公告列表（公开，可筛选 type）
     */
    @GetMapping("/categories/{categoryId}/notices")
    public R<List<CategoryNotice>> listByCategory(
            @PathVariable Long categoryId,
            @RequestParam(required = false) Integer type) {
        return R.ok(noticeService.listByCategory(categoryId, type));
    }

    /**
     * 公告详情（公开）
     */
    @GetMapping("/notices/{id}")
    public R<CategoryNotice> detail(@PathVariable Long id) {
        CategoryNotice notice = noticeService.getById(id);
        if (notice == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "公告不存在");
        }
        return R.ok(notice);
    }

    /**
     * 发布公告（管理员 / 管辖版主）
     */
    @PostMapping("/categories/{categoryId}/notices")
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public R<CategoryNotice> create(@PathVariable Long categoryId,
                                     @RequestBody NoticeCreateRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        User currentUser = userService.getById(userId);

        // 版主只能在自己的管辖版块发布
        if ("MODERATOR".equals(currentUser.getRole())) {
            // TODO: 校验版主管辖关系（根据实际业务设计 moderator_category 关联表或 user 字段）
        }

        CategoryNotice notice = new CategoryNotice();
        notice.setCategoryId(categoryId);
        notice.setTitle(request.getTitle());
        notice.setContent(request.getContent());
        notice.setType(request.getType());
        notice.setAuthorId(userId);
        notice.setIsPinned(request.getIsPinned() != null ? request.getIsPinned() : 0);
        notice.setStatus(1);
        noticeService.save(notice);
        return R.ok(notice);
    }

    /**
     * 编辑公告
     */
    @PatchMapping("/notices/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public R<Void> update(@PathVariable Long id, @RequestBody CategoryNotice updateData) {
        // TODO: 权限校验（发布者/版主/管理员）
        updateData.setId(id);
        noticeService.updateById(updateData);
        return R.ok("更新成功");
    }

    /**
     * 删除公告
     */
    @DeleteMapping("/notices/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public R<Void> delete(@PathVariable Long id) {
        noticeService.removeById(id);
        return R.ok("删除成功");
    }
}
```

### 8.5 帖子模块（含可见权限、草稿）

#### 8.5.1 PostVisibilityService — 可见权限核心服务

此服务是整个帖子系统的关键，在列表查询和详情访问时动态判断用户是否有权查看帖子。

```java
// src/main/java/com/techhub/service/PostVisibilityService.java
package com.techhub.service;

import com.techhub.entity.Post;

/**
 * 帖子可见权限过滤服务
 */
public interface PostVisibilityService {

    /**
     * 判断当前用户是否有权查看指定帖子
     * @param post 帖子对象
     * @param currentUserId 当前用户ID（null 表示游客）
     * @param isAdmin 是否为管理员（管理员可看全部）
     * @return true=可查看，false=无权查看（返回404）
     */
    boolean canView(Post post, Long currentUserId, boolean isAdmin);
}
```

```java
// src/main/java/com/techhub/service/impl/PostVisibilityServiceImpl.java
package com.techhub.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.techhub.entity.Follow;
import com.techhub.entity.Post;
import com.techhub.mapper.FollowMapper;
import com.techhub.service.PostVisibilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PostVisibilityServiceImpl implements PostVisibilityService {

    private final FollowMapper followMapper;

    @Override
    public boolean canView(Post post, Long currentUserId, boolean isAdmin) {
        // 管理员可看所有帖子
        if (isAdmin) return true;

        // 作者自己可见
        if (currentUserId != null && currentUserId.equals(post.getAuthorId())) return true;

        int vis = post.getVisibility();

        // 0: 公开 → 所有人
        if (vis == 0) return true;

        // 1: 登录可见 → 登录用户
        if (vis == 1) return currentUserId != null;

        // 2: 粉丝可见 → 当前用户关注了作者
        if (vis == 2) {
            if (currentUserId == null) return false;
            return followMapper.exists(new LambdaQueryWrapper<Follow>()
                    .eq(Follow::getFollowerId, currentUserId)
                    .eq(Follow::getFolloweeId, post.getAuthorId()));
        }

        // 3: 私密 → 仅作者
        return false; // 作者判断已在前面处理
    }
}
```

#### 8.5.2 PostService — 业务逻辑

```java
// src/main/java/com/techhub/service/impl/PostServiceImpl.java（核心方法）
package com.techhub.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.techhub.common.BusinessException;
import com.techhub.common.ResultCode;
import com.techhub.dto.post.PostCreateRequest;
import com.techhub.dto.post.PostListQuery;
import com.techhub.dto.post.PostVO;
import com.techhub.entity.*;
import com.techhub.mapper.*;
import com.techhub.security.SecurityUtils;
import com.techhub.service.PostService;
import com.techhub.service.PostVisibilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostServiceImpl extends ServiceImpl<PostMapper, Post>
        implements PostService {

    private final PostVisibilityService visibilityService;
    private final PostKeywordMapper postKeywordMapper;
    private final PostDraftMapper postDraftMapper;

    /**
     * 发布帖子
     * - 保存帖子
     * - 提取关键词存入 post_keyword 表
     * - 发布成功后删除对应草稿
     */
    @Override
    @Transactional
    public Post createPost(PostCreateRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();

        Post post = new Post();
        post.setTitle(request.getTitle());
        post.setContent(request.getContent());
        post.setCategoryId(request.getCategoryId());
        post.setAuthorId(userId);
        post.setType(0);  // 普通
        post.setStatus(1); // 正常
        post.setVisibility(request.getVisibility());
        post.setViewCount(0);
        post.setLikeCount(0);
        post.setCommentCount(0);
        post.setDivineCommentCount(0);
        post.setEligibleForDivine(0);
        save(post);

        // 提取关键词（异步或同步）
        // keywordExtractor.extractAndSave(post);

        // 删除对应草稿（新帖草稿或编辑草稿）
        postDraftMapper.delete(new LambdaQueryWrapper<PostDraft>()
                .eq(PostDraft::getUserId, userId)
                .and(w -> w.isNull(PostDraft::getPostId)
                         .or().eq(PostDraft::getPostId, request.getDraftPostId())));

        return post;
    }

    /**
     * 帖子列表（含可见性动态过滤 + 分页）
     */
    @Override
    public IPage<PostVO> listPosts(PostListQuery query) {
        Long currentUserId = SecurityUtils.isAuthenticated()
                ? SecurityUtils.getCurrentUserId() : null;
        boolean isAdmin = "ROLE_ADMIN".equals(SecurityUtils.getCurrentUserRole());

        Page<Post> page = new Page<>(query.getPage(), query.getSize());

        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<>();
        // 版块筛选
        if (query.getCategoryId() != null) {
            wrapper.eq(Post::getCategoryId, query.getCategoryId());
        }
        // 关键词模糊搜索
        if (query.getKeyword() != null && !query.getKeyword().isBlank()) {
            wrapper.like(Post::getTitle, query.getKeyword());
        }
        // 排序
        if ("hot".equals(query.getSort())) {
            wrapper.orderByDesc(Post::getLikeCount, Post::getCreateTime);
        } else {
            // 置顶优先，再按时间倒序
            wrapper.orderByDesc(Post::getType, Post::getCreateTime);
        }
        // 逻辑删除过滤
        wrapper.eq(Post::getDeleted, 0);

        IPage<Post> postPage = baseMapper.selectPage(page, wrapper);

        // 可见性过滤 + 转为 VO
        List<PostVO> voList = postPage.getRecords().stream()
                .filter(post -> visibilityService.canView(post, currentUserId, isAdmin))
                .map(this::toVO)
                .collect(Collectors.toList());

        Page<PostVO> voPage = new Page<>(postPage.getCurrent(), postPage.getSize(),
                postPage.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    /**
     * 帖子详情（可见性校验）
     */
    @Override
    public PostVO getPostDetail(Long postId) {
        Post post = getById(postId);
        if (post == null || post.getDeleted() == 1) {
            throw new BusinessException(ResultCode.NOT_FOUND, "帖子不存在");
        }

        Long currentUserId = SecurityUtils.isAuthenticated()
                ? SecurityUtils.getCurrentUserId() : null;
        boolean isAdmin = "ROLE_ADMIN".equals(SecurityUtils.getCurrentUserRole());

        // 可见权限校验：无权限返回 404（避免泄露帖子存在信息）
        if (!visibilityService.canView(post, currentUserId, isAdmin)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "帖子不存在");
        }

        // 增加浏览量（异步更佳，此处简化）
        post.setViewCount(post.getViewCount() + 1);
        updateById(post);

        return toVO(post);
    }

    /**
     * 编辑帖子（仅作者可编辑自己的帖子）
     */
    @Override
    public void updatePost(Long postId, PostUpdateRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        Post post = getById(postId);
        if (post == null || post.getDeleted() == 1) {
            throw new BusinessException(ResultCode.NOT_FOUND, "帖子不存在");
        }
        if (!userId.equals(post.getAuthorId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只能编辑自己的帖子");
        }

        Post update = new Post();
        update.setId(postId);
        if (request.getTitle() != null) update.setTitle(request.getTitle());
        if (request.getContent() != null) update.setContent(request.getContent());
        if (request.getVisibility() != null) update.setVisibility(request.getVisibility());
        updateById(update);
    }

    /**
     * 删除帖子（软删除，作者/管理员）
     */
    @Override
    public void deletePost(Long postId) {
        Long userId = SecurityUtils.getCurrentUserId();
        Post post = getById(postId);
        if (post == null) throw new BusinessException(ResultCode.NOT_FOUND, "帖子不存在");

        boolean isAdmin = "ROLE_ADMIN".equals(SecurityUtils.getCurrentUserRole());
        if (!isAdmin && !userId.equals(post.getAuthorId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权删除");
        }

        // 逻辑删除
        post.setDeleted(1);
        updateById(post);
    }

    private PostVO toVO(Post post) {
        PostVO vo = new PostVO();
        vo.setId(post.getId());
        vo.setTitle(post.getTitle());
        vo.setContent(post.getContent());
        vo.setCategoryId(post.getCategoryId());
        vo.setAuthorId(post.getAuthorId());
        vo.setType(post.getType());
        vo.setVisibility(post.getVisibility());
        vo.setViewCount(post.getViewCount());
        vo.setLikeCount(post.getLikeCount());
        vo.setCommentCount(post.getCommentCount());
        vo.setDivineCommentCount(post.getDivineCommentCount());
        vo.setStatus(post.getStatus());
        vo.setCreateTime(post.getCreateTime());
        vo.setUpdateTime(post.getUpdateTime());
        return vo;
    }
}
```

#### 8.5.3 PostController

```java
// src/main/java/com/techhub/controller/PostController.java
package com.techhub.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.techhub.common.R;
import com.techhub.dto.post.*;
import com.techhub.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    /** 帖子列表 */
    @GetMapping
    public R<IPage<PostVO>> list(PostListQuery query) {
        return R.ok(postService.listPosts(query));
    }

    /** 发布帖子（需登录） */
    @PostMapping
    public R<PostVO> create(@Valid @RequestBody PostCreateRequest request) {
        return R.ok("发布成功", postService.createPost(request));
    }

    /** 帖子详情 */
    @GetMapping("/{id}")
    public R<PostVO> detail(@PathVariable Long id) {
        return R.ok(postService.getPostDetail(id));
    }

    /** 编辑帖子（作者） */
    @PatchMapping("/{id}")
    public R<Void> update(@PathVariable Long id,
                          @Valid @RequestBody PostUpdateRequest request) {
        postService.updatePost(id, request);
        return R.ok("编辑成功");
    }

    /** 删除帖子（作者/管理员） */
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        postService.deletePost(id);
        return R.ok("删除成功");
    }
}
```

#### 8.5.4 草稿模块 DraftController

```java
// src/main/java/com/techhub/controller/DraftController.java
package com.techhub.controller;

import com.techhub.common.R;
import com.techhub.dto.draft.DraftSaveRequest;
import com.techhub.entity.PostDraft;
import com.techhub.security.SecurityUtils;
import com.techhub.service.DraftService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/drafts")
@RequiredArgsConstructor
public class DraftController {

    private final DraftService draftService;

    /** 保存草稿（Upsert） */
    @PostMapping
    public R<PostDraft> save(@RequestBody DraftSaveRequest request) {
        return R.ok(draftService.saveDraft(request));
    }

    /** 草稿列表 */
    @GetMapping
    public R<List<PostDraft>> list() {
        Long userId = SecurityUtils.getCurrentUserId();
        return R.ok(draftService.listByUserId(userId));
    }

    /** 草稿详情 */
    @GetMapping("/{draftId}")
    public R<PostDraft> detail(@PathVariable Long draftId) {
        return R.ok(draftService.getById(draftId));
    }

    /** 检查草稿是否存在 */
    @GetMapping("/check")
    public R<PostDraft> check(@RequestParam(required = false) Long postId) {
        Long userId = SecurityUtils.getCurrentUserId();
        return R.ok(draftService.checkDraft(userId, postId));
    }

    /** 删除草稿 */
    @DeleteMapping("/{draftId}")
    public R<Void> delete(@PathVariable Long draftId) {
        draftService.removeById(draftId);
        return R.ok("草稿已删除");
    }

    /** 按帖子ID删除草稿 */
    @DeleteMapping
    public R<Void> deleteByPostId(@RequestParam Long postId) {
        Long userId = SecurityUtils.getCurrentUserId();
        draftService.deleteByPostId(userId, postId);
        return R.ok("草稿已删除");
    }
}
```

#### 8.5.5 管理员编辑权限说明

系统设计中，**管理员可以编辑和删除任意用户的帖子**。这是内容管理的必要功能：

- `PostServiceImpl.updatePost()` 和 `deletePost()` 方法中，管理员角色（`ADMIN`）豁免作者身份校验
- 具体实现：`if (!userId.equals(post.getAuthorId()) && !isAdmin)` — 管理员通过短路求值绕过作者检查
- `PostService.java` 接口 Javadoc 明确标注「仅作者或管理员可操作」
- 管理员操作记录在 `update_time` 字段中（MyBatis-Plus 自动填充）

> **安全说明**：此设计符合论坛内容管理需求，管理员需对全站内容质量负责。
> 如需审计管理员操作记录，可在未来版本中添加操作日志表。

### 8.6 回复与神评模块

#### 8.6.1 神评自动判定逻辑

```java
// src/main/java/com/techhub/service/impl/DivineCommentServiceImpl.java
package com.techhub.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.techhub.entity.Comment;
import com.techhub.entity.CommentRecommend;
import com.techhub.entity.Post;
import com.techhub.mapper.CommentMapper;
import com.techhub.mapper.CommentRecommendMapper;
import com.techhub.mapper.PostMapper;
import com.techhub.service.DivineCommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DivineCommentServiceImpl extends ServiceImpl<CommentMapper, Comment>
        implements DivineCommentService {

    private final PostMapper postMapper;
    private final CommentRecommendMapper recommendMapper;

    private static final int MIN_LIKE_COUNT = 10;        // 点赞阈值
    private static final int MIN_RECOMMEND_COUNT = 5;    // 推荐阈值
    private static final int MIN_COMMENT_COUNT_FOR_ELIGIBLE = 10; // 帖子开启推荐的评论数阈值

    /**
     * 推荐神评（用户操作）
     * 前置条件：帖子评论数≥10，用户注册≥7天
     */
    @Override
    @Transactional
    public void recommend(Long commentId, Long userId) {
        // 防重
        if (recommendMapper.exists(new LambdaQueryWrapper<CommentRecommend>()
                .eq(CommentRecommend::getCommentId, commentId)
                .eq(CommentRecommend::getUserId, userId))) {
            throw new BusinessException(ResultCode.CONFLICT, "已推荐过该评论");
        }

        // 检查帖子是否开启神评资格
        Comment comment = getById(commentId);
        Post post = postMapper.selectById(comment.getPostId());
        if (post.getEligibleForDivine() == 0) {
            throw new BusinessException(ResultCode.FORBIDDEN, "该帖子暂不支持推荐神评");
        }

        // 保存推荐记录
        CommentRecommend recommend = new CommentRecommend();
        recommend.setCommentId(commentId);
        recommend.setUserId(userId);
        recommendMapper.insert(recommend);

        // 增加推荐计数
        comment.setRecommendCount(comment.getRecommendCount() + 1);
        updateById(comment);

        // 检查是否达到神评标准
        checkDivineThreshold(comment);
    }

    /**
     * 检查评论是否达到神评标准（双维度：点赞≥10 且 推荐≥5）
     */
    private void checkDivineThreshold(Comment comment) {
        if (comment.getIsDivine() == 1) return; // 已是神评

        if (comment.getLikeCount() >= MIN_LIKE_COUNT
                && comment.getRecommendCount() >= MIN_RECOMMEND_COUNT) {
            // 标记为神评
            comment.setIsDivine(1);
            comment.setDivineTime(LocalDateTime.now());
            updateById(comment);

            // 更新帖子神评计数
            Post post = postMapper.selectById(comment.getPostId());
            post.setDivineCommentCount(post.getDivineCommentCount() + 1);
            postMapper.updateById(post);
        }
    }

    /**
     * 定时任务：检查神评是否掉标
     * 当点赞或推荐数低于阈值时自动取消神评
     */
    @Override
    @Transactional
    public void checkAndRevokeDivine() {
        List<Comment> divineComments = lambdaQuery()
                .eq(Comment::getIsDivine, 1)
                .list();

        for (Comment comment : divineComments) {
            if (comment.getLikeCount() < MIN_LIKE_COUNT
                    || comment.getRecommendCount() < MIN_RECOMMEND_COUNT) {
                comment.setIsDivine(0);
                comment.setDivineTime(null);
                updateById(comment);

                Post post = postMapper.selectById(comment.getPostId());
                post.setDivineCommentCount(
                        Math.max(0, post.getDivineCommentCount() - 1));
                postMapper.updateById(post);
            }
        }
    }
}
```

#### 8.6.2 CommentController

```java
// src/main/java/com/techhub/controller/CommentController.java
package com.techhub.controller;

import com.techhub.common.R;
import com.techhub.entity.Comment;
import com.techhub.security.SecurityUtils;
import com.techhub.service.CommentService;
import com.techhub.service.DivineCommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;
    private final DivineCommentService divineCommentService;

    /** 帖子回复列表 */
    @GetMapping("/posts/{postId}/comments")
    public R<List<Comment>> listByPost(@PathVariable Long postId) {
        return R.ok(commentService.listByPostId(postId));
    }

    /** 发表回复 */
    @PostMapping("/posts/{postId}/comments")
    public R<Comment> create(@PathVariable Long postId, @RequestBody Comment comment) {
        comment.setPostId(postId);
        comment.setUserId(SecurityUtils.getCurrentUserId());
        commentService.createComment(comment);
        return R.ok(comment);
    }

    /** 删除回复 */
    @DeleteMapping("/comments/{id}")
    public R<Void> delete(@PathVariable Long id) {
        commentService.deleteComment(id);
        return R.ok("删除成功");
    }

    /** 推荐神评（需登录且注册≥7天） */
    @PostMapping("/comments/{id}/recommend")
    public R<Void> recommend(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        divineCommentService.recommend(id, userId);
        return R.ok("推荐成功");
    }

    /** 取消推荐 */
    @DeleteMapping("/comments/{id}/recommend")
    public R<Void> cancelRecommend(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        divineCommentService.cancelRecommend(id, userId);
        return R.ok("已取消推荐");
    }

    /** 神评列表 */
    @GetMapping("/posts/{postId}/divine-comments")
    public R<List<Comment>> divineComments(@PathVariable Long postId) {
        return R.ok(commentService.listDivineByPostId(postId));
    }
}
```

### 8.7 互动模块（点赞、收藏、关注）

```java
// src/main/java/com/techhub/controller/InteractionController.java
package com.techhub.controller;

import com.techhub.common.R;
import com.techhub.security.SecurityUtils;
import com.techhub.service.InteractionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class InteractionController {

    private final InteractionService interactionService;

    // ---- 帖子点赞 ----
    @PostMapping("/posts/{id}/likes")
    public R<Void> likePost(@PathVariable Long id) {
        interactionService.like(SecurityUtils.getCurrentUserId(), "POST", id);
        return R.ok("点赞成功");
    }

    @DeleteMapping("/posts/{id}/likes")
    public R<Void> unlikePost(@PathVariable Long id) {
        interactionService.unlike(SecurityUtils.getCurrentUserId(), "POST", id);
        return R.ok("取消点赞");
    }

    // ---- 回复点赞 ----
    @PostMapping("/comments/{id}/likes")
    public R<Void> likeComment(@PathVariable Long id) {
        interactionService.like(SecurityUtils.getCurrentUserId(), "COMMENT", id);
        return R.ok("点赞成功");
    }

    @DeleteMapping("/comments/{id}/likes")
    public R<Void> unlikeComment(@PathVariable Long id) {
        interactionService.unlike(SecurityUtils.getCurrentUserId(), "COMMENT", id);
        return R.ok("取消点赞");
    }

    // ---- 收藏 ----
    @PostMapping("/posts/{id}/favorites")
    public R<Void> favorite(@PathVariable Long id) {
        interactionService.favorite(SecurityUtils.getCurrentUserId(), id);
        return R.ok("收藏成功");
    }

    @DeleteMapping("/posts/{id}/favorites")
    public R<Void> unfavorite(@PathVariable Long id) {
        interactionService.unfavorite(SecurityUtils.getCurrentUserId(), id);
        return R.ok("取消收藏");
    }
}
```

```java
// src/main/java/com/techhub/controller/FollowController.java
package com.techhub.controller;

import com.techhub.common.R;
import com.techhub.security.SecurityUtils;
import com.techhub.service.FollowService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class FollowController {

    private final FollowService followService;

    /** 关注用户 */
    @PostMapping("/users/{id}/follow")
    public R<Void> follow(@PathVariable Long id) {
        followService.follow(SecurityUtils.getCurrentUserId(), id);
        return R.ok("关注成功");
    }

    /** 取消关注 */
    @DeleteMapping("/users/{id}/follow")
    public R<Void> unfollow(@PathVariable Long id) {
        followService.unfollow(SecurityUtils.getCurrentUserId(), id);
        return R.ok("取消关注");
    }

    /** 检查是否关注 */
    @GetMapping("/users/{id}/follow")
    public R<Boolean> isFollowing(@PathVariable Long id) {
        return R.ok(followService.isFollowing(SecurityUtils.getCurrentUserId(), id));
    }
}
```

### 8.8 通知模块

```java
// src/main/java/com/techhub/controller/NotificationController.java
package com.techhub.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.techhub.common.R;
import com.techhub.entity.Notification;
import com.techhub.security.SecurityUtils;
import com.techhub.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /** 通知列表（分页） */
    @GetMapping
    public R<IPage<Notification>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = SecurityUtils.getCurrentUserId();
        return R.ok(notificationService.listByUserId(userId, page, size));
    }

    /** 标记单条已读 */
    @PatchMapping("/{id}/read")
    public R<Void> markRead(@PathVariable Long id) {
        notificationService.markRead(id);
        return R.ok();
    }

    /** 全部标记已读 */
    @PatchMapping("/read-all")
    public R<Void> markAllRead() {
        Long userId = SecurityUtils.getCurrentUserId();
        notificationService.markAllRead(userId);
        return R.ok();
    }

    /** 未读数量 */
    @GetMapping("/unread-count")
    public R<Long> unreadCount() {
        Long userId = SecurityUtils.getCurrentUserId();
        return R.ok(notificationService.countUnread(userId));
    }
}
```

### 8.9 推荐模块

**推荐策略**：基于内容的推荐 + 协同过滤。定时更新用户画像（关键词权重）和帖子相似度矩阵，推荐时从 Redis 缓存读取，结果经过可见权限过滤。

```java
// src/main/java/com/techhub/controller/RecommendationController.java
package com.techhub.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.techhub.common.R;
import com.techhub.dto.post.PostVO;
import com.techhub.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    /**
     * 首页个性化推荐（"猜你喜欢"）
     * - 用户画像匹配帖子关键词
     * - 过滤不可见帖子
     * - 缓存加速
     */
    @GetMapping("/recommendations")
    public R<IPage<PostVO>> recommend(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return R.ok(recommendationService.getPersonalizedRecommendations(page, size));
    }

    /**
     * 相关帖子推荐
     * - 基于帖子相似度矩阵
     * - 过滤不可见帖子
     */
    @GetMapping("/posts/{id}/related")
    public R<IPage<PostVO>> relatedPosts(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "5") int size) {
        return R.ok(recommendationService.getRelatedPosts(id, page, size));
    }
}
```

### 8.10 AI 辅助模块

```java
// src/main/java/com/techhub/controller/AiController.java
package com.techhub.controller;

import com.techhub.common.BusinessException;
import com.techhub.common.R;
import com.techhub.common.ResultCode;
import com.techhub.dto.ai.AiQaRequest;
import com.techhub.entity.AiQaHistory;
import com.techhub.entity.AiSummary;
import com.techhub.entity.Post;
import com.techhub.security.SecurityUtils;
import com.techhub.service.AiService;
import com.techhub.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/posts/{postId}/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;
    private final PostService postService;

    private static final int MIN_CONTENT_LENGTH = 50;

    /**
     * 生成/刷新 AI 总结
     */
    @PostMapping("/summary")
    public R<AiSummary> generateSummary(@PathVariable Long postId) {
        // 内容长度检查
        Post post = postService.getById(postId);
        if (post == null || post.getContent().length() < MIN_CONTENT_LENGTH) {
            throw new BusinessException(ResultCode.CONTENT_TOO_SHORT,
                    "帖子内容过短（需≥" + MIN_CONTENT_LENGTH + "字符），暂不支持 AI 总结");
        }

        Long userId = SecurityUtils.getCurrentUserId();
        AiSummary summary = aiService.generateSummary(userId, postId, post.getContent());
        return R.ok(summary.getStatus() == 1 ? "生成成功" : "生成中", summary);
    }

    /**
     * 获取已有 AI 总结（私有数据）
     */
    @GetMapping("/summary")
    public R<AiSummary> getSummary(@PathVariable Long postId) {
        Long userId = SecurityUtils.getCurrentUserId();
        AiSummary summary = aiService.getSummary(userId, postId);
        if (summary == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "暂无 AI 总结");
        }
        if (summary.getStatus() == 2) {
            return R.fail(503, summary.getErrorMessage() != null
                    ? summary.getErrorMessage() : "AI 总结生成失败");
        }
        return R.ok(summary);
    }

    /**
     * AI 问答（需先有总结，基于帖子原文回答）
     */
    @PostMapping("/qa")
    public R<AiQaHistory> askQuestion(@PathVariable Long postId,
                                       @RequestBody AiQaRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        Post post = postService.getById(postId);

        // 校验：必须先有总结
        AiSummary summary = aiService.getSummary(userId, postId);
        if (summary == null || summary.getStatus() != 1) {
            throw new BusinessException(ResultCode.FORBIDDEN, "请先生成 AI 总结");
        }

        AiQaHistory history = aiService.ask(userId, postId,
                post.getContent(), request.getQuestion());
        return R.ok(history);
    }

    /**
     * AI 问答历史（私有数据）
     */
    @GetMapping("/qa/history")
    public R<List<AiQaHistory>> qaHistory(@PathVariable Long postId) {
        Long userId = SecurityUtils.getCurrentUserId();
        return R.ok(aiService.getQaHistory(userId, postId));
    }
}
```

### 8.11 管理后台模块

#### 8.11.1 AdminUserController（用户管理）

```java
// src/main/java/com/techhub/controller/admin/AdminUserController.java
package com.techhub.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.techhub.common.R;
import com.techhub.entity.User;
import com.techhub.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserService userService;

    /** 用户列表 */
    @GetMapping
    public R<IPage<User>> list(@RequestParam(defaultValue = "1") int page,
                                @RequestParam(defaultValue = "20") int size,
                                @RequestParam(required = false) String keyword) {
        return R.ok(userService.listUsers(page, size, keyword));
    }

    /** 封禁/解封 */
    @PatchMapping("/{id}/ban")
    public R<Void> ban(@PathVariable Long id, @RequestParam boolean ban) {
        userService.updateStatus(id, ban ? 0 : 1);
        return R.ok(ban ? "已封禁" : "已解封");
    }

    /** 修改角色 */
    @PatchMapping("/{id}/role")
    public R<Void> updateRole(@PathVariable Long id, @RequestParam String role) {
        userService.updateRole(id, role);
        return R.ok("角色已更新");
    }
}
```

#### 8.11.2 AdminPostController（帖子管理）

```java
// src/main/java/com/techhub/controller/admin/AdminPostController.java
package com.techhub.controller.admin;

import com.techhub.common.R;
import com.techhub.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/posts")
@RequiredArgsConstructor
public class AdminPostController {

    private final PostService postService;

    /** 全站帖子管理（不过滤可见性，管理员可见所有帖子） */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public R<IPage<PostVO>> listAll(@RequestParam(defaultValue = "1") int page,
                                     @RequestParam(defaultValue = "20") int size) {
        return R.ok(postService.listAllPosts(page, size));
    }

    /** 设置精华/置顶 */
    @PatchMapping("/{id}/type")
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public R<Void> setType(@PathVariable Long id, @RequestParam int type) {
        // type: 0普通 1精华 2置顶
        postService.setType(id, type);
        return R.ok("设置成功");
    }

    /** 锁定/解锁 */
    @PatchMapping("/{id}/lock")
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public R<Void> lock(@PathVariable Long id, @RequestParam boolean lock) {
        postService.lockPost(id, lock);
        return R.ok(lock ? "已锁定" : "已解锁");
    }

    /** 强制删除（物理删除或逻辑删除） */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> forceDelete(@PathVariable Long id) {
        postService.forceDelete(id);
        return R.ok("已强制删除");
    }
}
```

#### 8.11.3 AdminStatisticsController（站点统计）

```java
// src/main/java/com/techhub/controller/admin/AdminStatisticsController.java
package com.techhub.controller.admin;

import com.techhub.common.R;
import com.techhub.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/statistics")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminStatisticsController {

    private final StatisticsService statisticsService;

    /**
     * 站点统计
     * 返回：用户增长、帖子量、活跃度、推荐效果
     */
    @GetMapping
    public R<Map<String, Object>> statistics(
            @RequestParam(defaultValue = "userGrowth") String type) {
        return R.ok(statisticsService.getStatistics(type));
    }
}
```

### 8.12 文件上传模块

使用 dromara/x-file-storage 统一处理文件上传，基于本地文件系统存储，可无缝切换至 OSS/MinIO。**核心机制**：实现 `FileRecorder` 接口，让 x-file-storage 在上传成功后自动将文件元数据写入 `file_detail` 表，无需在 Controller 中手动插入。

#### 8.12.1 FileDetailRecorder — 实现 FileRecorder 接口

```java
// src/main/java/com/techhub/config/FileRecorderConfig.java
package com.techhub.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.techhub.entity.FileDetail;
import com.techhub.mapper.FileDetailMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.recorder.FileRecorder;
import org.springframework.stereotype.Component;

/**
 * 文件上传记录器 — 实现 x-file-storage 的 FileRecorder 接口
 * 
 * x-file-storage 在每次上传成功后自动调用 save() 方法，
 * 将文件元数据写入 file_detail 表。
 * 
 * 同时在文件删除时自动调用 delete() 方法清理数据库记录。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FileDetailRecorder implements FileRecorder {

    private final FileDetailMapper fileDetailMapper;

    /**
     * 上传成功后自动调用，将 FileInfo 转为 FileDetail 存入数据库
     */
    @SneakyThrows
    @Override
    public boolean save(FileInfo fileInfo) {
        FileDetail detail = toFileDetail(fileInfo);
        int rows = fileDetailMapper.insert(detail);
        if (rows > 0) {
            fileInfo.setId(detail.getId()); // 回写 ID，后续可通过 getByUrl 查询
        }
        log.debug("文件记录已保存: id={}, url={}", detail.getId(), detail.getUrl());
        return rows > 0;
    }

    /**
     * 根据 URL 从数据库获取 FileInfo（用于下载、删除等操作）
     */
    @SneakyThrows
    @Override
    public FileInfo getByUrl(String url) {
        FileDetail detail = fileDetailMapper.selectOne(
                new LambdaQueryWrapper<FileDetail>().eq(FileDetail::getUrl, url));
        if (detail == null) return null;
        return toFileInfo(detail);
    }

    /**
     * 文件删除后自动调用，清理数据库记录
     */
    @Override
    public boolean delete(String url) {
        int rows = fileDetailMapper.delete(
                new LambdaQueryWrapper<FileDetail>().eq(FileDetail::getUrl, url));
        return rows > 0;
    }

    /** FileInfo → FileDetail */
    private FileDetail toFileDetail(FileInfo info) {
        FileDetail detail = new FileDetail();
        detail.setId(info.getId());
        detail.setUrl(info.getUrl());
        detail.setSize(info.getSize());
        detail.setFilename(info.getFilename());
        detail.setOriginalFilename(info.getOriginalFilename());
        detail.setBasePath(info.getBasePath());
        detail.setPath(info.getPath());
        detail.setExt(info.getExt());
        detail.setContentType(info.getContentType());
        detail.setPlatform(info.getPlatform());
        detail.setObjectId(info.getObjectId());
        detail.setObjectType(info.getObjectType());
        detail.setAttr(info.getAttr() != null ? info.getAttr().toString() : null);
        detail.setCreateTime(info.getCreateTime() != null
                ? info.getCreateTime().toLocalDateTime() : null);
        return detail;
    }

    /** FileDetail → FileInfo（逆向转换，便于后续操作） */
    private FileInfo toFileInfo(FileDetail detail) {
        FileInfo info = new FileInfo();
        info.setId(detail.getId());
        info.setUrl(detail.getUrl());
        info.setSize(detail.getSize());
        info.setFilename(detail.getFilename());
        info.setOriginalFilename(detail.getOriginalFilename());
        info.setBasePath(detail.getBasePath());
        info.setPath(detail.getPath());
        info.setExt(detail.getExt());
        info.setContentType(detail.getContentType());
        info.setPlatform(detail.getPlatform());
        info.setObjectId(detail.getObjectId());
        info.setObjectType(detail.getObjectType());
        return info;
    }
}
```

> **工作原理**：x-file-storage 在文件上传成功后自动调用 `FileRecorder.save(fileInfo)`，因此只需实现此接口并注册为 Spring Bean，无需在 Controller 中手动写插入代码。

#### 8.12.2 FileController

```java
// src/main/java/com/techhub/controller/FileController.java
package com.techhub.controller;

import com.techhub.common.BusinessException;
import com.techhub.common.R;
import com.techhub.common.ResultCode;
import lombok.RequiredArgsConstructor;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService fileStorageService;

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final String[] ALLOWED_IMAGE_TYPES = {
            "image/jpeg", "image/png", "image/gif", "image/webp"
    };

    /**
     * 上传文件（头像、帖子内嵌图片等）
     * 需登录，仅允许图片类型，大小限制 5MB
     * 
     * 使用 setObjectId / setObjectType 设置关联信息，
     * 上传成功后 FileDetailRecorder 自动将记录写入 file_detail 表。
     *
     * @param objectType 对象类型（如 user_avatar, post_image）
     * @param objectId   关联对象ID（如帖子ID、用户ID）
     */
    @PostMapping("/upload")
    @PreAuthorize("isAuthenticated()")
    public R<Map<String, String>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "post_image") String objectType,
            @RequestParam(required = false) String objectId) {

        // 校验文件大小
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(ResultCode.BAD_REQUEST,
                    "文件大小不能超过 " + (MAX_FILE_SIZE / 1024 / 1024) + "MB");
        }

        // 校验文件类型
        String contentType = file.getContentType();
        boolean allowed = false;
        for (String allowedType : ALLOWED_IMAGE_TYPES) {
            if (allowedType.equals(contentType)) {
                allowed = true;
                break;
            }
        }
        if (!allowed) {
            throw new BusinessException(ResultCode.BAD_REQUEST,
                    "仅支持 JPG / PNG / GIF / WebP 格式图片");
        }

        // 上传文件（FileDetailRecorder 会自动保存记录到 file_detail 表）
        FileInfo fileInfo = fileStorageService.of(file)
                .setPath("uploads/")
                .setObjectId(objectId)     // 关联对象ID，自动存入 file_detail.object_id
                .setObjectType(objectType) // 关联对象类型，自动存入 file_detail.object_type
                .upload();

        if (fileInfo == null) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "文件上传失败");
        }

        Map<String, String> result = new HashMap<>();
        result.put("url", fileInfo.getUrl());
        result.put("filename", fileInfo.getFilename());
        result.put("size", fileInfo.getSize());
        return R.ok("上传成功", result);
    }
}
```

#### 8.12.3 FileDetail 实体

```java
// src/main/java/com/techhub/entity/FileDetail.java
package com.techhub.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("file_detail")
public class FileDetail {

    @TableId
    private String id;            // 文件ID（由x-file-storage生成，非雪花）

    private String url;
    private Long size;
    private String filename;
    private String originalFilename;
    private String basePath;
    private String path;
    private String ext;
    private String contentType;
    private String platform;
    private String thUrl;
    private String thFilename;
    private Long thSize;
    private String thContentType;
    private String objectId;      // 关联对象ID
    private String objectType;    // 关联对象类型
    private String metadata;
    private String attr;
    private LocalDateTime createTime;
}
```

#### 8.12.4 FileDetailMapper

```java
// src/main/java/com/techhub/mapper/FileDetailMapper.java
package com.techhub.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.techhub.entity.FileDetail;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FileDetailMapper extends BaseMapper<FileDetail> {
}
```

#### 8.12.5 application.yml 文件存储配置详解

```yaml
# 已在 6.1 application.yml 中配置，此处独立展示
dromara:
  x-file-storage:
    default-platform: local-1       # 默认存储平台
    local:
      - platform: local-1           # 平台标识
        enable-storage: true        # 启用存储
        base-path: /data/uploads/   # 本地存储根目录（Docker映射或宿主机路径）
        domain: http://localhost:8080/file/   # 文件访问域名前缀
```

> **扩展说明**：x-file-storage 支持同时配置多个存储平台（本地 + OSS + MinIO），通过 `fileStorageService.of(file).setPlatform("aliyun-oss-1")` 动态切换。本地开发使用 local-1，生产环境可切换到云存储，无需修改业务代码。

> **FileRecorder 联动**：实现 `FileRecorder` 接口后，可通过 `fileStorageService.getFileInfoByUrl(url)` 直接从数据库获取 FileInfo 对象，支持 `exists()`、`download()`、`delete()` 等后续操作。

---

## 9. API 文档（Knife4j）

### 9.1 配置

```yaml
# application.yml 追加
springdoc:
  swagger-ui:
    path: /swagger-ui.html
    tags-sorter: alpha
    operations-sorter: alpha
  api-docs:
    path: /v3/api-docs
  group-configs:
    - group: 'default'
      paths-to-match: '/**'
      packages-to-scan: com.techhub.controller

knife4j:
  enable: true
  setting:
    language: zh_cn
```

### 9.2 访问地址

| 文档 | URL |
|------|-----|
| Knife4j 增强 UI | `http://localhost:8080/doc.html` |
| 原生 Swagger UI | `http://localhost:8080/swagger-ui.html` |
| OpenAPI JSON | `http://localhost:8080/v3/api-docs` |

### 9.3 Controller 注解示例

```java
@Tag(name = "认证管理", description = "用户注册、登录")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    @Operation(summary = "用户登录", description = "使用用户名和密码登录，返回JWT Token")
    @PostMapping("/login")
    public R<LoginResponse> login(
            @Parameter(description = "登录请求") @Valid @RequestBody LoginRequest request) {
        // ...
    }
}
```

---

## 10. 测试

### 10.1 JUnit 5 + MockMvc 接口测试

```java
// src/test/java/com/techhub/controller/AuthControllerTest.java
package com.techhub.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techhub.dto.auth.LoginRequest;
import com.techhub.dto.auth.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional  // 自动回滚，保持数据库干净
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldRegisterSuccessfully() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("testuser");
        request.setPassword("password123");
        request.setEmail("test@example.com");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("注册成功"));
    }

    @Test
    void shouldLoginWithValidCredentials() throws Exception {
        // 先注册
        RegisterRequest regRequest = new RegisterRequest();
        regRequest.setUsername("logintest");
        regRequest.setPassword("password123");
        regRequest.setEmail("login@example.com");
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(regRequest)));

        // 再登录
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("logintest");
        loginRequest.setPassword("password123");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").exists())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"));
    }

    @Test
    void shouldRejectDuplicateUsername() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("duptest");
        request.setPassword("password123");
        request.setEmail("dup1@example.com");

        // 第一次注册
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // 第二次同用户名注册
        request.setEmail("dup2@example.com");
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(jsonPath("$.code").value(409))
                .andExpect(jsonPath("$.message").value("用户名已存在"));
    }
}
```

### 10.2 PostService 单元测试

```java
// src/test/java/com/techhub/service/PostServiceTest.java
package com.techhub.service;

import com.techhub.entity.Post;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class PostServiceTest {

    @Autowired
    private PostService postService;

    @Test
    void shouldCreatePostWithCorrectDefaults() {
        // 此测试需要先注入模拟的 SecurityContext
        // 实际测试中应使用 @WithMockUser 或 SecurityContext 手动设置
    }
}
```

### 10.3 运行测试

```bash
# 全部测试
mvn test

# 指定测试类
mvn test -Dtest=AuthControllerTest

# 生成测试报告
mvn surefire-report:report
```

---

## 11. 构建与部署

### 11.1 构建

```bash
# 编译打包
mvn clean package -DskipTests

# 跳过测试
mvn clean package -DskipTests

# 指定环境
mvn clean package -DskipTests -Pprod
```

### 11.2 application-prod.yml（生产环境）

```yaml
# src/main/resources/application-prod.yml
server:
  port: ${SERVER_PORT:8080}

spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    hikari:
      maximum-pool-size: 50

  data:
    redis:
      host: ${REDIS_HOST}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD}

mybatis-plus:
  configuration:
    log-impl: org.apache.ibatis.logging.nologging.NoLoggingImpl   # 生产环境不打印SQL
```

### 11.3 Dockerfile

```dockerfile
# Dockerfile
FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app

COPY target/techhub-backend-*.jar app.jar

EXPOSE 8080

ENV SPRING_PROFILES_ACTIVE=prod

ENTRYPOINT ["java", "-jar", "-Djava.security.egd=file:/dev/./urandom", "app.jar"]
```

### 11.4 部署

```bash
# 构建镜像
docker build -t techhub-backend:latest .

# 启动（配合 docker-compose，需要 --profile full 启用 app 服务）
docker compose --profile full up -d

# 单独启动后端
docker run -d \
  --name techhub-backend \
  -p 8080:8080 \
  -e DB_URL=jdbc:mysql://mysql:3306/techhub \
  -e DB_USERNAME=root \
  -e DB_PASSWORD=your_password \
  -e REDIS_HOST=redis \
  techhub-backend:latest
```

---

## 12. 类清单

### 12.1 控制器（Controller）

| # | 类 | 路径前缀 | 说明 |
|---|-----|---------|------|
| 1 | `AuthController` | `/api/v1/auth` | 注册、登录 |
| 2 | `UserController` | `/api/v1/users` | 用户信息、个人主页 |
| 3 | `PostController` | `/api/v1/posts` | 帖子CRUD（含可见权限） |
| 4 | `CommentController` | `/api/v1/posts/{postId}/comments` | 回复、神评推荐 |
| 5 | `CategoryController` | `/api/v1/categories` | 版块管理 |
| 6 | `NoticeController` | `/api/v1/categories/{id}/notices` | 版块公告 |
| 7 | `NotificationController` | `/api/v1/notifications` | 通知管理 |
| 8 | `RecommendationController` | `/api/v1/recommendations` | 推荐流 |
| 9 | `AiController` | `/api/v1/posts/{id}/ai` | AI 总结与问答 |
| 10 | `DraftController` | `/api/v1/drafts` | 草稿管理 |
| 11 | `InteractionController` | `/api/v1` | 点赞、收藏 |
| 12 | `FollowController` | `/api/v1/users/{id}/follow` | 关注 |
| 13 | `FileController` | `/api/v1/files` | 文件上传（头像、图片） |
| 14 | `AdminUserController` | `/api/v1/admin/users` | 用户管理（后台） |
| 15 | `AdminPostController` | `/api/v1/admin/posts` | 帖子管理（后台） |
| 16 | `AdminCategoryController` | `/api/v1/admin/categories` | 版块管理（后台） |
| 17 | `AdminNoticeController` | `/api/v1/admin/notices` | 公告管理（后台） |
| 18 | `AdminDivineController` | `/api/v1/admin/comments` | 神评管理（后台） |
| 19 | `AdminStatisticsController` | `/api/v1/admin/statistics` | 站点统计 |

### 12.2 服务层（Service）

| # | 接口 | 实现 | 职责 |
|---|------|------|------|
| 1 | `UserService` | `UserServiceImpl` | 用户CRUD、角色管理 |
| 2 | `PostService` | `PostServiceImpl` | 帖子CRUD、可见性过滤、草稿清理 |
| 3 | `PostVisibilityService` | `PostVisibilityServiceImpl` | 四级可见权限判定 |
| 4 | `CommentService` | `CommentServiceImpl` | 回复管理 |
| 5 | `DivineCommentService` | `DivineCommentServiceImpl` | 神评推荐、双维度判定、掉标检查 |
| 6 | `CategoryService` | `CategoryServiceImpl` | 版块管理 |
| 7 | `NoticeService` | `NoticeServiceImpl` | 版块公告管理 |
| 8 | `NotificationService` | `NotificationServiceImpl` | 通知生成、已读管理 |
| 9 | `RecommendationService` | `RecommendationServiceImpl` | 个性化推荐、相似帖子 |
| 10 | `AiService` | `AiServiceImpl` | AI总结、问答、Prompt管理 |
| 11 | `DraftService` | `DraftServiceImpl` | 草稿Upsert、查询、清理 |
| 12 | `InteractionService` | `InteractionServiceImpl` | 点赞、收藏（防重、计数同步） |
| 13 | `FollowService` | `FollowServiceImpl` | 关注、粉丝管理 |
| 14 | `StatisticsService` | `StatisticsServiceImpl` | 数据统计（用户增长、帖子量等） |

### 12.3 Mapper

| # | 接口 | 对应表 |
|---|------|--------|
| 1 | `UserMapper` | `user` |
| 2 | `PostMapper` | `post` |
| 3 | `CommentMapper` | `comment` |
| 4 | `CommentRecommendMapper` | `comment_recommend` |
| 5 | `CategoryMapper` | `category` |
| 6 | `CategoryNoticeMapper` | `category_notice` |
| 7 | `UserLikeMapper` | `user_like` |
| 8 | `FavoriteMapper` | `favorite` |
| 9 | `FollowMapper` | `follow` |
| 10 | `NotificationMapper` | `notification` |
| 11 | `UserProfileMapper` | `user_profile` |
| 12 | `PostKeywordMapper` | `post_keyword` |
| 13 | `PostSimilarityMapper` | `post_similarity` |
| 14 | `AiSummaryMapper` | `ai_summary` |
| 15 | `AiQaHistoryMapper` | `ai_qa_history` |
| 16 | `PostDraftMapper` | `post_draft` |
| 17 | `FileDetailMapper` | `file_detail` |

### 12.4 安全组件

| # | 类 | 职责 |
|---|-----|------|
| 1 | `SecurityConfig` | SecurityFilterChain、BCrypt、认证管理器 |
| 2 | `JwtTokenProvider` | Token 生成、验证、解析 |
| 3 | `JwtAuthenticationFilter` | OncePerRequestFilter，提取Token注入SecurityContext |
| 4 | `SecurityUtils` | 静态工具，获取当前用户ID/角色 |

### 12.5 公共组件

| # | 类 | 职责 |
|---|-----|------|
| 1 | `R<T>` | 统一响应体 |
| 2 | `ResultCode` | 状态码枚举 |
| 3 | `BusinessException` | 业务异常 |
| 4 | `GlobalExceptionHandler` | 全局异常拦截转R格式 |
| 5 | `MybatisPlusConfig` | 分页插件、防全表更新 |
| 6 | `JacksonConfig` | Long→String序列化 |
| 7 | `RedisConfig` | RedisTemplate配置 |
| 8 | `WebMvcConfig` | CORS跨域 |
| 9 | `FileDetailRecorder` | x-file-storage FileRecorder 实现，自动持久化文件元数据 |

### 12.6 定时任务

| # | 类 | 执行周期 | 职责 |
|---|-----|---------|------|
| 1 | `DivineCommentScheduler` | 每5分钟 | 神评掉标检查 |
| 2 | `RecommendationScheduler` | 每天凌晨2点 | 更新用户画像、帖子相似度矩阵 |
| 3 | `HotPostScheduler` | 每小时 | 更新热门帖子缓存 |

---

## 附录 A：关键 API 矩阵（后端视角）

| 模块 | 接口 | Method | 认证 | 角色 | 核心逻辑 |
|------|------|--------|------|------|----------|
| 认证 | `/auth/register` | POST | 否 | - | BCrypt加密，唯一性校验 |
| 认证 | `/auth/login` | POST | 否 | - | 密码验证，生成JWT |
| 用户 | `/users/me` | GET | 是 | USER+ | 返回当前用户信息（脱敏） |
| 用户 | `/users/{id}` | GET | 否 | - | 公开信息（脱敏邮箱） |
| 帖子 | `/posts` | GET | 否 | - | 列表+可见性动态过滤+分页 |
| 帖子 | `/posts/{id}` | GET | 否 | - | 详情+可见性404判断 |
| 帖子 | `/posts` | POST | 是 | USER+ | 创建+关键词提取+草稿清理 |
| 帖子 | `/posts/{id}` | PATCH | 是 | 作者 | 编辑（可改visibility） |
| 帖子 | `/posts/{id}` | DELETE | 是 | 作者/ADMIN | 软删除 |
| 神评 | `/comments/{id}/recommend` | POST | 是 | USER+ | 防重+注册天数+双维度达标 |
| AI | `/posts/{id}/ai/summary` | POST | 是 | USER+ | 内容长度检查+异步调用 |
| AI | `/posts/{id}/ai/qa` | POST | 是 | USER+ | 需先有总结+Prompt限定 |
| 推荐 | `/recommendations` | GET | 否 | - | 用户画像匹配+可见性过滤 |
| 管理 | `/admin/users/{id}/ban` | PATCH | 是 | ADMIN | 封禁/解封 |
| 管理 | `/admin/posts/{id}/lock` | PATCH | 是 | ADMIN/MOD | 锁定/解锁 |
| 文件 | `/files/upload` | POST | 是 | USER+ | 上传图片（FileRecorder自动记录到file_detail） |

---

> 本文档涵盖 TechHub 后端工程的全部设计与实现细节。开发时请严格按照包结构组织代码，复用公共组件和 BaseMapper，确保事务一致性与安全隔离。所有业务逻辑放在 Service 层，Controller 仅负责参数接收与响应。

> **与前端文档对照阅读**：前端文档定义了 API 请求格式与交互细节，后端文档定义了接口实现与数据逻辑。两者的 API 路由、请求体、响应体格式严格一致。前后端协作时，以 `docs/TechHub 技术社区论坛 —— 综合课程设计方案.md` 的 API 设计章节为准。
