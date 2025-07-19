use std::sync::Arc;

use std::{thread, time};
use std::io::{Read, Write};

use quick_protobuf::{BytesReader, MessageRead};

use crate::protobuf::file_description;


enum ParsingStatus {
    Waiting,
    FileInfo,
}

pub fn run_client(config: rustls::ServerConfig, mut stream: std::net::TcpStream) {

    let mut conn: rustls::ServerConnection = rustls::ServerConnection::new(Arc::new(config)).unwrap();

    conn.complete_io(&mut stream).unwrap();

    println!("s1");

    conn.writer().write_all(b"Hello from the server").unwrap();
    println!("s2");
    conn.complete_io(&mut stream).unwrap();
    println!("s3");
    let mut status: ParsingStatus = ParsingStatus::Waiting;
    
    let mut buf = [0; 1024*8]; // should more than enough to contain any message
    let mut index = 0;
    let mut len = 0;

    let mut received = 0;
    loop{
        let mut disconnect = false;

        if conn.complete_io(&mut stream).is_err(){
            println!("Client in error state");
            disconnect = true;
        };

        match conn.reader().read(&mut buf[index..]){
            Ok(len) => {
                if len == 0{
                    disconnect = true;
                }else{
                    index += len; 
                }
            },
            Err(_) => {},
        };

        //println!("readed: {}", index);

        match status{
            ParsingStatus::Waiting => {
                if index >= 1 + 2 {
                    match buf[0]{
                        0 => {
                            status = ParsingStatus::FileInfo;
                            len = (buf[1] as usize) << 8 | buf[2] as usize;
                            //println!("Found FileInfo! len {}", len);
                        }
                        1_u8..=u8::MAX => {
                            // DESINCRONIZED! throw away everything
                            println!("DESINCRONIZED! {}", buf[0]);
                            index = 0;
                        }
                    }
                }
            },
            ParsingStatus::FileInfo =>{
                if index >= 1 + 2 + len{
                    let mut reader = BytesReader::from_bytes(&buf[3..3+len]);
                    let decode = file_description::FileInfo::from_reader(&mut reader, &buf[3..3+len]);
                    match decode{
                        Ok(mess) => {
                            //println!("Received message info: {:?}", mess);
                            index = 0;
                            status = ParsingStatus::Waiting;
                            received += 1;
                        },
                        Err(err) => println!("decode error: {:?}", err),
                    }
                }
            },
        }
        if index >= buf.len(){
            println!("Invalid data, disconnect");
            break;
        }else if disconnect{
            println!("Client disconnected");
            break;
        }else{
            //thread::sleep(time::Duration::from_millis(10));
        }
    }
    println!("received {received}");
} 