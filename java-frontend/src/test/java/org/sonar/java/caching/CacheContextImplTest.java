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

import org.junit.jupiter.api.Test;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.cache.ReadCache;
import org.sonar.api.batch.sensor.cache.WriteCache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class CacheContextImplTest {
  @Test
  void isCacheEnabled_returns_true_when_context_implements_isCacheEnabled_and_is_true() {
    SensorContext sensorContext = mock(SensorContext.class);
    doReturn(true).when(sensorContext).isCacheEnabled();
    var readCache = mock(ReadCache.class);
    doReturn(readCache).when(sensorContext).previousCache();
    var writeCache = mock(WriteCache.class);
    doReturn(writeCache).when(sensorContext).nextCache();

    CacheContextImpl cci = CacheContextImpl.of(sensorContext);
    assertThat(cci.isCacheEnabled()).isTrue();

    assertThat(cci.getReadCache()).isEqualTo(new JavaReadCacheImpl(readCache));
    assertThat(cci.getWriteCache()).isEqualTo(new JavaWriteCacheImpl(writeCache));
  }

  @Test
  void isCacheEnabled_returns_false_when_appropriate() {
    SensorContext sensorContext = mock(SensorContext.class);
    doReturn(false).when(sensorContext).isCacheEnabled();

    var readCache = mock(ReadCache.class);
    doReturn(readCache).when(sensorContext).previousCache();
    var writeCache = mock(WriteCache.class);
    doReturn(writeCache).when(sensorContext).nextCache();

    CacheContextImpl cci = CacheContextImpl.of(sensorContext);
    assertThat(cci.isCacheEnabled()).isFalse();

    assertThat(cci.getReadCache()).isEqualTo(new JavaReadCacheImpl(readCache));
    assertThat(cci.getWriteCache()).isEqualTo(new JavaWriteCacheImpl(writeCache));
  }

  @Test
  void isCacheEnabled_returns_false_in_case_of_api_mismatch() {
    SensorContext sensorContext = mock(SensorContext.class);
    doThrow(new NoSuchMethodError("boom")).when(sensorContext).isCacheEnabled();

    CacheContextImpl cci = CacheContextImpl.of(sensorContext);
    assertThat(cci.isCacheEnabled()).isFalse();

    assertThat(cci.getReadCache())
      .isInstanceOf(DummyCache.class)
      .isSameAs(cci.getWriteCache());
  }

}
