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
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;

import static org.assertj.core.api.Assertions.assertThat;

class SelectorTest {

  @Test
  void test_find_package_keyword() {
    var findPackageKeywordQuery = new CompilationUnitQuery<TestContext>()
      .packageDeclaration()
      .packageKeyword()
      .visit((ctx, it) -> ctx.reportIssue(it, "I'm findPackageKeywordQuery"));

    var findMethodNamesNotDeepQuery = new CompilationUnitQuery<TestContext>()
      .types()
      .filterClassTree()
      .members()
      .filterMethodTree()
      .simpleName()
      .visit((ctx, it) -> ctx.reportIssue(it, "It's method1: " + it.name()));

    var findAllMethodNamesQuery = new CompilationUnitQuery<TestContext>()
      .subtreesIf((ctx, tree) -> !tree.is(Kind.METHOD, Kind.LAMBDA_EXPRESSION))
      .filterMethodTree()
      .simpleName()
      .visit((ctx, it) -> ctx.reportIssue(it, "It's method2: " + it.name()));

    var compilationUnit = JParserTestUtils.parse("""
      package org.foo;
      class A {
        private int a;
      
        int foo() {
          return a;
        }
        class B {
          int bar() {
            return a;
          }
        }
      }
      """);

    var ctx = new TestContext();

    findPackageKeywordQuery.apply(ctx, compilationUnit);
    findMethodNamesNotDeepQuery.apply(ctx, compilationUnit);
    findAllMethodNamesQuery.apply(ctx, compilationUnit);

    assertThat(ctx.issues())
      .containsExactly(
        "1:1-1:8 I'm findPackageKeywordQuery",
        "5:7-5:10 It's method1: foo",
        "5:7-5:10 It's method2: foo",
        "9:9-9:12 It's method2: bar");
  }

  public record TestContext(List<String> issues) {
    public TestContext() {
      this(new ArrayList<>());
    }

    public void reportIssue(Tree tree, String message) {
      issues.add(tree.firstToken().range().start() + "-" + tree.lastToken().range().end() + " " + message);
    }
  }

}
