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
package org.sonar.java.semsitter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Pair of shared mappings the bridge creates for one process. Payloads stay in the mappings.
 * The pipe only carries a one-byte ping: {@code 1} data is ready, {@code 2} the mapping grew.
 */
final class SharedSlots implements AutoCloseable {

  static final byte READY = 'R';
  static final byte ATTACHED = 'A';
  static final byte DATA = 1;
  static final byte GROW = 2;

  private static final int HEADER = 24;
  private static final int MAGIC = 0x31424A53;
  private static final int MAX = 512 * 1024 * 1024;

  private final RandomAccessFile requestFile;
  private final RandomAccessFile responseFile;
  private MappedByteBuffer request;
  private MappedByteBuffer response;

  private SharedSlots(RandomAccessFile requestFile, MappedByteBuffer request, RandomAccessFile responseFile, MappedByteBuffer response) {
    this.requestFile = requestFile;
    this.request = request;
    this.responseFile = responseFile;
    this.response = response;
  }

  static SharedSlots open(long pid) throws IOException {
    Path dir = Files.isDirectory(Path.of("/dev/shm")) ? Path.of("/dev/shm") : Path.of(System.getProperty("java.io.tmpdir"));
    RandomAccessFile requestFile = new RandomAccessFile(dir.resolve("sjb-" + pid + "-q").toFile(), "rw");
    RandomAccessFile responseFile = new RandomAccessFile(dir.resolve("sjb-" + pid + "-a").toFile(), "rw");
    return new SharedSlots(requestFile, map(requestFile), responseFile, map(responseFile));
  }

  void writeRequest(byte[] bytes, OutputStream toBridge, InputStream fromBridge) throws IOException {
    request = write(requestFile, request, bytes, toBridge, fromBridge);
  }

  byte[] readResponse(InputStream fromBridge, OutputStream toBridge) throws IOException {
    Read read = read(responseFile, response, fromBridge, toBridge);
    response = read.buffer;
    return read.payload;
  }

  @Override
  public void close() throws IOException {
    requestFile.close();
    responseFile.close();
  }

  private static MappedByteBuffer write(RandomAccessFile file, MappedByteBuffer buffer, byte[] bytes, OutputStream toPeer, InputStream fromPeer) throws IOException {
    int need = HEADER + bytes.length;
    if (need > buffer.capacity()) {
      int cap = buffer.capacity();
      while (cap < need) {
        if (cap >= MAX) {
          throw new IOException("protobuf message of " + bytes.length + " bytes exceeds the shared mapping");
        }
        cap = Math.min(cap * 2, MAX);
      }
      file.setLength(cap);
      header(buffer, GROW, cap, cap);
      ping(toPeer, GROW);
      if (readPing(fromPeer) != GROW) {
        throw new IOException("shared mapping grow was not acknowledged");
      }
      buffer = map(file);
    }
    buffer.position(HEADER);
    buffer.put(bytes);
    header(buffer, (byte) 0, bytes.length, buffer.capacity());
    ping(toPeer, DATA);
    return buffer;
  }

  private static Read read(RandomAccessFile file, MappedByteBuffer buffer, InputStream fromPeer, OutputStream toPeer) throws IOException {
    while (true) {
      switch (readPing(fromPeer)) {
        case GROW -> {
          int cap = (int) len(buffer);
          file.setLength(cap);
          buffer = map(file);
          ping(toPeer, GROW);
        }
        case DATA -> {
          int size = (int) len(buffer);
          if (HEADER + size > buffer.capacity()) {
            throw new IOException("payload of " + size + " bytes does not fit the shared mapping");
          }
          byte[] payload = new byte[size];
          buffer.position(HEADER);
          buffer.get(payload);
          return new Read(buffer, payload);
        }
        default -> throw new IOException("unexpected shared-memory ping");
      }
    }
  }

  private static void header(MappedByteBuffer buffer, int kind, long len, long cap) {
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(0, MAGIC);
    buffer.putInt(4, kind);
    buffer.putLong(8, len);
    buffer.putLong(16, cap);
  }

  private static long len(MappedByteBuffer buffer) throws IOException {
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    if (buffer.getInt(0) != MAGIC) {
      throw new IOException("shared mapping has no bridge header");
    }
    return buffer.getLong(8);
  }

  private static MappedByteBuffer map(RandomAccessFile file) throws IOException {
    MappedByteBuffer buffer = file.getChannel().map(FileChannel.MapMode.READ_WRITE, 0, file.length());
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    return buffer;
  }

  private static void ping(OutputStream toPeer, byte value) throws IOException {
    toPeer.write(value);
    toPeer.flush();
  }

  private static byte readPing(InputStream fromPeer) throws IOException {
    int value = fromPeer.read();
    if (value < 0) {
      throw new IOException("semsitter bridge closed the ping pipe");
    }
    return (byte) value;
  }

  private record Read(MappedByteBuffer buffer, byte[] payload) {
  }
}
