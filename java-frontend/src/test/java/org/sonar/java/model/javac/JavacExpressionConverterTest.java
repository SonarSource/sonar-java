/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.model.javac;

import org.sonar.java.javac.source.tree.ClassTree;
import org.sonar.java.javac.source.tree.MethodTree;
import org.sonar.java.javac.source.tree.ReturnTree;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.ArrayAccessExpressionTree;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ConditionalExpressionTree;
import org.sonar.plugins.java.api.tree.InstanceOfTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewArrayTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeCastTree;
import org.sonar.plugins.java.api.tree.UnaryExpressionTree;

import static org.assertj.core.api.Assertions.assertThat;

class JavacExpressionConverterTest {

  @Test
  void converts_common_expression_kinds() throws IOException {
    assertThat(convert("value").kind()).isEqualTo(Tree.Kind.IDENTIFIER);
    assertThat(convert("value.field").kind()).isEqualTo(Tree.Kind.MEMBER_SELECT);
    assertThat(convert("42").kind()).isEqualTo(Tree.Kind.INT_LITERAL);
    assertThat(convert("a + 2").kind()).isEqualTo(Tree.Kind.PLUS);
    assertThat(convert("-a").kind()).isEqualTo(Tree.Kind.UNARY_MINUS);
    assertThat(convert("a++").kind()).isEqualTo(Tree.Kind.POSTFIX_INCREMENT);
    assertThat(convert("a += 2").kind()).isEqualTo(Tree.Kind.PLUS_ASSIGNMENT);
    assertThat(convert("(a)").kind()).isEqualTo(Tree.Kind.PARENTHESIZED_EXPRESSION);
    assertThat(convert("new Object()").kind()).isEqualTo(Tree.Kind.NEW_CLASS);
    assertThat(convert("flag ? left : right").kind()).isEqualTo(Tree.Kind.CONDITIONAL_EXPRESSION);
    assertThat(convert("values[index]").kind()).isEqualTo(Tree.Kind.ARRAY_ACCESS_EXPRESSION);
    assertThat(convert("(String) value").kind()).isEqualTo(Tree.Kind.TYPE_CAST);
    assertThat(convert("value instanceof String").kind()).isEqualTo(Tree.Kind.INSTANCE_OF);
  }

  @Test
  void converts_method_arguments_and_separators() throws IOException {
    MethodInvocationTree invocation = (MethodInvocationTree) convert("call(1, value)");
    assertThat(invocation.arguments()).hasSize(2);
    assertThat(invocation.arguments().separators()).hasSize(1);
    assertThat(invocation.arguments().openParenToken().text()).isEqualTo("(");
  }

  @Test
  void converts_conditional_array_access_cast_and_instanceof_tokens() throws IOException {
    ConditionalExpressionTree conditional = (ConditionalExpressionTree) convert("flag ? left : right");
    assertThat(conditional.questionToken().text()).isEqualTo("?");
    assertThat(conditional.colonToken().text()).isEqualTo(":");

    ArrayAccessExpressionTree access = (ArrayAccessExpressionTree) convert("values[index]");
    assertThat(access.dimension().openBracketToken().text()).isEqualTo("[");
    assertThat(access.dimension().closeBracketToken().text()).isEqualTo("]");

    TypeCastTree cast = (TypeCastTree) convert("(String) value");
    assertThat(cast.type().firstToken().text()).isEqualTo("String");
    assertThat(cast.closeParenToken().text()).isEqualTo(")");

    InstanceOfTree instanceOf = (InstanceOfTree) convert("value instanceof String");
    assertThat(instanceOf.instanceofKeyword().text()).isEqualTo("instanceof");
    assertThat(instanceOf.type().firstToken().text()).isEqualTo("String");
  }

  @Test
  void converts_expression_lambda() throws IOException {
    assertThat(convert("() -> 1").kind()).isEqualTo(Tree.Kind.LAMBDA_EXPRESSION);
  }

  @Test
  void preserves_literal_concatenation_folded_by_javac() throws IOException {
    assertThat(convert("\"A\" + \"B\"").kind()).isEqualTo(Tree.Kind.PLUS);
  }

  @Test
  void preserves_left_associativity_when_javac_folds_string_suffix() throws IOException {
    BinaryExpressionTree outer = (BinaryExpressionTree) convert("\"head\" + variable + \"one\" + \"two\"");
    assertThat(outer.rightOperand().firstToken().text()).isEqualTo("\"two\"");
    BinaryExpressionTree preceding = (BinaryExpressionTree) outer.leftOperand();
    assertThat(preceding.rightOperand().firstToken().text()).isEqualTo("\"one\"");
    assertThat(preceding.leftOperand().kind()).isEqualTo(Tree.Kind.PLUS);
  }

  @Test
  void preserves_unary_minus_folded_by_javac() throws IOException {
    UnaryExpressionTree negative = (UnaryExpressionTree) convert("-1");
    assertThat(negative.kind()).isEqualTo(Tree.Kind.UNARY_MINUS);
    assertThat(negative.expression().kind()).isEqualTo(Tree.Kind.INT_LITERAL);
    assertThat(negative.expression().firstToken().text()).isEqualTo("1");
  }

  @Test
  void converts_empty_trailing_array_dimensions() throws IOException {
    NewArrayTree array = (NewArrayTree) convert("new int[0][]");
    assertThat(array.type().kind()).isEqualTo(Tree.Kind.PRIMITIVE_TYPE);
    assertThat(array.dimensions()).hasSize(2);
    assertThat(array.dimensions().get(0).expression()).isNotNull();
    assertThat(array.dimensions().get(1).expression()).isNull();
  }

  @Test
  void registers_each_nested_expression() throws IOException {
    String source = "class Sample { int run() { return (a + 1) * b; } }";
    try (JavacParser.Result result = JavacParser.parse("Sample.java", source)) {
      ClassTree type = (ClassTree) result.unit().getTypeDecls().get(0);
      MethodTree method = (MethodTree) type.getMembers().get(0);
      ReturnTree statement = (ReturnTree) method.getBody().getStatements().get(0);
      List<Tree.Kind> kinds = new ArrayList<>();
      new JavacExpressionConverter(result.unit(), result.sourcePositions(), new SourceTokens(source),
        (javacTree, sonarTree) -> kinds.add(sonarTree.kind())).convert(statement.getExpression());
      assertThat(kinds).contains(Tree.Kind.IDENTIFIER, Tree.Kind.INT_LITERAL, Tree.Kind.PLUS,
        Tree.Kind.PARENTHESIZED_EXPRESSION, Tree.Kind.MULTIPLY);
    }
  }

  private static ExpressionTree convert(String expression) throws IOException {
    String source = "class Sample { Object run() { return " + expression + "; } }";
    try (JavacParser.Result result = JavacParser.parse("Sample.java", source)) {
      ClassTree type = (ClassTree) result.unit().getTypeDecls().get(0);
      MethodTree method = (MethodTree) type.getMembers().get(0);
      ReturnTree statement = (ReturnTree) method.getBody().getStatements().get(0);
      return new JavacExpressionConverter(result.unit(), result.sourcePositions(), new SourceTokens(source))
        .convert(statement.getExpression());
    }
  }
}
