# 修复：CORS 白名单精确匹配，禁止通配符 + allowCredentials 同时使用

| 属性 | 值 |
|------|-----|
| 分支 | `fix/cors-credentials-origin-whitelist` |
| 日期 | 2026-06-23 |
| 类型 | 安全修复（Critical） |
| Issue | [#66](https://github.com/my123m/TechHubBBS/issues/66) |
| 影响范围 | 后端 `WebMvcConfig.java`、`application.yml`、`application-prod.yml`、`.env.example`；文档 `README.md`、`后端开发文档.md`、`技术方案文档.md` |

## 根因分析

`WebMvcConfig.java:25-28` 同时启用 `allowCredentials(true)` 和 `allowedOriginPatterns("*")`：

```java
registry.addMapping("/api/**")
        .allowedOriginPatterns("*")
        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        .allowedHeaders("*")
        .allowCredentials(true);
```

根据 CORS 规范，`allowCredentials=true` 时 `Access-Control-Allow-Origin` 不能为通配符 `*`。`allowedOriginPatterns("*")` 虽绕过了字面 `*` 限制（Spring 内部对 matchAll 模式返回具体 Origin），但实际效果等同于**允许任意来源携带凭据**发起跨域请求。任何恶意网站均可通过 `credentials: 'include'` + 用户浏览器自动附带的 Cookie（或注入的 Authorization header）读取 `/api/**` 接口响应。

生产配置文件 `application-prod.yml:22-23` 已定义 `cors.allowed-origins`，但 `WebMvcConfig` 使用硬编码通配符，生产配置从未生效。

违反 `AGENTS.md §8`「CORS 白名单」安全条款。

## 修复方案

将 `allowedOriginPatterns("*")` 替换为 `allowedOrigins(配置白名单)`。Spring 允许 `allowedOrigins(精确列表)` + `allowCredentials(true)`（仅回显请求 Origin 在列表中的值），这是 CORS 规范的正确用法。

白名单通过 `cors.allowed-origins` 配置注入，各环境差异化提供：

- **dev**（`application.yml`）：`http://localhost:5173`（Vite 开发服务器，开箱即用）
- **prod**（`application-prod.yml`）：`${CORS_ORIGINS}`（fail-fast，无默认回退，与 JWT 修复一致）
- **test**：继承基础 `application.yml` 的 dev 默认值，无需额外配置

具体改动：

| 文件 | 改动 |
|------|------|
| `backend/src/main/java/com/techhub/config/WebMvcConfig.java` | 新增 `@Value("${cors.allowed-origins}") List<String> allowedOrigins`；`.allowedOriginPatterns("*")` → `.allowedOrigins(allowedOrigins.toArray(new String[0]))` |
| `backend/src/main/resources/application.yml` | 新增 `cors.allowed-origins: http://localhost:5173`（dev 默认） |
| `backend/src/main/resources/application-prod.yml` | `cors.allowed-origins: ${CORS_ORIGINS:https://your-domain.com}` → `cors.allowed-origins: ${CORS_ORIGINS}`（去除占位回退，fail-fast） |
| `backend/.env.example` | 新增 `CORS_ORIGINS` 可选项说明 |
| `README.md` | 环境变量表新增 `CORS_ORIGINS`；安全规范新增 CORS 白名单条目；合并重复安全规范段落 |
| `docs/TechHub 后端开发文档.md` | §6.4 代码片段更新为新实现 |
| `docs/TechHub 技术方案文档.md` | §7.4 环境安全补 CORS 白名单；§10 风险矩阵新增「CORS 白名单通配」行 |

## 行为变化

| 场景 | 修复前 | 修复后 |
|------|--------|--------|
| 任意 Origin 带 Authorization 请求 `/api/**` | 返回数据（`Access-Control-Allow-Origin` 回显请求 Origin） | CORS 拒绝（Origin 不在白名单，浏览器拦截；非浏览器请求正常返回但无 CORS 头） |
| 本地 `mvn spring-boot:run` | 任何 Origin 可通过 | 仅 `http://localhost:5173` 可通过（Vite 开发服务器） |
| 生产部署未配置 `CORS_ORIGINS` | 任意 Origin 可携带凭据请求 | 启动期抛 `IllegalArgumentException`，**拒绝启动** |
| 测试 `mvn test` | 通过 | 通过（继承基础 `application.yml` 的 dev 默认值） |

## 文件变更

| 操作 | 文件 |
|------|------|
| 修改 | `backend/src/main/java/com/techhub/config/WebMvcConfig.java` |
| 修改 | `backend/src/main/resources/application.yml` |
| 修改 | `backend/src/main/resources/application-prod.yml` |
| 修改 | `backend/.env.example` |
| 修改 | `README.md` |
| 修改 | `docs/TechHub 后端开发文档.md` |
| 修改 | `docs/TechHub 技术方案文档.md` |
| 新增 | `docs/features/cors-credentials-fix.md`（本文件） |

## 验证方式

- `mvn -q -pl backend compile` — 编译通过
- `mvn -q -pl backend test` — 全部测试通过（含 354 集成测试），无新增失败
- 手动验证：`curl -H "Origin: https://evil.com" http://localhost:8080/api/v1/posts` → 无 `Access-Control-Allow-Origin` 头；`curl -H "Origin: http://localhost:5173" http://localhost:8080/api/v1/posts` → 回显 `Access-Control-Allow-Origin: http://localhost:5173`
- fail-fast 验证：临时注释 `application-prod.yml` 的 `cors.allowed-origins` 后启动，确认抛 `Could not resolve placeholder 'cors.allowed-origins'`

## 已知限制

- 未接入 Spring Security 的 `CorsConfigurationSource` bean（当前 CORS 仅通过 `WebMvcConfigurer` 处理，与 Security Filter Chain 配合依赖 JWT Filter 对 OPTIONS 返回 200 的特殊处理）。属后续架构增强，不在本 issue 范围。
- 不支持动态白名单（如运行时通过管理接口增删 Origin）。如需可按 `ponytail:` 标注路径升级为从数据库/配置中心读取。
- 未实现 `allowedHeaders` 白名单（仍为 `*`）。仅 Origin 白名单已消除核心凭据泄漏风险；Header 白名单属纵深防御，后续可独立加固。
