ALTER TABLE file_upload
    ADD COLUMN upload_checked_at TIMESTAMP(6) NULL DEFAULT NULL,
    ADD INDEX idx_file_upload_pending_check (is_uploaded, upload_checked_at, upload_check_at, id);
