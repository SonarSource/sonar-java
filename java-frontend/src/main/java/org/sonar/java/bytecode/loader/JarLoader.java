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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

class JarLoader implements Loader {

  private final JarFile jarFile;
  private final URL jarUrl;

  /**
   * @throws IllegalStateException if an I/O error has occurred
   */
  public JarLoader(@Nullable File file) {
    if (file == null) {
      throw new IllegalArgumentException("file can't be null");
    }
    try {
      jarFile = new JarFile(file);
      jarUrl = new URL("jar", "", -1, file.getAbsolutePath() + "!/");
    } catch (IOException e) {
      throw new IllegalStateException("Unable to open " + file.getAbsolutePath(), e);
    }
  }

  @Override
  public URL findResource(String name) {
    ZipEntry entry = jarFile.getEntry(name);
    if (entry != null) {
      try {
        return new URL(jarUrl, name, new JarEntryHandler(entry));
      } catch (MalformedURLException e) {
        return null;
      }
    }
    return null;
  }

  @Override
  public byte[] loadBytes(String name) {
    try {
      ZipEntry entry = jarFile.getEntry(name);
      if (entry == null) {
        return new byte[0];
      }

      try (InputStream is = jarFile.getInputStream(entry)) {
        byte[] buffer = new byte[8192];
        ByteArrayOutputStream array = new ByteArrayOutputStream((int) entry.getSize());
        int i;
        while ((i = is.read(buffer)) >= 0) {
          array.write(buffer, 0, i);
        }
        return array.toByteArray();
      }
    } catch (IOException e) {
      // TODO Godin: not sure that we should silently ignore exception here,
      // e.g. it can be thrown if file corrupted
      return new byte[0];
    }
  }

  @Override
  public void close() {
    try {
      jarFile.close();
    } catch (IOException e) {
      // ignore
    }
  }

  private class JarEntryHandler extends URLStreamHandler {

    private ZipEntry entry;

    JarEntryHandler(ZipEntry entry) {
      this.entry = entry;
    }

    @Override
    protected URLConnection openConnection(URL u) throws IOException {
      return new URLConnection(u) {
        @Override
        public void connect() throws IOException {
          // nop
        }

        @Override
        public int getContentLength() {
          return (int) entry.getSize();
        }

        @Override
        public InputStream getInputStream() throws IOException {
          return jarFile.getInputStream(entry);
        }
      };
    }
  }

}
