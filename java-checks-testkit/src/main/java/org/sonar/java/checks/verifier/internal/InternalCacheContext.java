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
package org.sonar.java.checks.verifier.internal;

import org.sonar.plugins.java.api.caching.CacheContext;
import org.sonar.plugins.java.api.caching.JavaReadCache;
import org.sonar.plugins.java.api.caching.JavaWriteCache;

import javax.annotation.Nullable;
import java.util.Objects;

public class InternalCacheContext implements CacheContext {
  private boolean isEnabled;
  private JavaReadCache readCache;
  private JavaWriteCache writeCache;

  public InternalCacheContext(boolean isEnabled, @Nullable JavaReadCache readCache, @Nullable JavaWriteCache writeCache) {
    this.isEnabled = isEnabled;
    this.readCache = readCache;
    this.writeCache = writeCache;
  }

  @Override
  public boolean isCacheEnabled() {
    return isEnabled;
  }

  @Override
  public JavaReadCache getReadCache() {
    return readCache;
  }

  @Override
  public JavaWriteCache getWriteCache() {
    return writeCache;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    InternalCacheContext that = (InternalCacheContext) o;
    return isEnabled == that.isEnabled && Objects.equals(readCache, that.readCache) && Objects.equals(writeCache, that.writeCache);
  }

  @Override
  public int hashCode() {
    return Objects.hash(isEnabled, readCache, writeCache);
  }
}
