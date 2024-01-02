/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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
package org.sonar.java.caching;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.sonar.api.batch.sensor.cache.WriteCache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class JavaWriteCacheImplTest {

  @Test
  void write_bytes_proxies_the_expected_value() {
    String key = "key";
    byte[] data = "message".getBytes(StandardCharsets.UTF_8);
    WriteCache writeCache = mock(WriteCache.class);

    verify(writeCache, never()).write(key, data);

    doThrow(new IllegalArgumentException("boom")).when(writeCache).write(key, data);
    JavaWriteCacheImpl cache = new JavaWriteCacheImpl(writeCache);
    assertThatThrownBy(() -> cache.write(key, data))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("boom");
    verify(writeCache, times(1)).write(key, data);

    doNothing().when(writeCache).write(key, data);
    cache.write(key, data);
    verify(writeCache, times(2)).write(key, data);
  }

  @Test
  void write_InputStream_proxies_the_expected_value() {
    String key = "key";
    InputStream data = new ByteArrayInputStream("message".getBytes(StandardCharsets.UTF_8));
    WriteCache writeCache = mock(WriteCache.class);

    verify(writeCache, never()).write(key, data);
    doThrow(new IllegalArgumentException("boom")).when(writeCache).write(key, data);
    JavaWriteCacheImpl cache = new JavaWriteCacheImpl(writeCache);
    assertThatThrownBy(() -> cache.write(key, data))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("boom");
    verify(writeCache, times(1)).write(key, data);

    doNothing().when(writeCache).write(key, data);
    cache.write(key, data);
    verify(writeCache, times(2)).write(key, data);
  }

  @Test
  void copyFromPrevious_proxies_the_expected_value() {
    String key = "key";
    WriteCache writeCache = mock(WriteCache.class);

    verify(writeCache, never()).copyFromPrevious(key);

    doThrow(new IllegalArgumentException("boom")).when(writeCache).copyFromPrevious(key);
    JavaWriteCacheImpl cache = new JavaWriteCacheImpl(writeCache);
    assertThatThrownBy(() -> cache.copyFromPrevious(key))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("boom");
    verify(writeCache, times(1)).copyFromPrevious(key);

    doNothing().when(writeCache).copyFromPrevious(key);
    cache.copyFromPrevious(key);
    verify(writeCache, times(2)).copyFromPrevious(key);
  }

  @Test
  void equality_is_only_based_on_WriteCache_field() {
    WriteCache writeCache = mock(WriteCache.class);
    assertThat(new JavaWriteCacheImpl(writeCache)).hasSameHashCodeAs(new JavaWriteCacheImpl(writeCache));
    assertThat(new JavaWriteCacheImpl(writeCache)).isEqualTo(new JavaWriteCacheImpl(writeCache));
  }
}
