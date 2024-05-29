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
import javax.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.event.Level;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.cache.ReadCache;
import org.sonar.api.batch.sensor.cache.WriteCache;
import org.sonar.api.config.Configuration;
import org.sonar.api.testfixtures.log.LogAndArguments;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;
import org.sonar.java.SonarComponents;
import org.sonar.plugins.java.api.caching.CacheContext;
import org.sonar.plugins.java.api.caching.SonarLintCache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class CacheContextImplTest {
  @RegisterExtension
  LogTesterJUnit5 logTester = new LogTesterJUnit5().setLevel(Level.DEBUG);

  @Test
  void should_use_dummy_cache_if_sonarcomponents_is_unavailable() {
    var cci = CacheContextImpl.of(null);

    verifyCacheContextUsesDummyCache(cci);
  }

  @Test
  void should_use_dummy_cache_if_neither_sonarlint_cache_nor_sensor_context_cache_are_available() {
    var sonarComponents = mockSonarComponents(null, null);
    var cci = CacheContextImpl.of(sonarComponents);

    verifyCacheContextUsesDummyCache(cci);
  }

  @Test
  void isCacheEnabled_returns_true_when_context_implements_isCacheEnabled_and_is_true() {
    var sensorContext = mockSensorContext();
    doReturn(true).when(sensorContext).isCacheEnabled();

    var sonarComponents = mockSonarComponents(sensorContext, null);

    CacheContextImpl cci = CacheContextImpl.of(sonarComponents);
    assertThat(cci.isCacheEnabled()).isTrue();

    assertThat(cci.getReadCache()).isEqualTo(new JavaReadCacheImpl(sensorContext.previousCache()));
    assertThat(cci.getWriteCache()).isEqualTo(new JavaWriteCacheImpl(sensorContext.nextCache()));
  }

  @Test
  void isCacheEnabled_returns_false_when_appropriate() {
    var sensorContext = mockSensorContext();
    doReturn(false).when(sensorContext).isCacheEnabled();

    var sonarComponents = mockSonarComponents(sensorContext, null);

    CacheContextImpl cci = CacheContextImpl.of(sonarComponents);
    assertThat(cci.isCacheEnabled()).isFalse();

    verifyCacheContextUsesDummyCache(cci);
  }

  @Test
  void isCacheEnabled_returns_false_in_case_of_api_mismatch() {
    var sensorContext = mockSensorContext();
    doThrow(new NoSuchMethodError("boom")).when(sensorContext).isCacheEnabled();

    var sonarComponents = mockSonarComponents(sensorContext, null);

    CacheContextImpl cci = CacheContextImpl.of(sonarComponents);
    assertThat(cci.isCacheEnabled()).isFalse();

    verifyCacheContextUsesDummyCache(cci);
  }

  @Test
  void of_logs_at_debug_level_when_the_api_is_not_supported() {
    logTester.setLevel(Level.DEBUG);

    var sensorContext = mockSensorContext();
    doThrow(new NoSuchMethodError("bim")).when(sensorContext).isCacheEnabled();

    var sonarComponents = mockSonarComponents(sensorContext, null);

    CacheContextImpl.of(sonarComponents);
    List<String> logs = logTester.getLogs(Level.DEBUG).stream()
      .map(LogAndArguments::getFormattedMsg)
      .toList();
    assertThat(logs)
      .hasSize(1)
      .contains("Missing cache related method from sonar-plugin-api: bim.");
  }

  @Test
  void override_flag_caching_enabled_true() {
    var context = mockSensorContext();
    doReturn(false).when(context).isCacheEnabled();
    Configuration config = mock(Configuration.class);
    doReturn(config).when(context).config();

    var sonarComponents = mockSonarComponents(context, null);

    assertThat(CacheContextImpl.of(sonarComponents).isCacheEnabled()).isFalse();
    doReturn(Optional.of(true)).when(config).getBoolean(CacheContextImpl.SONAR_CACHING_ENABLED_KEY);
    assertThat(CacheContextImpl.of(sonarComponents).isCacheEnabled()).isTrue();
  }

  @Test
  void override_flag_caching_enabled_false() {
    var context = mockSensorContext();
    doReturn(true).when(context).isCacheEnabled();
    Configuration config = mock(Configuration.class);
    doReturn(config).when(context).config();

    var sonarComponents = mockSonarComponents(context, null);

    assertThat(CacheContextImpl.of(sonarComponents).isCacheEnabled()).isTrue();
    doReturn(Optional.of(false)).when(config).getBoolean(CacheContextImpl.SONAR_CACHING_ENABLED_KEY);
    assertThat(CacheContextImpl.of(sonarComponents).isCacheEnabled()).isFalse();
  }

  @Test
  void should_use_sonarlint_cache_when_available() {
    var sonarLintCache = mock(SonarLintCache.class);
    var sonarComponents = mockSonarComponents(null, sonarLintCache);

    CacheContextImpl cci = CacheContextImpl.of(sonarComponents);
    verifyCacheContextUsesSonarLintCache(cci, sonarLintCache);
  }

  @Test
  void should_prefer_sonarlint_cache_when_sensor_context_also_offers_caching() {
    var sonarLintCache = mock(SonarLintCache.class);

    var sensorContext = mockSensorContext();
    doReturn(true).when(sensorContext).isCacheEnabled();

    var sonarComponents = mockSonarComponents(sensorContext, sonarLintCache);

    CacheContextImpl cci = CacheContextImpl.of(sonarComponents);
    verifyCacheContextUsesSonarLintCache(cci, sonarLintCache);
  }

  @Test
  void should_use_sonarlint_cache_even_when_caching_is_disabled_in_the_sensor_context() {
    var sonarLintCache = mock(SonarLintCache.class);

    var sensorContext = mockSensorContext();
    doReturn(false).when(sensorContext).isCacheEnabled();

    var sonarComponents = mockSonarComponents(sensorContext, sonarLintCache);

    CacheContextImpl cci = CacheContextImpl.of(sonarComponents);
    verifyCacheContextUsesSonarLintCache(cci, sonarLintCache);
  }

  @Test
  void should_use_sonarlint_cache_even_when_caching_is_disabled_by_the_configuration() {
    var sonarLintCache = mock(SonarLintCache.class);

    var config = mock(Configuration.class);
    doReturn(Optional.of(false)).when(config).getBoolean(CacheContextImpl.SONAR_CACHING_ENABLED_KEY);

    var sensorContext = mockSensorContext();
    doReturn(false).when(sensorContext).isCacheEnabled();
    doReturn(config).when(sensorContext).config();

    var sonarComponents = mockSonarComponents(sensorContext, sonarLintCache);

    CacheContextImpl cci = CacheContextImpl.of(sonarComponents);
    verifyCacheContextUsesSonarLintCache(cci, sonarLintCache);
  }

  private SensorContext mockSensorContext() {
    SensorContext sensorContext = mock(SensorContext.class);
    var readCache = mock(ReadCache.class);
    doReturn(readCache).when(sensorContext).previousCache();
    var writeCache = mock(WriteCache.class);
    doReturn(writeCache).when(sensorContext).nextCache();

    return sensorContext;
  }

  private SonarComponents mockSonarComponents(@Nullable SensorContext sensorContext, @Nullable SonarLintCache sonarLintCache) {
    var sonarComponents = mock(SonarComponents.class);
    doReturn(sensorContext).when(sonarComponents).context();
    doReturn(sonarLintCache).when(sonarComponents).sonarLintCache();

    return sonarComponents;
  }

  private static void verifyCacheContextUsesDummyCache(CacheContext cacheContext) {
    assertThat(cacheContext.isCacheEnabled()).isFalse();

    assertThat(cacheContext.getReadCache())
      .isInstanceOf(DummyCache.class)
      .isSameAs(cacheContext.getWriteCache());
  }

  private static void verifyCacheContextUsesSonarLintCache(CacheContext cacheContext, SonarLintCache sonarLintCache) {
    // CacheContext does not expose the underlying cache.
    // Hence, we have to test whether the read/write behaviour that it is exposing will affect the sonarLintCache.

    cacheContext.getReadCache().read("key");
    verify(sonarLintCache, times(1)).read("key");

    var bytes = new byte[0];
    cacheContext.getWriteCache().write("key", bytes);
    verify(sonarLintCache, times(1)).write("key", bytes);
  }
}
