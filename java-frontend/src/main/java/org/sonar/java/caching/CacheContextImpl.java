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

import java.util.Optional;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.java.SonarComponents;
import org.sonar.plugins.java.api.caching.CacheContext;
import org.sonar.plugins.java.api.caching.JavaReadCache;
import org.sonar.plugins.java.api.caching.JavaWriteCache;
import org.sonar.plugins.java.api.caching.SonarLintCache;

public class CacheContextImpl implements CacheContext {
  /**
   * Can be set to {@code true} or {@code false} to override whether the cache is enabled or not. Note that even if this is set to true,
   * the engine may not use caching anyway, if the server does not support it.
   */
  public static final String SONAR_CACHING_ENABLED_KEY = "sonar.java.caching.enabled";

  private static final Logger LOGGER = LoggerFactory.getLogger(CacheContextImpl.class);

  private final boolean isCacheEnabled;
  private final JavaReadCache readCache;
  private final JavaWriteCache writeCache;

  private CacheContextImpl(boolean isCacheEnabled, JavaReadCache readCache, JavaWriteCache writeCache) {
    this.isCacheEnabled = isCacheEnabled;
    this.readCache = readCache;
    this.writeCache = writeCache;
  }

  public static CacheContextImpl of(@Nullable SonarComponents sonarComponents) {
    if (sonarComponents == null) {
      return dummyCache();
    }

    var sensorContext = sonarComponents.context();
    if (sensorContext == null) {
      return dummyCache();
    }

    try {
      // TODO: Should we ignore the cache enabled setting if a SonarLintCache is available?
      var shouldConstructCacheContext = shouldConstructCacheContext(sensorContext);
      LOGGER.trace("Caching is enabled: {}", shouldConstructCacheContext);
      if (!shouldConstructCacheContext) {
        return dummyCache();
      }

      var sonarLintCache = sonarComponents.sonarLintCache();
      if (sonarLintCache != null) {
        return fromSonarLintCache(sonarLintCache);
      }

      return fromSensorContext(sensorContext);
    } catch (NoSuchMethodError error) {
      // TODO: Check if we really need to wrap all this in exception handling
      LOGGER.debug("Missing cache related method from sonar-plugin-api: {}.", error.getMessage());
      return dummyCache();
    }
  }

  private static CacheContextImpl dummyCache() {
    var dummyCache = new DummyCache();
    return new CacheContextImpl(false, dummyCache, dummyCache);
  }

  private static boolean shouldConstructCacheContext(SensorContext context) {
    return
      Optional.ofNullable(context.config())
      .flatMap(config -> config.getBoolean(SONAR_CACHING_ENABLED_KEY))
      .map(flag -> {
        LOGGER.debug("Forcing caching behavior. Caching will be enabled: {}", flag);
        return flag;
      })
      .orElse(context.isCacheEnabled());
  }

  private static CacheContextImpl fromSensorContext(SensorContext context) {
    return new CacheContextImpl(
      true,
      new JavaReadCacheImpl(context.previousCache()),
      new JavaWriteCacheImpl(context.nextCache())
    );
  }

  private static CacheContextImpl fromSonarLintCache(SonarLintCache sonarLintCache) {
    return new CacheContextImpl(
      true,
      new JavaReadCacheImpl(sonarLintCache),
      new JavaWriteCacheImpl(sonarLintCache)
    );
  }

  @Override
  public boolean isCacheEnabled() {
    return isCacheEnabled;
  }

  public JavaReadCache getReadCache() {
    return readCache;
  }

  public JavaWriteCache getWriteCache() {
    return writeCache;
  }
}
