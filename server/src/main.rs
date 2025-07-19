extern crate rcgen;

use std::error::Error as StdError;
use std::fs::File;
use std::io::{BufReader, Read};
use std::net::TcpListener;
use qrcode::QrCode;

mod protobuf;
mod client;

fn main() -> Result<(), Box<dyn StdError>> {

    //LestoBackupper::run(iced::Settings::default());

    let cert_file = "cert.key";
    let private_key_file = "private.key";

    let mut buffer_cert_file = Vec::new();
    File::open(cert_file)
        .expect("Failed to open file")
        .read_to_end(&mut buffer_cert_file)
        .expect("Failed to read file");
    
    let mut cursor = std::io::Cursor::new(&buffer_cert_file);
    let certs = rustls_pemfile::certs(&mut cursor)
        .collect::<Result<Vec<_>, _>>()?;
    let private_key =
        rustls_pemfile::private_key(&mut BufReader::new(&mut File::open(private_key_file)?))?
            .unwrap();
    let config = rustls::ServerConfig::builder()
        .with_no_client_auth()
        .with_single_cert(certs, private_key)?;

    print_cert(buffer_cert_file);

    let listener = TcpListener::bind(format!("[::]:{}", 4443)).unwrap();
    let (stream, _) = listener.accept()?;

    println!("Connected client: {:?}", stream);

    client::run_client(config, stream);

    Ok(())
}

fn print_cert<D: AsRef<[u8]>>(data: D){
    // Encode some data into bits.
    let code = QrCode::new(data).unwrap();

    // You can also render it into a string.
    /*
    let string = code.render()
        .light_color(' ')
        .dark_color('#')
        .build();
    println!("{}", string);
    */

    // Render the bits into an image.
    let image = code.render::<image::Luma<u8>>().build();

    // Save the image.
    image.save("/tmp/qrcode.png").unwrap();
}

#[test]
fn create_cert() {
    match hostname::get() {
        Ok(hostname_os) => {
            let hostname = hostname_os.to_str().unwrap();
            println!("Hostname: {}", hostname);
            use rcgen::generate_simple_self_signed;
            // Generate a certificate that's valid for "localhost" and "hello.world.example"
            let mut subject_alt_names = vec![hostname.to_string()];

            let network_interfaces = local_ip_address::list_afinet_netifas().unwrap();

            for (name, ip) in network_interfaces.iter() {
                if name != "lo"{
                    subject_alt_names.push(ip.to_string());
                    println!("{}:\t{:?}", name, ip);
                }
            }

            let cert = generate_simple_self_signed(subject_alt_names).unwrap();
            println!("{}", cert.serialize_pem().unwrap());
            println!("{}", cert.serialize_private_key_pem());
        },
        Err(e) => eprintln!("Error getting hostname: {}", e),
    }
}
