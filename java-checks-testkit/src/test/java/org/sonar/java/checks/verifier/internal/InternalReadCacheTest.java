/*
 * SonarQube Java
 * Copyright (C) 2012-2022 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.java.checks.verifier.internal;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

class InternalReadCacheTest {

  @Test
  void readCache_returns_the_expected_value() {
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
}
