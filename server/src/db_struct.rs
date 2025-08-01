use diesel::prelude::*;
use server::schema::backups;

#[derive(Queryable, Debug)]
pub struct FileEntry {
    pub local_id: i32,
    pub filepath: String,
    pub md5: String,
    pub sha256: String,
    pub collision_id: i32,
    pub expected_size: i32,
}

#[derive(Insertable, Debug)]
#[diesel(table_name = server::schema::backups)]
#[diesel(check_for_backend(diesel::sqlite::Sqlite))]
pub struct NewFileEntry<'a> {
    pub filepath: &'a str,
    pub md5: &'a str,
    pub sha256: &'a str,
    pub collision_id: i32,
    pub expected_size: i32,
}

pub fn find_by_hashes(
    conn: &mut SqliteConnection,
    target_md5: &str,
    target_sha256: &str,
) -> QueryResult<Vec<FileEntry>> {
    use self::backups::dsl::*;

    backups
        .filter(md5.eq(target_md5))
        .filter(sha256.eq(target_sha256))
        .load(conn)
}

pub fn find_by_path(
    conn: &mut SqliteConnection,
    path: &str,
) -> QueryResult<Vec<FileEntry>> {
    use self::backups::dsl::*;

    backups
        .filter(filepath.eq(path))
        .load(conn)
}