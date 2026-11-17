-- 请先在工具中选中要验证的数据库；本文件不会切换数据库。
SET NAMES utf8mb4;
-- 1. 应为十二张表。
SELECT COUNT(*) AS table_count FROM information_schema.tables WHERE table_schema=DATABASE();
-- 2. 一件商品不能存在多笔有效订单；正常结果为空。
SELECT product_id,COUNT(*) FROM orders WHERE order_status NOT IN ('已取消','已退款') GROUP BY product_id HAVING COUNT(*)>1;
-- 3. 消息发送者必须属于会话；正常结果为空。
SELECT m.message_id FROM messages m JOIN conversations c USING(conversation_id) WHERE m.sender_id NOT IN (c.initiator_id,c.peer_id);
-- 4. 评价人必须参与对应交易；正常结果为空。
SELECT r.review_id FROM reviews r JOIN orders o USING(order_id) JOIN products p USING(product_id) WHERE r.reviewer_id NOT IN(o.buyer_id,p.seller_id);
-- 5. 密码不能为空；只显示计数，不显示哈希。
SELECT 'users' AS entity,COUNT(*) AS empty_passwords FROM users WHERE password_hash='' UNION ALL SELECT 'admins',COUNT(*) FROM admins WHERE password_hash='';
-- 6. 按分类统计公开在售商品。
SELECT category,COUNT(*) AS total,ROUND(AVG(price_cents)/100,2) AS avg_yuan FROM products WHERE audit_status='通过' AND product_status='在售' GROUP BY category;
-- 7. 订单联表查询，体现快照与当前商品标题的区别。
SELECT o.order_id,b.nickname AS buyer,s.nickname AS seller,o.snapshot_title,p.title AS current_title,o.order_status,(o.deal_price_cents+o.shipping_fee_cents)/100 AS total_yuan FROM orders o JOIN products p USING(product_id) JOIN users b ON b.user_id=o.buyer_id JOIN users s ON s.user_id=p.seller_id ORDER BY o.order_id DESC;

-- 8. 旧版字段/表不应存在，正常结果为0。
SELECT COUNT(*) AS obsolete_columns FROM information_schema.columns WHERE table_schema=DATABASE() AND ((table_name='products' AND column_name='condition_label') OR (table_name='reviews' AND column_name='rating') OR (table_name='admins' AND column_name='real_name'));
SELECT COUNT(*) AS obsolete_tables FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='conversation_products';
-- 9. 会话无序组合不应重复；正常结果为空。
SELECT pair_low,pair_high,COUNT(*) FROM conversations GROUP BY pair_low,pair_high HAVING COUNT(*)>1;
