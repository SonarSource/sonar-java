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
package org.sonar.java.caching;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.sonar.api.batch.sensor.cache.ReadCache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class JavaReadCacheImplTest {
  @Test
  void read_proxies_the_expected_value() {
    String key = "a convenient key";
    String missingKey = "non existing key";
    byte[] data = "Hello".getBytes(StandardCharsets.UTF_8);
    ReadCache readCache = mock(ReadCache.class);

    doReturn(new ByteArrayInputStream(data)).when(readCache).read(eq(key));
    try (InputStream read = new JavaReadCacheImpl(readCache).read(key)) {
      assertThat(read).hasBinaryContent(data);
    } catch (IOException e) {
      fail("This is not expected");
    }
    verify(readCache, times(1)).read(eq(key));

    doThrow(new IllegalArgumentException("boom")).when(readCache).read(eq(missingKey));
    assertThatThrownBy(() -> new JavaReadCacheImpl(readCache).read(missingKey))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("boom");
    verify(readCache, times(1)).read(eq(missingKey));
  }

  @Test
  void contains_proxies_the_expected_value() {
    String key = "key";
    String missingKey = "missing";
    ReadCache readCache = mock(ReadCache.class);

    doReturn(true).when(readCache).contains(eq(key));
    assertThat(new JavaReadCacheImpl(readCache).contains(key)).isTrue();
    verify(readCache, times(1)).contains(eq(key));

    doReturn(false).when(readCache).contains(eq(missingKey));
    assertThat(new JavaReadCacheImpl(readCache).contains(missingKey)).isFalse();
    verify(readCache, times(1)).contains(eq(missingKey));
  }
}
