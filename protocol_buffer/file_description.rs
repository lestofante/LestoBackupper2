// Automatically generated rust module for 'file_description.proto' file

#![allow(non_snake_case)]
#![allow(non_upper_case_globals)]
#![allow(non_camel_case_types)]
#![allow(unused_imports)]
#![allow(unknown_lints)]
#![allow(clippy::all)]
#![cfg_attr(rustfmt, rustfmt_skip)]


use std::borrow::Cow;
use quick_protobuf::{MessageInfo, MessageRead, MessageWrite, BytesReader, Writer, WriterBackend, Result};
use quick_protobuf::sizeofs::*;
use super::*;

#[allow(clippy::derive_partial_eq_without_eq)]
#[derive(Debug, Default, PartialEq, Clone)]
pub struct FileInfo<'a> {
    pub id: i64,
    pub size: i64,
    pub hash: Cow<'a, str>,
    pub name: Cow<'a, str>,
}

impl<'a> MessageRead<'a> for FileInfo<'a> {
    fn from_reader(r: &mut BytesReader, bytes: &'a [u8]) -> Result<Self> {
        let mut msg = Self::default();
        while !r.is_eof() {
            match r.next_tag(bytes) {
                Ok(8) => msg.id = r.read_int64(bytes)?,
                Ok(16) => msg.size = r.read_int64(bytes)?,
                Ok(26) => msg.hash = r.read_string(bytes).map(Cow::Borrowed)?,
                Ok(34) => msg.name = r.read_string(bytes).map(Cow::Borrowed)?,
                Ok(t) => { r.read_unknown(bytes, t)?; }
                Err(e) => return Err(e),
            }
        }
        Ok(msg)
    }
}

impl<'a> MessageWrite for FileInfo<'a> {
    fn get_size(&self) -> usize {
        0
        + if self.id == 0i64 { 0 } else { 1 + sizeof_varint(*(&self.id) as u64) }
        + if self.size == 0i64 { 0 } else { 1 + sizeof_varint(*(&self.size) as u64) }
        + if self.hash == "" { 0 } else { 1 + sizeof_len((&self.hash).len()) }
        + if self.name == "" { 0 } else { 1 + sizeof_len((&self.name).len()) }
    }

    fn write_message<W: WriterBackend>(&self, w: &mut Writer<W>) -> Result<()> {
        if self.id != 0i64 { w.write_with_tag(8, |w| w.write_int64(*&self.id))?; }
        if self.size != 0i64 { w.write_with_tag(16, |w| w.write_int64(*&self.size))?; }
        if self.hash != "" { w.write_with_tag(26, |w| w.write_string(&**&self.hash))?; }
        if self.name != "" { w.write_with_tag(34, |w| w.write_string(&**&self.name))?; }
        Ok(())
    }
}

