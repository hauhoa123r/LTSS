ALTER TABLE moderation_records
    ADD COLUMN target_snapshot JSON NULL AFTER review_id;
