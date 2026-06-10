/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.utils;

import org.junit.jupiter.api.Test;
import org.sonar.java.model.JParserTestUtils;
import org.sonar.java.test.classpath.TestClasspathUtils;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;

import static org.assertj.core.api.Assertions.assertThat;

class PackageUtilsTest {

  // ---- packageName(PackageDeclarationTree, String) --------------------------

  @Test
  void packageName_no_package_returns_empty_string() {
    assertThat(packageName("class A{}")).isEmpty();
  }

  @Test
  void packageName_identifier_package() {
    assertThat(packageName("package foo; class A{}")).isEqualTo("foo");
  }

  @Test
  void packageName_member_select_package() {
    assertThat(packageName("package foo.bar.plop; class A{}")).isEqualTo("foo.bar.plop");
  }

  @Test
  void packageName_different_separator() {
    assertThat(packageName("package foo.bar.plop; class A{}", "/")).isEqualTo("foo/bar/plop");
  }

  // ---- packageNameOf(Symbol) ------------------------------------------------

  @Test
  void packageNameOf_returns_package_of_class_symbol() {
    var classpath = TestClasspathUtils.DEFAULT_MODULE.getClassPath();
    CompilationUnitTree tree = JParserTestUtils.parse("PackageUtilsTestClass",
      "package com.example.pkg; class PackageUtilsTestClass {}", classpath);
    Symbol classSymbol = ((ClassTree) tree.types().get(0)).symbol();
    assertThat(PackageUtils.packageNameOf(classSymbol)).isEqualTo("com.example.pkg");
  }

  @Test
  void packageNameOf_returns_empty_string_for_default_package() {
    var classpath = TestClasspathUtils.DEFAULT_MODULE.getClassPath();
    CompilationUnitTree tree = JParserTestUtils.parse("DefaultPkgClass",
      "class DefaultPkgClass {}", classpath);
    Symbol classSymbol = ((ClassTree) tree.types().get(0)).symbol();
    assertThat(PackageUtils.packageNameOf(classSymbol)).isEmpty();
  }

  @Test
  void packageNameOf_returns_package_of_nested_class_symbol() {
    var classpath = TestClasspathUtils.DEFAULT_MODULE.getClassPath();
    CompilationUnitTree tree = JParserTestUtils.parse("Outer",
      "package com.example.pkg; class Outer { static class Inner {} }", classpath);
    ClassTree outerClass = (ClassTree) tree.types().get(0);
    ClassTree innerClass = outerClass.members().stream()
      .filter(ClassTree.class::isInstance)
      .map(ClassTree.class::cast)
      .findFirst().orElseThrow();
    assertThat(PackageUtils.packageNameOf(innerClass.symbol())).isEqualTo("com.example.pkg");
  }

  // ---- Helpers --------------------------------------------------------------

  private static String packageName(String code) {
    return packageName(code, ".");
  }

  private static String packageName(String code, String separator) {
    CompilationUnitTree tree = JParserTestUtils.parse(code);
    return PackageUtils.packageName(tree.packageDeclaration(), separator);
  }
}
