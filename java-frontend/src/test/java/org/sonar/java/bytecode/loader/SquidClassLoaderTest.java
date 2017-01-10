/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
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

import com.google.common.collect.Iterators;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

public class SquidClassLoaderTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private SquidClassLoader classLoader;

  @After
  public void tearDown() {
    IOUtils.closeQuietly(classLoader);
  }

  /**
   * See SONAR-2824:
   * Created ClassLoader should be able to load classes only from JDK and from provided list of JAR-files,
   * thus it shouldn't be able to load his class.
   */
  @Test
  public void shouldBeIsolated() throws Exception {
    classLoader = new SquidClassLoader(Collections.emptyList());
    assertThat(classLoader.loadClass("java.lang.Integer")).isNotNull();
    assertThat(classLoader.getResource("java/lang/Integer.class")).isNotNull();
    thrown.expect(ClassNotFoundException.class);
    classLoader.loadClass(SquidClassLoader.class.getName());
  }

  @Test
  public void createFromJar() throws Exception {
    File jar = new File("src/test/files/bytecode/lib/hello.jar");
    classLoader = new SquidClassLoader(Arrays.asList(jar));

    assertThat(classLoader.loadClass("org.sonar.tests.Hello")).isNotNull();
    assertThat(classLoader.getResource("org/sonar/tests/Hello.class")).isNotNull();
    assertThat(Iterators.forEnumeration(classLoader.findResources("org/sonar/tests/Hello.class"))).hasSize(1);
    thrown.expect(ClassNotFoundException.class);
    classLoader.loadClass("foo.Unknown");
  }
  
  @Test
  public void createFromAar() throws Exception {
    File jar = new File("src/test/files/classpath/lib/oklog-1.0.1.aar");
    classLoader = new SquidClassLoader(Arrays.asList(jar));

    assertThat(classLoader.loadClass("com.github.simonpercic.oklog.BuildConfig")).isNotNull();
    assertThat(classLoader.getResource("com/github/simonpercic/oklog/BuildConfig.class")).isNotNull();
    assertThat(Iterators.forEnumeration(classLoader.findResources("com/github/simonpercic/oklog/BuildConfig.class"))).hasSize(1);
    thrown.expect(ClassNotFoundException.class);
    classLoader.loadClass("foo.Unknown");
  }

  @Test
  public void unknownJarIsIgnored() throws Exception {
    File jar = new File("src/test/files/bytecode/lib/unknown.jar");
    classLoader = new SquidClassLoader(Arrays.asList(jar));

    assertThat(classLoader.getResource("org/sonar/tests/Hello.class")).isNull();

    classLoader.close();
  }

  /**
   * SONAR-3693
   */
  @Test
  public void not_jar_is_ignored() throws Exception {
    File jar = new File("src/test/files/bytecode/src/tags/TagName.java");
    classLoader = new SquidClassLoader(Arrays.asList(jar));
  }

  @Test
  public void createFromDirectory() throws Exception {
    File dir = new File("src/test/files/bytecode/bin/");
    classLoader = new SquidClassLoader(Arrays.asList(dir));

    assertThat(classLoader.loadClass("tags.TagName")).isNotNull();
    assertThat(classLoader.getResource("tags/TagName.class")).isNotNull();
    assertThat(Iterators.forEnumeration(classLoader.findResources("tags/TagName.class"))).hasSize(1);
    thrown.expect(ClassNotFoundException.class);
    classLoader.loadClass("tags.Unknown");
  }

  @Test
  public void testFindResource() throws Exception {
    File dir = new File("src/test/files/bytecode/bin/");
    classLoader = new SquidClassLoader(Arrays.asList(dir, dir));
    assertThat(classLoader.findResource("tags/TagName.class")).isNotNull();
    assertThat(classLoader.findResource("notfound")).isNull();
  }

  @Test
  public void testFindResources() throws Exception {
    File dir = new File("src/test/files/bytecode/bin/");
    classLoader = new SquidClassLoader(Arrays.asList(dir, dir));

    assertThat(Iterators.forEnumeration(classLoader.findResources("tags/TagName.class"))).hasSize(2);
    assertThat(Iterators.forEnumeration(classLoader.findResources("notfound"))).hasSize(0);
  }

  @Test
  public void closeCanBeCalledMultipleTimes() throws Exception {
    File jar = new File("src/test/files/bytecode/lib/hello.jar");
    classLoader = new SquidClassLoader(Arrays.asList(jar));
    classLoader.close();
    classLoader.close();
  }

}
