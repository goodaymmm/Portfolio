-- 初期ロールの追加
INSERT INTO roles (name, description) VALUES
('ROLE_ADMIN', 'Administrator role'),
('ROLE_MANAGER', 'Manager role'),
('ROLE_USER', 'User role');

-- 初期管理者ユーザーの追加
-- パスワード: 'admin' をBCryptでハッシュ化した値
INSERT INTO users (username, password, email, enabled) VALUES
('admin', '$2a$10$rJ6rYtdpLiPD1H5TNAG3/.YxhXZyZOWqt3H5RKhvDGEYN9Yr1wKsG', 'admin@example.com', true);

-- 管理者ユーザーにADMINロールを割り当て
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE u.username = 'admin' AND r.name = 'ROLE_ADMIN'; 