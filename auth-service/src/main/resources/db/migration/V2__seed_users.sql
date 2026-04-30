-- Seed data: default roles + admin/user accounts.
-- Passwords are BCrypt-hashed via Spring Security's delegating encoder
-- (the {bcrypt} prefix tells the encoder which algorithm to use).
--   admin / admin
--   user  / user

INSERT INTO roles (created_at, name, description)
VALUES (CURRENT_TIMESTAMP, 'ADMIN', 'Platform administrators'),
       (CURRENT_TIMESTAMP, 'USER',  'Standard authenticated users');

INSERT INTO users (created_at, status, username, email, password_hash, enabled)
VALUES (CURRENT_TIMESTAMP, 'ACTIVE', 'admin', 'admin@enterprise.local',
        '{bcrypt}$2a$10$DowJonesA7a4iy.2NjXh4OBEwT8TX4hnZJ34NQ7eA4xQwwJXxPQEa', TRUE),
       (CURRENT_TIMESTAMP, 'ACTIVE', 'user',  'user@enterprise.local',
        '{bcrypt}$2a$10$DowJonesA7a4iy.2NjXh4OBEwT8TX4hnZJ34NQ7eA4xQwwJXxPQEa', TRUE);

-- admin → ADMIN + USER ;  user → USER
INSERT INTO user_roles (user_id, role_id)
VALUES ((SELECT id FROM users WHERE username = 'admin'),
        (SELECT id FROM roles WHERE name = 'ADMIN')),
       ((SELECT id FROM users WHERE username = 'admin'),
        (SELECT id FROM roles WHERE name = 'USER')),
       ((SELECT id FROM users WHERE username = 'user'),
        (SELECT id FROM roles WHERE name = 'USER'));
