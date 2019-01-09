/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
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
package org.sonar.java.bytecode.loader;

import javax.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

class FileSystemLoader implements Loader {

  private Path baseDirPath;

  public FileSystemLoader(@Nullable File baseDir) {
    if (baseDir == null) {
      throw new IllegalArgumentException("baseDir can't be null");
    }
    this.baseDirPath = baseDir.toPath();
  }

  @Override
  public URL findResource(String name) {
    if (baseDirPath == null) {
      throw new IllegalStateException("Loader closed");
    }
    Path filePath = baseDirPath.resolve(name);
    if (filePath.toFile().isFile()) {
      try {
        return filePath.toUri().toURL();
      } catch (MalformedURLException e) {
        return null;
      }
    }
    return null;
  }

  @Override
  public byte[] loadBytes(String name) {
    if (baseDirPath == null) {
      throw new IllegalStateException("Loader closed");
    }
    Path filePath = baseDirPath.resolve(name);
    if (!filePath.toFile().exists()) {
      return new byte[0];
    }

    try {
      return Files.readAllBytes(filePath);
    } catch (IOException e) {
      return new byte[0];
    }
  }

  @Override
  public void close() {
    baseDirPath = null;
  }

}
