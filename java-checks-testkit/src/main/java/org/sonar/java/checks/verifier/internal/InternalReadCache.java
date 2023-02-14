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
package org.sonar.java.checks.verifier.internal;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import org.sonar.api.batch.sensor.cache.ReadCache;

public class InternalReadCache implements ReadCache {
  private final Map<String, byte[]> data = new HashMap<>();

  @Override
  public InputStream read(String key) {
    if (!data.containsKey(key)) {
      throw new IllegalArgumentException(String.format("cache does not contain key \"%s\"", key));
    }
    byte[] buf = data.get(key);
    if (buf == null) {
      return new ByteArrayInputStream(new byte[0]);
    }
    return new ByteArrayInputStream(buf);
  }

  @Override
  public boolean contains(String key) {
    return data.containsKey(key);
  }

  public InternalReadCache put(String key, byte[] data) {
    this.data.put(key, data);
    return this;
  }

  public InternalReadCache putAll(Map<String, byte[]> data) {
    this.data.putAll(data);
    return this;
  }

  public InternalReadCache putAll(InternalWriteCache writeCache) {
    return this.putAll(writeCache.getData());
  }
}
