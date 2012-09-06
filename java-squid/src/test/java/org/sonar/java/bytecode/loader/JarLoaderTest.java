/*
 * Sonar Java
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
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
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URL;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.fail;

public class JarLoaderTest {

  @Test(expected = IllegalArgumentException.class)
  public void shouldThrowIllegalArgumentException() throws Exception {
    new JarLoader(null);
  }

  @Test
  public void testFindResource() throws Exception {
    File jar = new File("src/test/files/bytecode/lib/hello.jar");
    JarLoader loader = new JarLoader(jar);

    assertThat(loader.findResource("notfound")).isNull();

    URL url = loader.findResource("META-INF/MANIFEST.MF");
    assertThat(url).isNotNull();
    assertThat(url.toString()).startsWith("jar:");
    assertThat(url.toString()).endsWith("hello.jar!/META-INF/MANIFEST.MF");

    InputStream is = url.openStream();
    try {
      assertThat(IOUtils.readLines(is)).contains("Manifest-Version: 1.0");
    } finally {
      IOUtils.closeQuietly(is);
    }

    loader.close();

    try {
      loader.findResource("META-INF/MANIFEST.MF");
      fail();
    } catch (IllegalStateException e) {
      // ok
    }
  }

  @Test
  public void testLoadBytes() throws Exception {
    File jar = new File("src/test/files/bytecode/lib/hello.jar");
    JarLoader loader = new JarLoader(jar);

    assertThat(loader.loadBytes("notfound")).isNull();

    byte[] bytes = loader.loadBytes("META-INF/MANIFEST.MF");
    assertThat(bytes).isNotNull();
    ByteArrayInputStream is = new ByteArrayInputStream(bytes);
    assertThat(IOUtils.readLines(is)).contains("Manifest-Version: 1.0");

    loader.close();

    try {
      loader.loadBytes("META-INF/MANIFEST.MF");
      fail();
    } catch (IllegalStateException e) {
      // ok
    }
  }

  @Test
  public void closeCanBeCalledMultipleTimes() throws Exception {
    File jar = new File("src/test/files/bytecode/lib/hello.jar");
    JarLoader loader = new JarLoader(jar);
    loader.close();
    loader.close();
  }

}
