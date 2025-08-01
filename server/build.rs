fn main() {
    let mut config = prost_build::Config::new();
    config.out_dir("src/protobuf");

    config.compile_protos(&["../protocol_buffer/file_description.proto"], &["../protocol_buffer"]).unwrap();
}