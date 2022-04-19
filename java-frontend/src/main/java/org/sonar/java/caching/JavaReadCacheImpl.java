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

import org.sonar.api.batch.sensor.cache.ReadCache;
import org.sonar.plugins.java.api.caching.JavaReadCache;

import java.io.InputStream;
import java.util.Objects;

public class JavaReadCacheImpl implements JavaReadCache {
  private ReadCache readCache;

  public JavaReadCacheImpl(ReadCache readCache) {
    this.readCache = readCache;
  }

  @Override
  public InputStream read(String key) {
    return readCache.read(key);
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
