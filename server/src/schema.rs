// @generated automatically by Diesel CLI.

diesel::table! {
    backups (local_id) {
        local_id -> Integer,
        filepath -> Text,
        md5 -> Text,
        sha256 -> Text,
        collision_id -> Integer,
        expected_size -> Integer,
    }
}
