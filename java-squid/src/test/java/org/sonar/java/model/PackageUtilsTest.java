/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
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
package org.sonar.java.model;

import com.google.common.base.Charsets;
import com.sonar.sslr.api.typed.ActionParser;
import org.junit.Test;
import org.sonar.java.ast.parser.JavaParser;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;

import static org.fest.assertions.Assertions.assertThat;

public class PackageUtilsTest {

  private static final ActionParser PARSER = JavaParser.createParser(Charsets.UTF_8);

  @Test
  public void no_package_empty_string() {
    assertThat(packageName("class A{}")).isEqualTo("");
  }

  @Test
  public void identifier_package() {
    assertThat(packageName("package foo; class A{}")).isEqualTo("foo");
  }

  @Test
  public void member_select_package() {
    assertThat(packageName("package foo.bar.plop; class A{}")).isEqualTo("foo.bar.plop");
  }

  @Test
  public void different_separator() {
    assertThat(packageName("package foo.bar.plop; class A{}", "/")).isEqualTo("foo/bar/plop");
  }

  private static String packageName(String code) {
    return packageName(code, ".");
  }

  private static String packageName(String code, String separator) {
    CompilationUnitTree tree = (CompilationUnitTree) PARSER.parse(code);
    return PackageUtils.packageName(tree.packageDeclaration(), separator);
  }


}