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

import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.plugins.java.api.caching.CacheContext;
import org.sonar.plugins.java.api.caching.JavaReadCache;
import org.sonar.plugins.java.api.caching.JavaWriteCache;

public class CacheContextImpl implements CacheContext {
  private static final Logger LOGGER = Loggers.get(CacheContextImpl.class);

  private final boolean isCacheEnabled;
  private final JavaReadCache readCache;
  private final JavaWriteCache writeCache;

  private CacheContextImpl(boolean isCacheEnabled, JavaReadCache readCache, JavaWriteCache writeCache) {
    this.isCacheEnabled = isCacheEnabled;
    this.readCache = readCache;
    this.writeCache = writeCache;
  }

  public static CacheContextImpl of(SensorContext context) {
    try {
      return new CacheContextImpl(
        context.isCacheEnabled(),
        new JavaReadCacheImpl(context.previousAnalysisCache()),
        new JavaWriteCacheImpl(context.nextCache())
      );
    } catch (NoSuchMethodError error) {
      LOGGER.info(error.getMessage());
      DummyCache dummyCache = new DummyCache();
      return new CacheContextImpl(false, dummyCache, dummyCache);
    }
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
