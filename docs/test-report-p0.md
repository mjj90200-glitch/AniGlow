# P0 测试报告

> 执行日期：2026-06-01
> 测试范围：贝叶斯评分算法、JWT 认证、Agent 情感/意图解析、关键 API 集成
> 结果：70 个测试全部通过

## 测试概况

| 测试类 | 数量 | 类型 | 状态 |
|---|---|---|---|
| BayesianRatingServiceTest | 12 | 单元测试 | ✅ |
| JwtUtilsTest | 15 | 单元测试 | ✅ |
| AgentServiceEmotionTest | 25 | 单元测试 | ✅ |
| ApiIntegrationTest | 18 | 集成测试 | ✅ |

## 测试覆盖详情

### 1. 贝叶斯评分算法 (BayesianRatingService)

**正常计算**
- 评分人数多时偏向自身评分
- 评分人数等于置信权重(m=25)时平滑过渡
- 参数化验证回归效果（极少评分向全局平均分回归）

**边界与异常**
- `null` 评分 → 返回全局平均分 7.0
- `null` / `0` 评分人数 → 返回全局平均分
- 极大评分人数 (1,000,000) → 结果接近自身评分
- 输出精度为两位小数

### 2. JWT 认证 (JwtUtils)

**Token 生成**
- 生成包含用户名的有效 token
- 生成的 token 可被验证通过
- 带不同自定义 claims 生成不同 token
- 刷新 token 有效期长于普通 token

**Token 验证**
- 用户名不匹配时验证失败
- 过期 token 被 `validateJwtToken` 拒绝
- 签名损坏的 token 被拒绝
- 空字符串 / null / 无效格式 token 被拒绝
- 缺少签名部分的 token 被拒绝

**Token 解析**
- 从有效 token 提取用户名
- 从有效 token 提取过期时间
- 过期 token 仍可从异常中获取 Claims

### 3. Agent 情感/意图解析 (AgentService)

**情感标签解析 (parseEmotion)**
- `[happy]` / `[normal]` / `[complex]` 三种情感标签正确识别
- 无标签内容默认返回 `normal`
- 多行内容中的标签正确解析
- 标签在段落中间不被识别（必须行首）
- null / 空白 / 空字符串输入容错
- 未知标签名默认回退为 `normal`

**常见意图识别 (resolveCommonIntent)**
- 8 种意图关键词全覆盖：hello、what_doing、thanks、goodnight、miss_you、praise、love
- 无匹配意图返回空字符串
- 热门角色 (Rem、Onodera) 内置默认回复配置完整

### 4. API 集成测试

**动漫接口 (公开)**
- GET `/anime` — 分页列表、排序、翻页参数
- GET `/anime/{id}` — 详情、包括 genre、打分信息
- GET `/anime/search?keyword=` — 标题搜索、空结果
- GET `/anime/top-rated` — 按贝叶斯评分降序
- GET `/anime/genre/{genre}` — 按类型筛选
- GET `/anime/{id}` 不存在 → 404 统一格式

**评分接口**
- GET `/ratings/anime/{animeId}` — 公开访问
- POST `/ratings` 未登录 → 401

**社区接口**
- GET `/communities` — 社区列表、featured 字段
- GET `/communities/{slug}` — 社区详情、tags 数组
- GET `/communities/{slug}/posts` — 帖子分页
- GET `/communities/{slug}` 不存在 → 404
- POST `/communities/{slug}/posts` 未登录 → 401

**异常处理**
- 404 返回统一 `ApiResponse` 格式（success=false、message、timestamp）
- 未认证访问需认证资源 → 401

---

## 发现的问题

### 🔴 Bug：JwtUtils.validateJwtToken 未捕获签名异常

- **文件**: `aniglow-backend/src/main/java/com/aniglow/security/JwtUtils.java:92`
- **问题**: `validateJwtToken` 方法 `catch (SecurityException e)` 捕获的是 `java.lang.SecurityException`，而非 JJWT 的 `io.jsonwebtoken.security.SecurityException`。因为 `import io.jsonwebtoken.*` 通配符不会导入 `io.jsonwebtoken.security` 包下的类。
- **影响**: 当签名校验失败时（如 token 被篡改），JJWT 抛出 `io.jsonwebtoken.security.SignatureException`（继承自 `io.jsonwebtoken.security.SecurityException`），该异常未被捕获，向上传播为 500 错误，而非返回 `false`。
- **修复建议**: 在文件头部添加 `import io.jsonwebtoken.security.SecurityException;`，或在 catch 中显式使用全限定类名。

### 🟡 意图识别优先级冲突

- **文件**: `aniglow-backend/src/main/java/com/aniglow/service/AgentService.java:583-601`
- **问题 1**: `"你好厉害"` 被 hello 分支（匹配 `"你好"`）优先捕获，而非进入 praise 分支。因为 hello 检查在 praise 之前。
- **问题 2**: `"喜欢你"` 被 praise 分支（匹配 `"喜欢你"`）优先捕获，而非进入 love 分支。同样是因为 praise 检查在 love 之前且关键词列表存在重叠。
- **影响**: 部分夸奖/表白类对话被识别为错误的意图，导致 AI 角色做出不符合预期的回复。
- **修复建议**:
  - 调整分支顺序，将更精确的意图（love、praise）放在模糊意图（hello）之前。
  - 或将重叠关键词从较早分支中移除（如从 praise 中移除 `"喜欢你"`，从 hello 中移除 `"你好"` 之外的单独 `"你"` 匹配）。

---

## 测试基础设施

### 新增依赖

**pom.xml**:
```xml
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.wiremock</groupId>
    <artifactId>wiremock-standalone</artifactId>
    <version>3.5.4</version>
    <scope>test</scope>
</dependency>
```

### 测试配置

- 集成测试使用独立 MySQL 测试库 `aniglow_test`（`application-test.yml`）
- JPA `ddl-auto: create-drop` 确保每次测试表结构干净
- 定时任务 (`@Scheduled`) 在测试环境禁用
- AI API 在测试环境禁用 (`enabled: false`)

### 运行命令

```bash
# 运行所有测试
cd aniglow-backend
JAVA_HOME=/path/to/jdk-21 mvn test

# 运行单元测试
mvn test -Dtest="BayesianRatingServiceTest,JwtUtilsTest,AgentServiceEmotionTest"

# 运行集成测试
mvn test -Dtest="ApiIntegrationTest"
```

---

## 后续测试计划

| 优先级 | 范围 | 状态 |
|---|---|---|
| P0 | 贝叶斯评分 + JWT + Agent情感解析 + API集成 | ✅ 已完成 |
| P1 | Agent 额度控制 / SSE 流式 / 意图覆盖度 | 待执行 |
| P1 | 前端 composable / store 测试 | 待执行 |
| P2 | 安全测试 (SQL注入 / XSS / 越权) | 待执行 |
| P2 | E2E 关键路径 (Playwright) | 待执行 |
| P3 | 组件渲染测试 + Redis 集成测试 | 待执行 |
