-- ============================================================
-- TechHub 触发器脚本 (SQL Server 2022)
-- 文件: 07_create_triggers.sql
-- 说明:
--   trg_xxx_update_time (×5): 替代 MySQL ON UPDATE CURRENT_TIMESTAMP
--   trg_after_comment_insert: 评论插入后自动更新帖子评论计数
-- ============================================================

-- ==================== user 表更新触发器 ====================
IF OBJECT_ID('trg_user_update_time', 'TR') IS NOT NULL DROP TRIGGER [trg_user_update_time];
GO
CREATE TRIGGER [trg_user_update_time] ON [user] AFTER UPDATE AS
BEGIN
    SET NOCOUNT ON;
    UPDATE [user] SET [update_time] = GETDATE()
    WHERE [id] IN (SELECT [id] FROM [inserted]);
END;
GO

-- ==================== post 表更新触发器 ====================
IF OBJECT_ID('trg_post_update_time', 'TR') IS NOT NULL DROP TRIGGER [trg_post_update_time];
GO
CREATE TRIGGER [trg_post_update_time] ON [post] AFTER UPDATE AS
BEGIN
    SET NOCOUNT ON;
    UPDATE [post] SET [update_time] = GETDATE()
    WHERE [id] IN (SELECT [id] FROM [inserted]);
END;
GO

-- ==================== category_notice 表更新触发器 ====================
IF OBJECT_ID('trg_notice_update_time', 'TR') IS NOT NULL DROP TRIGGER [trg_notice_update_time];
GO
CREATE TRIGGER [trg_notice_update_time] ON [category_notice] AFTER UPDATE AS
BEGIN
    SET NOCOUNT ON;
    UPDATE [category_notice] SET [update_time] = GETDATE()
    WHERE [id] IN (SELECT [id] FROM [inserted]);
END;
GO

-- ==================== ai_summary 表更新触发器 ====================
IF OBJECT_ID('trg_ai_summary_update_time', 'TR') IS NOT NULL DROP TRIGGER [trg_ai_summary_update_time];
GO
CREATE TRIGGER [trg_ai_summary_update_time] ON [ai_summary] AFTER UPDATE AS
BEGIN
    SET NOCOUNT ON;
    UPDATE [ai_summary] SET [update_time] = GETDATE()
    WHERE [id] IN (SELECT [id] FROM [inserted]);
END;
GO

-- ==================== post_draft 表更新触发器 ====================
IF OBJECT_ID('trg_post_draft_update_time', 'TR') IS NOT NULL DROP TRIGGER [trg_post_draft_update_time];
GO
CREATE TRIGGER [trg_post_draft_update_time] ON [post_draft] AFTER UPDATE AS
BEGIN
    SET NOCOUNT ON;
    UPDATE [post_draft] SET [update_time] = GETDATE()
    WHERE [id] IN (SELECT [id] FROM [inserted]);
END;
GO

-- ==================== 评论插入后更新帖子计数 ====================
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
