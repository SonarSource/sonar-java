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

import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import org.junit.Test;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.fail;

public class SquidClassLoaderTest {

  /**
   * See SONAR-2824:
   * Created ClassLoader should be able to load classes only from JDK and from provided list of JAR-files,
   * thus it shouldn't be able to load his class.
   */
  @Test
  public void shouldBeIsolated() throws Exception {
    SquidClassLoader classLoader = new SquidClassLoader(Collections.EMPTY_LIST);
    try {
      classLoader.loadClass(SquidClassLoader.class.getName());
      fail();
    } catch (ClassNotFoundException e) {
      // ok
    }
    assertThat(classLoader.loadClass("java.lang.Integer")).isNotNull();
    assertThat(classLoader.getResource("java/lang/Integer.class")).isNotNull();
  }

  @Test
  public void createFromJar() throws Exception {
    File jar = new File("src/test/files/bytecode/lib/hello.jar");
    SquidClassLoader classLoader = new SquidClassLoader(Arrays.asList(jar));

    assertThat(classLoader.loadClass("org.sonar.tests.Hello")).isNotNull();
    assertThat(classLoader.getResource("org/sonar/tests/Hello.class")).isNotNull();
    List<URL> resources = Lists.newArrayList(Iterators.forEnumeration(classLoader.findResources("org/sonar/tests/Hello.class")));
    assertThat(resources.size()).isEqualTo(1);
    try {
      classLoader.loadClass("foo.Unknown");
      fail();
    } catch (ClassNotFoundException e) {
      // ok
    }

    classLoader.close();
  }

  @Test
  public void unknownJarIsIgnored() throws Exception {
    File jar = new File("src/test/files/bytecode/lib/unknown.jar");
    SquidClassLoader classLoader = new SquidClassLoader(Arrays.asList(jar));

    assertThat(classLoader.getResource("org/sonar/tests/Hello.class")).isNull();

    classLoader.close();
  }

  /**
   * SONAR-3693
   */
  @Test
  public void not_jar_is_ignored() throws Exception {
    File jar = new File("src/test/files/bytecode/src/tags/TagName.java");
    SquidClassLoader classLoader = new SquidClassLoader(Arrays.asList(jar));
    classLoader.close();
  }

  @Test
  public void createFromDirectory() throws Exception {
    File dir = new File("src/test/files/bytecode/bin/");
    SquidClassLoader classLoader = new SquidClassLoader(Arrays.asList(dir));

    assertThat(classLoader.loadClass("tags.TagName")).isNotNull();
    assertThat(classLoader.getResource("tags/TagName.class")).isNotNull();
    List<URL> resources = Lists.newArrayList(Iterators.forEnumeration(classLoader.findResources("tags/TagName.class")));
    assertThat(resources.size()).isEqualTo(1);
    try {
      classLoader.loadClass("tags.Unknown");
      fail();
    } catch (ClassNotFoundException e) {
      // ok
    }

    classLoader.close();
  }

  @Test
  public void testFindResources() throws Exception {
    File dir = new File("src/test/files/bytecode/bin/");
    SquidClassLoader classLoader = new SquidClassLoader(Arrays.asList(dir, dir));

    List<URL> resources = Lists.newArrayList(Iterators.forEnumeration(classLoader.findResources("tags/TagName.class")));
    assertThat(resources.size()).isEqualTo(2);

    classLoader.close();
  }

  @Test
  public void closeCanBeCalledMultipleTimes() throws Exception {
    File jar = new File("src/test/files/bytecode/lib/hello.jar");
    SquidClassLoader classLoader = new SquidClassLoader(Arrays.asList(jar));
    classLoader.close();
    classLoader.close();
  }

}
