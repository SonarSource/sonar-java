#!/usr/bin/env bash
# Regenerates prebuilt/bridge-<os>-<arch>.xz, packaged in the plugin JAR so CI needs neither cargo nor
# access to the private semsitter repository. Run on an Apple silicon Mac with Docker and SSH access to GitHub.
set -euo pipefail

cd "$(dirname "$0")"
work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT

export GIT_CONFIG_COUNT=1 GIT_CONFIG_KEY_0=url.ssh://git@github.com/.insteadOf GIT_CONFIG_VALUE_0=https://github.com/
export CARGO_NET_GIT_FETCH_WITH_CLI=true
target_dir="${CARGO_TARGET_DIR:-$PWD/target}"

cargo vendor --locked "$work/vendor" | sed "s#$work/vendor#/vendor#" > "$work/config.toml"
mkdir -p "$work/out"

docker_build() {
  local platform=$1 image=$2 setup=$3 target=$4 out=$5
  docker run --rm --platform "$platform" \
    -v "$PWD:/src:ro" -v "$work/vendor:/vendor:ro" -v "$work/config.toml:/usr/local/cargo/config.toml:ro" -v "$work/out:/out" \
    -e CARGO_TARGET_DIR=/tmp/target "$image" \
    sh -c "$setup && cd /src && cargo build --release --locked --offline $target && cp \$(ls /tmp/target/${target:+${target#--target }/}release/semsitter-bridge /tmp/target/${target:+${target#--target }/}release/semsitter-bridge.exe 2>/dev/null) /out/$out"
}

docker_build linux/amd64 rust:1-alpine "apk add --no-cache musl-dev" "" linux-x64
docker_build linux/arm64 rust:1-alpine "apk add --no-cache musl-dev" "" linux-aarch64
docker_build linux/arm64 rust:1 "apt-get update -qq && apt-get install -y -qq gcc-mingw-w64-x86-64 && rustup target add x86_64-pc-windows-gnu" \
  "--target x86_64-pc-windows-gnu" windows-x64

rustup target add x86_64-apple-darwin
cargo build --release --locked
cargo build --release --locked --target x86_64-apple-darwin
cp "$target_dir/release/semsitter-bridge" "$work/out/macos-aarch64"
cp "$target_dir/x86_64-apple-darwin/release/semsitter-bridge" "$work/out/macos-x64"

mkdir -p prebuilt
for os in linux-x64 linux-aarch64 macos-x64 macos-aarch64 windows-x64; do
  bin=bridge
  [[ $os == windows-x64 ]] && bin=bridge.exe
  mkdir -p "$work/$os"
  install -m 755 "$work/out/$os" "$work/$os/$bin"
  tar -cJf "prebuilt/bridge-$os.xz" -C "$work/$os" "$bin"
done
