# AGENTS.md — AI 智能体操作约束（所有智能体必读）

> **本仓库由主开发会话（Claude）主导，可能有多个 AI 会话并行工作。**
> 过去违反以下约束曾导致：全站宕机、容器误删、数据卷丢失、翻译进度清零。
> 本文件由主开发会话维护（2026-10-17）。任何修改需经用户批准。

---

## 🖥️ 项目运行方式（不要破坏）

网站通过 **Docker Compose 栈**运行，四容器全部 healthy：

| 容器 | 端口（宿主机→容器） | 数据 |
|---|---|---|
| `aniglow-frontend` | 3000→3000 | — |
| `aniglow-backend` | 8081→8081 | `/root/aniglow-data`（帖子照片） |
| `aniglow-mysql` | 3308→3306 | 具名卷 `aniglow_aniglow-mysql-data`（**全站唯一有效数据**） |
| `aniglow-redis` | 6380→6379 | 会话/缓存 |

> 完整技术细节见 [PROJECT.md](PROJECT.md)。

---

## 🚫 禁止操作（未经用户明确授权，一律不得执行）

### Docker / 基础设施
1. **容器生命周期**：不得 `docker stop` / `kill` / `rm` / `restart` 任何 `aniglow-*` 容器，不得 `docker compose down`
2. **数据库**：不得 `DROP` / `TRUNCATE` 数据库或表，不得删除卷 `aniglow_aniglow-mysql-data`
3. **旧数据备份**：不得删除/格式化 `aniglow-mysql-legacy-20261012`、`aniglow-redis-legacy-20261012` 及其数据卷（含继承前的完整数据）
4. **端口绑定**：3000 / 3001 / 3308 / 6380 / 8081 已被本项目占用；**3307 被其他项目占用**——新容器/服务不得绑定这些端口
5. **第二个 Docker Desktop**：不得再启动第二个 Docker Desktop 实例（双实例会导致引擎管道冲突、全栈间歇崩溃——2026-10 已发生）
6. **图片目录**：`/root/aniglow-data`（容器）与 `C:\Users\mjj\aniglow-data`（宿主机）是帖子照片的唯一存储，不得清空

### 数据库数据
7. **模型名**：`agent_characters.model` 不得写死为 `doubao-*` 系列（豆包模型名发给 DeepSeek API 会 400）——使用 `deepseek-chat` / `deepseek-flash`
8. **翻译数据**：`title_cn` / `synopsis_cn` 是翻译管道成果，不得清空重翻

### git 仓库
9. **不得 `git add -A` / `git add .`**——并行会话可能有未完成的文件，会互相扫入（2026-10 已发生：无关文件被误提交推送）。**逐个 `git add <文件>`**，提交前 `git status` 确认只包含自己的改动
10. **不得 force push**、不得改写已推送的历史
11. **密钥文件**：`.env` / `.env.local` / `e2e_cookies.txt` / 任何含 token/password/key 的文件不得提交（2026-10 已发生会话 Cookie 泄露推送，令牌已吊销）

---

## ✅ 允许的操作

- **代码开发**：自己功能范围内的源码文件（逐个 add，提交前确认）
- **读取状态**：`docker ps` / `logs` / `inspect`、数据库 SELECT
- **构建镜像**：`docker compose build` 可以，但不要因此重建/重启运行中的容器
- **应用内操作**：通过 API/网站执行的业务操作
- **测试账号**：使用 `e2e_final / E2eFinal2026`（已提权 ADMIN）进行验证

---

## 🤝 协调规则

| 职责 | 负责方 |
|---|---|
| 容器生命周期 / 数据库操作 / 基础设施变更 / 端口分配 | **主开发会话（Claude）** |
| 应用代码 / 功能开发 / 自身测试 | 各智能体自己 |

- 需要基础设施变更（端口/容器/数据卷/密钥）时：**写入任务说明，等待用户与主导会话执行**，不要自行操作
- 需要重启后端容器时：先告知用户，由主导会话执行
- 提交前发现工作区有他人的改动：**不要提交，不要还原**，跳过并说明

---

## 📖 项目现状与技术细节

见 [PROJECT.md](PROJECT.md)：容器端口表、数据规模、数据备份位置、AI 模型选择陷阱、时钟漂移等已验证事实。
