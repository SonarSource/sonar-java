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

import java.io.IOException;
import javax.annotation.CheckForNull;
import org.sonar.api.batch.sensor.cache.ReadCache;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.plugins.java.api.caching.JavaReadCache;

import java.io.InputStream;
import java.util.Objects;
import org.sonarsource.performance.measure.PerformanceMeasure;

public class JavaReadCacheImpl implements JavaReadCache {
  private static final Logger LOG = Loggers.get(JavaReadCacheImpl.class);

  private ReadCache readCache;

  public JavaReadCacheImpl(ReadCache readCache) {
    this.readCache = readCache;
  }

  @Override
  public InputStream read(String key) {
    PerformanceMeasure.Duration duration = PerformanceMeasure.start("JavaReadCache.read");
    InputStream read = readCache.read(key);
    duration.stop();
    return read;
  }

  @CheckForNull
  @Override
  public byte[] readBytes(String key) {
    PerformanceMeasure.Duration duration = PerformanceMeasure.start("JavaReadCache.readBytes");
    if (readCache.contains(key)) {
      try (var in = read(key)) {
        return in.readAllBytes();
      } catch (IOException e) {
        throw new CacheReadException(String.format("Unable to read data for key '%s'", key), e);
      } finally {
        duration.stop();
      }
    } else {
      LOG.trace(() -> String.format("Cache miss for key '%s'", key));
      duration.stop();
      return null;
    }
  }

  @Override
  public boolean contains(String key) {
    return readCache.contains(key);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    JavaReadCacheImpl that = (JavaReadCacheImpl) o;
    return Objects.equals(readCache, that.readCache);
  }

  @Override
  public int hashCode() {
    return Objects.hash(readCache);
  }
}
