-- 使用 MySQL 8.0.16+（推荐 8.4）的 root 管理连接执行。
-- 先把下面的示例密码改为你自己的数据库密码，再运行。
CREATE DATABASE IF NOT EXISTS xunwu CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'xunwu'@'localhost' IDENTIFIED BY 'ChangeThisDbPassword_2026';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, INDEX, REFERENCES ON xunwu.* TO 'xunwu'@'localhost';
-- 若该账号已存在，本脚本不会重置密码。请在 .env 填写实际密码。
-- 建表由应用首次连接时执行，也可选中 xunwu 后手动执行 schema.sql。
