-- ============================================================
-- TechHub 数据表创建脚本 (SQL Server 2022)
-- 文件: 02_create_tables.sql
-- 说明: 17 张表 + CHECK 约束 + 外键约束
-- ============================================================

-- ==================== 用户表 ====================
IF OBJECT_ID('[user]', 'U') IS NOT NULL DROP TABLE [user];
CREATE TABLE [user] (
    [id]          BIGINT          NOT NULL,
    [username]    NVARCHAR(50)    NOT NULL,
    [password]    NVARCHAR(255)   NOT NULL,
    [email]       NVARCHAR(100)   NULL,
    [avatar_url]  NVARCHAR(500)   NULL,
    [bio]         NVARCHAR(500)   NULL,
    [role]        NVARCHAR(20)    NOT NULL DEFAULT 'USER',
    [status]      TINYINT         NOT NULL DEFAULT 1,
    [create_time] DATETIME2       NOT NULL DEFAULT GETDATE(),
    [update_time] DATETIME2       NOT NULL DEFAULT GETDATE(),
    CONSTRAINT [pk_user] PRIMARY KEY CLUSTERED ([id]),
    CONSTRAINT [uk_user_username] UNIQUE ([username]),
    CONSTRAINT [uk_user_email] UNIQUE ([email]),
    CONSTRAINT [chk_user_role] CHECK ([role] IN ('USER','MODERATOR','ADMIN')),
    CONSTRAINT [chk_user_status] CHECK ([status] IN (0, 1))
);
GO

-- ==================== 版块表 ====================
IF OBJECT_ID('[category]', 'U') IS NOT NULL DROP TABLE [category];
CREATE TABLE [category] (
    [id]          BIGINT          NOT NULL,
    [name]        NVARCHAR(50)    NOT NULL,
    [description] NVARCHAR(255)   NULL,
    [sort_order]  INT             NOT NULL DEFAULT 0,
    [status]      TINYINT         NOT NULL DEFAULT 1,
    [create_time] DATETIME2       NOT NULL DEFAULT GETDATE(),
    CONSTRAINT [pk_category] PRIMARY KEY CLUSTERED ([id]),
    CONSTRAINT [uk_category_name] UNIQUE ([name]),
    CONSTRAINT [chk_category_status] CHECK ([status] IN (0, 1))
);
GO

-- ==================== 帖子表 ====================
IF OBJECT_ID('[post]', 'U') IS NOT NULL DROP TABLE [post];
CREATE TABLE [post] (
    [id]                   BIGINT          NOT NULL,
    [title]                NVARCHAR(200)   NOT NULL,
    [content]              NVARCHAR(MAX)   NOT NULL,
    [category_id]          BIGINT          NOT NULL,
    [author_id]            BIGINT          NOT NULL,
    [type]                 TINYINT         NOT NULL DEFAULT 0,
    [status]               TINYINT         NOT NULL DEFAULT 1,
    [visibility]           TINYINT         NOT NULL DEFAULT 0,
    [view_count]           INT             NOT NULL DEFAULT 0,
    [like_count]           INT             NOT NULL DEFAULT 0,
    [comment_count]        INT             NOT NULL DEFAULT 0,
    [divine_comment_count] INT             NOT NULL DEFAULT 0,
    [eligible_for_divine]  TINYINT         NOT NULL DEFAULT 0,
    [create_time]          DATETIME2       NOT NULL DEFAULT GETDATE(),
    [update_time]          DATETIME2       NOT NULL DEFAULT GETDATE(),
    [deleted]              TINYINT         NOT NULL DEFAULT 0,
    CONSTRAINT [pk_post] PRIMARY KEY CLUSTERED ([id]),
    CONSTRAINT [fk_post_category] FOREIGN KEY ([category_id]) REFERENCES [category] ([id]),
    CONSTRAINT [fk_post_author] FOREIGN KEY ([author_id]) REFERENCES [user] ([id]),
    CONSTRAINT [chk_post_type] CHECK ([type] IN (0, 1, 2)),
    CONSTRAINT [chk_post_status] CHECK ([status] IN (0, 1, 2)),
    CONSTRAINT [chk_post_visibility] CHECK ([visibility] IN (0, 1, 2, 3)),
    CONSTRAINT [chk_post_deleted] CHECK ([deleted] IN (0, 1))
);
GO

-- ==================== 回复表 ====================
IF OBJECT_ID('[comment]', 'U') IS NOT NULL DROP TABLE [comment];
CREATE TABLE [comment] (
    [id]               BIGINT          NOT NULL,
    [content]          NVARCHAR(MAX)   NOT NULL,
    [post_id]          BIGINT          NOT NULL,
    [user_id]          BIGINT          NOT NULL,
    [parent_id]        BIGINT          NULL,
    [reply_to_user_id] BIGINT          NULL,
    [like_count]       INT             NOT NULL DEFAULT 0,
    [recommend_count]  INT             NOT NULL DEFAULT 0,
    [is_divine]        TINYINT         NOT NULL DEFAULT 0,
    [divine_time]      DATETIME2       NULL,
    [create_time]      DATETIME2       NOT NULL DEFAULT GETDATE(),
    CONSTRAINT [pk_comment] PRIMARY KEY CLUSTERED ([id]),
    CONSTRAINT [fk_comment_post] FOREIGN KEY ([post_id]) REFERENCES [post] ([id]) ON DELETE CASCADE,
    CONSTRAINT [fk_comment_user] FOREIGN KEY ([user_id]) REFERENCES [user] ([id]) ON DELETE CASCADE,
    CONSTRAINT [chk_comment_is_divine] CHECK ([is_divine] IN (0, 1))
);
GO

-- ==================== 版块公告表 ====================
IF OBJECT_ID('[category_notice]', 'U') IS NOT NULL DROP TABLE [category_notice];
CREATE TABLE [category_notice] (
    [id]          BIGINT          NOT NULL,
    [category_id] BIGINT          NOT NULL,
    [title]       NVARCHAR(200)   NOT NULL,
    [content]     NVARCHAR(MAX)   NOT NULL,
    [type]        TINYINT         NOT NULL DEFAULT 0,
    [author_id]   BIGINT          NOT NULL,
    [is_pinned]   TINYINT         NOT NULL DEFAULT 0,
    [status]      TINYINT         NOT NULL DEFAULT 1,
    [create_time] DATETIME2       NOT NULL DEFAULT GETDATE(),
    [update_time] DATETIME2       NOT NULL DEFAULT GETDATE(),
    CONSTRAINT [pk_category_notice] PRIMARY KEY CLUSTERED ([id]),
    CONSTRAINT [fk_notice_category] FOREIGN KEY ([category_id]) REFERENCES [category] ([id]) ON DELETE CASCADE,
    CONSTRAINT [fk_notice_author] FOREIGN KEY ([author_id]) REFERENCES [user] ([id]) ON DELETE CASCADE,
    CONSTRAINT [chk_notice_type] CHECK ([type] IN (0, 1)),
    CONSTRAINT [chk_notice_is_pinned] CHECK ([is_pinned] IN (0, 1)),
    CONSTRAINT [chk_notice_status] CHECK ([status] IN (0, 1))
);
GO

-- ==================== 点赞表 ====================
IF OBJECT_ID('[user_like]', 'U') IS NOT NULL DROP TABLE [user_like];
CREATE TABLE [user_like] (
    [id]          BIGINT          NOT NULL,
    [user_id]     BIGINT          NOT NULL,
    [target_type] NVARCHAR(10)    NOT NULL,
    [target_id]   BIGINT          NOT NULL,
    [create_time] DATETIME2       NOT NULL DEFAULT GETDATE(),
    CONSTRAINT [pk_user_like] PRIMARY KEY CLUSTERED ([id]),
    CONSTRAINT [uk_user_like_target] UNIQUE ([user_id], [target_type], [target_id]),
    CONSTRAINT [fk_like_user] FOREIGN KEY ([user_id]) REFERENCES [user] ([id]) ON DELETE CASCADE,
    CONSTRAINT [chk_like_target_type] CHECK ([target_type] IN ('POST', 'COMMENT'))
);
GO

-- ==================== 收藏表 ====================
IF OBJECT_ID('[favorite]', 'U') IS NOT NULL DROP TABLE [favorite];
CREATE TABLE [favorite] (
    [id]          BIGINT          NOT NULL,
    [user_id]     BIGINT          NOT NULL,
    [post_id]     BIGINT          NOT NULL,
    [create_time] DATETIME2       NOT NULL DEFAULT GETDATE(),
    CONSTRAINT [pk_favorite] PRIMARY KEY CLUSTERED ([id]),
    CONSTRAINT [uk_favorite_user_post] UNIQUE ([user_id], [post_id]),
    CONSTRAINT [fk_fav_user] FOREIGN KEY ([user_id]) REFERENCES [user] ([id]) ON DELETE CASCADE,
    CONSTRAINT [fk_fav_post] FOREIGN KEY ([post_id]) REFERENCES [post] ([id]) ON DELETE CASCADE
);
GO

-- ==================== 关注表 ====================
IF OBJECT_ID('[follow]', 'U') IS NOT NULL DROP TABLE [follow];
CREATE TABLE [follow] (
    [id]          BIGINT          NOT NULL,
    [follower_id] BIGINT          NOT NULL,
    [followee_id] BIGINT          NOT NULL,
    [create_time] DATETIME2       NOT NULL DEFAULT GETDATE(),
    CONSTRAINT [pk_follow] PRIMARY KEY CLUSTERED ([id]),
    CONSTRAINT [uk_follow_follower_followee] UNIQUE ([follower_id], [followee_id]),
    CONSTRAINT [fk_follow_follower] FOREIGN KEY ([follower_id]) REFERENCES [user] ([id]) ON DELETE CASCADE,
    CONSTRAINT [fk_follow_followee] FOREIGN KEY ([followee_id]) REFERENCES [user] ([id]) ON DELETE NO ACTION
);
GO

-- ==================== 通知表 ====================
IF OBJECT_ID('[notification]', 'U') IS NOT NULL DROP TABLE [notification];
CREATE TABLE [notification] (
    [id]          BIGINT          NOT NULL,
    [user_id]     BIGINT          NOT NULL,
    [type]        NVARCHAR(20)    NOT NULL,
    [source_id]   BIGINT          NULL,
    [content]     NVARCHAR(500)   NOT NULL,
    [is_read]     TINYINT         NOT NULL DEFAULT 0,
    [create_time] DATETIME2       NOT NULL DEFAULT GETDATE(),
    CONSTRAINT [pk_notification] PRIMARY KEY CLUSTERED ([id]),
    CONSTRAINT [fk_notif_user] FOREIGN KEY ([user_id]) REFERENCES [user] ([id]) ON DELETE CASCADE,
    CONSTRAINT [chk_notif_type] CHECK ([type] IN ('REPLY','LIKE','FOLLOW','DIVINE','SYSTEM')),
    CONSTRAINT [chk_notif_is_read] CHECK ([is_read] IN (0, 1))
);
GO

-- ==================== 神评推荐记录表 ====================
IF OBJECT_ID('[comment_recommend]', 'U') IS NOT NULL DROP TABLE [comment_recommend];
CREATE TABLE [comment_recommend] (
    [id]          BIGINT          NOT NULL,
    [comment_id]  BIGINT          NOT NULL,
    [user_id]     BIGINT          NOT NULL,
    [create_time] DATETIME2       NOT NULL DEFAULT GETDATE(),
    CONSTRAINT [pk_comment_recommend] PRIMARY KEY CLUSTERED ([id]),
    CONSTRAINT [uk_comment_recommend_comment_user] UNIQUE ([comment_id], [user_id]),
    CONSTRAINT [fk_rec_comment] FOREIGN KEY ([comment_id]) REFERENCES [comment] ([id]) ON DELETE CASCADE,
    CONSTRAINT [fk_rec_user] FOREIGN KEY ([user_id]) REFERENCES [user] ([id]) ON DELETE NO ACTION
);
GO

-- ==================== 草稿表 ====================
IF OBJECT_ID('[post_draft]', 'U') IS NOT NULL DROP TABLE [post_draft];
CREATE TABLE [post_draft] (
    [id]            BIGINT          NOT NULL,
    [user_id]       BIGINT          NOT NULL,
    [post_id]       BIGINT          NULL,
    [title]         NVARCHAR(200)   NULL,
    [content]       NVARCHAR(MAX)   NULL,
    [category_id]   BIGINT          NULL,
    [visibility]    TINYINT         NULL DEFAULT 0,
    [last_saved_at] DATETIME2       NULL,
    [create_time]   DATETIME2       NOT NULL DEFAULT GETDATE(),
    [update_time]   DATETIME2       NOT NULL DEFAULT GETDATE(),
    CONSTRAINT [pk_post_draft] PRIMARY KEY CLUSTERED ([id]),
    CONSTRAINT [uk_post_draft_user_post] UNIQUE ([user_id], [post_id]),
    CONSTRAINT [fk_draft_user] FOREIGN KEY ([user_id]) REFERENCES [user] ([id]) ON DELETE CASCADE
);
GO

-- ==================== AI 总结表 ====================
IF OBJECT_ID('[ai_summary]', 'U') IS NOT NULL DROP TABLE [ai_summary];
CREATE TABLE [ai_summary] (
    [id]            BIGINT          NOT NULL,
    [user_id]       BIGINT          NOT NULL,
    [post_id]       BIGINT          NOT NULL,
    [content]       NVARCHAR(MAX)   NULL,
    [status]        TINYINT         NOT NULL DEFAULT 0,
    [error_message] NVARCHAR(500)   NULL,
    [create_time]   DATETIME2       NOT NULL DEFAULT GETDATE(),
    [update_time]   DATETIME2       NOT NULL DEFAULT GETDATE(),
    CONSTRAINT [pk_ai_summary] PRIMARY KEY CLUSTERED ([id]),
    CONSTRAINT [uk_ai_summary_user_post] UNIQUE ([user_id], [post_id]),
    CONSTRAINT [fk_ai_summary_user] FOREIGN KEY ([user_id]) REFERENCES [user] ([id]) ON DELETE CASCADE,
    CONSTRAINT [fk_ai_summary_post] FOREIGN KEY ([post_id]) REFERENCES [post] ([id]) ON DELETE CASCADE,
    CONSTRAINT [chk_ai_summary_status] CHECK ([status] IN (0, 1, 2))
);
GO

-- ==================== AI 问答记录表 ====================
IF OBJECT_ID('[ai_qa_history]', 'U') IS NOT NULL DROP TABLE [ai_qa_history];
CREATE TABLE [ai_qa_history] (
    [id]          BIGINT          NOT NULL,
    [user_id]     BIGINT          NOT NULL,
    [post_id]     BIGINT          NOT NULL,
    [question]    NVARCHAR(MAX)   NOT NULL,
    [answer]      NVARCHAR(MAX)   NOT NULL,
    [create_time] DATETIME2       NOT NULL DEFAULT GETDATE(),
    CONSTRAINT [pk_ai_qa_history] PRIMARY KEY CLUSTERED ([id]),
    CONSTRAINT [fk_qa_user] FOREIGN KEY ([user_id]) REFERENCES [user] ([id]) ON DELETE CASCADE,
    CONSTRAINT [fk_qa_post] FOREIGN KEY ([post_id]) REFERENCES [post] ([id]) ON DELETE CASCADE
);
GO

-- ==================== 用户画像表 ====================
IF OBJECT_ID('[user_profile]', 'U') IS NOT NULL DROP TABLE [user_profile];
CREATE TABLE [user_profile] (
    [user_id]          BIGINT          NOT NULL,
    [keyword_weights]  NVARCHAR(MAX)   NULL,
    [last_update_time] DATETIME2       NULL,
    CONSTRAINT [pk_user_profile] PRIMARY KEY CLUSTERED ([user_id]),
    CONSTRAINT [fk_profile_user] FOREIGN KEY ([user_id]) REFERENCES [user] ([id]) ON DELETE CASCADE
);
GO

-- ==================== 帖子关键词表 ====================
IF OBJECT_ID('[post_keyword]', 'U') IS NOT NULL DROP TABLE [post_keyword];
CREATE TABLE [post_keyword] (
    [post_id]      BIGINT          NOT NULL,
    [keyword]      NVARCHAR(100)   NOT NULL,
    [tfidf_weight] FLOAT           NOT NULL DEFAULT 0,
    CONSTRAINT [pk_post_keyword] PRIMARY KEY CLUSTERED ([post_id], [keyword]),
    CONSTRAINT [fk_kw_post] FOREIGN KEY ([post_id]) REFERENCES [post] ([id]) ON DELETE CASCADE
);
GO

-- ==================== 帖子相似度表 ====================
IF OBJECT_ID('[post_similarity]', 'U') IS NOT NULL DROP TABLE [post_similarity];
CREATE TABLE [post_similarity] (
    [post_id_a]        BIGINT          NOT NULL,
    [post_id_b]        BIGINT          NOT NULL,
    [similarity_score] FLOAT           NOT NULL DEFAULT 0,
    CONSTRAINT [pk_post_similarity] PRIMARY KEY CLUSTERED ([post_id_a], [post_id_b]),
    CONSTRAINT [fk_sim_post_a] FOREIGN KEY ([post_id_a]) REFERENCES [post] ([id]) ON DELETE CASCADE,
    CONSTRAINT [fk_sim_post_b] FOREIGN KEY ([post_id_b]) REFERENCES [post] ([id]) ON DELETE NO ACTION
);
GO

-- ==================== 文件记录表 ====================
IF OBJECT_ID('[file_detail]', 'U') IS NOT NULL DROP TABLE [file_detail];
CREATE TABLE [file_detail] (
    [id]                NVARCHAR(32)    NOT NULL,
    [url]               NVARCHAR(512)   NOT NULL,
    [size]              BIGINT          NULL,
    [filename]          NVARCHAR(256)   NULL,
    [original_filename] NVARCHAR(256)   NULL,
    [base_path]         NVARCHAR(256)   NULL,
    [path]              NVARCHAR(256)   NULL,
    [ext]               NVARCHAR(32)    NULL,
    [content_type]      NVARCHAR(128)   NULL,
    [platform]          NVARCHAR(32)    NULL,
    [th_url]            NVARCHAR(512)   NULL,
    [th_filename]       NVARCHAR(256)   NULL,
    [th_size]           BIGINT          NULL,
    [th_content_type]   NVARCHAR(128)   NULL,
    [object_id]         NVARCHAR(32)    NULL,
    [object_type]       NVARCHAR(32)    NULL,
    [metadata]          NVARCHAR(MAX)   NULL,
    [user_metadata]     NVARCHAR(MAX)   NULL,
    [th_metadata]       NVARCHAR(MAX)   NULL,
    [th_user_metadata]  NVARCHAR(MAX)   NULL,
    [attr]              NVARCHAR(MAX)   NULL,
    [file_acl]          NVARCHAR(32)    NULL,
    [th_file_acl]       NVARCHAR(32)    NULL,
    [hash_info]         NVARCHAR(MAX)   NULL,
    [upload_id]         NVARCHAR(128)   NULL,
    [upload_status]     INT             NULL,
    [create_time]       DATETIME2       NULL,
    CONSTRAINT [pk_file_detail] PRIMARY KEY CLUSTERED ([id])
);
GO

-- ==================== 文件分片信息表 ====================
IF OBJECT_ID('[file_part_detail]', 'U') IS NOT NULL DROP TABLE [file_part_detail];
CREATE TABLE [file_part_detail] (
    [id]          NVARCHAR(32)    NOT NULL,
    [platform]    NVARCHAR(32)    NULL,
    [upload_id]   NVARCHAR(128)   NULL,
    [e_tag]       NVARCHAR(255)   NULL,
    [part_number] INT             NULL,
    [part_size]   BIGINT          NULL,
    [hash_info]   NVARCHAR(MAX)   NULL,
    [create_time] DATETIME2       NULL,
    CONSTRAINT [pk_file_part_detail] PRIMARY KEY CLUSTERED ([id])
);
GO
