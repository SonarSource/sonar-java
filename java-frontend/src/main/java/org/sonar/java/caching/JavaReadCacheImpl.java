/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.caching;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import javax.annotation.CheckForNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.sensor.cache.ReadCache;
import org.sonar.plugins.java.api.caching.JavaReadCache;
import org.sonarsource.performance.measure.PerformanceMeasure;

public class JavaReadCacheImpl implements JavaReadCache {
  private static final Logger LOG = LoggerFactory.getLogger(JavaReadCacheImpl.class);

  private final ReadCache readCache;

  public JavaReadCacheImpl(ReadCache readCache) {
    this.readCache = readCache;
  }

  @Override
  public InputStream read(String key) {
    PerformanceMeasure.Duration duration = PerformanceMeasure.start("JavaReadCache.read");
    InputStream read;
    try {
      read = readCache.read(key);
    } finally {
      duration.stop();
    }
    return read;
  }

  @CheckForNull
  @Override
  public byte[] readBytes(String key) {
    PerformanceMeasure.Duration duration = PerformanceMeasure.start("JavaReadCache.readBytes");
    try {
      if (readCache.contains(key)) {
        try (var in = read(key)) {
          return in.readAllBytes();
        } catch (IOException e) {
          throw new CacheReadException(String.format("Unable to read data for key '%s'", key), e);
        }
      } else {
        LOG.trace("Cache miss for key '{}'", key);
        return null;
      }
    } finally {
      duration.stop();
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
