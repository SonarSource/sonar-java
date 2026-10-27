#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

cargo build --release --locked
host=$(rustc -vV | awk '/^host: / { print $2 }')
case "$host" in
  x86_64-unknown-linux-gnu|x86_64-unknown-linux-musl) name=bridge-linux-x64 ;;
  aarch64-unknown-linux-gnu|aarch64-unknown-linux-musl) name=bridge-linux-aarch64 ;;
  x86_64-pc-windows-gnu|x86_64-pc-windows-msvc) name=bridge-windows-x64 ;;
  x86_64-apple-darwin) name=bridge-macos-x64 ;;
  aarch64-apple-darwin) name=bridge-macos-aarch64 ;;
  *) echo "Unknown target: $host" >&2; exit 1 ;;
esac

target_dir="${CARGO_TARGET_DIR:-$PWD/target}"
mkdir -p "$PWD/target/release"
cp -f "${target_dir}/release/semsitter-bridge" "$PWD/target/release/bridge"
tar -cJf "$PWD/target/release/${name}.xz" -C "$PWD/target/release" bridge
