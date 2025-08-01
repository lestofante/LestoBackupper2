use std::{io::Bytes, sync::Arc};

use server::protobuf::file_description::{server_message::Kind, ServerMessage};
use tokio::{io::{AsyncReadExt, AsyncWriteExt}, net::TcpStream, sync::Mutex};
use tokio_rustls::server::TlsStream;

use crate::file_downloader::{ClientState, FileToDownload};
use prost::Message;

impl ClientState {
    pub fn new(tls_stream: TlsStream<TcpStream>, global_download_list: Arc<Mutex<Vec<FileToDownload>>>,) -> ClientState{
        ClientState{tls_stream, global_download_list}
    }

    pub async fn request_files(&mut self) -> std::io::Result<()> {
        let list = self.global_download_list.lock().await;

        for file in list.iter() {
            let protobuf = server::protobuf::file_description::FileToDownload {
                current_md5: file.current_md5.clone(),
                current_sha256: file.current_sha256.clone(),
                current_size: file.current_size as u64,
                file_id: Some(server::protobuf::file_description::UniqueFileId {
                    md5: file.file_id.md5.clone(),
                    sha256: file.file_id.sha256.clone(),
                    collision_id: file.file_id.collision_id,
                }),
            };

            let server_msg = ServerMessage {
                kind: Some(Kind::FileToDownload(protobuf)),
            };

            let mut buf = Vec::new();
            server_msg.encode(&mut buf).unwrap();

            let len = buf.len() as u32;
            self.tls_stream.write_all(&len.to_be_bytes()).await?;
            self.tls_stream.write_all(&buf).await?;
        }
        self.tls_stream.flush().await?;
        Ok(())
    }

    pub async fn read_delimited_message(&mut self) -> std::io::Result<server::protobuf::file_description::ClientMessage> {
        // Decode the varint length (max 10 bytes for u64)
        let mut len_buf = [0u8; 10];
        for i in 0..10 {
            self.tls_stream.read_exact(&mut len_buf[i..=i]).await?;
            if len_buf[i] & 0x80 == 0 {
                let mut buf = &len_buf[..=i];
                let len = prost::encoding::decode_varint(&mut buf);
                let len = len.map_err(|_| std::io::Error::new(std::io::ErrorKind::InvalidData, "invalid varint"))?;
                let mut msg_buf = vec![0u8; len as usize];
                self.tls_stream.read_exact(&mut msg_buf).await?;
                let msg = server::protobuf::file_description::ClientMessage::decode(&*msg_buf)?;
                return Ok(msg);
            }
        }
        Err(std::io::Error::new(std::io::ErrorKind::InvalidData, "varint too long"))
    }

    pub async fn handle_tls_client(&mut self) -> std::io::Result<()> {
        self.request_files().await?;

        let mut buf = [0u8; 1024];
        let mut len_buf = [0u8; 4];
        loop {
            // Decode ClientMessage from bytes
            match self.read_delimited_message().await {
                Ok(client_msg) => {
                    println!("Received ClientMessage: {:?}", client_msg);
                    // TODO: handle message, e.g. call request_files() if requested
                }
                Err(e) => {
                    eprintln!("Failed to decode ClientMessage: {}", e);
                    break;
                }
            }
        }
        println!("Client disconnected");

        Ok(())
    }
}