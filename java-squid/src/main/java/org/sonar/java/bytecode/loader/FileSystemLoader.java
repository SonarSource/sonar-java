/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.java.bytecode.loader;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

class FileSystemLoader implements Loader {

  private File baseDir;

  public FileSystemLoader(File baseDir) {
    if (baseDir == null) {
      throw new IllegalArgumentException("baseDir can't be null");
    }
    this.baseDir = baseDir;
  }

  @Override
  public URL findResource(String name) {
    if (baseDir == null) {
      throw new IllegalStateException("Loader closed");
    }
    File file = new File(baseDir, name);
    if (file.exists() && file.isFile()) {
      try {
        return file.toURI().toURL();
      } catch (MalformedURLException e) {
        return null;
      }
    }
    return null;
  }

  @Override
  public byte[] loadBytes(String name) {
    if (baseDir == null) {
      throw new IllegalStateException("Loader closed");
    }
    File file = new File(baseDir, name);
    if (!file.exists()) {
      return null;
    }
    InputStream is = null;
    try {
      is = new FileInputStream(file);
      return IOUtils.toByteArray(is);
    } catch (IOException e) {
      return null;
    } finally {
      IOUtils.closeQuietly(is);
    }
  }

  @Override
  public void close() {
    baseDir = null;
  }

}
