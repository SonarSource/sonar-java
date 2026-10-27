#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

if [[ -n "${TARGETS:-}" ]]; then
  mkdir -p target/release
  for target in $TARGETS; do
    case "$target" in
      x86_64-unknown-linux-musl)  name=bridge-linux-x64;     bin=bridge     ;;
      aarch64-unknown-linux-musl) name=bridge-linux-aarch64; bin=bridge     ;;
      x86_64-pc-windows-gnu)      name=bridge-windows-x64;   bin=bridge.exe ;;
      x86_64-apple-darwin)        name=bridge-macos-x64;     bin=bridge     ;;
      aarch64-apple-darwin)       name=bridge-macos-aarch64; bin=bridge     ;;
      *) echo "Unknown target: $target" >&2; exit 1 ;;
    esac
    tar -cJf "target/release/${name}.xz" -C "target/${target}/release" "${bin}"
  done
  exit 0
fi

cargo build --release --locked
host=$(rustc -vV | awk '/^host: / { print $2 }')
case "$host" in
  x86_64-unknown-linux-gnu|x86_64-unknown-linux-musl) name=bridge-linux-x64; bin=bridge; built=semsitter-bridge ;;
  aarch64-unknown-linux-gnu|aarch64-unknown-linux-musl) name=bridge-linux-aarch64; bin=bridge; built=semsitter-bridge ;;
  x86_64-pc-windows-gnu|x86_64-pc-windows-msvc) name=bridge-windows-x64; bin=bridge.exe; built=semsitter-bridge.exe ;;
  x86_64-apple-darwin) name=bridge-macos-x64; bin=bridge; built=semsitter-bridge ;;
  aarch64-apple-darwin) name=bridge-macos-aarch64; bin=bridge; built=semsitter-bridge ;;
  *) echo "Unknown target: $host" >&2; exit 1 ;;
esac
cargo_target="${CARGO_TARGET_DIR:-$PWD/target}"
mkdir -p "$PWD/target/release"
cp -f "${cargo_target}/release/${built}" "$PWD/target/release/${bin}"
tar -cJf "$PWD/target/release/${name}.xz" -C "$PWD/target/release" "${bin}"
