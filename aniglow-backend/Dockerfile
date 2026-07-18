# ═══════════════════════════════════════════════════════════════
# 萤火番舍 AniGlow · 后端 Dockerfile（Spring Boot 3.2 + Java 21）
# ═══════════════════════════════════════════════════════════════

# ─── Stage 1: Maven 构建阶段 ─────────────────────────────────
FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /build

# 先复制 pom.xml 利用 Docker 缓存加速依赖下载
COPY pom.xml ./
RUN mvn dependency:go-offline -B -q

# 复制源码
COPY src ./src

# 镜像构建同样执行测试，避免绕过 CI 门禁
RUN mvn clean verify -B -q

# ─── Stage 2: 运行阶段 ───────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runner

WORKDIR /app

# 创建非 root 用户
RUN addgroup -S aniglow && adduser -S aniglow -G aniglow

# 复制 JAR
COPY --from=builder /build/target/*.jar app.jar

# 生产环境配置
ENV SPRING_PROFILES_ACTIVE=docker
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

USER aniglow

EXPOSE 8081

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD pgrep -f "app.jar" || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
