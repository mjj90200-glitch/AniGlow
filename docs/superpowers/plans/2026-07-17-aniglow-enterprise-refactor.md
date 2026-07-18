# Aniglow 企业级重构路线图

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 Aniglow 动漫社区应用从 Vibe Coding 原型提升至企业级生产水准，覆盖安全止血、性能优化、架构偿债、运维工程化四个维度。

**Architecture:** 按"高收益/易修改"→"高风险/必须改"→"长线优化"三级优先级分阶段推进。每项任务给出目标、涉及文件、改法要点、验证方式，不逐行写完整代码。

**Tech Stack:** 前端 Nuxt 3 (Vue 3 + Pinia + Tailwind) / 后端 Spring Boot 3.5.16 + JPA + MySQL + Redis + JWT + Authing + 火山方舟 AI / Docker Compose 部署

**Implementation status (2026-07-18):** Phase 2 与 Phase 3 已全部完成并部署验证。具体改动与发布检查见 `docs/refactor/phase-2-3-implementation.md`。

---
---

## 🔴 Phase 1：安全止血（本周内必须完成）

### Task 1: 轮换所有已泄漏的密钥并杜绝明文

**Files:**
- Modify: `aniglow-backend/src/main/resources/application.yml`
- Modify: `aniglow-backend/src/main/resources/application-docker.yml`
- Modify: `docker-compose.yml`
- Create: `.env.example`

**Goal:** 数据库密码、火山方舟 API Key、JWT secret 曾明文进入旧 Git 历史——光改代码无用，必须轮换密钥 + 清历史 + 改为仅从环境变量读取。

**Approach:**
1. 登录数据库/火山方舟控制台/各外部服务，**轮换所有三组密钥**，拿到新值。
2. `application.yml` 默认段删除所有明文兜底值，改为 `${DB_PASSWORD}`、`${VOLCANO_API_KEY}`、`${JWT_SECRET}`，缺环境变量即启动失败——不留默认值。
3. `application-docker.yml` 同步去掉所有明文兜底。
4. `docker-compose.yml` 去掉所有 `password: ${MYSQL_ROOT_PASSWORD:-removed-default-password}` 的默认值，改为 `${MYSQL_ROOT_PASSWORD:?required}`。
5. 创建 `.env.example` 仅保留变量名（值清空或填 `changeme`），供新环境自举。
6. 用 `git filter-repo` 或 `BFG Repo-Cleaner` 清理 git 历史中的密钥痕迹（⚠️ 需团队协调，所有人先提交，操作后 force push 到 master 需全员重新 clone）。

**Verification:**
- 使用密钥扫描工具检查源码和 Git 历史，确保没有真实凭据残留。
- `docker-compose up` 在缺 `.env` 时应直接报错退出，不静默启动。

---

### Task 2: 锁死 AI Agent 接口鉴权

**Files:**
- Modify: `aniglow-backend/src/main/java/com/aniglow/config/SecurityConfig.java`
- Modify: `aniglow-backend/src/main/java/com/aniglow/service/AgentService.java`
- Modify: `aniglow-backend/src/main/java/com/aniglow/controller/AgentController.java`
- Create: `aniglow-backend/src/test/java/com/aniglow/security/AgentAuthTest.java`

**Goal:** `/agent/**` 当前 permitAll，且 `userId` 从客户端请求体取值→配额/会员可任意绕过，LLM 费用可被白嫖。改为：需要认证 + userId 从 JWT SecurityContext 取。

**Approach:**
1. `SecurityConfig.java:57` 删除 `"/agent/**"` 从 permitAll 列表，或改成 `"/agent/public/**"`(如需保留公开入口) 并限制具体路由。
2. `AgentService.java:442` 的 `resolveUserId(request)` 改为从 `SecurityContextHolder.getContext().getAuthentication().getPrincipal()` 取 `UserDetailsImpl` 的 ID，**不再信任请求体**。
3. `AgentController.java` 中 `@PostMapping("/chat")` 和 `/chat/stream` 加 `@AuthenticationPrincipal UserDetailsImpl user` 参数，传进 Service。
4. `AgentService.java:215-234` 的 `activateMonthlyMembership()` 同样改为从认证上下文取 userId。
5. 前端 `composables/useAgent.ts` 的 `chat()` 和 `chatStream()` 不再发送请求体里的 `userId` 字段，后端从 token 解析。

**Verification:**
- `curl -X POST /api/agent/chat -H "Content-Type: application/json" -d '{"message":"hi","userId":"attacker"}'` 未带 token → 返回 401。
- 带有效 token 调用 `/chat`，后端使用的 userId = token 中的真实用户 ID，请求体中的 userId 被忽略。
- `POST /agent/membership/activate-monthly` 未带 token → 401。

---

### Task 3: 补部署资产进 Git + 上最小 CI

**Files:**
- Create: `.github/workflows/ci.yml`
- Stage (git add): `Dockerfile`, `docker-compose.yml`, `aniglow-backend/Dockerfile`, `aniglow-backend/src/main/resources/application-docker.yml`, `mysql-init/init.sql`, `start-backend.sh`, `.env.example`

**Goal:** 整套部署资产（Dockerfile、compose、建库 SQL）当前仅在本机，未纳入版本控制。同时完全没有 CI 门禁。

**Approach:**
1. 先确认上述文件已去掉所有默认密码（Task 1 完成后），然后 `git add` 提交。
2. 创建 `.github/workflows/ci.yml`，最小流水线:
   ```yaml
   name: CI
   on: [push, pull_request]
   jobs:
     frontend:
       runs-on: ubuntu-latest
       steps:
         - uses: actions/checkout@v4
         - uses: pnpm/action-setup@v2
           with: { version: 10 }
         - run: pnpm install --frozen-lockfile
         - run: pnpm test
         - run: pnpm build
     backend:
       runs-on: ubuntu-latest
       services:
         mysql:
           image: mysql:8.0
           env: { MYSQL_ROOT_PASSWORD: test, MYSQL_DATABASE: aniglow_test }
           ports: [3306]
           options: --health-cmd "mysqladmin ping" --health-interval 10s --health-timeout 5s --health-retries 5
       steps:
         - uses: actions/checkout@v4
         - uses: actions/setup-java@v4
           with: { java-version: '21', distribution: 'temurin' }
         - run: mvn -f aniglow-backend/pom.xml verify
   ```
3. `aniglow-backend/Dockerfile:18` 删除 `-DskipTests`，镜像构建必须跑测试。

**Verification:**
- GitHub Actions 页面看到两个 job 成功变绿（或失败时阻止合并）。
- 另一台机器 `git clone` → `cp .env.example .env` → `docker-compose up` 可启动。

---

### Task 4: 全站关键接口加限流

**Files:**
- Modify: `aniglow-backend/pom.xml` — 加 `com.github.vladimir-bukhtoyarov:bucket4j-core` 依赖
- Create: `aniglow-backend/src/main/java/com/aniglow/config/RateLimitFilter.java`
- Modify: `aniglow-backend/src/main/java/com/aniglow/config/SecurityConfig.java`

**Goal:** 登录/注册/发帖/上传当前无任何服务端频率限制，可暴力破解或刷帖。用 Bucket4j + 内存/Redis 实现。

**Approach:**
1. 写 `RateLimitFilter` — OncePerRequestFilter，用 ConcurrentHashMap(或 Redis) 存 IP-user 的 token bucket。
2. 规则：`/auth/login` 5次/分钟/IP；`/auth/register` 3次/分钟/IP；`POST /communities/*` 10次/分钟/用户；`POST /communities/upload/images` 5次/分钟/用户。
3. 超限返回 429 + JSON `{ code: 429, message: "请求过于频繁，请稍后再试" }`。
4. 在 SecurityConfig 中把此 Filter 加到 `addFilterBefore`。

**Verification:**
- 同一 IP 连续 6 次 POST `/auth/login` → 第 6 次返回 429。
- 等一分钟后可再次请求成功。

---
---

## 🟢 Phase 2：高收益快赢（1-2周，一行到几行即可大幅改善）

### Task 5: 消除三个核心 N+1 查询

**Files:**
- Modify: `aniglow-backend/src/main/java/com/aniglow/entity/Anime.java`
- Modify: `aniglow-backend/src/main/java/com/aniglow/repository/RatingRepository.java`
- Modify: `aniglow-backend/src/main/java/com/aniglow/repository/AnimeRepository.java`
- Modify: `aniglow-backend/src/main/java/com/aniglow/controller/RatingController.java`

**Goal:** 首页番剧列表、评分列表、排行榜每页分别触发 1+20、1+40、1+20 次 SQL——一处注解即可消除。

**Approach:**
1. `Anime.java:125` — `@ElementCollection(fetch = EAGER)` 改为 `fetch = LAZY`，类上加 `@BatchSize(size = 50)`。列表查询不需要 genres 时不加载。
2. `AnimeRepository.java` — `findAll(pageable)` 之上加不去 genres 的投影版本用于列表。或直接在 entity 层面加 `default_batch_fetch_size: 50`（`application.yml` 的 `spring.jpa.properties`）。
3. `RatingRepository.java` — `findRecentReviews()` 和 `findPopularReviewsByAnimeId()` 加 `@EntityGraph(attributePaths = {"user", "anime"})`。
4. `RatingController.java:238-249` 的 `convertToDto` 不再逐条触发 LAZY 加载（因为 EntityGraph 已在查询时预取）。

**Verification:**
- 开启 `spring.jpa.properties.hibernate.show_sql: true`（临时），请求首页和评分列表，SQL 条数从 1+20+N 降为 2-3 条。
- 功能无回归，数据正确。

---

### Task 6: 修复评分内存分页

**Files:**
- Modify: `aniglow-backend/src/main/java/com/aniglow/repository/RatingRepository.java`
- Modify: `aniglow-backend/src/main/java/com/aniglow/controller/RatingController.java`

**Goal:** `RatingController.java:51-55` 的 `findByAnimeId(animeId)` 拉取该番**全部评分**到内存再 skip/limit（pageable 构造了但未使用）。热门番 1 万评分全加载。

**Approach:**
1. `RatingRepository.java` 加: `Page<Rating> findByAnimeIdOrderByCreatedAtDesc(Long animeId, Pageable pageable)`。
2. `RatingController.java:51-55` 改为传入 pageable: `ratingRepository.findByAnimeIdOrderByCreatedAtDesc(animeId, pageable)`。

**Verification:**
- `curl "/api/ratings/anime/1?page=0&size=20"` —— 数据库只执行含 LIMIT 20 的查询，不拉全表。

---

### Task 7: 列表接口加 Redis 缓存

**Files:**
- Modify: `aniglow-backend/src/main/java/com/aniglow/service/RedisRankingService.java` (或新建 CacheConfig)
- Modify: `aniglow-backend/src/main/java/com/aniglow/controller/AnimeController.java`
- Modify: `aniglow-backend/src/main/java/com/aniglow/controller/CommunityController.java`

**Goal:** `@EnableCaching` 已声明但全项目零 `@Cacheable`。番剧列表/社区盒子/排行榜每次实时查库，Redis 只用在了排行榜和配额。

**Approach:**
1. 确认 `RedisCacheManager` Bean 配置存在（`application.yml` Redis 配置正常）。
2. `AnimeController.getAllAnime()` 加 `@Cacheable(value = "animeList", key = "#page + ':' + #size + ':' + #sortBy", unless = "#result == null")`，TTL 5 分钟。
3. 番剧详情 `getAnimeById()` 加 `@Cacheable(value = "animeDetail", key = "#id")`。
4. `CommunityController` 的 `listCommunities()` 加 `@Cacheable(value = "communityBoxes", key = "#page + ':' + #size")`。
5. 写操作（新增番剧/评分/发帖/删帖）的接口加 `@CacheEvict(value = "...", allEntries = true)` 使缓存失效。

**Verification:**
- 第一次请求 `/api/anime?page=0&size=20` 触发 SQL，第二次同一请求不触发 SQL（Redis 命中）。
- 新建评分后再次请求，缓存被驱逐，返回含新数据的最新列表。

---

### Task 8: 数据库索引补充

**Files:**
- Create: `mysql-init/migration/001_add_missing_indexes.sql`
- Modify: `aniglow-backend/src/main/java/com/aniglow/entity/Anime.java`

**Goal:** 季度页全扫（缺 `(year,season)` 索引）、番剧按 status 筛选无索引、全表模糊搜索走全扫。

**Approach:**
1. 创建迁移 SQL:
   ```sql
   ALTER TABLE anime ADD INDEX idx_year_season (year, season);
   ALTER TABLE anime ADD INDEX idx_status (status);
   ALTER TABLE anime ADD FULLTEXT INDEX idx_ft_title (title, title_english, title_cn, search_aliases);
   ```
2. `AnimeRepository.java` 模糊搜索方法改为 `WHERE MATCH(title, title_english, title_cn, search_aliases) AGAINST(:keyword IN BOOLEAN MODE)`（或保留 LIKE 做回退，FULLTEXT 走在前）。
3. `Anime.java` entity 类上补 `@Table(indexes = {...})` 注解与新索引一致。

**Verification:**
- `EXPLAIN SELECT * FROM anime WHERE year = 2024 AND season = 'SPRING'` 显示 `type: ref, key: idx_year_season`（不走 ALL）。
- 全文搜索 "鬼灭" 返回相关番剧，速度明显快于 LIKE 全扫。

---

### Task 9: 关闭生产环境调试泄漏 + open-in-view

**Files:**
- Modify: `aniglow-backend/src/main/resources/application.yml`
- Modify: `aniglow-backend/src/main/resources/application-docker.yml`

**Goal:** 默认 profile 下 `show-sql:true` + `logging.level.org.hibernate.SQL: DEBUG`，生产日志会被 SQL 撑爆；`open-in-view: true` 让数据库连接持有到视图渲染完成。

**Approach:**
1. `application.yml` 默认段:
   - `ddl-auto` 改为 `validate`（或保持 `update` 但仅 dev profile）。
   - `show-sql` 改 `false`。
   - 全局日志 `root: INFO`（删除默认段里的 DEBUG）。
   - 加 `spring.jpa.open-in-view: false`。
2. `application-docker.yml` 作为 docker/compose 部署专用，保持 `ddl-auto: validate`, `show-sql: false`, `logging.level.root: WARN`。
3. 如需开发调试，另建 `application-dev.yml`，`show-sql: true` + DEBUG 仅 dev 开启。

**Verification:**
- `docker-compose up` 后日志不出现大量 Hibernate SQL 输出。
- `curl /api/anime/1` 接口仍正常返回。

---
---

## 🔵 Phase 3：结构性偿债（1个月+，基础夯实）

### Task 10: 抽 Service 层，事务下沉

**Files:**
- Create: `aniglow-backend/src/main/java/com/aniglow/service/CommunityService.java`
- Create: `aniglow-backend/src/main/java/com/aniglow/service/AuthService.java`
- Create: `aniglow-backend/src/main/java/com/aniglow/service/RatingService.java`
- Create: `aniglow-backend/src/main/java/com/aniglow/service/UserDisplayNameResolver.java`（消除四处复制粘贴）
- Modify: `aniglow-backend/src/main/java/com/aniglow/controller/CommunityController.java`
- Modify: `aniglow-backend/src/main/java/com/aniglow/controller/AuthController.java`
- Modify: `aniglow-backend/src/main/java/com/aniglow/controller/RatingController.java`

**Goal:** Controller 当前直连 Repository，业务逻辑散落 Web 层，事务注解标在 Controller 上。改为: Controller 只做参数绑定与响应组装，Service 承载事务。

**Approach:**
1. 按领域创建 Service（CommunityService / AuthService / RatingService），各自注入所需 Repository。
2. 将 Controller 中的业务逻辑逐段迁入 Service 方法，Service 方法加 `@Transactional`。
3. `UserDisplayNameResolver` 收敛四处复制的 `resolvePublicDisplayName / isSafePublicName` 逻辑，作为 `@Component` 注入各 Service。
4. Controller 层删除所有 `@Transactional` 和直接的 Repository 注入（只保留 Service 注入）。
5. 逐步迁移，每迁一个 Controller 就跑对应的测试确认无回归。

**Verification:**
- Controller 类不再直接注入任何 Repository Bean，只注入 Service。
- 所有 `@Transactional` 注解仅在 Service 层出现。
- 发帖、评分、登录、Authing 注册等核心流程功能不变。

---

### Task 11: 拆分前端巨型组件 + 统一 API 封装

**Files:**
- Create: `components/agent/GalgameLayout.vue`、`components/agent/MobileChat.vue`、`components/agent/EmotionStickers.vue`
- Modify: `pages/agent.vue`（变为布局壳，只根据设备切换子组件）
- Create: `composables/useApi.ts`（统一 `$fetch` 实例 + 拦截器）
- Modify: `composables/useAnime.ts`、`composables/useCommunity.ts`、`composables/useAgent.ts`（改为通过 useApi 调用）
- Modify: `pages/anime/[id].vue`（拆分子组件: AnimeHero / AnimeDetailTabs / AnimeSidebar）
- Modify: `pages/index.vue`（抽 AnimeGrid、SeasonFilter、CommunityBox 等子组件）

**Goal:** `agent.vue` 1813 行同一文件混合桌面/移动布局；API 调用缺少统一的错误处理与 token 注入。

**Approach:**
1. 创建 `composables/useApi.ts`:
   ```ts
   export const useApi = () => {
     const { token } = useAuth()
     const config = useRuntimeConfig()
     return $fetch.create({
       baseURL: config.public.backendUrl,
       onRequest({ options }) {
         if (token.value) options.headers.set('Authorization', `Bearer ${token.value}`)
       },
       onResponseError({ response }) {
         if (response.status === 401) { /* 跳登录 */ }
         // 统一 toast 错误提示
       }
     })
   }
   ```
2. `useAnime` / `useCommunity` / `useAgent` 去重——删除各自 `const api = (path)=>...` 的定义，改用 `useApi()`。
3. `agent.vue` 按视口拆分: `<client-only>` 里用 `useMediaQuery` 判断，桌面渲染 `GalgameLayout`（对话+角色立绘），移动渲染 `MobileChat`（微信风格气泡+表情贴纸）。
4. `anime/[id].vue` 1260 行按功能区拆分子组件。`index.vue` 796 行抽 AnimeGrid/SeasonFilter/CommunityBox。

**Verification:**
- `agent.vue` 文件从 1813 行降到 < 150 行（仅布局壳）。
- 所有 API 调用路径经过 `useApi` 实例，401 自动跳登录，网络错误有 toast 提示。
- 桌面/移动布局切换正常，功能无回归。

---

### Task 12: 升级框架与清理依赖

**Files:**
- Modify: `aniglow-backend/pom.xml`
- Modify: `package.json`
- Modify: `internal/nuxt/paths.mjs` (入 git)
- Delete: `package-lock.json`

**Goal:** Spring Boot 3.2.5 已 EOL；Authing 依赖可疑（bg 后缀非正式版、V2 旧 SDK）；双锁文件；internal 补丁未入库。

**Approach:**
1. `pom.xml`: `spring-boot-starter-parent` 升到 `3.3.x` 或 `3.4.x`（OSS 持续有安全补丁的版本）。同步更新 springdoc 版本。Lombok 版本统一属性+依赖走 `${lombok.version}`。去掉显式 HikariCP 声明（spring-boot-starter-data-jpa 自带）。
2. `package.json`: 联系 Authing 确认正式版 SDK 替代方案（bg 版本可能是内部测试构建）。双锁文件统一保留 `pnpm-lock.yaml`，删除 `package-lock.json`。
3. `internal/` 目录 `git add` + 提交，并在目录内加 README 解释这是 Nuxt vitest 兼容补丁（模拟 `#internal/nuxt/paths`），以及改 `baseURL` 需要同步改此处。

**Verification:**
- `mvn dependency:tree` 无冲突，`mvn -f aniglow-backend test` 通过。
- `pnpm install --frozen-lockfile` 成功，无 peer dependency 警告。
- `pnpm test` 通过。
- 另一台 clone 后 `pnpm install` 不会因为 `#internal` 导入找不到文件而报错。

---

### Task 13: 引入 Flyway 数据库版本化迁移

**Files:**
- Modify: `aniglow-backend/pom.xml` — 加 `flyway-core` + `flyway-mysql`
- Create: `aniglow-backend/src/main/resources/db/migration/V1__baseline.sql`
- Modify: `aniglow-backend/src/main/resources/application.yml` — `ddl-auto: validate`

**Goal:** 表结构全靠 `ddl-auto: update` 演进，多实例部署或回滚时是灾难。引入 Flyway 版本化管理。

**Approach:**
1. `pom.xml` 加:
   ```xml
   <dependency>
     <groupId>org.flywaydb</groupId>
     <artifactId>flyway-core</artifactId>
   </dependency>
   <dependency>
     <groupId>org.flywaydb</groupId>
     <artifactId>flyway-mysql</artifactId>
   </dependency>
   ```
2. 从现有数据库 `mysqldump --no-data` 导出当前表结构生成 `V1__baseline.sql`（或让 Hibernate `ddl-auto: create` 跑一次导出）。
3. `application.yml` 改 `ddl-auto: validate`（Flyway 接管建表，Hibernate 只验证 entity 与表结构一致）。
4. 后续所有表结构变更写成 `V2__<description>.sql`，不可再依赖 `update`。
5. docker-compose 中确认 Flyway 在 app 启动时自动执行迁移（`spring.flyway.enabled: true`）。

**Verification:**
- 清空 test 库 → 启动应用 → Flyway 日志输出 `Successfully applied 1 migration(s)`，表结构被自动创建。
- 修改 entity 但忘了写迁移 SQL → 启动时 `validate` 报 mismatch 而非静默失败。

---

### Task 14: 上可观测性（Actuator + TraceId + 日志滚动）

**Files:**
- Modify: `aniglow-backend/pom.xml` — 加 `spring-boot-starter-actuator` + `micrometer-registry-prometheus`
- Modify: `aniglow-backend/src/main/resources/application.yml`
- Create: `aniglow-backend/src/main/resources/logback-spring.xml`
- Create: `aniglow-backend/src/main/java/com/aniglow/config/TraceIdFilter.java`

**Goal:** 无 Actuator（却有 springdoc show-actuator 空引用）、无 traceId 链路追踪、日志无滚动切割（nohup 裸重定向）。上了之后 docker-compose 的 healthcheck 可改为调 `/actuator/health` 真实探活。

**Approach:**
1. `pom.xml` 加 actuator + prometheus 依赖。
2. `application.yml`:
   ```yaml
   management:
     endpoints:
       web:
         exposure:
           include: health, info, prometheus
     endpoint:
       health:
         show-details: when-authorized
   ```
3. `logback-spring.xml`:
   - RollingFileAppender，按天切割 + 保留 30 天 + 总大小上限 3GB。
   - pattern 注入 `%X{traceId}` 到每条日志。
   - 控制台输出保持（compose logs 可看），INFO 以上写到文件。
4. `TraceIdFilter` — OncePerRequestFilter，`MDC.put("traceId", UUID.randomUUID().toString().replace("-","").substring(0,12))`，响应头 `X-Trace-Id` 回传。
5. `docker-compose.yml` 中 backend healthcheck 从 `pgrep -f app.jar` 改为 `wget -qO- http://localhost:8081/api/actuator/health`。

**Verification:**
- `curl /api/actuator/health` → `{"status":"UP","components":{"db":{"status":"UP"},"redis":{"status":"UP"}}}`。
- 发起 HTTP 请求 → 响应头含 `X-Trace-Id: a1b2c3d4e5f6` → 后端日志里对应行能搜到此 traceId。
- 日志文件在 `logs/` 下按日期滚动，旧文件被自动压缩。

---

### Task 15: 补关键测试 + 前端测试从零起步

**Files:**
- Create: `aniglow-backend/src/test/java/com/aniglow/security/JwtAuthenticationFilterTest.java`
- Create: `aniglow-backend/src/test/java/com/aniglow/service/AgentVoteServiceTest.java`
- Create: `tests/useAnime.test.ts` (前端 vitest)
- Create: `tests/useAuth.test.ts`
- Modify: `vitest.config.ts`

**Goal:** 后端缺 JwtAuthenticationFilter 和 AgentVoteService 的单元测试；前端测试为零（唯一文件是占位 `expect(1+1)`，且路径不匹配跑不到）。

**Approach:**
1. `JwtAuthenticationFilterTest` — 覆盖: 有效 token 放行并设置 SecurityContext / 过期 token 返回 401 / 缺失 token 放行（公开路由由 SecurityConfig 处理）/ 篡改 token 返回 401。
2. `AgentVoteServiceTest` — 覆盖: 周四结算 cron 逻辑 / 票数前三晋升代理的逻辑 / 平票处理。
3. 前端:
   - `vitest.config.ts` 确认 include 路径正确 (`'tests/**/*.test.ts'`)。
   - `tests/useAnime.test.ts` — mock `$fetch`，测 `fetchAnimeList` 正常解包返回值、fetch 失败返回空数组。
   - `tests/useAuth.test.ts` — 测 sessionStorage 恢复 token、logout 清除状态。

**Verification:**
- `mvn -f aniglow-backend test` 全部测试通过（包括新增的两套）。
- `pnpm test` 不再跑 1 个占位测试，而是跑 ≥ 5 个有意义的前端测试且全通过。

---
---

## 📋 执行顺序总览

```
Phase 1 (本周)
  Task 1 → Task 2 → Task 3 → Task 4
  (密钥轮换是前置依赖，先做。其他三项可部分并行)

Phase 2 (1-2周)
  Task 6 → Task 5 → Task 8 → Task 7 → Task 9
  (修复内存分页先，确保数据正确；再加索引和缓存)

Phase 3 (1个月+)
  Task 10 ──→ Task 11
               Task 12
  Task 13 ──→ Task 14
               Task 15
  (Service层和Flyway是最大块，但独立，可并行推进)
```

**风险提示:**
- Task 1 (git filter-repo) 需要全团队协调，建议选低峰窗口操作。
- Task 12 (Spring Boot 升级) 可能带来 API 兼容性问题（如 spring-web 6.1→6.2），先在分支上验证。
- Task 13 (Flyway) 首次基线生成时务必从生产库导出真实结构，避免遗漏表。
