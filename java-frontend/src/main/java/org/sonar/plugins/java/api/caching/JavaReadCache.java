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
package org.sonar.plugins.java.api.caching;

import java.io.InputStream;
import javax.annotation.CheckForNull;

public interface JavaReadCache {
  /**
   * Returns an input stream for the data cached with the provided {@code key}. It is the responsibility of the caller to close the stream.
   */
  InputStream read(String key);

  /**
   * @return the array of bytes stored for the given key, if any. {@code null} otherwise.
   */
  @CheckForNull
  byte[] readBytes(String key);

  /**
   * Checks whether the cache contains the provided {@code key}.
   */
  boolean contains(String key);
}
