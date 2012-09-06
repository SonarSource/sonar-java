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

import org.junit.Test;

import java.io.File;
import java.net.URL;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.fail;

public class FileSystemLoaderTest {

  @Test(expected = IllegalArgumentException.class)
  public void shouldThrowIllegalArgumentException() throws Exception {
    new FileSystemLoader(null);
  }

  @Test
  public void testFindResource() throws Exception {
    File dir = new File("src/test/files/bytecode/bin/");
    FileSystemLoader loader = new FileSystemLoader(dir);

    assertThat(loader.findResource("notfound")).isNull();

    URL url = loader.findResource("tags/TagName.class");
    assertThat(url).isNotNull();
    assertThat(url.toString()).startsWith("file:");
    assertThat(url.toString()).endsWith("TagName.class");

    loader.close();

    try {
      loader.findResource("tags/TagName.class");
      fail();
    } catch (IllegalStateException e) {
      // ok
    }
  }

  @Test
  public void testLoadBytes() throws Exception {
    File dir = new File("src/test/files/bytecode/bin/");
    FileSystemLoader loader = new FileSystemLoader(dir);

    assertThat(loader.loadBytes("notfound")).isNull();

    assertThat(loader.loadBytes("tags/TagName.class")).isNotNull();

    loader.close();

    try {
      loader.loadBytes("tags/TagName.class");
      fail();
    } catch (IllegalStateException e) {
      // ok
    }
  }

  @Test
  public void closeCanBeCalledMultipleTimes() throws Exception {
    File dir = new File("src/test/files/bytecode/bin/");
    FileSystemLoader loader = new FileSystemLoader(dir);
    loader.close();
    loader.close();
  }

}
