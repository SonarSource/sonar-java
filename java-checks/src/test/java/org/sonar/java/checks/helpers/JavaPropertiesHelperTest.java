/*
 * SonarQube Java
 * Copyright (C) 2012-2022 SonarSource SA
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

import org.junit.jupiter.api.Test;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.lang.reflect.Constructor;
import static org.assertj.core.api.Assertions.assertThat;

class JavaPropertiesHelperTest {

  @Test
  void private_constructor() throws Exception {
    Constructor<JavaPropertiesHelper> constructor = JavaPropertiesHelper.class.getDeclaredConstructor();
    assertThat(constructor.isAccessible()).isFalse();
    constructor.setAccessible(true);
    constructor.newInstance();
  }

  @Test
  void null_if_not_get_property() throws Exception {
    ExpressionTree tree = firstExpression("void foo(java.util.Properties props){ props.setProperty(\"myKey\", \"myValue\"); }");
    ExpressionTree defaultValue = JavaPropertiesHelper.retrievedPropertyDefaultValue(tree);
    assertThat(defaultValue).isNull();

    tree = firstExpression("void foo(){ System.out.println(); }");
    defaultValue = JavaPropertiesHelper.retrievedPropertyDefaultValue(tree);
    assertThat(defaultValue).isNull();
  }

  @Test
  void null_if_get_property_without_default_value() throws Exception {
    ExpressionTree tree = firstExpression("void foo(java.util.Properties props){ props.getProperty(\"myKey\"); }");
    ExpressionTree defaultValue = JavaPropertiesHelper.retrievedPropertyDefaultValue(tree);
    assertThat(defaultValue).isNull();
  }
  
  @Test
  void null_if_not_identifier_or_method_invocation() throws Exception {
    ExpressionTree tree = firstExpression("void foo(int a){ a++; }");
    ExpressionTree defaultValue = JavaPropertiesHelper.retrievedPropertyDefaultValue(tree);
    assertThat(defaultValue).isNull();
  }

  @Test
  void retrieve_default_value_on_method_invocation() throws Exception {
    ExpressionTree tree = firstExpression("void foo(java.util.Properties props){ props.getProperty(\"myKey\", \"defaultValue\"); }");
    ExpressionTree defaultValue = JavaPropertiesHelper.retrievedPropertyDefaultValue(tree);
    assertThat(defaultValue).isNotNull();
    assertThat(defaultValue.is(Tree.Kind.STRING_LITERAL)).isTrue();
  }

  @Test
  void retrieve_default_value_on_identifier() throws Exception {
    ExpressionTree tree = firstExpression(
      "void foo(String prop){ foo(myValue); } "
        + "java.util.Properties props = new java.util.Properties();"
        + "String myValue = props.getProperty(\"myKey\", \"defaultValue\");");
    ExpressionTree defaultValue = JavaPropertiesHelper.retrievedPropertyDefaultValue(((MethodInvocationTree) tree).arguments().get(0));
    assertThat(defaultValue).isNotNull();
    assertThat(defaultValue.is(Tree.Kind.STRING_LITERAL)).isTrue();
  }

  @Test
  void null_if_variable_used_more_than_once() throws Exception {
    ExpressionTree tree = firstExpression(
      "void foo(String prop){ foo(myValue); myValue = 0; } "
        + "java.util.Properties props = new java.util.Properties();"
        + "String myValue = props.getProperty(\"myKey\", \"defaultValue\");");
    ExpressionTree defaultValue = JavaPropertiesHelper.retrievedPropertyDefaultValue(((MethodInvocationTree) tree).arguments().get(0));
    assertThat(defaultValue).isNull();
  }

  @Test
  void null_if_variable_not_initialized() throws Exception {
    ExpressionTree tree = firstExpression(
      "void foo(String prop){ foo(myValue);} "
        + "String myValue;");
    ExpressionTree defaultValue = JavaPropertiesHelper.retrievedPropertyDefaultValue(((MethodInvocationTree) tree).arguments().get(0));
    assertThat(defaultValue).isNull();
  }

  @Test
  void null_if_variable_not_initilialized_method_invocation() throws Exception {
    ExpressionTree tree = firstExpression(
      "void foo(String prop){ foo(myValue);} "
        + "java.util.Properties props = new java.util.Properties();"
        + "String myValue = \"hello\";");
    ExpressionTree defaultValue = JavaPropertiesHelper.retrievedPropertyDefaultValue(((MethodInvocationTree) tree).arguments().get(0));
    assertThat(defaultValue).isNull();
  }

  @Test
  void null_if_unknown_symbol() throws Exception {
    ExpressionTree tree = firstExpression("void foo() { unknown(e -> e.method()); }");
    ExpressionTree functionCallArgument = ((MethodInvocationTree) tree).arguments().get(0);
    MethodInvocationTree methodCallInsideLambda = (MethodInvocationTree)((LambdaExpressionTree)functionCallArgument).body();
    ExpressionTree defaultValue = JavaPropertiesHelper.retrievedPropertyDefaultValue(methodCallInsideLambda);
    assertThat(defaultValue).isNull();
  }

  private ExpressionTree firstExpression(String code) {
    CompilationUnitTree compilationUnitTree = JParserTestUtils.parse("class A { " + code + "}");
    ClassTree firstType = (ClassTree) compilationUnitTree.types().get(0);
    StatementTree firstStatement = ((MethodTree) firstType.members().get(0)).block().body().get(0);
    return ((ExpressionStatementTree) firstStatement).expression();
  }

}
