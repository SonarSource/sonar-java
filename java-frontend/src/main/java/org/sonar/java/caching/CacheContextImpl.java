/*
 * SonarQube Java
 * Copyright (C) 2012-2023 SonarSource SA
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
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.plugins.java.api.caching.CacheContext;
import org.sonar.plugins.java.api.caching.JavaReadCache;
import org.sonar.plugins.java.api.caching.JavaWriteCache;

public class CacheContextImpl implements CacheContext {
  /**
   * Can be set to {@code true} or {@code false} to override whether the cache is enabled or not. Note that even if this is set to true,
   * the engine may not use caching anyway, if the server does not support it.
   */
  public static final String SONAR_CACHING_ENABLED_KEY = "sonar.java.caching.enabled";

  private static final Logger LOGGER = Loggers.get(CacheContextImpl.class);

  private final boolean isCacheEnabled;
  private final JavaReadCache readCache;
  private final JavaWriteCache writeCache;

  private CacheContextImpl(boolean isCacheEnabled, JavaReadCache readCache, JavaWriteCache writeCache) {
    this.isCacheEnabled = isCacheEnabled;
    this.readCache = readCache;
    this.writeCache = writeCache;
  }

  public static CacheContextImpl of(@Nullable SensorContext context) {

    if (context != null) {
      try {
        boolean cacheEnabled =
          (context.config() == null ? Optional.<Boolean>empty() : context.config().getBoolean(SONAR_CACHING_ENABLED_KEY))
            .map(flag -> {
              LOGGER.debug(() -> "Forcing caching behavior. Caching will be enabled: " + flag);
              return flag;
            })
            .orElse(context.isCacheEnabled());

        LOGGER.trace(() -> "Caching is enabled: " + cacheEnabled);

        if (cacheEnabled) {
          return new CacheContextImpl(
            true,
            new JavaReadCacheImpl(context.previousCache()),
            new JavaWriteCacheImpl(context.nextCache())
          );
        }
      } catch (NoSuchMethodError error) {
        LOGGER.debug(() -> String.format("Missing cache related method from sonar-plugin-api: %s.", error.getMessage()));
      }
    }

    DummyCache dummyCache = new DummyCache();
    return new CacheContextImpl(false, dummyCache, dummyCache);
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
