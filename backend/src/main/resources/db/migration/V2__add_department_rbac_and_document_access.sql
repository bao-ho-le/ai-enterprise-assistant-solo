-- ============================================================
-- V2: Department, RBAC (role_permissions), document sharing,
--     resource ownership + soft-delete audit columns.
--
-- Lưu ý: dự án đang chạy spring.jpa.hibernate.ddl-auto=update và
-- spring.flyway.enabled=false, nên schema thực tế do Hibernate tạo.
-- File này giữ đúng schema tương ứng để bật Flyway khi triển khai thật.
-- ============================================================

CREATE TABLE IF NOT EXISTS departments (
    id BIGSERIAL PRIMARY KEY,

    name VARCHAR(255) NOT NULL,
    description TEXT,

    manager_id BIGINT,

    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,

    CONSTRAINT fk_departments_manager
        FOREIGN KEY (manager_id) REFERENCES users(id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_departments_name ON departments(name);
CREATE INDEX IF NOT EXISTS idx_department_manager_id ON departments(manager_id);
CREATE INDEX IF NOT EXISTS idx_department_created_at ON departments(created_at);

-- ------------------------------------------------------------
-- Role / Permission (system-defined, chỉ mapping là dữ liệu)
-- ------------------------------------------------------------

CREATE TABLE IF NOT EXISTS role_permissions (
    id BIGSERIAL PRIMARY KEY,

    role VARCHAR(30) NOT NULL,
    permission VARCHAR(50) NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_role_permission ON role_permissions(role, permission);
CREATE INDEX IF NOT EXISTS idx_role_permission_role ON role_permissions(role);

-- ------------------------------------------------------------
-- User thuộc Department + chuẩn hoá role cũ
-- ------------------------------------------------------------

ALTER TABLE users ADD COLUMN IF NOT EXISTS department_id BIGINT;

ALTER TABLE users
    DROP CONSTRAINT IF EXISTS fk_users_department;

ALTER TABLE users
    ADD CONSTRAINT fk_users_department
    FOREIGN KEY (department_id) REFERENCES departments(id);

UPDATE users
SET role = 'EMPLOYEE'
WHERE role NOT IN ('ADMIN', 'MANAGER', 'EMPLOYEE', 'SUPERVISOR');

-- ------------------------------------------------------------
-- Ownership + soft delete audit cho Document / Folder
-- ------------------------------------------------------------

ALTER TABLE documents ADD COLUMN IF NOT EXISTS owner_id BIGINT;
ALTER TABLE documents ADD COLUMN IF NOT EXISTS department_id BIGINT;
ALTER TABLE documents ADD COLUMN IF NOT EXISTS deleted_by BIGINT;

CREATE INDEX IF NOT EXISTS idx_document_owner_id ON documents(owner_id);
CREATE INDEX IF NOT EXISTS idx_document_department_id ON documents(department_id);

ALTER TABLE folders ADD COLUMN IF NOT EXISTS owner_id BIGINT;
ALTER TABLE folders ADD COLUMN IF NOT EXISTS department_id BIGINT;
ALTER TABLE folders ADD COLUMN IF NOT EXISTS deleted_by BIGINT;

CREATE INDEX IF NOT EXISTS idx_folder_owner_id ON folders(owner_id);
CREATE INDEX IF NOT EXISTS idx_folder_department_id ON folders(department_id);

-- ------------------------------------------------------------
-- Shared documents
-- ------------------------------------------------------------

CREATE TABLE IF NOT EXISTS document_accesses (
    id BIGSERIAL PRIMARY KEY,

    document_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    granted_by BIGINT,

    created_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_document_access_document
        FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE,

    CONSTRAINT fk_document_access_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,

    CONSTRAINT fk_document_access_granted_by
        FOREIGN KEY (granted_by) REFERENCES users(id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_document_access_document_user
    ON document_accesses(document_id, user_id);

CREATE INDEX IF NOT EXISTS idx_document_access_document_id ON document_accesses(document_id);
CREATE INDEX IF NOT EXISTS idx_document_access_user_id ON document_accesses(user_id);

-- ------------------------------------------------------------
-- Conversation ownership
-- ------------------------------------------------------------

ALTER TABLE ai_conversations ADD COLUMN IF NOT EXISTS user_id BIGINT;

CREATE INDEX IF NOT EXISTS idx_ai_conversation_user_id ON ai_conversations(user_id);

-- Conversation cũ không có chủ sẽ không ai đọc được nữa: gán về admin đầu tiên.
UPDATE ai_conversations
SET user_id = (
    SELECT id FROM users
    ORDER BY CASE WHEN role = 'ADMIN' THEN 0 ELSE 1 END, id
    LIMIT 1
)
WHERE user_id IS NULL;

-- ------------------------------------------------------------
-- AI usage ownership
-- ------------------------------------------------------------

ALTER TABLE ai_usage_logs ADD COLUMN IF NOT EXISTS user_id BIGINT;
ALTER TABLE ai_usage_logs ADD COLUMN IF NOT EXISTS department_id BIGINT;

CREATE INDEX IF NOT EXISTS idx_ai_usage_user_id ON ai_usage_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_ai_usage_department_id ON ai_usage_logs(department_id);
