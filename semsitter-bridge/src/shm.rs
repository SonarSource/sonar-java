/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */

//! Two shared mappings and a one-byte ping on the pipe.
//!
//! The payload never travels on the pipe. `1` means the mapping holds a protobuf message,
//! `2` means the peer grew it and this side must remap. The directory entry is removed once
//! both sides have mapped, so nothing is left to read as a file. On Linux the files live in
//! `/dev/shm`.

use std::fs::OpenOptions;
use std::io::{Read, Write};
use std::path::{Path, PathBuf};
use std::sync::atomic::{Ordering, fence};

use anyhow::{Result, bail};
use memmap2::MmapMut;

pub const READY: u8 = b'R';
pub const ATTACHED: u8 = b'A';
const DATA: u8 = 1;
const GROW: u8 = 2;
const INITIAL: usize = 32 * 1024 * 1024;
const MAX: usize = 512 * 1024 * 1024;
const HEADER: usize = 24;
const MAGIC: u32 = 0x3142_4A53;

pub struct Link {
  id: String,
  request: Slot,
  response: Slot,
  created: bool,
}

struct Slot {
  path: PathBuf,
  file: std::fs::File,
  map: MmapMut,
}

impl Link {
  pub fn create(id: &str) -> Result<Self> {
    Ok(Link {
      request: Slot::create(&slot_path(id, "q"), INITIAL)?,
      response: Slot::create(&slot_path(id, "a"), INITIAL)?,
      id: id.to_string(),
      created: true,
    })
  }

  pub fn open(id: &str) -> Result<Self> {
    Ok(Link {
      request: Slot::open(&slot_path(id, "q"))?,
      response: Slot::open(&slot_path(id, "a"))?,
      id: id.to_string(),
      created: false,
    })
  }

  pub fn id(&self) -> &str {
    &self.id
  }

  /// Drops the directory entries. Mappings stay valid.
  pub fn unlink(&self) {
    let _ = std::fs::remove_file(slot_path(&self.id, "q"));
    let _ = std::fs::remove_file(slot_path(&self.id, "a"));
  }

  pub fn write_request(&mut self, bytes: &[u8], to_peer: &mut impl Write, from_peer: &mut impl Read) -> Result<()> {
    self.request.write(bytes, to_peer, from_peer)
  }

  pub fn read_request(&mut self, from_peer: &mut impl Read, to_peer: &mut impl Write) -> Result<Vec<u8>> {
    self.request.read(from_peer, to_peer)
  }

  pub fn write_response(&mut self, bytes: &[u8], to_peer: &mut impl Write, from_peer: &mut impl Read) -> Result<()> {
    self.response.write(bytes, to_peer, from_peer)
  }

  pub fn read_response(&mut self, from_peer: &mut impl Read, to_peer: &mut impl Write) -> Result<Vec<u8>> {
    self.response.read(from_peer, to_peer)
  }
}

impl Drop for Link {
  fn drop(&mut self) {
    if self.created {
      self.unlink();
    }
  }
}

impl Slot {
  fn create(path: &Path, len: usize) -> Result<Self> {
    if let Some(parent) = path.parent() {
      std::fs::create_dir_all(parent)?;
    }
    let file = OpenOptions::new().read(true).write(true).create(true).truncate(true).open(path)?;
    file.set_len(len as u64)?;
    let mut slot = Slot {
      path: path.to_path_buf(),
      map: unsafe { MmapMut::map_mut(&file)? },
      file,
    };
    slot.put_header(0, 0, len as u64);
    Ok(slot)
  }

  fn open(path: &Path) -> Result<Self> {
    let file = OpenOptions::new().read(true).write(true).open(path)?;
    let len = file.metadata()?.len() as usize;
    Ok(Slot {
      path: path.to_path_buf(),
      map: unsafe { MmapMut::map_mut(&file)? },
      file,
    })
    .and_then(|slot| {
      if slot.map.len() != len {
        bail!("mapping length mismatch for {}", path.display());
      }
      Ok(slot)
    })
  }

  fn write(&mut self, bytes: &[u8], to_peer: &mut impl Write, from_peer: &mut impl Read) -> Result<()> {
    let need = HEADER + bytes.len();
    if need > self.map.len() {
      let mut cap = self.map.len().max(INITIAL);
      while cap < need {
        if cap >= MAX {
          bail!("protobuf message of {} bytes exceeds the shared mapping", bytes.len());
        }
        cap = cap.saturating_mul(2).min(MAX);
      }
      self.file.set_len(cap as u64)?;
      self.put_header(GROW as u32, cap as u64, cap as u64);
      fence(Ordering::Release);
      ping(to_peer, GROW)?;
      let ack = read_ping(from_peer)?;
      if ack != GROW {
        bail!("shared mapping grow was not acknowledged");
      }
      self.remap(cap)?;
    }
    self.map[HEADER..HEADER + bytes.len()].copy_from_slice(bytes);
    self.put_header(0, bytes.len() as u64, self.map.len() as u64);
    fence(Ordering::Release);
    ping(to_peer, DATA)?;
    Ok(())
  }

  fn read(&mut self, from_peer: &mut impl Read, to_peer: &mut impl Write) -> Result<Vec<u8>> {
    loop {
      match read_ping(from_peer)? {
        GROW => {
          let cap = self.header_len()? as usize;
          self.file.set_len(cap as u64)?;
          self.remap(cap)?;
          fence(Ordering::Release);
          ping(to_peer, GROW)?;
        }
        DATA => {
          fence(Ordering::Acquire);
          let len = self.header_len()? as usize;
          if HEADER + len > self.map.len() {
            bail!("payload of {len} bytes does not fit the shared mapping");
          }
          return Ok(self.map[HEADER..HEADER + len].to_vec());
        }
        other => bail!("unexpected shared-memory ping {other}"),
      }
    }
  }

  fn remap(&mut self, len: usize) -> Result<()> {
    self.map = unsafe { MmapMut::map_mut(&self.file)? };
    if self.map.len() < len {
      bail!("remapped {} is shorter than {len}", self.path.display());
    }
    Ok(())
  }

  fn put_header(&mut self, kind: u32, len: u64, cap: u64) {
    self.map[0..4].copy_from_slice(&MAGIC.to_le_bytes());
    self.map[4..8].copy_from_slice(&kind.to_le_bytes());
    self.map[8..16].copy_from_slice(&len.to_le_bytes());
    self.map[16..24].copy_from_slice(&cap.to_le_bytes());
  }

  fn header_len(&self) -> Result<u64> {
    let magic = u32::from_le_bytes(self.map[0..4].try_into().unwrap());
    if magic != MAGIC {
      bail!("shared mapping has no bridge header");
    }
    Ok(u64::from_le_bytes(self.map[8..16].try_into().unwrap()))
  }
}

fn ping(to_peer: &mut impl Write, byte: u8) -> Result<()> {
  to_peer.write_all(&[byte])?;
  to_peer.flush()?;
  Ok(())
}

fn read_ping(from_peer: &mut impl Read) -> Result<u8> {
  let mut byte = [0u8; 1];
  from_peer.read_exact(&mut byte)?;
  Ok(byte[0])
}

pub fn slot_dir() -> PathBuf {
  let shm = PathBuf::from("/dev/shm");
  if shm.is_dir() { shm } else { std::env::temp_dir() }
}

fn slot_path(id: &str, which: &str) -> PathBuf {
  slot_dir().join(format!("sjb-{id}-{which}"))
}
