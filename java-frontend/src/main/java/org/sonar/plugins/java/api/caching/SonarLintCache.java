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
package org.sonar.plugins.java.api.caching;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import org.sonar.api.Beta;
import org.sonar.api.batch.sensor.cache.ReadCache;
import org.sonar.api.batch.sensor.cache.WriteCache;
import org.sonarsource.api.sonarlint.SonarLintSide;

/**
 * Component used in SonarLint to transfer data in memory between plugins.
 * At the time of writing, this is used only by the DBD plugin, to consume IRs produced by DBD custom rules in SonarLint context.
 * This component is just an intermediate solution until a dedicated mechanism to communicate between plugins with sufficient capabilities
 * is available.
 * <p>
 * By default, this component has {@code SINGLE_ANALYSIS} lifetime, meaning that it does not need to be manually cleared after analysis.
 */
@SonarLintSide()
@Beta
public class SonarLintCache implements ReadCache, WriteCache {

  private final Map<String, byte[]> cache = new HashMap<>();


  @Override
  public InputStream read(String s) {
    if (!contains(s)) {
      throw new IllegalArgumentException(String.format("SonarLintCache does not contain key \"%s\"", s));
    }
    return new ByteArrayInputStream(cache.get(s));
  }

  @Override
  public boolean contains(String s) {
    return cache.containsKey(s);
  }

  @Override
  public void write(String s, InputStream inputStream) {
    try {
      write(s, inputStream.readAllBytes());
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public void write(String s, byte[] bytes) {
    if (contains(s)) {
      throw new IllegalArgumentException(String.format("Same key cannot be written to multiple times (%s)", s));
    }
    cache.put(s, bytes);
  }

  @Override
  public void copyFromPrevious(String s) {
    throw new UnsupportedOperationException("SonarLintCache does not allow to copy from previous.");
  }
}
