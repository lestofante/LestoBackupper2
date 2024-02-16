extern crate rcgen;

use std::{env, thread, time};
use std::error::Error as StdError;
use std::fs::File;
use std::io::{BufReader, Read, Write};
use std::net::TcpListener;
use std::sync::Arc;

fn main() -> Result<(), Box<dyn StdError>> {
    let mut args = env::args();
    args.next();
    let cert_file = "cert.key";
    let private_key_file = "private.key";

    let certs = rustls_pemfile::certs(&mut BufReader::new(&mut File::open(cert_file)?))
        .collect::<Result<Vec<_>, _>>()?;
    let private_key =
        rustls_pemfile::private_key(&mut BufReader::new(&mut File::open(private_key_file)?))?
            .unwrap();
    let config = rustls::ServerConfig::builder()
        .with_no_client_auth()
        .with_single_cert(certs, private_key)?;

    let listener = TcpListener::bind(format!("[::]:{}", 4443)).unwrap();
    let (mut stream, _) = listener.accept()?;

    println!("Connected client: {:?}", stream);

    let mut conn = rustls::ServerConnection::new(Arc::new(config)).unwrap();
    conn.complete_io(&mut stream).unwrap();

    println!("s1");

    conn.writer().write_all(b"Hello from the server").unwrap();
    println!("s2");
    conn.complete_io(&mut stream).unwrap();
    println!("s3");
    loop{
        let mut buf = [0; 64];
        conn.complete_io(&mut stream).unwrap();
        if let Ok(len) = conn.reader().read(&mut buf){
            if len == 0{
                println!("Client disconnected");
                break;
            }
            println!("Received message from client: {:?}", &buf[..len]);
            
        }
        thread::sleep(time::Duration::from_millis(10));
    }

    Ok(())
}

#[test]
fn create_cert() {
    
    use rcgen::generate_simple_self_signed;
    // Generate a certificate that's valid for "localhost" and "hello.world.example"
    let subject_alt_names = vec!["hello.world.example".to_string(),
        "localhost".to_string()];

    let cert = generate_simple_self_signed(subject_alt_names).unwrap();
    println!("{}", cert.serialize_pem().unwrap());
    println!("{}", cert.serialize_private_key_pem());
}
