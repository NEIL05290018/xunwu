SET NAMES utf8mb4;
-- MySQL 8.0.16+ / 8.4. Exactly 12 business tables; UTF-8; money in integer cents.
CREATE TABLE IF NOT EXISTS `admins` (
  `admin_id` INT NOT NULL AUTO_INCREMENT,
  `login_name` VARCHAR(255) NOT NULL,
  `password_hash` VARCHAR(255) NOT NULL ,
  PRIMARY KEY (`admin_id`),
  UNIQUE KEY uq_admin_login (`login_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `users` (
  `user_id` INT NOT NULL AUTO_INCREMENT,
  `nickname` VARCHAR(255) NOT NULL,
  `phone` VARCHAR(255) NOT NULL,
  `account_status` VARCHAR(255) NOT NULL,
  `password_hash` VARCHAR(255) NOT NULL ,
  PRIMARY KEY (`user_id`),
  UNIQUE KEY uq_user_phone (`phone`),
  CONSTRAINT ck_users_status CHECK (account_status IN ('正常','停用'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `addresses` (
  `address_id` INT NOT NULL AUTO_INCREMENT,
  `user_id` INT NOT NULL,
  `recipient_name` VARCHAR(255) NOT NULL,
  `recipient_phone` VARCHAR(255) NOT NULL,
  `detail_address` VARCHAR(255) NOT NULL,
  PRIMARY KEY (`address_id`),
  FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `products` (
  `product_id` INT NOT NULL AUTO_INCREMENT,
  `seller_id` INT NOT NULL,
  `title` VARCHAR(255) NOT NULL,
  `description` TEXT NOT NULL,
  `price_cents` INT NOT NULL,
  `category` VARCHAR(255) NOT NULL,
  `support_express` INT NOT NULL,
  `support_pickup` INT NOT NULL,
  `product_status` VARCHAR(255) NOT NULL,
  `audit_status` VARCHAR(255) NOT NULL,
  `audit_admin_id` INT NULL,
  `audit_note` TEXT NULL,
  PRIMARY KEY (`product_id`),
  FOREIGN KEY (`seller_id`) REFERENCES `users` (`user_id`),
  FOREIGN KEY (`audit_admin_id`) REFERENCES `admins` (`admin_id`),
  CONSTRAINT ck_products_0 CHECK (price_cents > 0),
  CONSTRAINT ck_products_1 CHECK (support_express IN (0,1)),
  CONSTRAINT ck_products_2 CHECK (support_pickup IN (0,1)),
  CONSTRAINT ck_products_3 CHECK (support_express + support_pickup > 0),
  CONSTRAINT ck_products_4 CHECK (product_status IN ('草稿','在售','锁定','已售','下架')),
  CONSTRAINT ck_products_5 CHECK (audit_status IN ('未提交','待审核','通过','驳回'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `product_images` (
  `image_id` INT NOT NULL AUTO_INCREMENT,
  `product_id` INT NOT NULL,
  `image_url` MEDIUMTEXT NOT NULL,
  PRIMARY KEY (`image_id`),
  FOREIGN KEY (`product_id`) REFERENCES `products` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `orders` (
  `order_id` INT NOT NULL AUTO_INCREMENT,
  `product_id` INT NOT NULL,
  `buyer_id` INT NOT NULL,
  `snapshot_title` VARCHAR(255) NOT NULL,
  `deal_price_cents` INT NOT NULL,
  `shipping_fee_cents` INT NOT NULL,
  `delivery_method` VARCHAR(255) NOT NULL,
  `recipient_name` VARCHAR(255) NULL,
  `recipient_phone` VARCHAR(255) NULL,
  `recipient_address` VARCHAR(255) NULL,
  `order_status` VARCHAR(255) NOT NULL,
  `carrier` VARCHAR(255) NULL,
  `tracking_no` VARCHAR(255) NULL,
  `created_at` VARCHAR(255) NOT NULL,
  PRIMARY KEY (`order_id`),
  `active_product_id` INT GENERATED ALWAYS AS (CASE WHEN order_status IN ('待付款','待交付','待收货','已完成') THEN product_id ELSE NULL END) STORED,
  UNIQUE KEY uq_active_product (`active_product_id`),
  FOREIGN KEY (`product_id`) REFERENCES `products` (`product_id`),
  FOREIGN KEY (`buyer_id`) REFERENCES `users` (`user_id`),
  CONSTRAINT ck_orders_0 CHECK (deal_price_cents > 0),
  CONSTRAINT ck_orders_1 CHECK (shipping_fee_cents >= 0),
  CONSTRAINT ck_orders_2 CHECK (delivery_method IN ('快递','自提')),
  CONSTRAINT ck_orders_3 CHECK (order_status IN ('待付款','待交付','待收货','已完成','已取消','已退款')),
  CONSTRAINT ck_orders_pickup CHECK (delivery_method <> '自提' OR shipping_fee_cents=0),
  CONSTRAINT ck_orders_address CHECK (delivery_method <> '快递' OR (recipient_name IS NOT NULL AND recipient_phone IS NOT NULL AND recipient_address IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `conversations` (
  `conversation_id` INT NOT NULL AUTO_INCREMENT,
  `initiator_id` INT NOT NULL,
  `peer_id` INT NOT NULL,
  `created_at` VARCHAR(255) NOT NULL,
  PRIMARY KEY (`conversation_id`),
  KEY ix_conversation_initiator (`initiator_id`),
  `pair_low` INT GENERATED ALWAYS AS (LEAST(initiator_id,peer_id)) STORED,
  `pair_high` INT GENERATED ALWAYS AS (GREATEST(initiator_id,peer_id)) STORED,
  UNIQUE KEY uq_conversation_pair (`pair_low`,`pair_high`),
  FOREIGN KEY (`initiator_id`) REFERENCES `users` (`user_id`),
  FOREIGN KEY (`peer_id`) REFERENCES `users` (`user_id`),
  CONSTRAINT ck_conversations_0 CHECK (initiator_id <> peer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `messages` (
  `message_id` INT NOT NULL AUTO_INCREMENT,
  `conversation_id` INT NOT NULL,
  `sender_id` INT NOT NULL,
  `content` TEXT NOT NULL,
  `sent_at` VARCHAR(255) NOT NULL,
  PRIMARY KEY (`message_id`),
  FOREIGN KEY (`conversation_id`) REFERENCES `conversations` (`conversation_id`),
  FOREIGN KEY (`sender_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `refunds` (
  `refund_id` INT NOT NULL AUTO_INCREMENT,
  `order_id` INT NOT NULL,
  `reason` TEXT NOT NULL,
  `refund_status` VARCHAR(255) NOT NULL,
  `admin_id` INT NULL,
  `admin_opinion` TEXT NULL,
  `created_at` VARCHAR(255) NOT NULL,
  PRIMARY KEY (`refund_id`),
  UNIQUE KEY uq_refund_order (`order_id`),
  FOREIGN KEY (`order_id`) REFERENCES `orders` (`order_id`),
  FOREIGN KEY (`admin_id`) REFERENCES `admins` (`admin_id`),
  CONSTRAINT ck_refunds_status CHECK (refund_status IN ('待处理','已同意','已拒绝','退款成功'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `reviews` (
  `review_id` INT NOT NULL AUTO_INCREMENT,
  `order_id` INT NOT NULL,
  `reviewer_id` INT NOT NULL,
  `content` TEXT NOT NULL,
  `created_at` VARCHAR(255) NOT NULL,
  PRIMARY KEY (`review_id`),
  UNIQUE KEY uq_review_author (`order_id`,`reviewer_id`),
  FOREIGN KEY (`order_id`) REFERENCES `orders` (`order_id`),
  FOREIGN KEY (`reviewer_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `reports` (
  `report_id` INT NOT NULL AUTO_INCREMENT,
  `reporter_id` INT NOT NULL,
  `product_id` INT NOT NULL,
  `reason` TEXT NOT NULL,
  `report_status` VARCHAR(255) NOT NULL,
  `admin_id` INT NULL,
  `handle_note` TEXT NULL,
  `created_at` VARCHAR(255) NOT NULL,
  PRIMARY KEY (`report_id`),
  FOREIGN KEY (`reporter_id`) REFERENCES `users` (`user_id`),
  FOREIGN KEY (`product_id`) REFERENCES `products` (`product_id`),
  FOREIGN KEY (`admin_id`) REFERENCES `admins` (`admin_id`),
  CONSTRAINT ck_reports_status CHECK (report_status IN ('待处理','成立','不成立'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `favorites` (
  `user_id` INT NOT NULL,
  `product_id` INT NOT NULL,
  `favorited_at` VARCHAR(255) NOT NULL,
  PRIMARY KEY (`user_id`,`product_id`),
  FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`),
  FOREIGN KEY (`product_id`) REFERENCES `products` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

