# TechHub SQL Server Database Scripts

SQL Server 2022 T-SQL 数据库脚本，由 TechHub MySQL 方案转换而来，用于《数据库原理及应用》课程设计。

## 执行顺序

```
01_create_database.sql       → 创建数据库 techhub
02_create_tables.sql         → 创建 17 张表 + CHECK 约束
03_create_indexes.sql        → 创建非聚集索引
04_create_views.sql          → 创建 3 个视图
05_create_procedures.sql     → 创建 3 个存储过程
06_create_functions.sql      → 创建 2 个自定义函数
07_create_triggers.sql       → 创建 6 个触发器
08_seed_data.sql             → 插入种子数据
```

## 执行方式

在 SQL Server Management Studio (SSMS) 中按序号依次打开并执行（F5），或使用 sqlcmd：

```bash
sqlcmd -S localhost -U sa -P your_password -i 01_create_database.sql
sqlcmd -S localhost -U sa -P your_password -i 02_create_tables.sql
...
```

## 数据库对象清单

| 类型 | 数量 | 说明 |
|------|------|------|
| 数据表 | 17 | 含 user, post, comment, category 等 |
| CHECK 约束 | 13 | 枚举字段值域约束 |
| 外键约束 | 23 | 参照完整性约束 |
| 非聚集索引 | 20+ | 联合索引优化查询性能 |
| 视图 | 3 | v_hot_posts, v_user_stats, v_post_detail |
| 存储过程 | 3 | sp_publish_post, sp_toggle_like, sp_delete_post |
| 自定义函数 | 2 | fn_get_user_role, fn_calc_hot_score |
| 触发器 | 6 | 5×update_time 自动更新 + 1×评论计数 |

## 种子数据

- 3 个用户（admin / moderator / user）
- 5 个版块（技术讨论 / 问答求助 / 项目展示 / 资源分享 / 站务管理）
- 10 条版块公告
- 15 篇帖子
- 22 条评论
- 42 条点赞记录
- 6 条收藏
- 3 条关注
- 11 条神评推荐
- 17 条通知
- 推荐系统数据（post_keyword / post_similarity / user_profile）

## 转换说明

原项目使用 MySQL 8.0，本脚本已完整转换为 SQL Server 2022 T-SQL 语法：

- 字符类型统一为 `NVARCHAR` / `NVARCHAR(MAX)`（Unicode）
- `DATETIME` → `DATETIME2`
- `DOUBLE` → `FLOAT`
- `CURRENT_TIMESTAMP` → `GETDATE()`
- MySQL 特有子句（ENGINE / CHARSET / COLLATE / ROW_FORMAT）全部移除
- 索引从内联 `KEY` 语法转换为独立 `CREATE NONCLUSTERED INDEX`
- `ON UPDATE CURRENT_TIMESTAMP` 由触发器替代
- 新增 CHECK 约束弥补应用层枚举的数据库层校验
