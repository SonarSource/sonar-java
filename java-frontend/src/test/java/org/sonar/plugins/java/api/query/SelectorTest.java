/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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
package org.sonar.plugins.java.api.query;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.sonar.java.model.JParserTestUtils;
import org.sonar.plugins.java.api.query.Selector.Context;
import org.sonar.plugins.java.api.tree.Tree;

import static org.assertj.core.api.Assertions.assertThat;

class SelectorTest {

  @Test
  void test_find_package_keyword() {
    var findPackageKeywordQuery = new CompilationUnitQuery()
      .packageDeclaration()
      .packageKeyword()
      .visit((ctx, it) -> ctx.reportIssue(it, "I'm findPackageKeywordQuery"));

    var findAllMethodNamesQuery = new CompilationUnitQuery()
      .types()
      .filterClassTree()
      .members()
      .filterMethodTree()
      .simpleName()
      .visit((ctx, it) -> ctx.reportIssue(it, "It's method: " + it.name()));

    var compilationUnit = JParserTestUtils.parse("""
      package org.foo;
      class A {
        private int a;
        int foo() {
          return a;
        }
      }
      """);

    var ctx = new TestContext();

    findPackageKeywordQuery.apply(ctx, compilationUnit);
    findAllMethodNamesQuery.apply(ctx, compilationUnit);

    assertThat(ctx.issues())
      .containsExactly(
        "1:1-1:8 I'm findPackageKeywordQuery",
        "4:7-4:10 It's method: foo");
  }

  record TestContext(List<String> issues) implements Context {
    public TestContext() {
      this(new ArrayList<>());
    }

    public void reportIssue(Tree tree, String message) {
      issues.add(tree.firstToken().range().start() + "-" + tree.lastToken().range().end() + " " + message);
    }
  }

}
