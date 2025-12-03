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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.fail;

class InternalReadCacheTest {

  @Test
  void read_returns_the_expected_value() {
    InternalReadCache cache = new InternalReadCache();
    String key = "a convenient key";
    byte[] data = "Some data".getBytes(StandardCharsets.UTF_8);
    cache.put(key, data);
    try (InputStream read = cache.read(key)) {
      assertThat(read).hasBinaryContent(data);
    } catch (IOException e) {
      fail("This is not expected");
    }
  }

  @Test
  void read_returns_an_empty_InputStream_when_value_is_null() {
    InternalReadCache cache = new InternalReadCache();
    String key = "an existing key";
    cache.put(key, null);
    assertThat(cache.read(key))
      .isInstanceOf(InputStream.class)
      .isEmpty();
  }

  @Test
  void read_throws_an_IllegalArgumentException_when_the_key_does_not_match_anything() {
    InternalReadCache cache = new InternalReadCache();
    assertThatThrownBy(() -> cache.read("non existing key"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("cache does not contain key \"non existing key\"");
  }

  @Test
  void contains_returns_true_when_key_is_present() {
    InternalReadCache cache = new InternalReadCache();
    cache.put("key", "data".getBytes(StandardCharsets.UTF_8));
    assertThat(cache.contains("key")).isTrue();
  }

  @Test
  void contains_returns_false_when_key_is_absent() {
    InternalReadCache cache = new InternalReadCache();
    assertThat(cache.contains("non existing key")).isFalse();
  }

  @Test
  void putAll_from_writeCache() {
    var readCache = new InternalReadCache();
    var writeCache = new InternalWriteCache();

    var value = new byte[]{'f', 'o', 'o'};
    var key = "some key";
    writeCache.write(key, value);

    assertThat(readCache.contains(key)).isFalse();
    readCache.putAll(writeCache);
    assertThat(readCache.contains(key)).isTrue();
  }
}
