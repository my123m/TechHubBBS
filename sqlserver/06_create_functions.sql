-- ============================================================
-- TechHub 自定义函数脚本 (SQL Server 2022)
-- 文件: 06_create_functions.sql
-- ============================================================

-- ==================== 获取用户角色名 ====================
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

-- ==================== 计算帖子热度分 ====================
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
