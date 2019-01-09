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
package org.sonar.java.ast.visitors;

import com.sonar.sslr.api.typed.ActionParser;
import java.util.List;
import org.junit.Test;
import org.sonar.java.ast.parser.JavaParser;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import static org.assertj.core.api.Assertions.assertThat;

public class ComplexityVisitorTest {

  private final ActionParser<Tree> p = JavaParser.createParser();


  @Test
  public void lambda_complexity() throws Exception {
    CompilationUnitTree cut = (CompilationUnitTree) p.parse("class A { Function f = s -> {if(s.isEmpty()) return s; return new MyClass(){ void foo(){if(a) return;} };};}");
    ExpressionTree lambda = ((VariableTree) ((ClassTree) cut.types().get(0)).members().get(0)).initializer();
    List<Tree> nodes = new ComplexityVisitor().getNodes(lambda);
    assertThat(nodes).hasSize(2);
  }

  @Test
  public void method_complexity() throws Exception {
    CompilationUnitTree cut = (CompilationUnitTree) p.parse("class A {" +
        " Object foo(){" +
        " if(a) { " +
        "    return new MyClass(){ " +
        "        void foo(){" +
        "            if(a) {return;} " +
        "        } " +
        "    };" +
        " } " +
        "}}");
    MethodTree methodTree = (MethodTree) ((ClassTree) cut.types().get(0)).members().get(0);
    List<Tree> nodes = new ComplexityVisitor().getNodes(methodTree);
    assertThat(nodes).hasSize(2);
  }

  @Test
  public void switch_handling() throws Exception {
    CompilationUnitTree cut = (CompilationUnitTree) p.parse(
      "class A {" +
        "  String foo(int a) {" +
        "    switch (a) {" +
        "      case 0:" +
        "        return \"none\";" +
        "      case 1:" +
        "        return \"one\";" +
        "      case 2:" +
        "        return \"many\";" +
        "      default:" +
        "        return \"it's complicated\";" +
        "    }" +
        "  }" +
        "}");
    MethodTree methodTree = (MethodTree) ((ClassTree) cut.types().get(0)).members().get(0);
    List<Tree> nodes = new ComplexityVisitor().getNodes(methodTree);
    // default case does not count.
    assertThat(nodes).hasSize(4);
  }
}
