/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.model;

import org.junit.jupiter.api.Test;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;

import static org.assertj.core.api.Assertions.assertThat;

class PackageUtilsTest {

  @Test
  void no_package_empty_string() {
    assertThat(packageName("class A{}")).isEmpty();
  }

  @Test
  void identifier_package() {
    assertThat(packageName("package foo; class A{}")).isEqualTo("foo");
  }

  @Test
  void member_select_package() {
    assertThat(packageName("package foo.bar.plop; class A{}")).isEqualTo("foo.bar.plop");
  }

  @Test
  void different_separator() {
    assertThat(packageName("package foo.bar.plop; class A{}", "/")).isEqualTo("foo/bar/plop");
  }

  private static String packageName(String code) {
    return packageName(code, ".");
  }

  private static String packageName(String code, String separator) {
    CompilationUnitTree tree = JParserTestUtils.parse(code);
    return PackageUtils.packageName(tree.packageDeclaration(), separator);
  }

}
