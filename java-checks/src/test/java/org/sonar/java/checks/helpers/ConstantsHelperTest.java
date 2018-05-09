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

import com.sonar.sslr.api.typed.ActionParser;
import java.util.Collections;
import org.junit.Test;

import java.lang.reflect.Constructor;
import org.sonar.java.ast.parser.JavaParser;
import org.sonar.java.bytecode.loader.SquidClassLoader;
import org.sonar.java.resolve.SemanticModel;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.Tree;

import static org.assertj.core.api.Assertions.assertThat;

public class ConstantsHelperTest {
  private final ActionParser<Tree> p = JavaParser.createParser();

  @Test
  public void private_constructor() throws Exception {
    Constructor constructor = ConstantsHelper.class.getDeclaredConstructor();
    assertThat(constructor.isAccessible()).isFalse();
    // call for coverage
    constructor.setAccessible(true);
    constructor.newInstance();
  }

  @Test
  public void isStringLiteralWithValue_withNull_returnsFalse() {
    boolean result = ConstantsHelper.isStringLiteralWithValue(null, "expected");
    assertThat(result).isFalse();
  }

  @Test
  public void isStringLiteralWithValue_withNonStringLiteral_returnsFalse() {
    ExpressionTree tree = firstExpression("void foo(java.util.Properties props){ props.setProperty(\"myKey\", \"myValue\"); }");
    boolean result = ConstantsHelper.isStringLiteralWithValue(tree, "expected");
    assertThat(result).isFalse();
  }

  @Test
  public void isStringLiteralWithValue_withOtherValue_returnsFalse() {
    ExpressionTree tree = getReturnExpression("void foo(java.util.Properties props){ return \"other than expected\"; }");
    boolean result = ConstantsHelper.isStringLiteralWithValue(tree, "expected");
    assertThat(result).isFalse();
  }

  @Test
  public void isStringLiteralWithValue_withExpectedValue_returnsTrue() {
    ExpressionTree tree = getReturnExpression("void foo(java.util.Properties props){ return \"expected\"; }");
    boolean result = ConstantsHelper.isStringLiteralWithValue(tree, "expected");
    assertThat(result).isTrue();
  }

  private ExpressionTree firstExpression(String code) {
    CompilationUnitTree compilationUnitTree = (CompilationUnitTree) p.parse("class A { " + code + "}");
    SemanticModel.createFor(compilationUnitTree, new SquidClassLoader(Collections.emptyList()));
    ClassTree firstType = (ClassTree) compilationUnitTree.types().get(0);
    StatementTree firstStatement = ((MethodTree) firstType.members().get(0)).block().body().get(0);
    return ((ExpressionStatementTree) firstStatement).expression();
  }

  private ExpressionTree getReturnExpression(String code) {
    CompilationUnitTree compilationUnitTree = (CompilationUnitTree) p.parse("class A { " + code + "}");
    SemanticModel.createFor(compilationUnitTree, new SquidClassLoader(Collections.emptyList()));
    ClassTree firstType = (ClassTree) compilationUnitTree.types().get(0);
    ReturnStatementTree returnExpression = (ReturnStatementTree) ((MethodTree) firstType.members().get(0)).block().body().get(0);
    return returnExpression.expression();
  }
}
