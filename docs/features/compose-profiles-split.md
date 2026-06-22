# 优化：Docker Compose 通过 Profiles 分离本地开发与全栈部署

| 属性 | 值 |
|------|-----|
| 分支 | `refactor/compose-profiles-split` |
| 日期 | 2026-06-22 |
| 类型 | 配置优化 |
| 影响范围 | `backend/docker-compose.yml`、README、后端开发文档 §6.7/§11.4 |

## 背景

现有 `docker-compose.yml` 混合了基础设施服务（mysql、redis）和应用服务（app）。`docker compose up -d` 会触发完整构建流程，但本地开发场景下 app 通常在 IDE 中运行，无需容器化启动。

同时 README 和开发文档描述的默认行为是"只起 MySQL + Redis"，与实际 compose 文件行为不一致。

## 改动

### 1. `backend/docker-compose.yml`

给 `app` 服务添加 `profiles: ["full"]`（1 行）：

```yaml
app:
  profiles: ["full"]
  build:
    ...
```

行为变化：

| 命令 | 启动服务 | 场景 |
|------|---------|------|
| `docker compose up -d` | mysql + redis | 本地开发 |
| `docker compose --profile full up -d` | mysql + redis + app | 全栈部署 |

### 2. 文档同步

- **后端开发文档 §6.7**：补充 Profiles 机制说明（"本地开发" vs "全栈部署"两种用法）
- **后端开发文档 §11.4**：部署命令改为 `docker compose --profile full up -d`
- **README**："启动基础设施"部分补充全栈部署提示

## 验证

```bash
cd backend
docker compose config --services              # 预期：只列出 mysql、redis
docker compose --profile full config --services  # 预期：列出 mysql、redis、app
docker compose config | grep -q "^services:"     # YAML 语法校验
```

## 已知限制

- 向后兼容性：已有 `docker compose up -d` 依赖 app 容器启动的用户，需改为 `--profile full up -d`。本地开发场景不受影响（app 在 IDE 运行）。
- CI 无影响（测试走 Maven + H2，不依赖 docker compose）。
