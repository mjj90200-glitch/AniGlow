-- 萤火番舍 AniGlow · MySQL 初始化脚本
CREATE DATABASE IF NOT EXISTS aniglow
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

-- 创建应用用户（如果不存在）
CREATE USER IF NOT EXISTS 'aniglow'@'%' IDENTIFIED BY 'removed-default-password';
GRANT ALL PRIVILEGES ON aniglow.* TO 'aniglow'@'%';
FLUSH PRIVILEGES;
