-- Run after the base schema, on a backed-up database. No raw credentials are stored here.
CREATE TABLE IF NOT EXISTS security_role (code VARCHAR(32) PRIMARY KEY, label VARCHAR(80) NOT NULL);
CREATE TABLE IF NOT EXISTS security_permission (code VARCHAR(80) PRIMARY KEY, label VARCHAR(80) NOT NULL);
CREATE TABLE IF NOT EXISTS security_role_permission (
    role_code VARCHAR(32) NOT NULL, permission_code VARCHAR(80) NOT NULL,
    PRIMARY KEY(role_code,permission_code),
    FOREIGN KEY(role_code) REFERENCES security_role(code),
    FOREIGN KEY(permission_code) REFERENCES security_permission(code)
);
CREATE TABLE IF NOT EXISTS security_user_role (
    user_id INT NOT NULL, role_code VARCHAR(32) NOT NULL, PRIMARY KEY(user_id,role_code),
    FOREIGN KEY(user_id) REFERENCES user(id) ON DELETE CASCADE,
    FOREIGN KEY(role_code) REFERENCES security_role(code)
);
CREATE TABLE IF NOT EXISTS security_account (
    user_id INT PRIMARY KEY, token_version INT NOT NULL DEFAULT 0,
    FOREIGN KEY(user_id) REFERENCES user(id) ON DELETE CASCADE
);
CREATE TABLE IF NOT EXISTS security_refresh (
    token_hash CHAR(64) PRIMARY KEY, user_id INT NOT NULL, token_version INT NOT NULL, expires_at BIGINT NOT NULL,
    FOREIGN KEY(user_id) REFERENCES user(id) ON DELETE CASCADE
);
INSERT INTO security_role(code,label) SELECT 'USER','普通用户' WHERE NOT EXISTS(SELECT 1 FROM security_role WHERE code='USER');
INSERT INTO security_role(code,label) SELECT 'ADMIN','管理员' WHERE NOT EXISTS(SELECT 1 FROM security_role WHERE code='ADMIN');
INSERT INTO security_permission(code,label) SELECT 'book:manage','管理图书' WHERE NOT EXISTS(SELECT 1 FROM security_permission WHERE code='book:manage');
INSERT INTO security_permission(code,label) SELECT 'user:manage','管理用户' WHERE NOT EXISTS(SELECT 1 FROM security_permission WHERE code='user:manage');
INSERT INTO security_permission(code,label) SELECT 'review:moderate','审核书评' WHERE NOT EXISTS(SELECT 1 FROM security_permission WHERE code='review:moderate');
INSERT INTO security_permission(code,label) SELECT 'stats:read','读取管理统计' WHERE NOT EXISTS(SELECT 1 FROM security_permission WHERE code='stats:read');
INSERT INTO security_role_permission(role_code,permission_code)
    SELECT 'ADMIN',p.code FROM security_permission p WHERE NOT EXISTS
    (SELECT 1 FROM security_role_permission rp WHERE rp.role_code='ADMIN' AND rp.permission_code=p.code);
INSERT INTO security_account(user_id,token_version)
    SELECT u.id,0 FROM user u WHERE NOT EXISTS(SELECT 1 FROM security_account a WHERE a.user_id=u.id);
-- One-time bootstrap for users present before this migration. Existing role assignments are preserved.
INSERT INTO security_user_role(user_id,role_code)
    SELECT u.id,CASE WHEN u.role=1 THEN 'ADMIN' ELSE 'USER' END FROM user u
    WHERE NOT EXISTS(SELECT 1 FROM security_user_role ur WHERE ur.user_id=u.id);
