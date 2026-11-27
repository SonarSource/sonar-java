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
package org.sonar.java.semsitter;

import org.junit.jupiter.api.Test;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.MethodReferenceTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonarsource.semsitter.wire.Node;
import org.sonarsource.semsitter.wire.Ref;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.java.semsitter.TranslationStateFixtures.all;
import static org.sonar.java.semsitter.TranslationStateFixtures.inner;
import static org.sonar.java.semsitter.TranslationStateFixtures.kind;
import static org.sonar.java.semsitter.TranslationStateFixtures.leaf;
import static org.sonar.java.semsitter.TranslationStateFixtures.mutate;
import static org.sonar.java.semsitter.TranslationStateFixtures.recorded;
import static org.sonar.java.semsitter.TranslationStateFixtures.translate;
import static org.sonar.java.semsitter.TranslationStateFixtures.where;

class LambdaTranslatorTest {

  @Test
  void a_method_reference_keeps_its_explicit_type_arguments() {
    Node typeArguments = inner("type_arguments", leaf("<", "<", 25, 45), leaf("type_identifier", "Missing", 25, 46), leaf(">", ">", 25, 53));
    CompilationUnitTree unit = translate(mutate(recorded("Lambdas"), where(kind("method_reference").and(node -> node.getStartLine() == 25),
      node -> node.toBuilder().addChildren(2, typeArguments).build())), 21);

    MethodReferenceTree reference = all(unit, MethodReferenceTree.class).stream().filter(tree -> tree.method().name().equals("toString")).findFirst().orElseThrow();

    assertThat(reference.typeArguments()).hasSize(1);
    assertThat(((TypeTree) reference.typeArguments().get(0)).symbolType().name()).isEqualTo("Missing");
  }

  @Test
  void an_unbound_reference_to_a_method_returning_a_class_variable_is_translated() {
    CompilationUnitTree unit = translate(mutate(recorded("Lambdas"), where(node -> node.getStartLine() == 37 && node.getRefsCount() > 0, LambdaTranslatorTest::optionalGet)), 21);

    MethodReferenceTree reference = all(unit, MethodReferenceTree.class).stream().filter(tree -> tree.method().name().equals("get")).findFirst().orElseThrow();

    assertThat(reference.method().symbol().owner().name()).isEqualTo("Optional");
    assertThat(((ExpressionTree) reference.expression()).symbolType().is("java.util.Optional")).isTrue();
  }

  @Test
  void classes_and_lambdas_nested_in_a_lambda_body_own_their_declarations() {
    Node local = inner("class_declaration", leaf("class", "class", 21, 4), field(leaf("identifier", "Local", 21, 10), "name"),
      field(inner("class_body", leaf("{", "{", 21, 16), leaf("}", "}", 21, 17)), "body"));
    Node nested = inner("expression_statement", inner("lambda_expression",
      field(inner("formal_parameters", leaf("(", "(", 21, 19), leaf(")", ")", 21, 20)), "parameters"), leaf("->", "->", 21, 22),
      field(inner("block", leaf("{", "{", 21, 25), leaf("}", "}", 21, 26)), "body")), leaf(";", ";", 21, 27));
    CompilationUnitTree unit = translate(mutate(recorded("Lambdas"), where(kind("block").and(node -> node.getStartLine() == 20),
      node -> node.toBuilder().addChildren(1, local).addChildren(2, nested).build())), 21);

    LambdaExpressionTree typed = all(unit, LambdaExpressionTree.class).stream().filter(lambda -> lambda.parameters().size() == 2 && lambda.body().is(Tree.Kind.BLOCK)).findFirst().orElseThrow();

    assertThat(((BlockTree) typed.body()).body()).extracting(Tree::kind).containsExactly(Tree.Kind.CLASS, Tree.Kind.EXPRESSION_STATEMENT, Tree.Kind.RETURN_STATEMENT);
    assertThat(typed.symbolType().is("java.util.function.BiFunction")).isTrue();
  }

  @Test
  void a_lambda_with_fewer_parameters_than_its_target_keeps_its_parameter_owners() {
    CompilationUnitTree unit = translate(mutate(recorded("Lambdas"), where(kind("inferred_parameters").and(node -> node.getStartLine() == 19), LambdaTranslatorTest::firstParameterOnly)), 21);

    LambdaExpressionTree sum = all(unit, LambdaExpressionTree.class).stream().filter(lambda -> lambda.arrowToken().range().start().line() == 19).findFirst().orElseThrow();

    assertThat(sum.parameters()).hasSize(1);
    Symbol owner = sum.parameters().get(0).symbol().owner();
    assertThat(owner instanceof JMethodSymbol method && method.id != null && method.id.contains("$lambda$")).isFalse();
  }

  private static Node firstParameterOnly(Node parameters) {
    Node.Builder builder = parameters.toBuilder().clearChildren();
    parameters.getChildrenList().stream().filter(child -> !",".equals(child.getText()) && !"y".equals(child.getText())).forEach(builder::addChildren);
    return builder.build();
  }

  private static Node optionalGet(Node node) {
    return switch (node.getText()) {
      case "String" -> retarget(node.toBuilder().setText("Optional").build(), "ref", "java.util.Optional`1");
      case "toUpperCase" -> retarget(node.toBuilder().setText("get").build(), "r", "java.util.Optional#get()");
      default -> node;
    };
  }

  private static Node retarget(Node node, String access, String target) {
    return node.toBuilder().clearRefs().addRefs(Ref.newBuilder().setAccess(access).setOutcome("bound").setTarget(target)).build();
  }

  private static Node field(Node node, String name) {
    return node.toBuilder().setField(name).build();
  }
}
