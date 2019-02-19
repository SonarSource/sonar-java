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

import com.google.common.collect.Iterators;
import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;

import static org.assertj.core.api.Assertions.assertThat;

public class SquidClassLoaderTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Rule
  public LogTester logTester = new LogTester();

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
  public void should_read_child_classes_first() throws Exception {
    classLoader = new SquidClassLoader(Collections.singletonList(new File("src/test/files/bytecode/lib/likeJdkJar.jar")));
    URL resource = classLoader.getResource("java/lang/String.class");
    assertThat(resource).isNotNull();
    assertThat(resource.getFile()).contains("likeJdkJar.jar!");
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
  public void empty_archive_should_not_fail() throws Exception {
    File jar = new File("src/test/files/bytecode/lib/emptyArchive.jar");
    classLoader = new SquidClassLoader(Arrays.asList(jar));

    assertThat(classLoader.getResource("dummy.class")).isNull();

    assertThat(logTester.logs()).isEmpty();

    classLoader.close();
  }

  @Test
  public void empty_file_should_not_fail_but_log_warning() {
    File jar = new File("src/test/files/bytecode/lib/emptyFile.jar");
    classLoader = new SquidClassLoader(Arrays.asList(jar));

    assertThat(classLoader.getResource("dummy.class")).isNull();

    assertThat(logTester.logs()).hasSize(2);
    List<String> warnings = logTester.logs(LoggerLevel.WARN);
    assertThat(warnings).hasSize(1);
    assertThat(warnings.get(0))
      .startsWith("Unable to load classes from '")
      .endsWith("emptyFile.jar\'");
    List<String> debugs = logTester.logs(LoggerLevel.DEBUG);
    assertThat(debugs).hasSize(1);
    assertThat(debugs.get(0))
      .startsWith("Unable to open")
      .endsWith("emptyFile.jar: zip file is empty");

    classLoader.close();
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

  @Test
  public void exceptionThrownWhenAlreadyClosed() {
    File jar = new File("src/test/files/bytecode/lib/hello.jar");
    classLoader = new SquidClassLoader(Arrays.asList(jar));
    classLoader.close();

    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("java.lang.IllegalStateException: zip file closed");
    classLoader.getResource("org/sonar/tests/Hello.class");
  }

  @Test
  public void test_loading_class() {
    SquidClassLoader classLoader = new SquidClassLoader(Collections.singletonList(new File("target/test-classes")));
    String className = getClass().getCanonicalName();
    byte[] bytes = classLoader.getBytesForClass(className);
    assertThat(bytes).isNotNull();
    ClassReader cr = new ClassReader(bytes);
    ClassNode classNode = new ClassNode();
    cr.accept(classNode, 0);
    assertThat(classNode.name).isEqualTo("org/sonar/java/bytecode/loader/SquidClassLoaderTest");
  }

  @Test
  public void empty_classloader_should_not_find_bytes() {
    SquidClassLoader classLoader = new SquidClassLoader(Collections.emptyList());
    String className = getClass().getCanonicalName();
    byte[] bytes = classLoader.getBytesForClass(className);
    assertThat(bytes).isNull();
  }

  @Test
  public void test_loading_java9_class() throws Exception {
    SquidClassLoader classLoader = new SquidClassLoader(Collections.singletonList(new File("src/test/files/bytecode/java9/bin")));
    byte[] bytes = classLoader.getBytesForClass("org.test.Hello9");
    assertThat(bytes).isNotNull();
    ClassReader cr = new ClassReader(bytes);
    ClassNode classNode = new ClassNode();
    cr.accept(classNode, 0);
    assertThat(classNode.version).isEqualTo(Opcodes.V9);
    classLoader.close();
  }

  @Test
  public void test_loading_java10_class() throws Exception {
    SquidClassLoader classLoader = new SquidClassLoader(Collections.singletonList(new File("src/test/files/bytecode/java10/bin")));
    byte[] bytes = classLoader.getBytesForClass("org.foo.A");
    assertThat(bytes).isNotNull();
    ClassReader cr = new ClassReader(bytes);
    ClassNode classNode = new ClassNode();
    cr.accept(classNode, 0);
    assertThat(classNode.version).isEqualTo(Opcodes.V10);
    classLoader.close();
  }

  @Test
  public void test_loading_java11_class() throws Exception {
    SquidClassLoader classLoader = new SquidClassLoader(Collections.singletonList(new File("src/test/files/bytecode/java11/bin")));
    byte[] bytes = classLoader.getBytesForClass("org.foo.A");
    assertThat(bytes).isNotNull();
    ClassReader cr = new ClassReader(bytes);
    ClassNode classNode = new ClassNode();
    cr.accept(classNode, 0);
    assertThat(classNode.version).isEqualTo(Opcodes.V11);
    classLoader.close();
  }
}
