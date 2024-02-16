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

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.event.Level;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.cache.ReadCache;
import org.sonar.api.batch.sensor.cache.WriteCache;
import org.sonar.api.config.Configuration;
import org.sonar.api.testfixtures.log.LogAndArguments;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

class CacheContextImplTest {
  @RegisterExtension
  LogTesterJUnit5 logTester = new LogTesterJUnit5().setLevel(Level.DEBUG);

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

    assertThat(cci.getReadCache()).isInstanceOf(DummyCache.class);
    assertThat(cci.getWriteCache()).isInstanceOf(DummyCache.class);
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
    logTester.setLevel(Level.DEBUG);
    SensorContext sensorContext = mock(SensorContext.class);
    doThrow(new NoSuchMethodError("bim")).when(sensorContext).isCacheEnabled();
    CacheContextImpl.of(sensorContext);
    List<String> logs = logTester.getLogs(Level.DEBUG).stream()
      .map(LogAndArguments::getFormattedMsg)
      .toList();
    assertThat(logs)
      .hasSize(1)
      .contains("Missing cache related method from sonar-plugin-api: bim.");
  }

  @Test
  void override_flag_caching_enabled_true() {
    SensorContext context = mock(SensorContext.class);
    doReturn(false).when(context).isCacheEnabled();
    doReturn(mock(ReadCache.class)).when(context).previousCache();
    doReturn(mock(WriteCache.class)).when(context).nextCache();

    Configuration config = mock(Configuration.class);
    doReturn(config).when(context).config();

    assertThat(CacheContextImpl.of(context).isCacheEnabled()).isFalse();
    doReturn(Optional.of(true)).when(config).getBoolean(CacheContextImpl.SONAR_CACHING_ENABLED_KEY);
    assertThat(CacheContextImpl.of(context).isCacheEnabled()).isTrue();
  }

  @Test
  void override_flag_caching_enabled_false() {
    SensorContext context = mock(SensorContext.class);
    doReturn(true).when(context).isCacheEnabled();
    doReturn(mock(ReadCache.class)).when(context).previousCache();
    doReturn(mock(WriteCache.class)).when(context).nextCache();

    Configuration config = mock(Configuration.class);
    doReturn(config).when(context).config();

    assertThat(CacheContextImpl.of(context).isCacheEnabled()).isTrue();
    doReturn(Optional.of(false)).when(config).getBoolean(CacheContextImpl.SONAR_CACHING_ENABLED_KEY);
    assertThat(CacheContextImpl.of(context).isCacheEnabled()).isFalse();
  }
}
