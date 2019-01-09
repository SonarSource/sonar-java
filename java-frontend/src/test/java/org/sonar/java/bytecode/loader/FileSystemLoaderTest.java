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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.File;
import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;

public class FileSystemLoaderTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void shouldThrowIllegalArgumentException() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("baseDir can't be null");
    new FileSystemLoader(null);
  }

  @Test
  public void testFindResource() throws Exception {
    File dir = new File("src/test/files/bytecode/bin/");
    FileSystemLoader loader = new FileSystemLoader(dir);

    assertThat(loader.findResource("notfound")).isNull();
    assertThat(loader.findResource("tags"))
      .as("existing directories should not be used - only files").isNull();

    URL url = loader.findResource("tags/TagName.class");
    assertThat(url).isNotNull();
    assertThat(url.toString()).startsWith("file:");
    assertThat(url.toString()).endsWith("TagName.class");

    loader.close();

    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Loader closed");
    loader.findResource("tags/TagName.class");
  }

  @Test
  public void testLoadBytes() throws Exception {
    File dir = new File("src/test/files/bytecode/bin/");
    FileSystemLoader loader = new FileSystemLoader(dir);

    assertThat(loader.loadBytes("notfound")).isEmpty();

    assertThat(loader.loadBytes("tags/TagName.class")).isNotEmpty();

    loader.close();

    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Loader closed");
    loader.loadBytes("tags/TagName.class");
  }

  @Test
  public void closeCanBeCalledMultipleTimes() throws Exception {
    File dir = new File("src/test/files/bytecode/bin/");
    FileSystemLoader loader = new FileSystemLoader(dir);
    loader.close();
    loader.close();
  }

}
