# PROJECT.md — AniGlow · 萤火番舍（智能体技术简报）

> **本文件面向 AI 智能体/自动化工具**：记录项目现状、已实现功能、Docker 容器与端口、关键路径和已知陷阱。由主开发会话于 2026-10-17 维护，所有信息均经过实际验证。
>
> **给智能体的三条铁律**：
> 1. 本机可能与**其他 AI 会话（Codex）并行开发**——执行 `git add -A` 前必须先 `git status` 检查是否扫入他人改动
> 2. 后端容器控制台输出为 **GBK 编码**——用 python 以 `gbk` 解码，且 **bash 命令里不要写中文**（会被 GBK 损坏，用 UTF-8 文件或 MySQL HEX 传递）
> 3. git 推送/拉取依赖本地代理 `127.0.0.1:7897`（Clash）——代理未运行时会连不上 GitHub

---

## 一、项目概述

**AniGlow · 萤火番舍**：面向中文动漫用户的社区平台。

| 模块 | 技术 | 状态 |
|---|---|---|
| 前端 | Nuxt 3.21 (SSR) · Vue 3 · Pinia · Tailwind | ✅ 运行中 :3000 |
| 后端 | Java 21 · Spring Boot 3.5 · Spring Security(JWT) · **Spring AI 1.1.8** | ✅ 运行中 :8081（context `/api`） |
| 数据 | MySQL 8 (Flyway V1-V6) · Redis 7 | ✅ 运行中 |
| AI | DeepSeek（Spring AI 统一接口） | ✅ deepseek-chat |
| 图片 | ImageStore 抽象：本地磁盘 / 阿里云 OSS | ✅ OSS 已接（bucket `aniglow-images`，北京，公共读） |
| 数据源 | AniList 开放 API（每 6 小时同步，热度榜 100 页 × 50 条） | ✅ 已同步 4999 部 |

## 二、已实现功能（当前真实状态）

- **番剧图鉴**：AniList 同步（4999+ 部），四维组合筛选（题材 21 类 / 地区 / 年份区间 / 格式），贝叶斯评分排行
- **社区**：板块 + 帖子 + 楼中楼回复 + 点赞，帖子封面图
- **AI 角色对话**：流式 SSE、情感标签（[happy]/[normal]/[complex]）、短期记忆（30 轮）、每角色独立人设与提示词
- **翻译管道**：中文标题（15 部/次批量，官方译名优先）+ 中文简介（逐条），覆盖 99%+
- **用户系统**：本地注册/登录，JWT 双令牌（访问 15 分钟 + 刷新 7 天，刷新令牌 httpOnly Cookie），ADMIN 角色
- **图片存储**：`ImageStore` 接口抽象，`STORAGE_TYPE=local|oss` 切换；OSS 模式新上传直接进阿里云
- **运维**：健康检查 / Prometheus 指标 / 管理端点（同步、翻译、迁移、重建排行榜）

## 三、Docker 容器与端口（当前拓扑）

| 容器 | 镜像/来源 | 宿主机端口 → 容器端口 | 数据卷 | 状态 |
|---|---|---|---|---|
| `aniglow-frontend` | docker-compose.yml 构建 | **3000 → 3000** | — | healthy |
| `aniglow-backend` | docker-compose.yml 构建 | **8081 → 8081** | `C:/Users/mjj/aniglow-data` → `/root/aniglow-data` | healthy |
| `aniglow-mysql` | mysql:8.0 | **3308 → 3306**（override 强制替换） | 具名卷 `aniglow_aniglow-mysql-data` | healthy |
| `aniglow-redis` | redis:7-alpine | **6380 → 6379** | — | healthy |
| `aniglow-mysql-legacy-20261012` | mysql:8.0（已停止） | 3307 → 3306 | 匿名卷（**完整旧数据备份**：5160 番剧/25 用户/99% 翻译） | Exited |

### 端口占用总表（避免冲突）

| 端口 | 占用者 |
|---|---|
| 3000 | Compose 前端 |
| 3001 | （可选）开发模式前端 `pnpm dev --port 3001` |
| 3306 | （未占用；容器间走 Compose 内网 `mysql:3306`） |
| 3307 | **其他项目的 MySQL 容器（勿冲突）** |
| 3308 | 本项目 MySQL（宿主机映射） |
| 6379 | （空闲；容器间走 Compose 内网 `redis:6379`） |
| 6380 | Compose Redis |
| 7897 | Clash 代理（git push/pull 依赖它在线） |
| 8081 | Compose 后端 |

## 四、关键配置与凭据位置

| 内容 | 位置 | 备注 |
|---|---|---|
| 机器密钥（AI Key/OSS/DB 密码） | `.env.local` | gitignored，**勿提交** |
| 通用配置 | `.env` | gitignored |
| Compose 本机覆盖 | `docker-compose.override.yml` | gitignored；含 DeepSeek IP 钉定 + AI_MODEL + MySQL 端口/卷 |
| DeepSeek IP 钉定（系统级） | `C:\Windows\System32\drivers\etc\hosts` 有 `111.42.114.74 api.deepseek.com` | 国内负载均衡有两节点，`111.42.112.91` 曾故障；换 IP 时更新此行 |
| AI 翻译模型 | `aniglow.translation.model`（默认 deepseek-chat） | 对话模型由 `AI_MODEL` 控制 |

## 五、已验证的关键事实（智能体必读）

1. **AI 对话**：模型用 `deepseek-chat`（非推理）。**不要用 `deepseek-flash`**——它是推理模型，流式 delta 带 `reasoning_content`，Spring AI 1.1.8 流式解析会失败导致对话降级
2. **角色模型优先级**：`agent_characters.model`（非 NULL 时）> `AI_MODEL`。改模型时**两处都要核对**
3. **数据源**：AniList（`graphql.anilist.co`）。Jikan 代码已退役（保留未调度）。Bangumi API 国内被墙不可用
4. **DeepSeek 国内负载均衡**：两个节点 `111.42.114.74`（好）/ `111.42.112.91`（曾故障）。hosts + 容器 extra_hosts 已钉好节点。若 AI 调用超时，先测两节点连通性
5. **GBK 陷阱**：`docker exec` 输出、bash 中文参数都会 GBK 损坏。SQL 中文条件用 `CONCAT('%','中文','%')` 传参或 HEX；日志用 python `gbk` 解码
6. **git 代理**：`127.0.0.1:7897`（Clash）。代理不在线则 push/pull 失败
7. **时钟**：主机休眠后时钟可能落后，容器（WSL2）独立计时——跨容器时间对比不可靠

## 六、常用命令

```bash
# 一键启动（含就绪等待）
./start.sh

# 数据库操作（密码在 .env.local）
docker exec aniglow-mysql sh -c 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" --default-character-set=utf8mb4 aniglow -e "SQL语句"'

# 后端日志（GBK，python 解码）
docker logs --tail 100 aniglow-backend

# 翻译补齐（需 ADMIN 会话 Cookie）
curl -X POST http://localhost:3000/api/admin/repair/titles  -b cookie.txt
curl -X POST http://localhost:3000/api/admin/repair/synopses -b cookie.txt

# 手动全量同步番剧
curl -X POST http://localhost:3000/api/admin/sync/full -b cookie.txt
```

## 七、数据备份

| 备份 | 位置 |
|---|---|
| 全量 SQL dump（2026-10-17，含全部继承数据） | `C:\Users\mjj\aniglow_legacy_dump.sql`（8.8MB） |
| 旧容器冷备（含原始数据卷） | `aniglow-mysql-legacy-20261012`（已停止，数据完好） |
| 帖子照片文件 | 宿主机 `C:\Users\mjj\aniglow-data\images\`（已挂载进后端容器） |

## 八、当前数据规模（2026-10-17）

- 番剧 5160 部（Mac 迁移 + AniList 合并）、用户 26、中文标题 5159、中文简介 5156
- 帖子照片 17 张（宿主机 + OSS 双份：`aniglow-images.oss-cn-beijing.aliyuncs.com/images/2026/...`）
- 剩余 1 条标题 + 4 条简介为模型无法翻译的特殊条目（占比可忽略）
