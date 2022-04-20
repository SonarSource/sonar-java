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

import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.cache.ReadCache;
import org.sonar.api.batch.sensor.cache.WriteCache;
import org.sonar.api.utils.log.LogAndArguments;
import org.sonar.api.utils.log.LogTesterJUnit5;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.api.utils.log.Loggers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

class CacheContextImplTest {
  @RegisterExtension
  LogTesterJUnit5 logTester = new LogTesterJUnit5();

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

  @Test
  void of_logs_at_debug_level_when_the_api_is_not_supported() {
    Logger logger = Loggers.get(CacheContextImpl.class);
    logger.setLevel(LoggerLevel.DEBUG);
    SensorContext sensorContext = mock(SensorContext.class);
    doThrow(new NoSuchMethodError("bim")).when(sensorContext).isCacheEnabled();
    CacheContextImpl.of(sensorContext);
    List<String> logs = logTester.getLogs(LoggerLevel.DEBUG).stream()
      .map(LogAndArguments::getFormattedMsg)
      .collect(Collectors.toList());
    assertThat(logs)
      .hasSize(1)
      .contains("Missing cache related method from sonar-plugin-api: bim.");
  }
}
