-- ============================================================
-- TechHub 存储过程脚本 (SQL Server 2022)
-- 文件: 05_create_procedures.sql
-- ============================================================

-- ==================== 发布帖子 ====================
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

-- ==================== 点赞 / 取消点赞 ====================
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

-- ==================== 软删除帖子 ====================
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
