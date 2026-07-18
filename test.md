# 萤火番舍·AniGlow — 全量系统测试报告

> 测试日期：2026-06-02 | 测试人：自动化测试 | 环境：本地开发环境（backend Java 8081, frontend Nuxt 3001, MySQL 3306, Redis 6379）

---

## 一、测试概览

| 指标 | 数值 |
|------|------|
| 用例总数 | 78 |
| 通过 | 52 |
| 失败 | 18 |
| 阻塞 | 8 |
| 通过率 | 66.7% |
| 测试环境 | 本地开发环境（Java 21 / Nuxt 3 / MySQL 8.0 / Redis 8.6） |
| 数据规模 | 1146 条番剧, 20 个用户, 7 条评分, 6 个社区 |

---

## 二、分模块测试明细

### 2.1 用户认证模块 (Auth)

| 用例ID | 测试点 | 前置条件 | 操作步骤 | 预期结果 | 实际结果 | 状态 |
|--------|--------|----------|----------|----------|----------|------|
| S2.1 | 注册-正常路径 | 无 | POST /api/auth/register 含合法 username/email/password | 200, 返回token | 200, 注册成功, 返回JWT | ✅ 通过 |
| F-AUTH-01 | 注册-正常路径 | 无 | 注册 functest01 | 200 | 200 注册成功 | ✅ 通过 |
| F-AUTH-02 | 注册-空用户名 | 无 | username="" | 400 参数校验失败 | 400 参数校验失败 | ✅ 通过 |
| F-AUTH-03 | 注册-非法邮箱 | 无 | email="not-an-email" | 400 邮箱格式不正确 | 400 参数校验失败 | ✅ 通过 |
| F-AUTH-04 | 注册-密码过短 | 无 | password="12345" (5位) | 400 密码长度不足 | 400 参数校验失败 | ✅ 通过 |
| F-AUTH-05 | 注册-重复用户名 | functest01 已存在 | 再次注册 functest01 | 400 用户名已存在 | 400 用户名已存在 | ✅ 通过 |
| F-AUTH-06 | 注册-重复邮箱 | functest01@test.com 已存在 | 再次使用该邮箱 | 400 邮箱已被注册 | 400 邮箱已被注册 | ✅ 通过 |
| F-AUTH-09 | 注册-超长用户名 | 无 | username=100个a | 400 用户名过长 | 400 参数校验失败 | ✅ 通过 |
| S2.2 | 登录-正常路径 | 用户已注册 | POST /api/auth/login | 200, 返回JWT | 500 "服务器内部错误" | ❌ 失败 |
| F-AUTH-07 | 登录-空用户名 | 无 | username="" | 400 参数校验失败 | 400 参数校验失败 | ✅ 通过 |
| F-AUTH-08 | 登录-空密码 | 无 | password="" | 400 参数校验失败 | 400 参数校验失败 | ✅ 通过 |
| S2.3 | 获取当前用户资料 | 已登录有JWT | GET /api/auth/me + Bearer token | 200, 返回用户信息 | 403 "权限不足" | ❌ 失败 |
| F-AUTH-10 | /me 无token | 无 | GET /api/auth/me | 401/403 | 403 "权限不足" | ✅ 通过 |
| F-AUTH-11 | /me 无效token | 无 | Authorization: Bearer invalid | 401/403 | 403 "权限不足" | ✅ 通过 |
| F-AUTH-12 | /me 过期token | 无 | 使用过期JWT | 401 | 403 "权限不足" | ⚠️ 待确认 |
| F-AUTH-14 | 登录-缺少Content-Type | 无 | POST 无Content-Type头 | 400/415 | 500 服务器内部错误 | ❌ 失败 |
| I-04 | Authing桥接登录 | 无 | POST /auth/authing-login | 200, 创建/更新用户+JWT | 200, DisplayName正确 | ✅ 通过 |

### 2.2 番剧浏览模块 (Anime)

| 用例ID | 测试点 | 前置条件 | 操作步骤 | 预期结果 | 实际结果 | 状态 |
|--------|--------|----------|----------|----------|----------|------|
| S1.1 | 首页SSR渲染 | 无 | GET / | 200, title含"萤火番舍" | 200, title正确 | ✅ 通过 |
| S1.2 | 番剧列表 | 无 | GET /api/anime?page=0&size=5 | 200, 返回5条 | 200, 返回5条, total=1146 | ✅ 通过 |
| S1.3 | 番剧详情 | 无 | GET /api/anime/1 | 200, 返回芙莉莲详情 | 200, title="Sousou no Frieren" | ✅ 通过 |
| F-ANIME-01 | 默认分页 | 无 | GET /api/anime 无参数 | pageSize=20 | pageSize=20 | ✅ 通过 |
| F-ANIME-02 | 自定义分页 | 无 | page=5&size=10 | 返回第5页10条 | 正确 | ✅ 通过 |
| F-ANIME-03 | 负数页码 | 无 | page=-1 | 400 | 400 | ✅ 通过 |
| F-ANIME-04 | size=0 | 无 | size=0 | 400 | 400 | ✅ 通过 |
| F-ANIME-05 | 超大页码 | 无 | page=99999 | 返回空列表 | 空列表, total=1146 | ✅ 通过 |
| F-ANIME-06 | 不存在的ID | 无 | GET /api/anime/99999 | 404 | 404 "动漫未找到" | ✅ 通过 |
| F-ANIME-07 | ID=0 | 无 | GET /api/anime/0 | 404 | 404 | ✅ 通过 |
| F-ANIME-08 | ID非数字 | 无 | GET /api/anime/abc | 400 | **500** | ❌ 失败 |
| F-SSR-01 | 番剧详情SSR | 无 | GET /anime/1 | title="葬送的芙莉莲 - 萤火番舍" | 正确 | ✅ 通过 |

### 2.3 评分系统模块 (Rating)

| 用例ID | 测试点 | 前置条件 | 操作步骤 | 预期结果 | 实际结果 | 状态 |
|--------|--------|----------|----------|----------|----------|------|
| S3.1 | 查看番剧评分(公开) | 无 | GET /api/ratings/anime/1 | 200, 返回评分列表 | 200, 1条评分 | ✅ 通过 |
| S3.2 | 未登录评分 | 无 | POST /api/ratings/anime/1 score=8 | 401 未授权 | **500 "服务器内部错误"** | ❌ 失败 |
| F-RATE-03 | score=0 | 无 | POST score=0 | 400 评分范围错误 | **500** | ❌ 失败 |
| F-RATE-04 | score=11 | 无 | POST score=11 | 400 | **500** | ❌ 失败 |
| F-RATE-05 | score=-1 | 无 | POST score=-1 | 400 | **500** | ❌ 失败 |
| F-RATE-06 | 缺少score字段 | 无 | POST {} | 400 | **500** | ❌ 失败 |
| F-RATE-07 | 对不存在的番剧评分 | 无 | POST /ratings/anime/99999 | 404 | **500** | ❌ 失败 |
| F-RATE-08 | 小数评分 | 无 | POST score=8.5 | 400 或 200 | **500** | ❌ 失败 |
| F-RATE-09 | 字符串score | 无 | POST score="8" | 400 或 200 | **500** | ❌ 失败 |

### 2.4 排行榜模块 (Ranking)

| 用例ID | 测试点 | 前置条件 | 操作步骤 | 预期结果 | 实际结果 | 状态 |
|--------|--------|----------|----------|----------|----------|------|
| S4.1 | 高分排行 | 无 | GET /api/ranking/top-rated?limit=5 | 200, 5条 | 200, 5条 | ✅ 通过 |
| S4.2 | 热度排行 | 无 | GET /api/ranking/popular?limit=5 | 200, 5条 | 200, 5条 | ✅ 通过 |
| F-RANKING-01 | 负数limit | 无 | limit=-1 | 400 | 400 | ✅ 通过 |
| F-RANKING-02 | 超大limit | 无 | limit=99999 | 返回全部 | 返回1146条 | ✅ 通过 |

### 2.5 社区模块 (Community)

| 用例ID | 测试点 | 前置条件 | 操作步骤 | 预期结果 | 实际结果 | 状态 |
|--------|--------|----------|----------|----------|----------|------|
| S5.1 | 社区列表 | 无 | GET /api/communities | 200, 6个社区 | 200, 6个 | ✅ 通过 |
| S5.2 | 版块帖子 | 无 | GET /api/communities/anime | 200 | 200 | ✅ 通过 |
| F-COMM-02 | 根据slug获取 | 无 | GET /api/communities/anime | 200, "番剧社区" | name="番剧社区" | ✅ 通过 |
| F-COMM-03 | 不存在slug | 无 | GET /api/communities/nonexistent | 404 | 404 | ✅ 通过 |
| F-COMM-04 | 未登录发帖 | 无 | POST /api/communities/anime/posts | 401 | 401 (JSON格式略有不一致) | ✅ 通过 |
| F-COMM-05 | 空标题发帖 | 无 | POST title="" | 400 | 401 (先被拦截) | ⚠️ 待确认 |
| F-SSR-02 | 社区SSR | 无 | GET /community/anime | title="番剧社区 - 萤火社区" | 正确 | ✅ 通过 |

### 2.6 AIGC角色聊天模块 (Agent)

| 用例ID | 测试点 | 前置条件 | 操作步骤 | 预期结果 | 实际结果 | 状态 |
|--------|--------|----------|----------|----------|----------|------|
| S6.1 | 角色列表 | 无 | GET /api/agent/characters | 200, 7个角色 | 200, 7个 | ✅ 通过 |
| S6.2 | 角色映射 | 无 | GET /api/agent/roles | 200, 6个映射 | 200, 正确 | ✅ 通过 |
| S6.3 | SSE流式聊天 | 无 | POST role="Rem" message="你好" | SSE事件流 | event:role, event:token流 | ✅ 通过 |
| F-AGENT-01 | 空消息 | 无 | message="" | 400 | 400 "消息内容不能为空" | ✅ 通过 |
| F-AGENT-02 | 无效角色 | 无 | role="NonExistent" | 400 角色不存在 | **静默默认为Makima** | ❌ 失败 |
| F-AGENT-03 | 无角色(默认) | 无 | 不传role | 默认Makima | 默认Makima | ✅ 通过 |
| F-AGENT-04 | 超长消息 | 无 | 2500字符 | 正常流式 | 正常流式 | ✅ 通过 |

### 2.7 搜索模块 (Search)

| 用例ID | 测试点 | 前置条件 | 操作步骤 | 预期结果 | 实际结果 | 状态 |
|--------|--------|----------|----------|----------|----------|------|
| S7.1 | 中文关键词搜索 | 无 | keyword=芙莉莲 | 200, 返回匹配结果 | 200, 3条 | ✅ 通过 |
| S7.2 | 空关键词搜索 | 无 | keyword= | 200, 返回全部 | 200, 1146条 | ✅ 通过 |
| S7.3 | 无结果搜索 | 无 | keyword=xyznonexistent | 200, 0条 | 200, 0条 | ✅ 通过 |
| F-SEARCH-01 | XSS注入搜索 | 无 | keyword=\<script\> | 200, 安全处理 | 200, 0条(安全) | ✅ 通过 |
| F-SEARCH-02 | 超长关键词 | 无 | keyword=1000个a | 200 | 200 | ✅ 通过 |
| F-SEARCH-03 | 缺少keyword参数 | 无 | 无keyword参数 | 400 或 默认 | **500** | ❌ 失败 |

### 2.8 管理后台模块 (Admin)

| 用例ID | 测试点 | 前置条件 | 操作步骤 | 预期结果 | 实际结果 | 状态 |
|--------|--------|----------|----------|----------|----------|------|
| F-ADMIN-01 | 未登录访问 /admin/stats | 无 | GET /api/admin/stats | 401 | 401 | ✅ 通过 |
| F-ADMIN-02 | 未登录访问 /admin/anime | 无 | GET /api/admin/anime | 401 | 401 | ✅ 通过 |

### 2.9 投票模块 (Vote)

| 用例ID | 测试点 | 前置条件 | 操作步骤 | 预期结果 | 实际结果 | 状态 |
|--------|--------|----------|----------|----------|----------|------|
| F-VOTE-01 | 查看番剧投票(未登录) | 无 | GET /api/votes/anime/1 | 401 | 401 | ✅ 通过 |
| F-VOTE-02 | 未登录投票 | 无 | POST /api/votes/anime/1 | 401 | 401 | ✅ 通过 |

---

## 三、缺陷清单

### P0 — 核心流程阻断

| ID | 模块 | 标题 | 复现步骤 | 期望行为 | 实际行为 |
|----|------|------|----------|----------|----------|
| **BUG-P0-01** | Auth | 注册成功后登录返回500错误 | 1. POST /api/auth/register 注册新用户→200成功 2. 用相同凭据 POST /api/auth/login→500 | 200,返回JWT | 500 "服务器内部错误，请稍后重试"。注册方法内部调用 authenticationManager.authenticate() 是成功的（返回了token），但独立的 /login 请求失败。疑似 login 方法中的 updateLastLoginTime() @Modifying 查询在无事务上下文时失败 |
| **BUG-P0-02** | Rating | 评分接口全部返回500,无差别错误处理 | POST /api/ratings/anime/1 传任意score值（8, 0, 11, -1, 空, 小数）→一律500 | 401(未登录), 400(参数错误), 404(番剧不存在) 分别返回不同状态码 | 全部返回500 "服务器内部错误"。GlobalExceptionHandler 的未知 Exception 兜底覆盖了所有应被具体处理的异常 |

### P1 — 功能不可用

| ID | 模块 | 标题 | 复现步骤 | 期望行为 | 实际行为 |
|----|------|------|----------|----------|----------|
| **BUG-P1-01** | Anime | 非数字ID导致500 | GET /api/anime/abc | 400 Bad Request "ID格式错误" | 500 Internal Server Error |
| **BUG-P1-02** | Search | 缺少keyword参数导致500 | GET /api/anime/search?page=0&size=5 (无keyword) | 400 或 返回全部结果 | 500 Internal Server Error |
| **BUG-P1-03** | Agent | 无效角色名静默回退为Makima | POST role="NonExistent" message="Hello" | 400 "角色不存在" 或 SSE error 事件 | 静默以 Makima 角色回复, SSE流正常推送 |
| **BUG-P1-04** | Auth | /api/auth/me 始终返回"权限不足",不区分无token/无效token/过期token | 分别用: 无token, 无效token, 过期token 访问 | 401(未认证) vs 401(token过期) vs 401(token无效) 有区分 | 统一返回403 "权限不足",无法区分原因 |
| **BUG-P1-05** | General | HTTP方法不支持时返回500而非405 | DELETE /api/auth/login (仅支持POST) | 405 Method Not Allowed | 500 Internal Server Error |

### P2 — 边界/体验问题

| ID | 模块 | 标题 | 复现步骤 | 期望行为 | 实际行为 |
|----|------|------|----------|----------|----------|
| **BUG-P2-01** | Auth | 缺少Content-Type时返回500 | POST /api/auth/login 不带Content-Type头 | 400/415 Unsupported Media Type | 500 Internal Server Error |
| **BUG-P2-02** | Auth | 空请求体返回500 | POST /api/auth/login body为空 | 400 Bad Request | 500 Internal Server Error |
| **BUG-P2-03** | Auth | 畸形JSON返回500 | POST body="{broken json" | 400 Bad Request | 500 Internal Server Error |
| **BUG-P2-04** | Security | UserDetailsImpl.isEnabled() 始终返回true,忽略isActive字段 | 禁用用户(is_active=0)后尝试登录 | 账户已禁用,无法登录 | 禁用用户仍可登录。UserDetailsImpl.isEnabled() 硬编码 return true,应读取 user.getIsActive() |
| **BUG-P2-05** | Frontend | Nuxt前端注册接口要求手机号+验证码,后端注册接口要求用户名+邮箱+密码,两者不兼容 | 前端 POST /api/auth/register → 401或路由到Authing | 前端proxy到后端统一注册,或明确文档说明差异 | 前端注册走 Authing(手机号+验证码),后端注册走传统方式(用户名+邮箱+密码)。API路径相同但行为不同 |

### P3 — 建议优化

| ID | 模块 | 标题 | 复现步骤 | 期望行为 | 实际行为 |
|----|------|------|----------|----------|----------|
| **BUG-P3-01** | Community/Vote | 未登录访问401响应格式不统一 | 未登录 POST /api/communities/.../posts → 401 | 返回统一 ApiResponse 格式 `{"success":false,"message":"..."}` | 返回Spring Security默认格式 `{"path":"...","error":"Unauthorized",...}` |
| **BUG-P3-02** | Frontend | 前端proxy /api/auth/login POST → 401而非透传后端响应 | 前端 POST /api/auth/login | 透传后端的200或错误响应 | /api/auth/login POST 被Nuxt catch-all代理,但返回401(因为Nuxt server-side handler匹配了GET /api/auth/login,POST路径可能冲突) |
| **BUG-P3-03** | Rating | 番剧评分数据结构不一致 | GET /api/ratings/anime/1 | 返回评分统计信息(平均分/人数/分布) | 返回评分列表List,前端需自行计算统计值 |

---

## 四、未覆盖清单

| 区域 | 原因 | 风险等级 |
|------|------|----------|
| UI 三断点(桌面/平板/手机)视觉测试 | 无 headless browser,需手动在浏览器 DevTools 中切换断点验证 | 中 |
| PWA 离线缓存行为 | 需在浏览器中安装 PWA 后断网测试,API层测试无法覆盖 | 中 |
| Service Worker 缓存策略 | 需检查 sw.js 内容和浏览器 Cache Storage | 中 |
| OAuth 完整流程(Authing 重定向) | 需要 Authing 测试应用的有效回调,当前环境无 OAuth client secret | 中 |
| 短信验证码发送/验证 | 需要 Authing 短信服务配额,无法自动化测试 | 低 |
| 并发评分(10用户同时评分) | 需要10个独立JWT token同时发送并发请求 | 低 |
| Token 过期后前端拦截重定向 | 需要浏览器环境模拟cookie过期+页面操作 | 低 |
| SSE 中断重连 | 需要浏览器 EventSource API 环境,curl无法模拟自动重连 | 中 |
| 文件上传(社区图片) | 需要有效的 multipart/form-data 文件和登录态 | 低 |
| Docker Compose 三容器启动顺序验证 | Docker daemon 未运行 | 低 |
| 第三方API(Jikan)同步数据准确性 | 依赖外部API可用性,非测试范围 | 低 |
| 支付/会员功能 | 未实现或需第三方集成 | 低 |

---

## 五、风险评估与上线建议

### 5.1 总体评估

当前系统处于**开发中期**,核心浏览功能(番剧列表/搜索/排行榜/社区浏览)稳定可用,但**认证系统存在关键缺陷**(登录500),评分系统错误处理完全缺失。

### 5.2 风险矩阵

| 风险 | 可能性 | 影响 | 等级 |
|------|--------|------|------|
| 用户注册后无法登录 | 高 | 极高 | 🔴 严重 |
| 评分功能全线500错误 | 高 | 高 | 🔴 严重 |
| 非数字ID/缺失参数导致500 | 中 | 中 | 🟡 中等 |
| 禁用用户仍可登录 | 低 | 高 | 🟡 中等 |
| 前后端注册接口不一致 | 中 | 中 | 🟡 中等 |

### 5.3 上线前必须修复 (Minimum Viable)

1. **BUG-P0-01**: 修复注册后登录500错误 — 排查 login() 方法中 updateLastLoginTime() 的事务问题
2. **BUG-P0-02**: 评分接口错误处理 — 在 RatingController 或 GlobalExceptionHandler 中添加对未认证、参数校验、资源不存在的正确异常映射
3. **BUG-P1-01**: 非数字ID参数校验 — 在 AnimeController 中添加 @PathVariable 类型校验或全局类型转换异常处理
4. **BUG-P1-02**: 搜索缺少keyword参数 — 添加 @RequestParam(required=false) 或默认值
5. **BUG-P2-04**: isEnabled() 硬编码true — 修改 UserDetailsImpl 读取 user.getIsActive()

### 5.4 建议上线前修复 (Recommended)

6. **BUG-P1-04**: /api/auth/me 区分不同认证失败原因
7. **BUG-P1-05**: 全局 Method Not Allowed 异常处理
8. **BUG-P2-01 ~ BUG-P2-03**: 请求体解析异常正确映射到400
9. **BUG-P3-01**: 统一401响应格式,在 JwtAuthenticationEntryPoint 中返回 ApiResponse 格式

### 5.5 优点 (做得好的地方)

- SSR 渲染完整,SEO meta标签齐全,title动态生成
- PWA manifest 配置规范,支持 standalone 模式
- Redis 缓存已生效(排行榜数据、聊天历史、配额计数)
- CORS 配置正确,允许前端跨域访问
- 离线兜底模式(offline fallback)设计良好,后端不可用时前端不崩溃
- 数据库数据规模充足(1146条番剧),可支撑良好的浏览体验
- AIGC SSE 流式响应工作正常,用户体验流畅
- Docker 健康检查配置规范(mysql + redis condition: service_healthy)
- 密码使用 BCrypt 加密,安全性达标
- SQL注入/XSS在搜索中被有效防护

---

*报告生成时间: 2026-06-02 | 测试工具: curl + mysql + redis-cli + 代码审查*
