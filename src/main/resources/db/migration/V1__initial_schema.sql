CREATE TABLE app_users (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(254) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    CHECK (email = lower(trim(email)))
);

CREATE TABLE projects (
    id UUID PRIMARY KEY,
    project_key VARCHAR(10) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL,
    description TEXT,
    repository VARCHAR(2048),
    next_bug_number INTEGER NOT NULL DEFAULT 1 CHECK (next_bug_number > 0)
);

CREATE TABLE members (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_users(id),
    project_id UUID NOT NULL REFERENCES projects(id),
    active BOOLEAN NOT NULL DEFAULT true,
    UNIQUE (user_id, project_id),
    UNIQUE (id, project_id)
);

CREATE TABLE member_roles (
    member_id UUID NOT NULL REFERENCES members(id),
    role VARCHAR(20) NOT NULL CHECK (role IN ('MANAGER','TESTER','DEVELOPER','OBSERVER')),
    PRIMARY KEY (member_id, role)
);

CREATE TABLE user_sessions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_users(id),
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    csrf_token VARCHAR(64) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_sessions_expiry ON user_sessions(expires_at);

CREATE TABLE bugs (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES projects(id),
    number INTEGER NOT NULL CHECK (number > 0),
    author_id UUID NOT NULL,
    reviewer_id UUID,
    title VARCHAR(200) NOT NULL CHECK (length(trim(title)) > 0),
    description TEXT NOT NULL,
    steps TEXT NOT NULL,
    expected TEXT NOT NULL,
    actual TEXT NOT NULL,
    environment TEXT,
    severity VARCHAR(20) NOT NULL CHECK (severity IN ('BLOCKER','CRITICAL','MAJOR','MINOR')),
    priority VARCHAR(20) NOT NULL CHECK (priority IN ('URGENT','HIGH','NORMAL','LOW')),
    status VARCHAR(20) NOT NULL CHECK (status IN ('NEW','OPEN','IN_PROGRESS','IN_REVIEW','RETURNED','CLOSED','REJECTED')),
    revision BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    UNIQUE (project_id, number),
    FOREIGN KEY (author_id, project_id) REFERENCES members(id, project_id),
    FOREIGN KEY (reviewer_id, project_id) REFERENCES members(id, project_id)
);
CREATE INDEX idx_bugs_project_number ON bugs(project_id, number DESC);

CREATE TABLE history_entries (
    id UUID PRIMARY KEY,
    bug_id UUID NOT NULL REFERENCES bugs(id),
    actor_id UUID NOT NULL REFERENCES members(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    action VARCHAR(50) NOT NULL,
    field_name VARCHAR(100),
    before_value TEXT,
    after_value TEXT,
    comment TEXT
);
CREATE INDEX idx_history_bug ON history_entries(bug_id, created_at);
