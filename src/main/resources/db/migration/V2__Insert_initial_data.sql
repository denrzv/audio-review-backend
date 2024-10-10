-- V2__Insert_initial_data.sql

INSERT INTO categories (name, shortcut) VALUES
                                            ('Silent', 'S'),
                                            ('Voice', 'V'),
                                            ('Answering Machine', 'A'),
                                            ('Undefined', 'U'),
                                            ('Unclassified', 'N')
ON CONFLICT (name) DO NOTHING;

INSERT INTO users (username, password, role, active)
VALUES
    ('admin@test.io', '$2a$10$5sRB8ExhhwGm045dfnLgReVKS6PdTBXMebQZTJE5UYMSQbp9mQQXC', 'ADMIN', TRUE)
ON CONFLICT (username) DO NOTHING;