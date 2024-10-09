-- V1__Create_tables.sql

-- Enable necessary extensions (optional, based on your needs)
-- For example, UUID generation or full-text search
-- Uncomment if needed
-- CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 1. Create the 'users' table
CREATE TABLE IF NOT EXISTS users (
                                     id BIGSERIAL PRIMARY KEY,
                                     username VARCHAR(50) NOT NULL UNIQUE,
                                     password VARCHAR(255) NOT NULL,
                                     role VARCHAR(20) NOT NULL,
                                     active BOOLEAN NOT NULL DEFAULT TRUE,
                                     created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT (NOW() AT TIME ZONE 'utc'),
                                     updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT (NOW() AT TIME ZONE 'utc')
);

-- 2. Create the 'categories' table
CREATE TABLE IF NOT EXISTS categories (
                                          id BIGSERIAL PRIMARY KEY,
                                          name VARCHAR(50) NOT NULL UNIQUE,
                                          shortcut VARCHAR(1) NOT NULL UNIQUE
);

-- 3. Create the 'audio_files' table
CREATE TABLE IF NOT EXISTS audio_files (
                                           id BIGSERIAL PRIMARY KEY,
                                           filename VARCHAR(255) NOT NULL,
                                           filepath VARCHAR(500) NOT NULL UNIQUE,
                                           initial_category_id BIGINT NOT NULL,
                                           current_category_id BIGINT,
                                           uploaded_by BIGINT NOT NULL,
                                           uploaded_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
                                           FOREIGN KEY (initial_category_id) REFERENCES categories(id) ON DELETE RESTRICT,
                                           FOREIGN KEY (current_category_id) REFERENCES categories(id) ON DELETE SET NULL,
                                           FOREIGN KEY (uploaded_by) REFERENCES users(id) ON DELETE CASCADE
);

-- 4. Create the 'classifications' table
CREATE TABLE IF NOT EXISTS classifications (
                                               id BIGSERIAL PRIMARY KEY,
                                               user_id BIGINT NOT NULL,
                                               audio_file_id BIGINT NOT NULL,
                                               previous_category_id BIGINT,
                                               new_category_id BIGINT NOT NULL,
                                               classified_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
                                               FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                                               FOREIGN KEY (audio_file_id) REFERENCES audio_files(id) ON DELETE CASCADE,
                                               FOREIGN KEY (previous_category_id) REFERENCES categories(id) ON DELETE SET NULL,
                                               FOREIGN KEY (new_category_id) REFERENCES categories(id) ON DELETE RESTRICT
);

-- 5. Create indexes for performance optimization
CREATE INDEX IF NOT EXISTS idx_audio_files_initial_category ON audio_files(initial_category_id);
CREATE INDEX IF NOT EXISTS idx_audio_files_current_category ON audio_files(current_category_id);
CREATE INDEX IF NOT EXISTS idx_audio_files_uploaded_by ON audio_files(uploaded_by);
CREATE INDEX IF NOT EXISTS idx_classifications_user_id ON classifications(user_id);
CREATE INDEX IF NOT EXISTS idx_classifications_audio_file_id ON classifications(audio_file_id);
CREATE INDEX IF NOT EXISTS idx_classifications_new_category_id ON classifications(new_category_id);

-- 6. Add check constraints for the 'users' table to enforce valid roles
ALTER TABLE users
    ADD CONSTRAINT chk_user_role
        CHECK (role IN ('USER', 'ADMIN'));

-- 7. Add timestamps triggers (optional)
-- If you want to automatically update 'updated_at' on row updates, you can use triggers.
-- Here's an example using a simple trigger for the 'users' table:

-- Create a function to update the 'updated_at' column
CREATE OR REPLACE FUNCTION update_updated_at_column()
    RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW() AT TIME ZONE 'utc';
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Create the trigger on the 'users' table
CREATE TRIGGER trigger_update_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

-- Repeat similar triggers for other tables if needed