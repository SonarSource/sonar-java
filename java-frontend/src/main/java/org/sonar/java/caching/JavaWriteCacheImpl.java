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

import org.sonar.api.batch.sensor.cache.WriteCache;
import org.sonar.plugins.java.api.caching.JavaWriteCache;

import java.io.InputStream;
import java.util.Objects;
import org.sonarsource.performance.measure.PerformanceMeasure;

public class JavaWriteCacheImpl implements JavaWriteCache {
  private WriteCache writeCache;

  public JavaWriteCacheImpl(WriteCache writeCache) {
    this.writeCache = writeCache;
  }

  @Override
  public void write(String key, InputStream data) {
    PerformanceMeasure.Duration duration = PerformanceMeasure.start("JavaWriteCache.write");
    try {
      this.writeCache.write(key, data);
    } finally {
      duration.stop();
    }
  }

  @Override
  public void write(String key, byte[] data) {
    PerformanceMeasure.Duration duration = PerformanceMeasure.start("JavaWriteCache.write");
    try {
      this.writeCache.write(key, data);
    } finally {
      duration.stop();
    }
  }

  @Override
  public void copyFromPrevious(String key) {
    PerformanceMeasure.Duration duration = PerformanceMeasure.start("JavaWriteCache.copyFromPrevious");
    try {
      this.writeCache.copyFromPrevious(key);
    } finally {
      duration.stop();
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    JavaWriteCacheImpl that = (JavaWriteCacheImpl) o;
    return Objects.equals(writeCache, that.writeCache);
  }

  @Override
  public int hashCode() {
    return Objects.hash(writeCache);
  }
}
