-- ==================== TechHub 数据库初始化脚本 ====================
-- 基于 TechHub 后端开发文档 §5 数据库实现
-- 全部表强制：ENGINE=InnoDB, utf8mb4, BIGINT 雪花主键
-- 索引已按索引审计员评审结果优化

-- 强制初始化会话字符集为 utf8mb4
-- MySQL Docker entrypoint 执行 /docker-entrypoint-initdb.d/*.sql 时会话字符集默认可能为 latin1,
-- --character-set-server 只影响服务端字符集,不影响客户端连接协商,会导致表注释/中文写入乱码。
SET NAMES utf8mb4;

-- 创建数据库（如不存在）
CREATE DATABASE IF NOT EXISTS `techhub`
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE `techhub`;

-- ==================== 用户表 ====================
CREATE TABLE `user` (
    `id`          BIGINT       NOT NULL COMMENT '雪花主键',
    `username`    VARCHAR(50)  NOT NULL COMMENT '用户名',
    `password`    VARCHAR(255) NOT NULL COMMENT 'BCrypt 加密密码',
    `email`       VARCHAR(100) DEFAULT NULL COMMENT '邮箱（选填）',
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
    UNIQUE KEY `uk_name` (`name`),
    KEY `idx_status_sort` (`status`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='版块表';

-- ==================== 版块公告表 ====================
CREATE TABLE `category_notice` (
    `id`          BIGINT       NOT NULL COMMENT '雪花主键',
    `category_id` BIGINT       NOT NULL COMMENT '版块ID',
    `title`       VARCHAR(200) NOT NULL COMMENT '公告标题',
    `content`     LONGTEXT         NOT NULL COMMENT '公告内容（Markdown）',
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
-- 索引已优化：idx_cat_del_stat_time / idx_cat_del_stat_hot 覆盖版块+逻辑删除+状态+排序
CREATE TABLE `post` (
    `id`                   BIGINT       NOT NULL COMMENT '雪花主键',
    `title`                VARCHAR(200) NOT NULL COMMENT '标题',
    `content`              LONGTEXT         NOT NULL COMMENT '内容（Markdown原文，内嵌图片由x-file-storage上传后引用URL）',
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
    KEY `idx_cat_del_stat_time` (`category_id`, `deleted`, `status`, `create_time`),
    KEY `idx_cat_del_stat_hot` (`category_id`, `deleted`, `status`, `like_count`, `create_time`),
    KEY `idx_type_time` (`type`, `create_time`),
    KEY `idx_author_del_time` (`author_id`, `deleted`, `create_time`),
    CONSTRAINT `fk_post_category` FOREIGN KEY (`category_id`) REFERENCES `category` (`id`),
    CONSTRAINT `fk_post_author` FOREIGN KEY (`author_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='帖子表';

-- ==================== 回复表 ====================
-- 索引已优化：追加 idx_parent_time 树形回复、idx_post_divine_time 神评查询
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
    KEY `idx_parent_time` (`parent_id`, `create_time`),
    KEY `idx_post_divine_time` (`post_id`, `is_divine`, `create_time`),
    CONSTRAINT `fk_comment_post` FOREIGN KEY (`post_id`) REFERENCES `post` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_comment_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='回复表';

-- ==================== 点赞表 ====================
CREATE TABLE `user_like` (
    `id`          BIGINT      NOT NULL COMMENT '雪花主键',
    `user_id`     BIGINT      NOT NULL COMMENT '用户ID',
    `target_type` VARCHAR(10) NOT NULL COMMENT '目标类型 POST/COMMENT',
    `target_id`   BIGINT      NOT NULL COMMENT '目标ID',
    `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
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
    `source_id`   BIGINT       DEFAULT NULL COMMENT '来源ID（帖子/评论/用户ID）',
    `source_type` VARCHAR(20)  NOT NULL DEFAULT 'UNKNOWN' COMMENT '来源类型 POST/COMMENT/USER',
    `parent_id`   BIGINT       DEFAULT NULL COMMENT '上级帖子ID（来源为评论时记录所属帖子ID）',
    `content`     VARCHAR(500) NOT NULL COMMENT '通知内容',
    `is_read`     TINYINT      NOT NULL DEFAULT 0 COMMENT '是否已读 0否 1是',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_read_time` (`user_id`, `is_read`, `create_time`),
    CONSTRAINT `fk_notif_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知表';

-- ==================== 神评推荐记录表 ====================
-- 索引已优化：删除冗余 idx_comment_id（与 uk_comment_user 前导列重复），仅保留唯一索引
CREATE TABLE `comment_recommend` (
    `id`          BIGINT   NOT NULL COMMENT '雪花主键',
    `comment_id`  BIGINT   NOT NULL COMMENT '评论ID',
    `user_id`     BIGINT   NOT NULL COMMENT '推荐用户ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '推荐时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_comment_user` (`comment_id`, `user_id`),
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
    `post_id`      BIGINT       NOT NULL COMMENT '帖子ID',
    `keyword`      VARCHAR(100) NOT NULL COMMENT '关键词',
    `tfidf_weight` DOUBLE       NOT NULL DEFAULT 0 COMMENT 'TF-IDF 权重',
    PRIMARY KEY (`post_id`, `keyword`),
    KEY `idx_keyword` (`keyword`),
    CONSTRAINT `fk_kw_post` FOREIGN KEY (`post_id`) REFERENCES `post` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='帖子关键词表';

-- ==================== 帖子相似度表（推荐系统） ====================
CREATE TABLE `post_similarity` (
    `post_id_a`        BIGINT NOT NULL COMMENT '帖子A的ID',
    `post_id_b`        BIGINT NOT NULL COMMENT '帖子B的ID',
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
    `content`       LONGTEXT     DEFAULT NULL COMMENT 'AI 总结内容',
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
-- 索引已优化：idx_user_post 替换为 idx_user_post_time 覆盖问答历史时间排序
CREATE TABLE `ai_qa_history` (
    `id`          BIGINT   NOT NULL COMMENT '雪花主键',
    `user_id`     BIGINT   NOT NULL COMMENT '用户ID',
    `post_id`     BIGINT   NOT NULL COMMENT '帖子ID',
    `question`    TEXT     NOT NULL COMMENT '用户问题',
    `answer`      LONGTEXT     NOT NULL COMMENT 'AI 回答',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提问时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_post_time` (`user_id`, `post_id`, `create_time`),
    CONSTRAINT `fk_qa_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_qa_post` FOREIGN KEY (`post_id`) REFERENCES `post` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI问答记录表';

-- ==================== 帖子草稿表 ====================
-- 索引已优化：idx_user_id 替换为 idx_user_update 覆盖草稿列表按更新时间排序
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
    UNIQUE KEY `uk_user_post_draft` (`user_id`, `post_id`),
    KEY `idx_user_update` (`user_id`, `update_time`),
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
