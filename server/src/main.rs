use diesel::prelude::*;
use diesel::sqlite::SqliteConnection;
use rustls::pki_types::pem::PemObject;
use rustls::pki_types::{CertificateDer, PrivateKeyDer};
use rustls::ServerConfig;
use sha2::Digest;
use tokio::io::AsyncReadExt;
use tokio::net::TcpListener;
use tokio::select;
use tokio::sync::Mutex;
use tokio_rustls::{TlsAcceptor};
use toml::Value;
use std::error::Error;
use std::fs::{self, File};
use std::path::Path;
use std::time::Duration;
use std::{sync::Arc};
use std::io::{BufReader, Read};
use tokio_rustls::server::TlsStream;

use crate::file_downloader::{client_handler, ClientState, FileToDownload};

mod db_struct;

mod file_downloader;

fn load_certs(filename: &Path) -> Vec<CertificateDer<'static>> {
    CertificateDer::pem_file_iter(filename)
        .expect("cannot open certificate file")
        .map(|result| result.unwrap())
        .collect()
}

fn load_private_key(filename: &Path) -> PrivateKeyDer<'static> {
    PrivateKeyDer::from_pem_file(filename).expect("cannot read private key file")
}

fn calculate_hashes(path: &Path) -> std::io::Result<(String, String, usize)> {
    let file = File::open(path)?;
    let mut reader = BufReader::new(file);

    let mut sha256_hasher = sha2::Sha256::new();
    let mut md5_hasher = md5::Context::new();

    let mut buffer = [0u8; 8192];
    let mut filesize:usize = 0;
    loop {
        let n = reader.read(&mut buffer)?;
        if n == 0 { break; }
        sha256_hasher.update(&buffer[..n]);
        md5_hasher.consume(&buffer[..n]);
        filesize += n;
    }

    let sha256 = format!("{:x}", sha256_hasher.finalize());
    let md5 = format!("{:x}", md5_hasher.compute());

    Ok((sha256, md5, filesize))
}

// fn add_entry<'a>(conn: &mut SqliteConnection, file: &file_description::FileInfo) {
//     let _ = file;

// }

fn check_entry<'a>(conn: &'a mut SqliteConnection, path: &'a Path) -> Option<FileToDownload> {

    match calculate_hashes(path) {
        Ok((sha256, md5, filesize)) => {
            // Find existing entries with same hashes
            match db_struct::find_by_path(conn, path.to_str().unwrap()) {
                Ok(matches) => {
                    for entry in &matches {
                        // If content exists on disk and is identical, skip insert
                        if entry.expected_size as usize == filesize{
                            if entry.sha256 == sha256 && entry.md5 == md5 {
                                println!("File fully valid: {}", entry.filepath);
                                return None;
                            }else{
                                println!("File in database but with wrong sha256/md5: {} {}:{} {}:{}", entry.filepath, entry.sha256, sha256, entry.md5, md5);
                            }
                        }else{
                            println!("File in database possibly not complete: {} {}/{}", entry.filepath, filesize, entry.expected_size);
                            let file = file_downloader::FileToDownload::new(
                                md5.clone(), sha256.clone(), filesize,
                                file_downloader::UniqueFileId::new(entry.md5.clone(), entry.sha256.clone(), entry.collision_id)
                            );
                            return Some(file);
                        }
                    }
                    //if we are here, the file exist on FS but not in DB
                    println!("File NOT in database: {}", path.to_str().unwrap_or("file name could not be decoded"));
/*
                    // No identical file found: insert new entry
                    use diesel::insert_into;
                    use server::schema::backups;

                    let new_entry = db_struct::NewFileEntry {
                        filepath: path.to_str().unwrap(), // safe if path is valid UTF-8
                        md5: &md5,
                        sha256: &sha256,
                        collision_id: 0,
                        expected_size: todo!(), // TODO: increment if needed
                    };

                    if let Err(e) = insert_into(backups::table)
                        .values(&new_entry)
                        .execute(conn)
                    {
                        eprintln!("Failed to insert entry: {}", e);
                    } else {
                        println!("Inserted new file: {:?}", path);
                    }
*/
                }
                Err(e) => eprintln!("Query error: {}", e),
            }
        }
        Err(e) => eprintln!("Error hashing file: {}", e),
    }
    None
}

async fn list_files_in_dir(path: &Path, global_download_list: &Mutex<Vec<FileToDownload>>) {
    if !path.exists(){
        let _ = fs::create_dir_all(path);
    }
    if !path.is_dir(){
        panic!("Destination directory is NOT a dir");
    }
    let mut conn = SqliteConnection::establish("backup.db").expect("Failed to connect to DB");
    if let Ok(entries) = fs::read_dir(path) {
        for entry in entries.flatten() {
            let path = entry.path();
            if path.is_file() {
                let file = check_entry(&mut conn, path.as_path());
                if let Some(file) = file{
                    let mut list = global_download_list.lock().await;
                    list.push(file);
                }
                println!("{}", path.display());
            }
        }
    }
}

#[tokio::main]
async fn main() -> Result<(), Box<dyn Error>> {
    let mut global_download_list: Mutex<Vec<FileToDownload>> = Mutex::new(Vec::new());
    
    let raw = fs::read_to_string("config.toml").unwrap_or("cannot read config file!".to_string());
    let parsed: Value = toml::from_str(&raw).unwrap();
    let folder = parsed["folder"].as_str().unwrap();

    list_files_in_dir(Path::new(folder), &mut global_download_list);

    //for tcp_stream in listener.incoming() {

    let mut clients: Vec<ClientState> = vec![];

    run_server(global_download_list).await; //hang forever

    Ok(())
}

async fn run_server(global_download_list: Mutex<Vec<FileToDownload>>) {
    // Setup TLS config
    let certs = load_certs(Path::new("cert.pem"));
    let key = load_private_key(Path::new("private.pem"));

    let tls_config = ServerConfig::builder()
        .with_no_client_auth()
        .with_single_cert(certs, key)
        .expect("bad certificates/private key");

    let acceptor = TlsAcceptor::from(Arc::new(tls_config));

    let listener = TcpListener::bind("0.0.0.0:4443").await.unwrap();
    println!("Listening on https://0.0.0.0:4443");

    let mut ticker = tokio::time::interval(Duration::from_secs(1));

    let mutex_wrapper = Arc::new(global_download_list);
    loop {
        select! {
            accept_result = listener.accept() => {
                match accept_result {
                    Ok((tcp_stream, addr)) => {
                        let acceptor = acceptor.clone();
                        println!("Accepted: {}", addr);
                        
                        let client_global_list = mutex_wrapper.clone();
                        tokio::spawn(async move {
                            match acceptor.accept(tcp_stream).await {
                                Ok(stream) => {
                                    let mut client = ClientState::new(stream, client_global_list); // If needed
                                    client.handle_tls_client().await;
                                }
                                Err(e) => eprintln!("TLS error: {:?}", e),
                            }
                        });
                    }
                    Err(e) => eprintln!("Accept error: {}", e),
                }
            }

            _ = ticker.tick() => {
                println!("Timer tick");
            }
        }
    }
}
