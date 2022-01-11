/*
 * SonarQube Java
 * Copyright (C) 2012-2022 SonarSource SA
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
package org.sonar.java.model;

import com.google.common.io.Files;
import java.io.File;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.migrationsupport.rules.EnableRuleMigrationSupport;
import org.junit.rules.TemporaryFolder;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;

import static org.assertj.core.api.Assertions.assertThat;

@EnableRuleMigrationSupport
class JParserReleasingJarsTest {

  @org.junit.Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private static final String PROJECT_JAR = "src/test/files/other/project.jar";
  private static final String SOURCE = "package foo.bar;\n"
    // requires project.jar
    + "import org.foo.A;\n"
    + "class B {\n"
    + "  void m() {\n"
    + "    A a = new A();"
    + "    a.foo(true);\n"
    + "  }\n"
    + "}";

  /**
   * Control test: method should not be resolved without JAR in class path
   */
  @Test
  void jParser_should_not_resolve_method_without_jar_in_classPath() {
    Symbol foo = getFooSymbol(parse(Collections.emptyList()));
    assertThat(foo.isUnknown()).isTrue();
  }

  /**
   * Control test: method should be resolved with JAR in class path
   */
  @Test
  void jParser_should_resolve_method_with_jar_in_classPath() {
    List<File> classPath = Collections.singletonList(new File(PROJECT_JAR));
    Symbol foo = getFooSymbol(parse(classPath));

    assertThat(foo.isUnknown()).isFalse();
    assertThat(foo.isMethodSymbol()).isTrue();
    assertThat(((Symbol.MethodSymbol) foo).signature()).isEqualTo("org.foo.A#foo(Z)I");
  }

  /**
   * Control test: should be able to delete jar
   */
  @Test
  void should_be_able_to_delete_jar() throws Exception {
    File newJar = new File(temp.newFolder(), "project2.jar");
    Files.copy(new File(PROJECT_JAR), newJar);

    assertThat(newJar).exists();
    assertThat(newJar.delete()).isTrue();
    assertThat(newJar).doesNotExist();
  }

  /**
   * Combined tests: requires a jar to parse correctly, and jar should be released after use
   *
   * NOTE: Requires WINDOWS environment to FAIL
   */
  @Test
  void jParser_should_release_jar_after_use() throws Exception {
    File newJar = new File(temp.newFolder(), "project3.jar");
    Files.copy(new File(PROJECT_JAR), newJar);

    assertThat(newJar).exists();

    List<File> classPath = Collections.singletonList(newJar);
    JavaTree.CompilationUnitTreeImpl cu = parse(classPath);
    Symbol foo = getFooSymbol(cu);

    assertThat(foo.isUnknown()).isFalse();
    assertThat(foo.isMethodSymbol()).isTrue();
    assertThat(((Symbol.MethodSymbol) foo).signature()).isEqualTo("org.foo.A#foo(Z)I");

    // force clean
    cu.sema.cleanupEnvironment();

    // can be safely deleted
    assertThat(newJar.delete()).isTrue();
    assertThat(newJar).doesNotExist();
  }

  private static Symbol getFooSymbol(JavaTree.CompilationUnitTreeImpl cu) {
    ClassTree b = (ClassTree) cu.types().get(0);
    MethodTree m = (MethodTree) b.members().get(0);
    ExpressionStatementTree s = (ExpressionStatementTree) m.block().body().get(1);
    return ((MethodInvocationTree) s.expression()).symbol();
  }

  private static JavaTree.CompilationUnitTreeImpl parse(List<File> classPath) {
    return (JavaTree.CompilationUnitTreeImpl) JParserTestUtils.parse("B", SOURCE, classPath);
  }

}
