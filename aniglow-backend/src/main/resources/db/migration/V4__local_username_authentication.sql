ALTER TABLE users
    MODIFY COLUMN email VARCHAR(100) NULL,
    ADD COLUMN credentials_initialized TINYINT(1) NOT NULL DEFAULT 1
        COMMENT 'Whether the user has chosen local username/password credentials';

UPDATE users
SET credentials_initialized = 0
WHERE authing_id IS NOT NULL AND authing_id <> '';
