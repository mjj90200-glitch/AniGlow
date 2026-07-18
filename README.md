# AniGlow / 萤火番舍

面向中文动漫用户的社区产品，包含番剧检索与排行、真实用户评分、楼中楼讨论、社区帖子、每日投票任务和角色 AIGC 对话。

## 技术架构

- Web: Nuxt 3、Vue 3、Pinia、Tailwind CSS，SSR 开启。
- API: Java 21、Spring Boot 3.5、Spring Security、JPA。
- Data: MySQL 8、Redis 7、Flyway。
- Operations: Docker Compose、Actuator、Prometheus、结构化滚动日志、GitHub Actions。

请求统一使用 `ApiResponse<T>` 信封；前端业务请求通过 `useApi()` 注入 JWT、解析错误和透传 `X-Trace-Id`。后端按 Controller -> Service -> Repository 分层，事务只放在 Service。

## 本地启动

```bash
cp .env.example .env
pnpm install --frozen-lockfile
pnpm dev --host 0.0.0.0 --port 3001
```

后端需要 Java 21、MySQL 8 和 Redis 7。设置 `.env` 中的必要变量后运行：

```bash
cd aniglow-backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

生产式本地环境可直接使用：

```bash
docker compose up -d --build
curl http://localhost:8081/api/actuator/health
```

`.env` 不得提交。`MYSQL_PASSWORD`、`JWT_SECRET`、`AI_API_KEY` 和 `NUXT_AUTHING_APP_SECRET` 缺失时，生产编排会直接失败。

## 数据迁移

数据库结构由 `aniglow-backend/src/main/resources/db/migration` 中的 Flyway 脚本管理，Hibernate 在默认和生产环境只执行 `validate`。新增字段或索引时创建下一个 `V<版本>__<说明>.sql`，不要修改已经发布的迁移，也不要依赖 `ddl-auto=update`。

Jikan 外部同步不再阻塞应用启动。默认启动五分钟后执行定时同步；空库自动抓取默认关闭，可通过管理接口导入，或显式设置 `ANIGLOW_JIKAN_BOOTSTRAP_ON_EMPTY=true`。

Redis 缓存默认使用 `aniglow::` 键前缀。预发布、测试或多实例共享 Redis DB 时，必须通过 `ANIGLOW_CACHE_KEY_PREFIX` 设置独立命名空间。

## 质量门禁

```bash
pnpm test
pnpm exec nuxi typecheck
pnpm build
cd aniglow-backend && mvn verify
```

前端测试包含覆盖率阈值；后端覆盖鉴权、限流、JWT、分页、评分、投票、TraceId 和 API 集成。CI 在每次推送和 PR 上执行完整测试、类型检查与构建。

## 运维入口

- 健康检查：`GET /api/actuator/health`
- Prometheus：`GET /api/actuator/prometheus`，仅管理员可访问。
- OpenAPI：`GET /api/swagger-ui.html`
- 请求追踪：响应头 `X-Trace-Id` 与日志 MDC 中的 `traceId` 一致。
- 日志：默认写入 `logs/aniglow-backend.log`，按天和大小滚动并保留 30 天。

## 目录

```text
aniglow-backend/  Spring Boot API、迁移与测试
assets/           全局样式和页面样式
components/       按业务领域拆分的 Vue 组件
composables/      API 与页面业务编排
pages/            Nuxt 路由壳
stores/           Pinia 状态
tests/            前端单元测试
docs/             设计、重构计划与验收记录
```
