CREATE TABLE IF NOT EXISTS log_entries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    action VARCHAR(50) NOT NULL,
    details VARCHAR(500) NOT NULL,
    user_id BIGINT,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_log_entries_user_id ON log_entries(user_id);
CREATE INDEX idx_log_entries_created_at ON log_entries(created_at);
CREATE INDEX idx_log_entries_action ON log_entries(action); 