/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.checks.verifier.internal;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

class InternalWriteCacheTest {

  @Test
  void write_throws_an_IllegalArgumentException_when_writing_over_existing_key() {
    InternalWriteCache cache = new InternalWriteCache();
    final byte[] data = "message".getBytes(StandardCharsets.UTF_8);
    cache.write("key", data);
    assertThatThrownBy(() -> cache.write("key", data))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Same key cannot be written to multiple times (key)");
  }

  @Test
  void write_InputStream_throws_an_IllegalArgumentException_when_writing_over_existing_key() {
    InternalWriteCache cache = new InternalWriteCache();
    final byte[] data = "message".getBytes(StandardCharsets.UTF_8);
    InputStream in = new ByteArrayInputStream(data);
    cache.write("key", data);
    assertThatThrownBy(() -> cache.write("key", in))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Same key cannot be written to multiple times (key)");
  }

  @Test
  void write_InputStream_throws_an_IllegalStateException_when_an_IOException_occurs() throws IOException {
    InternalWriteCache cache = new InternalWriteCache();
    InputStream in = mock(InputStream.class);
    doThrow(new IOException()).when(in).readAllBytes();

    assertThatThrownBy(() -> cache.write("key", in))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Failed to read stream");
  }

  @Test
  void copyFromPrevious_thros_an_IllegalArgumentException_if_key_does_not_exist() {
    InternalWriteCache cache = new InternalWriteCache();
    cache.bind(new InternalReadCache());

    assertThatThrownBy(() -> cache.copyFromPrevious("key"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("cache does not contain key \"key\"");
  }

  @Test
  void copyFromPrevious_copies_from_readCache() {
    byte[] data = "message".getBytes(StandardCharsets.UTF_8);
    InternalReadCache readCache = new InternalReadCache().put("key", data);
    InternalWriteCache cache = new InternalWriteCache().bind(readCache);
    assertThat(cache.getData()).doesNotContainKey("key");
    cache.copyFromPrevious("key");
    assertThat(cache.getData()).containsEntry("key", data);
  }
}
