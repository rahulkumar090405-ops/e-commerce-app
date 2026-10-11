-- Existing installation: stop user-service, back up user_db, then apply ONCE
-- before starting the new code. There is no Flyway/Liquibase runner in this repo.
-- An empty DB may bootstrap with Hibernate first, then apply this script.
-- Guarded DDL also allows applying it after Hibernate's local schema update.
SET @add_enabled = IF(
    EXISTS(SELECT 1 FROM information_schema.columns
           WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'enabled'),
    'SELECT 1',
    'ALTER TABLE users ADD COLUMN enabled BOOLEAN NOT NULL DEFAULT TRUE'
);
PREPARE enabled_column FROM @add_enabled;
EXECUTE enabled_column;
DEALLOCATE PREPARE enabled_column;

-- The old UserRole entity also mapped this join table with a version column.
-- Existing installations keep those legacy columns; give membership inserts
-- their missing version default. Skip this ALTER if SHOW COLUMNS finds no version.
SET @fix_role_version = IF(
    EXISTS(SELECT 1 FROM information_schema.columns
           WHERE table_schema = DATABASE() AND table_name = 'user_roles' AND column_name = 'version'),
    'ALTER TABLE user_roles MODIFY COLUMN version INT NOT NULL DEFAULT 0',
    'SELECT 1'
);
PREPARE role_version_fix FROM @fix_role_version;
EXECUTE role_version_fix;
DEALLOCATE PREPARE role_version_fix;

CREATE TABLE IF NOT EXISTS google_identities (
    id BIGINT NOT NULL AUTO_INCREMENT,
    issuer VARCHAR(255) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    subject VARCHAR(255) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    user_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_google_issuer_subject UNIQUE (issuer, subject),
    CONSTRAINT fk_google_identity_user FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Enforce case-sensitive identifiers even if Hibernate bootstrapped this table.
ALTER TABLE google_identities
    MODIFY COLUMN issuer VARCHAR(255) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    MODIFY COLUMN subject VARCHAR(255) CHARACTER SET ascii COLLATE ascii_bin NOT NULL;
-- Do not insert links or assign roles automatically. See GOOGLE_ADMIN_LOGIN.md.
