-- 改密审计。可重复执行。密码只存 users.password_hash（BCrypt），本表不含明文。
-- 查看账号：SELECT id, username, last_login_at, password_changed_at, created_at FROM users ORDER BY id;
-- 改密记录：SELECT username, success, reason, client_ip, created_at FROM password_change_logs ORDER BY id DESC;

CREATE TABLE IF NOT EXISTS `password_change_logs` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NULL,
  `username` varchar(50) NOT NULL,
  `success` tinyint NOT NULL DEFAULT 0 COMMENT '1成功 0失败',
  `reason` varchar(64) NULL,
  `client_ip` varchar(64) NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_pwd_log_user` (`user_id`, `created_at`),
  KEY `idx_pwd_log_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='改密审计：不含明文密码，便于查看与统计';
