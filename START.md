# 🚀 AniGlow 启动指南（START.md）

> **TL;DR — 最快启动**：开机后等 Docker Desktop 自动就绪，容器自动拉起，直接打开 **http://localhost:3000**。
> 一切已运行时启动耗时 **0 秒**；容器全停时 `./start.sh` 一键启动，实测 **25 秒**完整就绪。

---

## ⚡ 日常启动（最常用的三种方式）

### 方式一：什么都不做（0 秒）

MySQL / Redis / 后端 / 前端四个容器均配置了 `restart: unless-stopped`——**开机后 Docker Desktop 自动就绪时，它们已经全部自动启动**。

直接打开：**http://localhost:3000**

> 若某容器被手动停止过：`docker compose start` 一键全部拉起（秒级）。

### 方式二：一键脚本（25 秒完整就绪）

```bash
./start.sh            # 启动并等待后端就绪（自动探测，就绪后提示访问地址）
./start.sh quick      # 只启动不等待，立即返回
```

脚本自动完成：加载 `.env.local` 密钥 → `docker compose up -d` → 轮询后端健康检查 → 输出访问地址。

### 方式三：冷启动（Docker Desktop 未运行，约 1-2 分钟）

开机后 Docker Desktop 会**自动启动**（无需手动）。若被手动退出过：

```bash
cmd //c start "" "C:\Users\mjj\AppData\Local\Programs\DockerDesktop\Docker Desktop.exe"
# 等待托盘鲸鱼图标变绿（约 30-60 秒），容器自动拉起
```

或直接：

```bash
./start.sh
```

---

## ⏱️ 实测启动耗时（2026-10-17 测量）

| 场景 | 耗时 | 说明 |
|---|---|---|
| 服务已在运行 | **0 秒** | 直接打开 localhost:3000 |
| 容器全停 → `docker compose start` | **~25 秒** | 后端 Spring Boot 启动 ~20s + 前端已并行就绪 |
| Docker 引擎关闭 → Docker Desktop 自启 | ~40-70 秒 | 引擎 ~5-30s + 容器自启 + 后端 ~20s |
| 全新机器首次部署 | 10-20 分钟 | 含镜像构建 + 数据同步，见下方「首次部署」 |

> 启动速度优化记录：后端健康检查间隔 30s→10s；前端不再等待后端 healthy（并行启动）；数据卷改用具名卷。

---

## 🥇 首次部署（全新机器）

```bash
git clone https://github.com/mjj90200-glitch/AniGlow.git
cd AniGlow
pnpm install

# 1. 配置环境变量
cp .env.example .env
# 编辑 .env：填入数据库密码、DeepSeek API Key、JWT_SECRET、AUTH_BRIDGE_SECRET

# 2. 生成机器特定密钥（数据库密码等，写入 .env.local，已被 gitignore）
#    参照 .env.example 中的注释逐项填写

# 3. 准备 MySQL（确保 3306 端口有 MySQL 8 且已创建 aniglow 库）
#    Flyway 会在后端首次启动时自动建表

# 4. 一键启动
./start.sh
```

首次启动约 3 分钟后自动从 AniList 同步 5000+ 部番剧；中文标题/简介由翻译管道补齐（管理端点触发，见下文）。

---

## 🔧 常用运维命令

```bash
docker compose ps                 # 查看容器状态
docker compose logs -f backend    # 跟踪后端日志
docker compose restart backend    # 重启后端
docker compose stop               # 停止全部（数据保留）
docker compose start              # 重新启动

# 翻译补齐（需 ADMIN 账号登录后的会话 Cookie）
curl -X POST http://localhost:3000/api/admin/repair/titles   -b cookie.txt
curl -X POST http://localhost:3000/api/admin/repair/synopses -b cookie.txt

# 手动全量同步番剧
curl -X POST http://localhost:3000/api/admin/sync/full -b cookie.txt
```

---

## 🛠️ 故障排查

| 症状 | 原因与处理 |
|---|---|
| 页面打不开（连接拒绝） | Docker Desktop 未运行 → 启动它，容器会自动拉起；或 `docker compose up -d` |
| 登录后管理操作提示「请先登录」 | 会话过期 → 重新登录；或账号非 ADMIN → 数据库提权 |
| AI 对话显示「暂时无法回应」 | DeepSeek API 故障/欠费 → 检查 api.deepseek.com 可达性与余额；恢复后自动痊愈 |
| 后端日志报 `UnknownHostException: mysql` | 容器网络分裂 → `docker compose down && docker compose up -d`（数据在具名卷，安全） |
| 图片 404 | 帖子照片存于宿主机 `C:\Users\mjj\aniglow-data`（已挂载进容器）；若切 OSS 模式则由 OSS 提供 |
| 端口冲突 3307/3308/6380 | 检查其他项目的容器占用；本项目的端口覆盖见 `docker-compose.override.yml` |

---

## 🔐 关键文件

| 文件 | 说明 | 是否提交 |
|---|---|---|
| `.env` | 通用配置（数据库/站点地址） | ❌ gitignored |
| `.env.local` | 机器特定密钥（AI Key/OSS/数据库密码） | ❌ gitignored |
| `.env.example` | 环境变量模板 | ✅ 提交 |
| `docker-compose.yml` | 基础编排 | ✅ 提交 |
| `docker-compose.override.yml` | 本机覆盖（端口/IP 钉定） | ❌ gitignored |
| `start.sh` | 一键启动脚本 | ✅ 提交 |
