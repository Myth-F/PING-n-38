-- Crée le login pour l'user admin
-- Login: admin.user
-- Password: admin123
-- L'utilisateur admin est necessary
INSERT INTO users (id, login, password, display_name, avatar, is_admin)
VALUES ('00000000-0000-0000-0000-000000000001', 'admin.user', 'admin123', 'Admin User', '',
        true)
    ON CONFLICT (id) DO NOTHING;