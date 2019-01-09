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
package org.sonar.java.resolve;

import com.sonar.sslr.api.typed.ActionParser;
import org.junit.Test;
import org.sonar.java.ast.parser.JavaParser;
import org.sonar.java.bytecode.loader.SquidClassLoader;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class LambdaBlockReturnVisitorTest {

  private final ActionParser<Tree> p = JavaParser.createParser();

  @Test
  public void test() throws Exception {
    CompilationUnitTree cut = (CompilationUnitTree) p.parse("class A {\n" +
      "        java.util.function.Consumer<String> c = s -> {\n" +
      "          if(s.length()>0) {\n" +
      "            return;\n" +
      "          }\n" +
      "          System.out.println(s);\n" +
      "        };\n" +
      "\n" +
      "        java.util.function.Function<String, String> f = s -> {\n" +
      "          if(s.length() > 0) {\n" +
      "            return s.replace('a', 'b');\n" +
      "          }\n" +
      "          return unknownSymbol;\n" +
      "        };\n" +
      "      }");
    SemanticModel.createFor(cut, new SquidClassLoader(Collections.emptyList()));
    List<VariableTree> vars = ((ClassTree) cut.types().get(0)).members().stream().map(m -> (VariableTree) m).collect(Collectors.toList());
    LambdaBlockReturnVisitor visitor = new LambdaBlockReturnVisitor();
    ((LambdaExpressionTree) vars.get(0).initializer()).body().accept(visitor);
    assertThat(visitor.types).isEmpty();
    visitor = new LambdaBlockReturnVisitor();
    ((LambdaExpressionTree) vars.get(1).initializer()).body().accept(visitor);
    assertThat(visitor.types).hasSize(1);
  }
}
