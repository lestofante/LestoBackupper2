-- Your SQL goes here
CREATE TABLE backups (
    local_id   INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    filepath   TEXT NOT NULL,
    md5        TEXT NOT NULL,
    sha256       TEXT NOT NULL,
    collision_id INTEGER NOT NULL,
    expected_size INTEGER NOT NULL
)