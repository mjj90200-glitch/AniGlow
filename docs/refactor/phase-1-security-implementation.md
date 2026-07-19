# Phase 1 security implementation record

## Secret management

- MySQL、JWT、AIGC、Authing 与认证桥接凭据只从运行环境读取，生产编排缺少必要变量时立即失败。
- 删除带默认账户口令的初始化 SQL，数据库结构统一由 Flyway 管理。
- Authing 应用密钥已确认不再使用曾暴露的旧值；运行环境配置文件权限限制为仅当前用户可读写。
- GitHub Actions 使用 Gitleaks 扫描完整 Git 历史；确定性的测试 JWT 仅按精确文件和精确值放行。
- 发布前保留受限权限的 Git bundle 备份，再从仓库历史中清除旧弱口令并强制更新远端主分支。

## Authentication boundary

- `/agent/**` 要求 JWT，用户身份只取自 Spring Security 上下文，不接受请求体中的 `userId`。
- Authing 到后端的账户桥接使用至少 32 字符的共享密钥，并通过常量时间比较验证。
- Nuxt 的后端 Token 交换只接受服务端验证过的 Authing Cookie 或 Bearer Token，不再接受客户端提交的任意用户资料。
- 未携带认证桥接密钥调用后端登录桥接接口返回 `401`。

## Abuse protection

- Spring Boot 对登录、注册、AIGC、社区发帖和图片上传实施服务端限流。
- Nuxt 对短信发送、密码登录、短信登录、注册、Authing 回调和 Token 交换实施独立限流。
- 前后端只在直接连接来源属于配置的可信代理网段时解析 `X-Forwarded-For`，避免伪造请求头绕过 IP 限流。
- 短信验证码同时按 IP 与手机号限流，达到阈值返回 `429` 和 `Retry-After`。

## Verification

- Frontend: 5 test files, 24 tests passed; typecheck and production build passed.
- Backend: 126 tests passed through `mvn verify`.
- Security: missing bridge secret returns `401`; unauthenticated Agent chat returns `401`; forged forwarding headers do not change the rate-limit identity.
- Deployment: Docker Compose rejects missing required secrets and validates successfully with a complete environment.
- Release: local reverse proxy, backend health endpoint, static module MIME types, and public domain are smoke-tested after deployment.
