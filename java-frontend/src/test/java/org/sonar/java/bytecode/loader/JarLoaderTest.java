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

import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;

public class JarLoaderTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void shouldThrowIllegalArgumentException() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("file can't be null");
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

    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("zip file closed");
    loader.findResource("META-INF/MANIFEST.MF");
  }

  @Test
  public void testLoadBytes() throws Exception {
    File jar = new File("src/test/files/bytecode/lib/hello.jar");
    JarLoader loader = new JarLoader(jar);

    assertThat(loader.loadBytes("notfound")).isEmpty();

    byte[] bytes = loader.loadBytes("META-INF/MANIFEST.MF");
    assertThat(bytes).isNotEmpty();
    ByteArrayInputStream is = new ByteArrayInputStream(bytes);
    assertThat(IOUtils.readLines(is)).contains("Manifest-Version: 1.0");

    loader.close();

    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("zip file closed");
    loader.loadBytes("META-INF/MANIFEST.MF");
  }

  @Test
  public void closeCanBeCalledMultipleTimes() throws Exception {
    File jar = new File("src/test/files/bytecode/lib/hello.jar");
    JarLoader loader = new JarLoader(jar);
    loader.close();
    loader.close();
  }

  @Test
  public void testCorruptedJar() {
    File jar = new File("src/test/files/bytecode/src/tags/TagName.java");
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Unable to open " + jar.getAbsolutePath());
    new JarLoader(jar);
  }

}
