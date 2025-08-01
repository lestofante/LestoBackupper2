use std::sync::Arc;

use tokio::sync::Mutex;
use tokio::net::TcpStream;
use tokio_rustls::server::TlsStream;

pub struct ClientState {
    tls_stream: TlsStream<TcpStream>,
    global_download_list: Arc<Mutex<Vec<FileToDownload>>>,
}

pub struct UniqueFileId {
    md5: String,
    sha256: String,
    collision_id: i32,
}

impl UniqueFileId {
    pub fn new(md5: String, sha256: String, collision_id: i32) -> UniqueFileId{
        UniqueFileId {
            md5,
            sha256,
            collision_id,
        }
    }
}

pub struct FileToDownload {
    pub(crate) current_md5: String,
    current_sha256: String,
    current_size: usize,
    file_id: UniqueFileId,
}

impl FileToDownload {
    pub fn new(current_md5: String, current_sha256: String, current_size: usize, file_id: UniqueFileId) -> FileToDownload{
        FileToDownload {
            current_md5,
            current_sha256,
            current_size,
            file_id,
        }
    }
}

pub mod client_handler;