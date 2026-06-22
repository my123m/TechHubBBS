-- ============================================================
-- TechHub 数据库完整初始化脚本 (SQL Server 2022)
-- 说明: 合并 01~08 共 8 个脚本为单一 T-SQL 文件
--       依次创建: 数据库 → 表 → 索引 → 视图 → 存储过程 → 函数 → 触发器 → 种子数据
-- 使用: sqlcmd -S <server> -i 00_combined.sql
-- ============================================================

-- ==================== 第一部分：创建数据库 ====================
IF DB_ID('techhub') IS NULL
    CREATE DATABASE [techhub];
GO

USE [techhub];
GO

-- ==================== 第二部分：创建数据表 (17张) ====================
IF OBJECT_ID('[user]', 'U') IS NOT NULL DROP TABLE [user];
CREATE TABLE [user] (
    [id]          BIGINT          NOT NULL,
    [username]    NVARCHAR(50)    NOT NULL,
    [password]    NVARCHAR(255)   NOT NULL,
    [email]       NVARCHAR(100)   NOT NULL,
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

IF OBJECT_ID('[user_profile]', 'U') IS NOT NULL DROP TABLE [user_profile];
CREATE TABLE [user_profile] (
    [user_id]          BIGINT          NOT NULL,
    [keyword_weights]  NVARCHAR(MAX)   NULL,
    [last_update_time] DATETIME2       NULL,
    CONSTRAINT [pk_user_profile] PRIMARY KEY CLUSTERED ([user_id]),
    CONSTRAINT [fk_profile_user] FOREIGN KEY ([user_id]) REFERENCES [user] ([id]) ON DELETE CASCADE
);
GO

IF OBJECT_ID('[post_keyword]', 'U') IS NOT NULL DROP TABLE [post_keyword];
CREATE TABLE [post_keyword] (
    [post_id]      BIGINT          NOT NULL,
    [keyword]      NVARCHAR(100)   NOT NULL,
    [tfidf_weight] FLOAT           NOT NULL DEFAULT 0,
    CONSTRAINT [pk_post_keyword] PRIMARY KEY CLUSTERED ([post_id], [keyword]),
    CONSTRAINT [fk_kw_post] FOREIGN KEY ([post_id]) REFERENCES [post] ([id]) ON DELETE CASCADE
);
GO

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

-- ==================== 第三部分：创建索引 ====================
CREATE NONCLUSTERED INDEX [idx_category_status_sort]
    ON [category] ([status], [sort_order]);
GO

CREATE NONCLUSTERED INDEX [idx_post_cat_del_stat_time]
    ON [post] ([category_id], [deleted], [status], [create_time]);
GO

CREATE NONCLUSTERED INDEX [idx_post_cat_del_stat_hot]
    ON [post] ([category_id], [deleted], [status], [like_count], [create_time]);
GO

CREATE NONCLUSTERED INDEX [idx_post_type_time]
    ON [post] ([type], [create_time]);
GO

CREATE NONCLUSTERED INDEX [idx_post_author_del_time]
    ON [post] ([author_id], [deleted], [create_time]);
GO

CREATE NONCLUSTERED INDEX [idx_comment_post_create_time]
    ON [comment] ([post_id], [create_time]);
GO

CREATE NONCLUSTERED INDEX [idx_comment_user_id]
    ON [comment] ([user_id]);
GO

CREATE NONCLUSTERED INDEX [idx_comment_parent_time]
    ON [comment] ([parent_id], [create_time]);
GO

CREATE NONCLUSTERED INDEX [idx_comment_post_divine_time]
    ON [comment] ([post_id], [is_divine], [create_time]);
GO

CREATE NONCLUSTERED INDEX [idx_notice_cat_status_pinned_time]
    ON [category_notice] ([category_id], [status], [is_pinned], [create_time]);
GO

CREATE NONCLUSTERED INDEX [idx_notice_author_id]
    ON [category_notice] ([author_id]);
GO

CREATE NONCLUSTERED INDEX [idx_follow_followee_id]
    ON [follow] ([followee_id]);
GO

CREATE NONCLUSTERED INDEX [idx_notification_user_read_time]
    ON [notification] ([user_id], [is_read], [create_time]);
GO

CREATE NONCLUSTERED INDEX [idx_ai_qa_history_user_post_time]
    ON [ai_qa_history] ([user_id], [post_id], [create_time]);
GO

CREATE NONCLUSTERED INDEX [idx_post_draft_user_update]
    ON [post_draft] ([user_id], [update_time]);
GO

CREATE NONCLUSTERED INDEX [idx_post_keyword_keyword]
    ON [post_keyword] ([keyword]);
GO

CREATE NONCLUSTERED INDEX [idx_post_similarity_post_b]
    ON [post_similarity] ([post_id_b]);
GO

CREATE NONCLUSTERED INDEX [idx_file_detail_object]
    ON [file_detail] ([object_type], [object_id]);
GO

-- idx_file_detail_url 已移除: url 列为 NVARCHAR(512)=1024字节, 超出非聚集索引键最大 900 字节限制
-- 如需按 URL 查询, 可考虑对 url 的前 450 个字符建索引或使用 INCLUDE 列

-- ==================== 第四部分：创建视图 ====================
IF OBJECT_ID('v_hot_posts', 'V') IS NOT NULL DROP VIEW [v_hot_posts];
GO
CREATE VIEW [v_hot_posts] AS
SELECT
    p.[id],
    p.[title],
    p.[view_count],
    p.[like_count],
    p.[comment_count],
    c.[name]        AS [category_name],
    u.[username]    AS [author_name],
    p.[type],
    p.[visibility],
    p.[create_time]
FROM [post] p
JOIN [category] c ON p.[category_id] = c.[id]
JOIN [user] u ON p.[author_id] = u.[id]
WHERE p.[deleted] = 0
  AND p.[status] = 1
  AND p.[visibility] = 0;
GO

IF OBJECT_ID('v_user_stats', 'V') IS NOT NULL DROP VIEW [v_user_stats];
GO
CREATE VIEW [v_user_stats] AS
SELECT
    u.[id],
    u.[username],
    u.[role],
    (SELECT COUNT(*) FROM [post] WHERE [author_id] = u.[id] AND [deleted] = 0) AS [post_count],
    (SELECT COUNT(*) FROM [follow] WHERE [followee_id] = u.[id]) AS [follower_count],
    (SELECT COUNT(*) FROM [follow] WHERE [follower_id] = u.[id]) AS [following_count],
    u.[create_time]
FROM [user] u;
GO

IF OBJECT_ID('v_post_detail', 'V') IS NOT NULL DROP VIEW [v_post_detail];
GO
CREATE VIEW [v_post_detail] AS
SELECT
    p.[id],
    p.[title],
    p.[content],
    p.[category_id],
    p.[author_id],
    p.[type],
    p.[status],
    p.[visibility],
    p.[view_count],
    p.[like_count],
    p.[comment_count],
    p.[divine_comment_count],
    p.[eligible_for_divine],
    p.[create_time],
    p.[update_time],
    p.[deleted],
    c.[name]     AS [category_name],
    u.[username] AS [author_name],
    u.[avatar_url]
FROM [post] p
JOIN [category] c ON p.[category_id] = c.[id]
JOIN [user] u ON p.[author_id] = u.[id]
WHERE p.[deleted] = 0;
GO

-- ==================== 第五部分：创建存储过程 ====================
IF OBJECT_ID('sp_publish_post', 'P') IS NOT NULL DROP PROCEDURE [sp_publish_post];
GO
CREATE PROCEDURE [sp_publish_post]
    @post_id     BIGINT,
    @title       NVARCHAR(200),
    @content     NVARCHAR(MAX),
    @category_id BIGINT,
    @author_id   BIGINT,
    @visibility  TINYINT = 0
AS
BEGIN
    SET NOCOUNT ON;
    BEGIN TRANSACTION;
    BEGIN TRY
        INSERT INTO [post] ([id], [title], [content], [category_id], [author_id], [visibility], [type], [status])
        VALUES (@post_id, @title, @content, @category_id, @author_id, @visibility, 0, 1);

        DELETE FROM [post_draft] WHERE [user_id] = @author_id AND [post_id] IS NULL;

        COMMIT TRANSACTION;
    END TRY
    BEGIN CATCH
        IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
        THROW;
    END CATCH
END;
GO

IF OBJECT_ID('sp_toggle_like', 'P') IS NOT NULL DROP PROCEDURE [sp_toggle_like];
GO
CREATE PROCEDURE [sp_toggle_like]
    @like_id     BIGINT,
    @user_id     BIGINT,
    @target_type NVARCHAR(10),
    @target_id   BIGINT
AS
BEGIN
    SET NOCOUNT ON;
    BEGIN TRANSACTION;
    BEGIN TRY
        IF EXISTS (SELECT 1 FROM [user_like]
                   WHERE [user_id] = @user_id
                     AND [target_type] = @target_type
                     AND [target_id] = @target_id)
        BEGIN
            DELETE FROM [user_like]
            WHERE [user_id] = @user_id
              AND [target_type] = @target_type
              AND [target_id] = @target_id;

            IF @target_type = 'POST'
                UPDATE [post] SET [like_count] = [like_count] - 1 WHERE [id] = @target_id;
            ELSE IF @target_type = 'COMMENT'
                UPDATE [comment] SET [like_count] = [like_count] - 1 WHERE [id] = @target_id;
        END
        ELSE
        BEGIN
            INSERT INTO [user_like] ([id], [user_id], [target_type], [target_id])
            VALUES (@like_id, @user_id, @target_type, @target_id);

            IF @target_type = 'POST'
                UPDATE [post] SET [like_count] = [like_count] + 1 WHERE [id] = @target_id;
            ELSE IF @target_type = 'COMMENT'
                UPDATE [comment] SET [like_count] = [like_count] + 1 WHERE [id] = @target_id;
        END

        COMMIT TRANSACTION;
    END TRY
    BEGIN CATCH
        IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
        THROW;
    END CATCH
END;
GO

IF OBJECT_ID('sp_delete_post', 'P') IS NOT NULL DROP PROCEDURE [sp_delete_post];
GO
CREATE PROCEDURE [sp_delete_post]
    @post_id BIGINT
AS
BEGIN
    SET NOCOUNT ON;
    UPDATE [post] SET [deleted] = 1, [update_time] = GETDATE() WHERE [id] = @post_id;
END;
GO

-- ==================== 第六部分：创建自定义函数 ====================
IF OBJECT_ID('fn_get_user_role', 'FN') IS NOT NULL DROP FUNCTION [fn_get_user_role];
GO
CREATE FUNCTION [fn_get_user_role](@user_id BIGINT)
RETURNS NVARCHAR(20)
AS
BEGIN
    DECLARE @role NVARCHAR(20);
    SELECT @role = [role] FROM [user] WHERE [id] = @user_id;
    RETURN ISNULL(@role, N'GUEST');
END;
GO

IF OBJECT_ID('fn_calc_hot_score', 'FN') IS NOT NULL DROP FUNCTION [fn_calc_hot_score];
GO
CREATE FUNCTION [fn_calc_hot_score](@post_id BIGINT)
RETURNS FLOAT
AS
BEGIN
    DECLARE @score FLOAT;
    SELECT @score = [like_count] * 3.0 + [view_count] * 0.1 + [comment_count] * 5.0
    FROM [post]
    WHERE [id] = @post_id;
    RETURN ISNULL(@score, 0);
END;
GO

-- ==================== 第七部分：创建触发器 ====================
IF OBJECT_ID('trg_user_update_time', 'TR') IS NOT NULL DROP TRIGGER [trg_user_update_time];
GO
CREATE TRIGGER [trg_user_update_time] ON [user] AFTER UPDATE AS
BEGIN
    SET NOCOUNT ON;
    UPDATE [user] SET [update_time] = GETDATE()
    WHERE [id] IN (SELECT [id] FROM [inserted]);
END;
GO

IF OBJECT_ID('trg_post_update_time', 'TR') IS NOT NULL DROP TRIGGER [trg_post_update_time];
GO
CREATE TRIGGER [trg_post_update_time] ON [post] AFTER UPDATE AS
BEGIN
    SET NOCOUNT ON;
    UPDATE [post] SET [update_time] = GETDATE()
    WHERE [id] IN (SELECT [id] FROM [inserted]);
END;
GO

IF OBJECT_ID('trg_notice_update_time', 'TR') IS NOT NULL DROP TRIGGER [trg_notice_update_time];
GO
CREATE TRIGGER [trg_notice_update_time] ON [category_notice] AFTER UPDATE AS
BEGIN
    SET NOCOUNT ON;
    UPDATE [category_notice] SET [update_time] = GETDATE()
    WHERE [id] IN (SELECT [id] FROM [inserted]);
END;
GO

IF OBJECT_ID('trg_ai_summary_update_time', 'TR') IS NOT NULL DROP TRIGGER [trg_ai_summary_update_time];
GO
CREATE TRIGGER [trg_ai_summary_update_time] ON [ai_summary] AFTER UPDATE AS
BEGIN
    SET NOCOUNT ON;
    UPDATE [ai_summary] SET [update_time] = GETDATE()
    WHERE [id] IN (SELECT [id] FROM [inserted]);
END;
GO

IF OBJECT_ID('trg_post_draft_update_time', 'TR') IS NOT NULL DROP TRIGGER [trg_post_draft_update_time];
GO
CREATE TRIGGER [trg_post_draft_update_time] ON [post_draft] AFTER UPDATE AS
BEGIN
    SET NOCOUNT ON;
    UPDATE [post_draft] SET [update_time] = GETDATE()
    WHERE [id] IN (SELECT [id] FROM [inserted]);
END;
GO

IF OBJECT_ID('trg_after_comment_insert', 'TR') IS NOT NULL DROP TRIGGER [trg_after_comment_insert];
GO
CREATE TRIGGER [trg_after_comment_insert] ON [comment] AFTER INSERT AS
BEGIN
    SET NOCOUNT ON;

    -- 更新帖子评论数
    UPDATE [post] SET [comment_count] = [comment_count] + 1
    WHERE [id] IN (SELECT [post_id] FROM [inserted]);

    -- 达到 10 条评论开启神评资格
    UPDATE [post] SET [eligible_for_divine] = 1
    WHERE [id] IN (SELECT [post_id] FROM [inserted])
      AND [comment_count] >= 10;
END;
GO

-- ==================== 第八部分：插入种子数据 ====================

-- ==================== user ====================
INSERT INTO [user] ([id], [username], [password], [email], [avatar_url], [bio], [role], [status], [create_time], [update_time]) VALUES (1, 'admin',    '$2a$10$sDZZncozKdyznb.ePB7qxuQGQWRjk/EFENqWsi8c82DSCRAdthDT2', 'admin@techhub.com',    NULL, N'系统管理员，热爱技术，擅长全栈开发',                              'ADMIN',     1, '2026-01-01 08:00:00', '2026-01-01 08:00:00'),
(2, 'moderator', '$2a$10$sDZZncozKdyznb.ePB7qxuQGQWRjk/EFENqWsi8c82DSCRAdthDT2', 'moderator@techhub.com', NULL, N'版主，Java 后端开发者，开源爱好者',                               'MODERATOR', 1, '2026-01-02 10:00:00', '2026-01-02 10:00:00'),
(3, 'user',     '$2a$10$sDZZncozKdyznb.ePB7qxuQGQWRjk/EFENqWsi8c82DSCRAdthDT2', 'user@techhub.com',      NULL, N'全栈工程师，正在学习 Spring Boot 和 Vue 3，欢迎交流',                'USER',      1, '2026-01-15 14:30:00', '2026-01-15 14:30:00');
GO

-- ==================== category ====================
INSERT INTO [category] ([id], [name], [description], [sort_order], [status], [create_time]) VALUES (10, N'技术讨论',  N'讨论前沿技术、架构设计、编程语言等各类技术话题',                1, 1, '2026-01-01 08:00:00'),
(11, N'问答求助',  N'遇到技术难题？在这里提问，社区小伙伴帮你解答',                  2, 1, '2026-01-01 08:00:00'),
(12, N'项目展示',  N'展示你的开源项目或作品，获取反馈与建议',                        3, 1, '2026-01-01 08:00:00'),
(13, N'资源分享',  N'分享优质学习资料、工具、书籍、视频教程等',                      4, 1, '2026-01-01 08:00:00'),
(14, N'站务管理',  N'社区公告、规则说明、意见反馈、版主招募等',                      5, 1, '2026-01-01 08:00:00');
GO

-- ==================== post ====================
INSERT INTO [post] ([id], [title], [content], [category_id], [author_id], [type], [status], [visibility], [view_count], [like_count], [comment_count], [divine_comment_count], [eligible_for_divine], [create_time], [update_time], [deleted]) VALUES (30, N'Spring Boot 3.5 最佳实践总结',
 N'## 前言

Spring Boot 3.5 带来了许多令人兴奋的特性，我在最近的项目迁移中总结了一些最佳实践，分享给大家。

## 1. 使用虚拟线程

```java
// application.yml
spring:
  threads:
    virtual:
      enabled: true
```

虚拟线程（Project Loom）在 I/O 密集型场景下性能提升显著。

> 注意：虚拟线程不适合 CPU 密集型任务，此时应使用传统平台线程。

## 2. 结构化日志

```java
@Slf4j
public class OrderService {
    public void createOrder(OrderDTO dto) {
        log.atInfo()
            .addKeyValue("userId", dto.getUserId())
            .addKeyValue("amount", dto.getAmount())
            .log("创建订单");
    }
}
```

结构化日志配合 ELK 或 Loki 可以极大提升问题排查效率。

## 3. 优雅的异常处理

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    public R<Void> handleBusiness(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());
        return R.error(e.getCode(), e.getMessage());
    }
}
```

## 4. 分层架构实践

- **Controller**: 只做参数校验和路由
- **Service**: 业务逻辑 + 事务
- **Mapper**: 数据访问（MyBatis-Plus）
- **Entity/DTO**: 数据模型分离

## 总结

Spring Boot 3.5 的升级过程相对平滑，建议大家在迁移时先从非核心服务开始，逐步推进。',
 10, 1, 1, 1, 0, 128, 15, 4, 0, 0, '2026-02-01 10:00:00', '2026-02-10 15:30:00', 0),
(31, N'Vue 3 Composition API 实战指南',
 N'## 为什么选择 Composition API？

Composition API 解决了 Options API 在复杂组件中的逻辑复用难题。

## 基础用法

```vue
<script setup lang="ts">
import { ref, computed, onMounted } from ''vue''

const count = ref(0)
const double = computed(() => count.value * 2)

function increment() {
  count.value++
}

onMounted(() => {
  console.log(''组件已挂载'')
})
</script>
```

## 自定义 Hook

```typescript
// useCounter.ts
import { ref } from ''vue''

export function useCounter(initialValue = 0) {
  const count = ref(initialValue)
  const increment = () => count.value++
  const decrement = () => count.value--
  
  return { count, increment, decrement }
}
```

## 与 TypeScript 结合

```typescript
interface User {
  id: number
  name: string
  email: string
}

const user = ref<User | null>(null)
```

## 常见陷阱

1. **响应式丢失**：解构 `reactive` 对象会导致响应式丢失，应使用 `toRefs`
2. **watch 的深度监听**：监听 `reactive` 对象默认开启深度监听
3. **组件通信**：推荐使用 `provide/inject` 替代多层 props 透传

## 推荐组合

- 状态管理：Pinia
- 路由：Vue Router 4
- HTTP：ofetch 或 axios

希望这篇指南对大家有帮助！',
 10, 2, 0, 1, 0, 89, 10, 3, 0, 0, '2026-02-10 14:00:00', '2026-02-15 09:20:00', 0),
(32, N'Docker 容器化部署从入门到实践',
 N'## 为什么需要容器化？

容器化解决了「在我机器上能跑」的经典问题。

## Dockerfile 编写

```dockerfile
# 多阶段构建
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

## Docker Compose

```yaml
version: ''3.8''
services:
  app:
    build: .
    ports:
      - "8080:8080"
    depends_on:
      - mysql
      - redis
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: root
  redis:
    image: redis:7
```

## 常用命令

```bash
# 构建镜像
docker build -t myapp:latest .

# 启动服务
docker-compose up -d

# 查看日志
docker-compose logs -f

# 清理
docker system prune -a
```

容器化部署是现代 DevOps 的基石，掌握它是每个后端开发者的必备技能。',
 10, 3, 0, 1, 1, 67, 8, 1, 0, 0, '2026-03-05 09:00:00', '2026-03-05 09:00:00', 0),
(33, N'微服务架构设计中的十大陷阱',
 N'## 1. 过度拆分

微服务不是拆得越细越好。对于小型团队来说，过度拆分会导致维护成本急剧上升。

**建议**：从单体起步，当确实遇到瓶颈时再拆分。

## 2. 忽略分布式事务

```java
// 使用 Saga 模式
@Saga
public class OrderSaga {
    @Step
    public void createOrder() { /* ... */ }
    
    @Compensation
    public void rollbackOrder() { /* ... */ }
}
```

## 3. 服务间通信过于复杂

同步调用（HTTP/gRPC）与异步消息（Kafka/RabbitMQ）需要合理搭配。

## 4. 缺乏可观测性

```yaml
# 必须的三件套
- 日志：ELK / Loki
- 指标：Prometheus + Grafana
- 链路追踪：Jaeger / Zipkin
```

## 5. 忽略配置管理

使用 Nacos / Apollo / Consul 统一管理配置，避免硬编码。

## 6. 数据库设计不当

每个服务应有自己的数据库，避免共享数据库。

## 7. 部署复杂度被低估

推荐使用 Kubernetes 管理容器编排，或至少使用 Docker Compose。

## 8. 忽略 API 版本管理

```
/v1/orders
/v2/orders  // 向后兼容
```

## 9. 测试策略不完善

- 单元测试：服务内部逻辑
- 契约测试：服务间接口
- 端到端测试：关键业务流程

## 10. 团队结构未对齐

康威定律：系统结构会反映团队结构。两个披萨团队对应 2-3 个微服务。

> 微服务是一种手段，不是目的。',
 10, 1, 0, 1, 0, 156, 20, 5, 1, 0, '2026-02-20 16:00:00', '2026-03-01 10:00:00', 0),
-- 问答求助
(34, N'MySQL 联合索引最左前缀原则求助',
 N'## 问题

我有一个 `user` 表，建立了联合索引 `idx_a_b_c(a, b, c)`，以下查询是否能用到索引？

```sql
SELECT * FROM user WHERE b = 1 AND c = 2;
SELECT * FROM user WHERE a = 1 AND c = 2;
SELECT * FROM user WHERE a = 1 ORDER BY c;
```

## 我的理解
1. 第一个查询：没用，因为跳过了 a
2. 第二个查询：只用到了 a 部分的索引
3. 第三个查询：可以用到索引，但 ORDER BY c 时由于跳过了 b，可能走 filesort

请问我的理解对吗？恳请大佬指点！',
 11, 3, 0, 1, 0, 95, 12, 4, 0, 0, '2026-03-10 11:00:00', '2026-03-10 11:00:00', 0),
(35, N'JWT Token 刷新机制的最佳实践',
 N'## 背景

我正在开发一个前后端分离的项目，JWT Token 有效期设置为 24 小时。现在需要实现 Token 自动刷新功能，但不确定哪种方案更好。

## 方案一：双 Token 方案
- Access Token（短期，15 分钟）+ Refresh Token（长期，7 天）
- Refresh Token 存储在数据库中，可以撤销

## 方案二：单 Token + 刷新接口
- Access Token 有效期 2 小时
- 在 Token 过期前调用刷新接口获取新 Token
- 使用 Redis 黑名单机制让旧 Token 失效

## 方案三：无刷新方案
- Token 有效期 24 小时
- 过期后重新登录

## 我的疑问
1. 大家的生产项目中用哪种方案比较多？
2. Refresh Token 需要存储吗？还是可以用签名验证？
3. 并发场景下 Token 刷新竞态问题怎么处理？',
 11, 3, 0, 1, 0, 112, 18, 5, 1, 0, '2026-03-15 15:00:00', '2026-03-15 15:00:00', 0),
(36, N'Redis 缓存雪崩和穿透的预防方案',
 N'## 问题场景

我们有一个高并发的查询接口，QPS 峰值到 3000+，最近出现了几次缓存雪崩导致数据库被打挂的情况。

## 当前方案

```java
public Object getData(String key) {
    // 1. 查缓存
    Object data = redisTemplate.opsForValue().get(key);
    if (data != null) return data;
    
    // 2. 查数据库
    data = queryFromDatabase(key);
    
    // 3. 回填缓存
    redisTemplate.opsForValue().set(key, data, 10, TimeUnit.MINUTES);
    return data;
}
```

## 存在问题
1. 缓存同时过期导致雪崩
2. 大量不存在的 key 导致穿透

## 已了解的方案
- 缓存 TTL 加随机值防雪崩
- 布隆过滤器防穿透
- 互斥锁保障击穿

请问还有哪些更好的实践推荐？',
 11, 2, 0, 1, 0, 78, 9, 2, 0, 0, '2026-04-01 13:00:00', '2026-04-01 13:00:00', 0),
(37, N'CI/CD 流水线中自动化测试配置求助',
 N'## 背景

团队正在搭建 CI/CD 流水线，使用 GitLab CI + Docker 部署，想在流水线中加入自动化测试环节。

## 当前 .gitlab-ci.yml 片段

```yaml
stages:
  - test
  - build
  - deploy

unit-test:
  stage: test
  script:
    - mvn test
```

## 需要解决的问题
1. 如何区分单元测试和集成测试？
2. 数据库依赖的测试如何处理？使用 Testcontainers 还是 H2？
3. 测试报告如何展示在 Merge Request 中？
4. 如何确保测试环境的隔离性？

请有经验的大佬指点！',
 11, 3, 0, 1, 1, 45, 5, 1, 0, 0, '2026-04-10 09:30:00', '2026-04-10 09:30:00', 0),
-- 项目展示
(38, N'TechHub 技术社区论坛 —— 全栈开源项目',
 N'## 项目简介

TechHub 是一个前后端分离的轻量级技术社区论坛，涵盖了用户认证、内容管理、互动系统、个性化推荐、AI 总结等完整功能。

## 技术栈

| 层级 | 技术 |
|------|------|
| 前端 | Vue 3 + TypeScript + Vite + Element Plus |
| 后端 | Spring Boot 3.5 + MyBatis-Plus + Spring Security |
| 数据库 | MySQL 8.0 + Redis 7 |
| 部署 | Docker + Docker Compose |

## 核心功能

- ✅ 用户注册登录（JWT + BCrypt）
- ✅ 帖子 CRUD（四级可见权限）
- ✅ 回复与神评机制
- ✅ 点赞 / 收藏 / 关注
- ✅ 通知系统
- ✅ AI 总结与问答
- ✅ 个性化推荐系统
- ✅ 草稿自动保存
- ✅ 管理后台

## 项目截图

> （稍后补充截图）

## 项目地址

[GitHub Repository](https://github.com/techhub/techhub-community)

## 开发心得

这个项目是我用来整合 Spring Boot 和 Vue 3 所学知识的实践项目，过程中遇到了不少挑战：

1. **雪花 ID 精度问题**：前端 JS 无法精确处理 19 位 BIGINT，需要序列化为字符串
2. **权限设计**：四级可见性 + 三级角色权限，需要前后端配合控制
3. **AI 集成**：异步调用 + 超时重试 + 用户数据隔离

欢迎 star 和 PR！',
 12, 1, 1, 1, 0, 210, 25, 6, 2, 1, '2026-03-20 08:00:00', '2026-04-15 16:00:00', 0),
(39, N'我的第一个全栈博客网站',
 N'## 项目介绍

用 Nuxt 3 + Prisma + SQLite 搭建的个人博客网站。

## 功能特性

- 📝 Markdown 文章编辑与发布
- 🏷️ 标签分类
- 🔍 全文搜索
- 🌙 暗色模式（跟随系统）
- 📱 响应式设计
- 💬 评论区（Giscus）

## 技术栈

```json
{
  "frontend": "Nuxt 3",
  "backend": "Nuxt Server API",
  "database": "SQLite + Prisma",
  "deploy": "Vercel",
  "comment": "Giscus (GitHub Discussions)"
}
```

## 亮点
- 使用 Nuxt 3 的 hybrid rendering（静态 + 服务端渲染）
- Markdown 内容自动生成目录导航
- 图片懒加载 + 渐进式加载

## 项目链接
[blog.techhub.com](https://blog.techhub.com) | [GitHub](https://github.com/user/my-blog)

欢迎各位来参观指导！',
 12, 3, 0, 1, 0, 56, 5, 1, 0, 0, '2026-04-05 14:00:00', '2026-04-05 14:00:00', 0),
(40, N'开源 CLI 工具 —— file-watchr 使用指南',
 N'## 项目背景

在日常开发中，经常需要监听文件变化并执行特定命令。虽然市面上已有类似工具，但大多功能单一或配置复杂。

## file-watchr 介绍

一个用 Rust 编写的跨平台文件监听 CLI 工具。

### 安装

```bash
# macOS\ brew install file-watchr

# 或从 GitHub Release 下载二进制
curl -L https://github.com/moderator/file-watchr/releases/latest/download/file-watchr-x86_64-unknown-linux-musl.tar.gz | tar xz
```

### 基本用法

```bash
# 监听当前目录的 .ts 文件变化
file-watchr --glob "**/*.ts" -- "npm run build"

# 监听多个目录
file-watchr --watch src --watch tests --exclude "**/*.test.ts" -- "cargo test"

# 延迟执行（防抖动）
file-watchr --debounce 1000 -- "make"
```

## 性能对比

| 工具 | 内存占用 | 启动速度 | 跨平台 |
|------|----------|----------|--------|
| file-watchr | 2.3 MB | 12ms | ✅ |
| nodemon | 45 MB | 800ms | ✅ |
| watchexec | 3.1 MB | 15ms | ✅ |

欢迎大家试用和提 issue！',
 12, 2, 0, 1, 2, 34, 3, 0, 0, 0, '2026-04-20 10:00:00', '2026-04-20 10:00:00', 0),
-- 资源分享
(41, N'2026 年 Java 后端开发者学习路线图',
 N'## 前言

这是一份面向初中级 Java 后端开发者的学习路线图，覆盖了从基础到进阶的核心知识点。

## 第一阶段：Java 核心基础

- [x] Java 语法基础（集合、泛型、反射、注解）
- [x] JVM 内存模型与垃圾回收
- [x] 多线程与并发编程
- [x] I/O 与 NIO

**推荐资源**：
- 《Java 核心技术 卷 I》
- 《深入理解 Java 虚拟机》
- [B站：尚硅谷 JVM 教程](https://www.bilibili.com)

## 第二阶段：主流框架

- Spring Framework（IoC、AOP、事务）
- Spring Boot（自动配置、起步依赖）
- Spring Security + JWT
- MyBatis / MyBatis-Plus

## 第三阶段：数据存储与缓存

```
MySQL → Redis → MongoDB（按需）
```

## 第四阶段：中间件

- RabbitMQ / Kafka（消息队列）
- Elasticsearch（搜索引擎）
- Nginx（反向代理、负载均衡）

## 第五阶段：DevOps

```bash
Docker → Kubernetes → CI/CD (GitLab CI / GitHub Actions)
```

## 第六阶段：进阶方向

- 微服务架构（Spring Cloud）
- 可观测性（Prometheus + Grafana + ELK）
- 性能调优

> 学习路线仅作参考，不必拘泥于顺序，结合实际项目需求学习效率更高。',
 13, 1, 1, 1, 0, 198, 22, 5, 1, 0, '2026-02-25 09:00:00', '2026-03-10 08:30:00', 0),
(42, N'五本程序员必读的架构设计书籍',
 N'## 1. 《系统设计面试》

> Alex Xu 著 · 适合面试准备和架构启蒙

本书以常见的系统设计面试题为切入点，逐步分析分布式系统的设计思路，非常适合架构设计入门。

## 2. 《DDIA（数据密集型应用系统设计）》

> Martin Kleppmann 著 · 进阶必读

这本书是分布式系统领域的圣经级著作，深入讲解了数据系统的底层原理。

## 3. 《领域驱动设计》

> Eric Evans 著 · DDD 经典

虽然这本书阅读门槛较高，但对复杂业务建模的帮助非常大。

## 4. 《重构：改善既有代码的设计》

> Martin Fowler 著 · 代码整洁之道

教你如何在不动摇系统根基的情况下逐步改善代码质量。

## 5. 《Clean Architecture》

> Robert C. Martin 著 · 架构原则

讲解了 SOLID 原则在架构层面的应用，对分层架构设计很有启发。

## 我的阅读建议

1. 先读 DIA（入门）→ DDA（进阶）→ Clean Architecture（总结）
2. 每读完一章，在项目中实践所学内容
3. 做好笔记，定期回顾',
 13, 3, 0, 1, 0, 87, 7, 2, 0, 0, '2026-03-28 16:00:00', '2026-03-28 16:00:00', 0),
(43, N'[课程] B站优质编程视频教程汇总',
 N'## 🔥 年度精选

### Java 方向
| 教程名称 | UP 主 | 适合人群 |
|----------|-------|----------|
| Spring Boot 从入门到精通 | 遇见狂神说 | 初学者 |
| JVM 性能调优实战 | 黑马程序员 | 进阶 |
| 微服务架构实战项目 | 尚硅谷 | 中级 |

### 前端方向
| 教程名称 | UP 主 | 适合人群 |
|----------|-------|----------|
| Vue 3 完全指南 | 程序员鱼皮 | 初中级 |
| TypeScript 从零开始 | 技术蛋 | 初学者 |
| 前端工程化实践 | 峰华前端 | 进阶 |

### DevOps 方向
| 教程名称 | UP 主 | 适合人群 |
|----------|-------|----------|
| Docker 容器化实战 | 遇见狂神说 | 初学者 |
| Kubernetes 入门到放弃 | 容器魔方 | 中级 |

## ⚠️ 温馨提示
- 教程建议 1.25x 或 1.5x 倍速观看
- 一定要跟着敲代码，只看不练等于白学
- 看到不懂的概念先查资料，不要停滞

持续更新中，欢迎在评论区补充优质教程！',
 13, 2, 0, 1, 1, 73, 6, 1, 0, 0, '2026-04-12 11:00:00', '2026-04-12 11:00:00', 0),
(44, N'2026 年开发者必备的 10 款效率工具',
 N'## 1. Warp - 现代化终端

基于 Rust 的终端模拟器，支持 AI 命令建议、智能补全、工作流保存。

## 2. Cursor - AI 编辑器

基于 VS Code 的 AI 驱动编辑器，集成 GPT-4 和 Claude 代码补全。

## 3. Bruno - API 调试工具

开源的 API 客户端，支持本地存储集合，替代 Postman。

## 4. OrbStack - Docker 桌面替代

macOS 上比 Docker Desktop 更轻量的容器管理工具。

## 5. Raycast - 效率启动器

macOS 效率工具，替代 Alfred 和 Spotlight。

## 6. Neovim - 编辑器

现代 Vim 体验，LSP 原生支持。

## 7. Lazygit - Git TUI

终端 Git 客户端，告别命令行 Git 的繁琐操作。

## 8. HTTPie - API 调试

更友好的 curl 替代品，响应高亮友好。

## 9. bat - cat 替代

带语法高亮和 Git 集成的文件查看器。

## 10. fd - find 替代

更快的文件搜索工具，语法更简洁。

```bash
# 安装方式
brew install warp bruno orbstack raycast neovim lazygit httpie bat fd
```

> 工具只是手段，提升效率的关键在于养成良好的开发习惯。',
 13, 3, 0, 1, 0, 112, 14, 3, 0, 0, '2026-05-01 08:00:00', '2026-05-01 08:00:00', 0),
-- 站务管理
(45, N'TechHub 社区发帖规范（2026版）',
 N'## 总则

为了营造良好的社区氛围，所有成员在 TechHub 发帖和回复时，请遵守以下规范。

## 发帖规范

### 标题要求
- 标题应**概括核心内容**，避免模糊表述
- 禁止使用「标题党」或误导性标题
- 求助帖建议包含问题关键词，如「MySQL 连接超时」

### 正文要求
- 使用 Markdown 排版，善用标题层级
- 代码使用代码块包裹并标注语言
- 贴出完整错误信息而非仅截图

## 回复规范

- 回复应**有实质性内容**，避免纯表情或「顶」「+1」等无意义回复
- 引用他人内容时使用引用语法
- 遇到不懂的问题可以等待他人解答，不要随意猜测

## 违规处理

| 违规行为 | 首次 | 二次 | 三次 |
|----------|------|------|------|
| 广告/推广 | 删帖警告 | 禁言 7 天 | 封禁 |
| 人身攻击 | 删帖警告 | 禁言 3 天 | 禁言 30 天 |
| 恶意灌水 | 删帖 | 禁言 1 天 | 禁言 7 天 |

## 修订历史
- 2026-05-01：初版发布

*本规范自发布之日起生效，管理团队保留最终解释权。*',
 14, 1, 2, 1, 0, 345, 30, 7, 2, 1, '2026-05-01 08:00:00', '2026-05-01 08:00:00', 0),
(46, N'遇到 Bug 或有问题？这样反馈更高效',
 N'## 提交 Bug 的正确姿势

### 第一步：确认是否为 Bug

在使用社区过程中遇到问题，请先确认：

1. 是否是因为操作不当导致的？
2. 是否已经是最新版本？
3. 有没有清空浏览器缓存试过？

### 第二步：收集信息

```markdown
## 环境信息
- 浏览器及版本：Chrome 125
- 操作系统：macOS 14.5
- 页面路径：/posts/123

## Bug 描述
在点击「发布」按钮后页面无响应，控制台报错如下：

## 复现步骤
1. 进入发布页面
2. 填写标题和内容
3. 点击「发布」按钮
4. 页面卡住无响应

## 控制台日志
```
Uncaught TypeError: Cannot read properties of null
    at Object.submit (post.js:42)
```
```

### 第三步：提交反馈

- 在站务管理版块发帖，选择「反馈」分类
- 或发送邮件至：feedback@techhub.com

> 管理团队会定期查看反馈，一般在 24 小时内回复。',
 14, 2, 0, 1, 0, 67, 4, 1, 0, 0, '2026-05-10 15:00:00', '2026-05-10 15:00:00', 0),
(47, N'关于社区功能改进的内部征求意见',
 N'## 背景

社区上线运营已经快半年了，收到了一些用户的反馈和建议。在正式对外征求意见之前，想先和管理团队内部讨论一下。

## 待讨论的功能改进

### 1. 积分系统升级

当前积分系统过于简单，建议引入：
- 每日签到积分
- 连续签到奖励
- 贡献值体系

### 2. 成就徽章系统

建议增加以下徽章：
- 🏅 「技术先锋」—— 发帖数超过 50
- 🏅 「热心答主」—— 回答被采纳超过 20 次
- 🏅 「社区元老」—— 注册满 1 年

### 3. 移动端适配

目前移动端体验较差，建议：
- 增加 PWA 支持
- 优化移动端布局
- 考虑开发小程序版本

## 请大家发表意见

以上改进方案还处于初步想法阶段，请大家从技术实现难度和维护成本角度给出建议。',
 14, 1, 0, 1, 3, 12, 2, 1, 0, 0, '2026-05-20 08:00:00', '2026-05-20 08:00:00', 0);

GO

-- ==================== comment ====================
INSERT INTO [comment] ([id], [content], [post_id], [user_id], [parent_id], [reply_to_user_id], [like_count], [recommend_count], [is_divine], [divine_time], [create_time]) VALUES
-- Post 30 (Spring Boot 3.5) 的回复
(50, N'非常感谢分享！想请教一下，虚拟线程在数据库连接池场景下有没有什么需要特别注意的？', 30, 3, NULL, NULL, 3, 0, 0, NULL, '2026-02-01 14:00:00'),
(51, N'好问题。使用虚拟线程时，连接池的 max-active 需要适当调大，因为虚拟线程不占用平台线程，可以同时处理更多请求。另外建议使用支持虚拟线程的连接池，比如 HikariCP 的最新版本。', 30, 1, 50, 3, 5, 2, 1, '2026-02-03 10:00:00', '2026-02-01 16:00:00'),
(52, N'结构化日志这个确实好用，我们团队用了之后排查问题的效率提升了不少。', 30, 2, NULL, NULL, 2, 0, 0, NULL, '2026-02-02 09:00:00'),
-- Post 31 (Vue 3 Composition API) 的回复
(53, N'补充一点：使用 `defineExpose` 可以让父组件通过 ref 调用子组件的方法，这在某些场景下比 emit 更直观。', 31, 1, NULL, NULL, 4, 0, 0, NULL, '2026-02-11 10:00:00'),
(54, N'谢谢补充！确实，`defineExpose` 在封装表单组件时非常实用。', 31, 2, 53, 1, 1, 0, 0, NULL, '2026-02-11 11:00:00'),
-- Post 33 (微服务十大陷阱) 的回复
(55, N'第三条深有感触！我们之前用 HTTP 同步调用链路过长，一个接口调了 6 个服务，延迟爆炸。后来引入了消息队列做异步解耦，效果好很多。', 33, 3, NULL, NULL, 6, 1, 0, NULL, '2026-02-21 09:00:00'),
(56, N'可观测性这点太真实了，没有链路追踪的时候排查跨服务问题简直是灾难。', 33, 2, NULL, NULL, 3, 0, 0, NULL, '2026-02-22 14:00:00'),
-- Post 34 (MySQL 联合索引) 的回复
(57, N'你的理解基本正确。不过第三个查询如果 c 是定值的话，其实有优化空间。另外 MySQL 8.0 引入了 Skip Scan 优化，某些情况下跳过的列也能用上索引。', 34, 1, NULL, NULL, 8, 1, 1, '2026-03-12 15:00:00', '2026-03-10 14:00:00'),
(58, N'补充一下，可以使用 `EXPLAIN` 查看执行计划来判断是否用到了索引。另外建索引时要考虑区分度，区分度低的列放在左边效果不好。', 34, 2, NULL, NULL, 4, 0, 0, NULL, '2026-03-11 10:00:00'),
(59, N'学到了，之前一直以为最左前缀只要包含第一个字段就能用全部索引。', 34, 3, NULL, NULL, 2, 0, 0, NULL, '2026-03-12 08:00:00'),
-- Post 35 (JWT Token 刷新) 的回复
(60, N'推荐方案一（双 Token）。Access Token 15 分钟过期 + Refresh Token 7 天，Refresh Token 存入 Redis 可以快速撤销。竞态问题可以通过 Refresh Token 轮换机制解决：每次刷新时同时发放新的 Refresh Token 并使旧 Refresh Token 失效。', 35, 1, NULL, NULL, 10, 3, 1, '2026-03-17 10:00:00', '2026-03-15 17:00:00'),
(61, N'方案二也有不少人在用，但缺点是前端需要拦截 401 主动刷新，逻辑相对复杂。', 35, 2, NULL, NULL, 3, 0, 0, NULL, '2026-03-16 09:00:00'),
-- Post 38 (TechHub 项目展示) 的回复
(62, N'这个项目很赞！想问一下推荐系统部分，冷启动阶段的效果如何？', 38, 3, NULL, NULL, 5, 0, 0, NULL, '2026-03-21 10:00:00'),
(63, N'冷启动阶段我们降级为热门帖子推荐。当用户有 5 次以上的互动后，画像开始生效，推荐效果会明显提升。具体的算法细节可以看项目里的 RecommendationService 实现。', 38, 1, 62, 3, 7, 2, 1, '2026-03-25 14:00:00', '2026-03-21 14:00:00'),
(64, N'神评机制的设计很有意思，避免了一味追求点赞数的短板。', 38, 2, NULL, NULL, 3, 0, 0, NULL, '2026-03-22 11:00:00'),
-- Post 41 (学习路线图) 的回复
(65, N'很全面的路线图！建议在第一阶段加上「数据结构与算法」，虽然面试导向，但对思维能力提升很有帮助。', 41, 2, NULL, NULL, 6, 0, 0, NULL, '2026-02-26 10:00:00'),
(66, N'感谢建议！确实，算法和数据结构应该作为贯穿始终的基础能力，已补充到路线图中。', 41, 1, 65, 2, 3, 0, 0, NULL, '2026-02-26 14:00:00'),
-- Post 45 (发帖规范) 的回复
(67, N'支持！规范的社区环境需要大家一起维护。建议在发帖编辑器中增加规范提示。', 45, 2, NULL, NULL, 8, 1, 1, '2026-05-03 10:00:00', '2026-05-01 10:00:00'),
(68, N'很好的建议，我们已经在开发中。发布页会添加一个「发帖规范」的折叠面板。', 45, 1, 67, 2, 5, 0, 0, NULL, '2026-05-01 11:00:00'),
(69, N'违规处理表格很清晰，不过建议增加申诉渠道的说明。', 45, 3, NULL, NULL, 4, 0, 0, NULL, '2026-05-02 09:00:00'),
-- Post 44 (效率工具) 的回复
(70, N'用过 WARP 之后确实回不去其他终端了，AI 命令建议功能太实用了。', 44, 1, NULL, NULL, 3, 0, 0, NULL, '2026-05-02 09:00:00'),
(71, N'推荐加上 `thefuck`，自动纠正输错的命令，和 Lazygit 配合使用体验极佳。', 44, 2, NULL, NULL, 4, 0, 0, NULL, '2026-05-02 10:00:00');

GO

-- ==================== category_notice ====================
INSERT INTO [category_notice] ([id], [category_id], [title], [content], [type], [author_id], [is_pinned], [status], [create_time], [update_time]) VALUES (20, 10, N'技术讨论版块须知',
 N'# 📋 技术讨论版块须知

欢迎来到技术讨论版块！在这里你可以：

## ✅ 鼓励的内容
- 前沿技术分享与讨论
- 架构设计经验交流
- 编程语言特性探讨
- 技术趋势分析

## ❌ 禁止的内容
- 与技术无关的水帖
- 人身攻击或引战内容
- 广告或商业推广

## 💡 发帖建议
1. 标题简明扼要，概括核心内容
2. 正文使用 Markdown 排版，善用代码块和列表
3. 引用他人观点时注明来源
4. 理性讨论，尊重不同观点

> 违反版规的帖子将按站规处理，多次违规将被限制发言。',
 0, 1, 1, 1, '2026-01-01 08:00:00', '2026-01-01 08:00:00'),
(21, 10, N'月度技术分享会 · 六月预告',
 N'# 🌟 月度技术分享会 · 六月预告

## 主题：Spring Boot 3.5 新特性深度解析

**时间**：2026年6月15日 20:00 - 21:30
**形式**：线上腾讯会议
**主讲人**：@admin

## 内容大纲
1. Spring Boot 3.5 核心变化概览
2. 虚拟线程支持实战
3. GraalVM Native Image 构建优化
4. 可观测性增强（Micrometer + OTel）

## 报名方式
在本帖下方回复「报名」即可，会议链接将在活动前一天通过站内通知发送。

---
*欢迎各位踊跃参加！*',
 1, 2, 0, 1, '2026-05-20 10:00:00', '2026-05-20 10:00:00'),
-- 问答求助
(22, 11, N'问答求助版块须知',
 N'# ❓ 问答求助版块须知

## 提问前请先做
1. **使用搜索引擎**：先搜索你的问题，可能已经有现成答案
2. **查看置顶帖**：常见问题已在 FAQ 中整理
3. **明确问题范围**：确定问题属于哪个技术领域

## 提问模板
```
## 环境信息
- 操作系统：
- 框架版本：
- JDK/Node 版本：

## 问题描述
明确描述你遇到的问题

## 已尝试的方案
列出你已经尝试过的解决方法

## 相关代码/日志
```
```

## 💡 奖励机制
- 优质提问可获得「技术之星」徽章
- 积极回答问题的用户将被推荐为版主候选人
- 被采纳的回答可获得积分奖励

> 注意：请勿提问与编程/技术无关的问题。',
 0, 1, 1, 1, '2026-01-01 08:00:00', '2026-01-01 08:00:00'),
(23, 11, N'优秀问答评选活动',
 N'# 🏆 优秀问答评选活动

## 活动时间
2026年6月1日 - 2026年6月30日

## 评选规则
- **最佳提问奖**：问题描述清晰、有研究价值，获赞最多的 3 个提问
- **最佳回答奖**：回答详尽、有代码示例，被采纳最多的 3 个回答
- **活跃贡献奖**：活动期间回答数量最多的 5 位用户

## 奖品
- 一等奖：技术图书任选一本（价值 100 元以内）
- 二等奖：社区专属勋章 + 积分 × 500
- 三等奖：积分 × 200

## 参与方式
无需报名，在问答版块正常发帖/回帖即自动参与评选。

*本活动最终解释权归 TechHub 管理团队所有。*',
 1, 2, 1, 1, '2026-05-25 09:00:00', '2026-05-25 09:00:00'),
-- 项目展示
(24, 12, N'项目展示版块须知',
 N'# 🚀 项目展示版块须知

## 你可以展示
- 个人开源项目（不限大小，有趣即可）
- 课堂作业 / 毕业设计作品
- 团队合作项目
- 商业产品（需标注「推广」标签）

## 发帖格式建议
```
## 项目名称

## 项目简介（100-200字）

## 技术栈
- 后端：
- 前端：
- 数据库：
- 部署：

## 项目截图

## 项目地址
[GitHub](链接) | [在线演示](链接)

## 开发心得
分享开发过程中遇到的挑战和解决方案
```

## ⚠️ 注意事项
- 严禁抄袭他人项目冒充原创
- 代码中有敏感信息（密钥、密码）请脱敏处理
- 推广内容请遵守相关法律法规',
 0, 1, 1, 1, '2026-01-01 08:00:00', '2026-01-01 08:00:00'),
(25, 12, N'开源项目推荐计划',
 N'# 🌟 开源项目推荐计划

每月由社区投票选出「本月最佳开源项目」，入选项目将获得：

1. 社区首页推荐位展示（一周）
2. 官方社交媒体转发推广
3. 项目作者获得「开源先锋」徽章

## 推荐方式
在项目展示版块发布项目帖，标题前缀 `[推荐]` 即可参与评选。

每月 25 日截止收集，28 日公布评选结果。',
 1, 2, 0, 1, '2026-05-18 14:00:00', '2026-05-18 14:00:00'),
-- 资源分享
(26, 13, N'资源分享版块须知',
 N'# 📚 资源分享版块须知

## 允许分享的资源类型
- 📖 技术书籍（推荐 / 读书笔记）
- 🎓 在线课程与教程
- 🛠️ 开发工具与插件
- 📄 技术文档与 Cheat Sheet
- 🎬 技术演讲视频
- 💻 项目模板与脚手架

## 发帖规范
1. 标题需包含资源类型标签，如 `[书籍]` `[课程]` `[工具]`
2. 正文需包含资源简介、适用人群、获取方式
3. 分享有版权的资源时请确保已获授权

## 🚫 禁止事项
- 分享盗版资源或侵权内容
- 包含恶意软件或钓鱼链接
- 重复发布相同资源

> 优质资源分享将被加精并收录至社区资源汇总帖。',
 0, 1, 1, 1, '2026-01-01 08:00:00', '2026-01-01 08:00:00'),
(27, 13, N'优质资源征集令',
 N'# 🎯 优质资源征集令

你平时从哪里学习新技术？有哪些珍藏的学习资源？快来分享给大家！

## 征集内容
- 你读过的最好的技术书籍
- 收藏的精品教程或课程
- 提升效率的开发工具
- 值得关注的博客或 newsletter

## 奖励
所有有效投稿均可获得 **100 积分**，优秀资源额外奖励 **社区专属勋章**。

*活动长期有效*',
 1, 2, 0, 1, '2026-05-22 11:00:00', '2026-05-22 11:00:00'),
-- 站务管理
(28, 14, N'站务管理版块须知',
 N'# 📢 站务管理版块须知

本版块用于发布社区官方公告和管理通知，普通用户也可在此反馈意见和建议。

## 内容分类
- **[公告]** 社区规则更新、系统维护通知
- **[反馈]** 功能建议、Bug 报告
- **[招募]** 版主招募、社区贡献者招募
- **[公示]** 违规处理公示、人事变动

## ⚠️ 注意
- 发帖请选择合适的分类前缀
- 投诉或举报请提供充分证据
- 管理团队将在 48 小时内回复反馈帖',
 0, 1, 1, 1, '2026-01-01 08:00:00', '2026-01-01 08:00:00'),
(29, 14, N'TechHub 版主招募公告',
 N'# 🙋 TechHub 版主招募公告

随着社区不断发展，我们需要更多热心成员加入管理团队！

## 招募岗位
| 版块 | 人数 |
|------|------|
| 技术讨论 | 2 人 |
| 问答求助 | 2 人 |
| 项目展示 | 1 人 |
| 资源分享 | 1 人 |

## 申请条件
1. 注册满 30 天且积分 ≥ 500
2. 在所申请版块有活跃且高质量的发帖/回复记录
3. 每周至少有 5 小时参与社区管理
4. 无违规记录

## 版主权益
- 专属版主徽章
- 每月社区积分奖励
- 参与社区治理决策
- 优先获得社区周边礼品

## 申请方式
在本帖下回复「申请 + 版块名称 + 自我介绍」即可，管理团队将在 7 个工作日内审核。',
 1, 1, 1, 1, '2026-05-15 08:00:00', '2026-05-15 08:00:00');
GO

-- ==================== user_like ====================
INSERT INTO [user_like] ([id], [user_id], [target_type], [target_id], [create_time]) VALUES (80, 2, 'POST', 30, '2026-02-01 12:00:00'),
(81, 3, 'POST', 30, '2026-02-01 13:00:00'),
(82, 1, 'POST', 31, '2026-02-11 09:00:00'),
(83, 3, 'POST', 31, '2026-02-11 10:00:00'),
(84, 1, 'POST', 33, '2026-02-21 08:00:00'),
(85, 2, 'POST', 33, '2026-02-21 09:00:00'),
(86, 3, 'POST', 33, '2026-02-21 10:00:00'),
(87, 1, 'POST', 34, '2026-03-10 12:00:00'),
(88, 2, 'POST', 34, '2026-03-10 13:00:00'),
(89, 3, 'POST', 35, '2026-03-15 16:00:00'),
(90, 1, 'POST', 35, '2026-03-15 16:30:00'),
(91, 2, 'POST', 38, '2026-03-20 10:00:00'),
(92, 3, 'POST', 38, '2026-03-20 11:00:00'),
(93, 2, 'POST', 41, '2026-02-25 10:00:00'),
(94, 3, 'POST', 41, '2026-02-25 11:00:00'),
(95, 1, 'POST', 44, '2026-05-01 09:00:00'),
(96, 2, 'POST', 44, '2026-05-01 10:00:00'),
(97, 1, 'POST', 45, '2026-05-01 09:00:00'),
(98, 3, 'POST', 45, '2026-05-01 10:00:00'),
(99, 2, 'POST', 45, '2026-05-01 11:00:00'),
(100, 1, 'COMMENT', 50, '2026-02-01 15:00:00'),
(101, 2, 'COMMENT', 50, '2026-02-01 16:00:00'),
(102, 3, 'COMMENT', 51, '2026-02-02 09:00:00'),
(103, 1, 'COMMENT', 51, '2026-02-02 10:00:00'),
(104, 2, 'COMMENT', 55, '2026-02-21 10:00:00'),
(105, 1, 'COMMENT', 55, '2026-02-21 11:00:00'),
(106, 3, 'COMMENT', 57, '2026-03-10 15:00:00'),
(107, 1, 'COMMENT', 57, '2026-03-10 16:00:00'),
(108, 2, 'COMMENT', 57, '2026-03-11 09:00:00'),
(109, 1, 'COMMENT', 60, '2026-03-15 18:00:00'),
(110, 2, 'COMMENT', 60, '2026-03-15 19:00:00'),
(111, 3, 'COMMENT', 60, '2026-03-16 09:00:00'),
(112, 1, 'COMMENT', 62, '2026-03-21 11:00:00'),
(113, 3, 'COMMENT', 63, '2026-03-22 10:00:00'),
(114, 1, 'COMMENT', 63, '2026-03-22 11:00:00'),
(115, 2, 'COMMENT', 65, '2026-02-26 11:00:00'),
(116, 1, 'COMMENT', 65, '2026-02-26 12:00:00'),
(117, 1, 'COMMENT', 67, '2026-05-01 11:00:00'),
(118, 3, 'COMMENT', 67, '2026-05-01 12:00:00'),
(119, 1, 'COMMENT', 68, '2026-05-01 12:00:00'),
(120, 1, 'COMMENT', 70, '2026-05-02 10:00:00'),
(121, 3, 'COMMENT', 71, '2026-05-02 11:00:00');

GO

-- ==================== favorite ====================
INSERT INTO [favorite] ([id], [user_id], [post_id], [create_time]) VALUES (130, 3, 30, '2026-02-03 10:00:00'),
(131, 2, 33, '2026-02-25 09:00:00'),
(132, 3, 38, '2026-03-25 10:00:00'),
(133, 2, 41, '2026-03-01 08:00:00'),
(134, 3, 44, '2026-05-02 10:00:00'),
(135, 2, 45, '2026-05-02 08:00:00');

GO

-- ==================== follow ====================
INSERT INTO [follow] ([id], [follower_id], [followee_id], [create_time]) VALUES (140, 3, 1, '2026-02-01 12:00:00'),
(141, 3, 2, '2026-02-10 15:00:00'),
(142, 2, 1, '2026-01-15 10:00:00');

GO

-- ==================== notification ====================
INSERT INTO [notification] ([id], [user_id], [type], [source_id], [content], [is_read], [create_time]) VALUES (170, 1, 'LIKE', 30, N'用户 moderator 点赞了你的帖子《Spring Boot 3.5 最佳实践总结》', 1, '2026-02-01 12:00:00'),
(171, 1, 'LIKE', 30, N'用户 admin 点赞了你的帖子《Spring Boot 3.5 最佳实践总结》', 0, '2026-02-01 13:00:00'),
(172, 1, 'REPLY', 30, N'用户 admin 回复了你的帖子《Spring Boot 3.5 最佳实践总结》', 1, '2026-02-01 16:00:00'),
(173, 1, 'LIKE', 30, N'用户 user 点赞了你的评论', 1, '2026-02-02 09:00:00'),
(174, 3, 'DIVINE', 51, N'你的评论被选为神评！', 0, '2026-02-03 10:00:00'),
(175, 3, 'FOLLOW', 140, N'用户 user 关注了你', 1, '2026-02-01 12:00:00'),
(176, 2, 'FOLLOW', 141, N'用户 user 关注了你', 0, '2026-02-10 15:00:00'),
(177, 1, 'FOLLOW', 142, N'用户 moderator 关注了你', 1, '2026-01-15 10:00:00'),
(178, 1, 'LIKE', 33, N'用户 user 点赞了你的帖子《微服务架构设计中的十大陷阱》', 0, '2026-02-21 10:00:00'),
(179, 3, 'LIKE', 34, N'用户 admin 点赞了你的帖子《MySQL 联合索引最左前缀原则求助》', 0, '2026-03-10 12:00:00'),
(180, 3, 'DIVINE', 57, N'你的评论被选为神评！', 0, '2026-03-12 15:00:00'),
(181, 3, 'DIVINE', 60, N'你的评论被选为神评！', 0, '2026-03-17 10:00:00'),
(182, 1, 'LIKE', 38, N'用户 moderator 点赞了你的帖子《TechHub 技术社区论坛 —— 全栈开源项目》', 0, '2026-03-20 10:00:00'),
(183, 1, 'LIKE', 38, N'用户 user 点赞了你的帖子《TechHub 技术社区论坛 —— 全栈开源项目》', 0, '2026-03-20 11:00:00'),
(184, 1, 'REPLY', 38, N'用户 user 回复了你的帖子《TechHub 技术社区论坛 —— 全栈开源项目》', 0, '2026-03-21 10:00:00'),
(185, 1, 'DIVINE', 63, N'你的评论被选为神评！', 0, '2026-03-25 14:00:00'),
(186, 2, 'DIVINE', 67, N'你的评论被选为神评！', 0, '2026-05-03 10:00:00');
GO

-- ==================== comment_recommend ====================
INSERT INTO [comment_recommend] ([id], [comment_id], [user_id], [create_time]) VALUES (150, 51, 2, '2026-02-02 10:00:00'),
(151, 51, 3, '2026-02-02 11:00:00'),
(152, 57, 2, '2026-03-11 10:00:00'),
(153, 57, 3, '2026-03-11 11:00:00'),
(154, 60, 2, '2026-03-16 10:00:00'),
(155, 60, 3, '2026-03-16 11:00:00'),
(156, 63, 2, '2026-03-22 10:00:00'),
(157, 63, 3, '2026-03-22 11:00:00'),
(158, 67, 1, '2026-05-01 11:00:00'),
(159, 67, 3, '2026-05-01 12:00:00'),
(160, 55, 1, '2026-02-22 09:00:00');
GO

PRINT N'TechHub 数据库初始化完成！';
GO
