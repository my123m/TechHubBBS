-- ============================================================
-- TechHub 非聚集索引脚本 (SQL Server 2022)
-- 文件: 03_create_indexes.sql
-- ============================================================

-- ==================== category 索引 ====================
CREATE NONCLUSTERED INDEX [idx_category_status_sort]
    ON [category] ([status], [sort_order]);
GO

-- ==================== post 索引 ====================
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

-- ==================== comment 索引 ====================
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

-- ==================== category_notice 索引 ====================
CREATE NONCLUSTERED INDEX [idx_notice_cat_status_pinned_time]
    ON [category_notice] ([category_id], [status], [is_pinned], [create_time]);
GO

CREATE NONCLUSTERED INDEX [idx_notice_author_id]
    ON [category_notice] ([author_id]);
GO

-- ==================== follow 索引 ====================
CREATE NONCLUSTERED INDEX [idx_follow_followee_id]
    ON [follow] ([followee_id]);
GO

-- ==================== notification 索引 ====================
CREATE NONCLUSTERED INDEX [idx_notification_user_read_time]
    ON [notification] ([user_id], [is_read], [create_time]);
GO

-- ==================== ai_qa_history 索引 ====================
CREATE NONCLUSTERED INDEX [idx_ai_qa_history_user_post_time]
    ON [ai_qa_history] ([user_id], [post_id], [create_time]);
GO

-- ==================== post_draft 索引 ====================
CREATE NONCLUSTERED INDEX [idx_post_draft_user_update]
    ON [post_draft] ([user_id], [update_time]);
GO

-- ==================== post_keyword 索引 ====================
CREATE NONCLUSTERED INDEX [idx_post_keyword_keyword]
    ON [post_keyword] ([keyword]);
GO

-- ==================== post_similarity 索引 ====================
CREATE NONCLUSTERED INDEX [idx_post_similarity_post_b]
    ON [post_similarity] ([post_id_b]);
GO

-- ==================== file_detail 索引 ====================
CREATE NONCLUSTERED INDEX [idx_file_detail_object]
    ON [file_detail] ([object_type], [object_id]);
GO

-- idx_file_detail_url 已移除: url 列为 NVARCHAR(512)=1024字节, 超出非聚集索引键最大 900 字节限制
