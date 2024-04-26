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

import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.sonar.plugins.java.api.query.QueryViewerUtils.TestContext;
import org.sonar.plugins.java.api.tree.Tree.Kind;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.plugins.java.api.query.QueryViewerUtils.applyQueryOnSourceCodeAndUpdateTheIssues;

public class QueryViewer {

  @Test
  void query_viewer() throws IOException {
    // ** Warning **
    // each time the test run, it update the following source code with the issues found by the query
    // so you can play with the query or the source code and see the result using one shortcut (run)
    var source = """
      package org.foo;
      
      class A {
        private int a;
      
        int foo() {
            ^^^ {1}
          return a;
          ^^^^^^ {2}
        }
      
        int bar() {
            ^^^ {1}
          return a;
          ^^^^^^ {2}
        }
      
        int boo(int b) {
          return b;
        }
      }
      """;

    var query = new CompilationUnitQuery<TestContext>()
      .types()
      .filterClassTree()
      .members()
      .filterMethodTree()
      .filter((ctx, method) -> method.parameters().isEmpty())
      .simpleName()
      .visit((ctx, simpleName) -> ctx.reportIssue(simpleName, "1"))
      .parent()
      .subtreesIf((ctx, tree) -> !tree.is(Kind.LAMBDA_EXPRESSION))
      .filterReturnStatementTree()
      .returnKeyword()
      .visit((ctx, keyword) -> ctx.reportIssue(keyword, "2"));

    String actual = applyQueryOnSourceCodeAndUpdateTheIssues(query, source, QueryViewer.class);
    assertThat(actual).isEqualTo(source);
  }

}
