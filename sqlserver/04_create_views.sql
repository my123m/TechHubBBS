-- ============================================================
-- TechHub 视图脚本 (SQL Server 2022)
-- 文件: 04_create_views.sql
-- ============================================================

-- ==================== 热门帖子视图 ====================
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

-- ==================== 用户统计视图 ====================
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

-- ==================== 帖子详情视图 ====================
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
