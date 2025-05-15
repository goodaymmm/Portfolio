-- 初期ロールの追加
INSERT INTO roles (name, description) VALUES
('ROLE_ADMIN', 'Administrator role'),
('ROLE_DEMO', 'Demo user role with full access but limited user management'),
('ROLE_MANAGER', 'Manager role'),
('ROLE_USER', 'User role');

-- 初期管理者ユーザーの追加
-- パスワード: 'admin' をBCryptでハッシュ化した値
INSERT INTO users (username, password, email, enabled) VALUES
('admin', '$2a$10$rJ6rYtdpLiPD1H5TNAG3/.YxhXZyZOWqt3H5RKhvDGEYN9Yr1wKsG', 'admin@example.com', true);

-- デモユーザーアカウントの追加
-- パスワード: 'demo' をBCryptでハッシュ化した値
INSERT INTO users (username, password, email, enabled) VALUES
('demo', '$2a$10$wSPHIBnJy41PRuLpVdErK.e3L5jKGGiy8Tm2vw0Q3vl4VzxfGwSAO', 'demo@example.com', true);

-- 管理者ユーザーにADMINロールを割り当て
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE u.username = 'admin' AND r.name = 'ROLE_ADMIN';

-- デモユーザーにDEMOロールを割り当て
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE u.username = 'demo' AND r.name = 'ROLE_DEMO';

-- デモユーザーにMANAGERロールを割り当て
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE u.username = 'demo' AND r.name = 'ROLE_MANAGER';

-- デモユーザーにUSERロールを割り当て
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE u.username = 'demo' AND r.name = 'ROLE_USER'; 