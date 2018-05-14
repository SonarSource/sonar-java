/*
 * SonarQube Java
 * Copyright (C) 2012-2018 SonarSource SA
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
package org.sonar.java.checks.helpers;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;
import org.sonar.java.ast.parser.JavaParser;
import org.sonar.java.bytecode.loader.SquidClassLoader;
import org.sonar.java.resolve.SemanticModel;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

import static org.fest.assertions.Assertions.assertThat;

public class ConstantUtilsTest {

  @Test
  public void constants() {
    File file = new File("src/test/java//org/sonar/java/checks/helpers/ClassWithConstants.java");
    CompilationUnitTree tree = (CompilationUnitTree) JavaParser.createParser().parse(file);
    SemanticModel.createFor(tree, new SquidClassLoader(Collections.singletonList(new File("target/test-classes"))));

    ClassTree classTree = (ClassTree) tree.types().get(0);
    assertThat(constantValuesInMethod(classTree, "literals")).containsExactly("hello", null, null);
    assertThat(constantValuesInMethod(classTree, "identifiers")).containsExactly("abc", "abcdef", null);
    assertThat(constantValuesInMethod(classTree, "parentheses")).containsExactly("abc");
    assertThat(constantValuesInMethod(classTree, "member_select")).containsExactly("abc");
    assertThat(constantValuesInMethod(classTree, "concatenation")).containsExactly("hello abc", null, null);
  }

  private List<String> constantValuesInMethod(ClassTree classTree, String methodName) {
    MethodTree method = classTree.members().stream()
      .filter(m -> m.is(Tree.Kind.METHOD))
      .map(MethodTree.class::cast)
      .filter(m -> methodName.equals(m.simpleName().name()))
      .findFirst()
      .orElseThrow(() -> new IllegalStateException("no method called " + methodName));
    return method.block().body().stream()
      .map(ExpressionStatementTree.class::cast)
      .map(ExpressionStatementTree::expression)
      .map(MethodInvocationTree.class::cast)
      .map(m -> ConstantUtils.resolveAsStringConstant(m.arguments().iterator().next()))
      .collect(Collectors.toList());
  }
}
