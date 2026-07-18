# P1 测试设计

> 日期：2026-06-01
> 范围：Agent 额度控制/SSE流式/意图覆盖度 + 前端 Composable/Store

## 1. 后端：AgentServiceQuotaAndSseTest

沿用现有模式：JUnit 5 + Mockito + 反射调用私有方法，不启动 Spring 容器。

### 1.1 额度控制（~12 用例）

- consumeQuota 首次调用写入 Redis key，后续递增
- 首次写入时设置 TTL（到次日零点）
- 免费用户每日限额 10 次，付费用户 100 次
- anonymous 用户按免费额度处理
- Redis increment 异常时临时放行（不阻塞用户）
- peekQuota 仅查询不消耗计数
- resolveQuotaLimit 根据 isPaidUser 返回不同限额
- isPaidUser：白名单 + 萤火月卡会员均视为付费
- resolveMembership：过期会员返回 inactive

### 1.2 SSE 流式（~10 用例）

- API key 为空时降级返回占位文本
- 配额耗尽时发送 quota 事件（含 limit/remaining/resetAt）
- 常见意图缓存命中时发送 cache 事件
- 情绪标签从 SSE token 流首段正确解析
- AI 未返回任何 token 时降级为阻塞式调用
- SseEmitter 超时设为 120 秒
- HTTP 非 200 时发送错误提示并 complete
- 流式异常时发送错误事件并 complete

### 1.3 意图覆盖度（~10 用例）

- "喜欢你" 归 love 不归 praise（重叠词优先级）
- "你好厉害" 归 praise 不归 hello
- COMMON_MESSAGE_CLEANUP 正则清洗标点/空格
- 标点干扰下的匹配："谢，谢。" → thanks
- 大小写混合："THANK YOU" → thanks
- 空消息/纯标点消息返回空字符串
- 冷门变体覆盖：what_doing 的 "忙什么呢"、goodnight 的 "我要睡了" 等

## 2. 前端 Composable 测试

新增依赖：vitest、@nuxt/test-utils、@pinia/testing、happy-dom

### 2.1 useAnime.test.ts（~15 用例）

- fetchTopRated 正确拼接 /anime/top-rated 路径和分页参数
- fetchAnimeList 传递 sortBy/direction 参数
- fetchAnimeById 拼接 /anime/{id} 路径
- searchAnime 传递 keyword 参数
- API 返回空/异常时降级返回空数组而非抛错
- submitRating 未登录时抛出认证错误
- submitRating 成功时返回 RatingDto
- voteFirefly 传递 animeId 并返回最新票数

### 2.2 useCommunity.test.ts（~12 用例）

- fetchCommunities 返回社区数组
- fetchCommunity 按 slug 获取详情
- fetchPosts 传递 sort/page/size 参数
- createPost 未登录时抛错
- uploadImages 构建 FormData 并传递 files
- likePost/likeReply 未登录时抛错
- API 异常时降级返回空数据

## 3. 前端 Store 测试

### 3.1 user.test.ts（~15 用例）

- restoreSession：cookie 无 token 时跳过恢复
- restoreSession：cookie 有 token + localStorage 有数据时恢复用户
- loginWithAuthing：正确提取 phone/name/avatar
- loginWithAuthing：保存 backendToken
- logout：清空 user、token、backendToken、localStorage
- ensureBackendToken：token 有效时直接返回 true
- ensureBackendToken：token 过期时重新请求
- ensureBackendToken：请求失败时返回 false
- completeProfile：本地保存 + 远端同步
- resolveStableName：手机号不作为显示名
- resolveStableName：默认名 "番舍同好" 回退为空

## 运行命令

```bash
# 后端
cd aniglow-backend
JAVA_HOME=/path/to/jdk-21 mvn test -Dtest="AgentServiceQuotaAndSseTest"

# 前端
npx vitest run
```
