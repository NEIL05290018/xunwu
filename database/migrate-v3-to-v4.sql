SET NAMES utf8mb4;
-- 仅适用于本仓库 v3.0 的 MySQL 十二表，不能用于上传的 SQLite 十三表。
-- 先停止旧应用、备份数据库，再选中正确数据库执行；只执行一次。
-- MySQL DDL 会自动提交。若某条报错，停止并核对已执行的步骤，不要整份盲目重跑。
-- 本脚本不删除业务记录，不重置账号密码。历史会话已丢失的发起者信息不能推断恢复。
ALTER TABLE users MODIFY password_hash VARCHAR(255) NOT NULL,
 ADD CONSTRAINT ck_users_status CHECK (account_status IN ('正常','停用'));
ALTER TABLE admins MODIFY password_hash VARCHAR(255) NOT NULL;
ALTER TABLE addresses MODIFY recipient_name VARCHAR(255) NOT NULL, MODIFY recipient_phone VARCHAR(255) NOT NULL;
ALTER TABLE products DROP CHECK ck_products_5,
 ADD CONSTRAINT ck_products_5 CHECK (audit_status IN ('未提交','待审核','通过','驳回'));
ALTER TABLE orders
 ADD CONSTRAINT ck_orders_pickup CHECK (delivery_method <> '自提' OR shipping_fee_cents=0),
 ADD CONSTRAINT ck_orders_address CHECK (delivery_method <> '快递' OR (recipient_name IS NOT NULL AND recipient_phone IS NOT NULL AND recipient_address IS NOT NULL));
ALTER TABLE refunds ADD CONSTRAINT ck_refunds_status CHECK (refund_status IN ('待处理','已同意','已拒绝','退款成功'));
ALTER TABLE reports ADD CONSTRAINT ck_reports_status CHECK (report_status IN ('待处理','成立','不成立'));
-- 先保留外键所需的普通索引，再替换原有唯一索引。
ALTER TABLE conversations ADD INDEX ix_conversation_initiator (initiator_id);
ALTER TABLE conversations DROP CHECK ck_conversations_0, DROP INDEX uq_conversation_pair,
 ADD COLUMN pair_low INT GENERATED ALWAYS AS (LEAST(initiator_id,peer_id)) STORED,
 ADD COLUMN pair_high INT GENERATED ALWAYS AS (GREATEST(initiator_id,peer_id)) STORED,
 ADD UNIQUE KEY uq_conversation_pair (pair_low,pair_high),
 ADD CONSTRAINT ck_conversations_0 CHECK (initiator_id <> peer_id);
