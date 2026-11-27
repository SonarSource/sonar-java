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

import java.util.List;
import org.junit.jupiter.api.Test;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
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

class InvocationTranslatorTest {

  @Test
  void a_bound_method_of_another_arity_is_unknown() {
    CompilationUnitTree unit = translate(mutate(recorded("Imports"), where(node -> node.getRefsList().stream().anyMatch(ref -> ref.getTarget().equals("java.lang.Math#abs(int)")),
      node -> node.toBuilder().setText("max").clearRefs().addRefs(Ref.newBuilder().setAccess("r").setOutcome("bound").setTarget("java.lang.Math#max(int,int)")).build())), 21);

    List<MethodInvocationTree> calls = all(unit, MethodInvocationTree.class).stream()
      .filter(call -> call.methodSelect() instanceof IdentifierTree name && name.name().equals("max"))
      .toList();

    assertThat(calls).isNotEmpty().allSatisfy(call -> {
      assertThat(call.methodSymbol().isUnknown()).isTrue();
      assertThat(call.methodSymbol().name()).isEqualTo("max");
    });
  }

  @Test
  void a_lambda_argument_creating_an_object_is_translated() {
    Node creation = inner("object_creation_expression", leaf("new", "new", 20, 20),
      leaf("type_identifier", "Object", 20, 24).toBuilder().setField("type").addRefs(Ref.newBuilder().setAccess("ref").setOutcome("bound").setTarget("java.lang.Object")).build(),
      inner("argument_list", leaf("(", "(", 20, 30), leaf(")", ")", 20, 31)).toBuilder().setField("arguments").build()).toBuilder().setField("body").build();
    CompilationUnitTree unit = translate(mutate(recorded("Initializers"), where(kind("lambda_expression").and(node -> node.getStartLine() == 20),
      node -> replaceBody(node, creation))), 21);

    LambdaExpressionTree lambda = all(unit, LambdaExpressionTree.class).get(0);

    assertThat(lambda.body()).isInstanceOfSatisfying(NewClassTree.class, created -> assertThat(created.symbolType().is("java.lang.Object")).isTrue());
    assertThat(((MethodInvocationTree) lambda.parent().parent()).methodSymbol().name()).isEqualTo("add");
  }

  @Test
  void a_creation_keeps_its_constructor_type_arguments() {
    Node typeArguments = inner("type_arguments", leaf("<", "<", 14, 25), leaf("type_identifier", "Other", 14, 26), leaf(">", ">", 14, 31)).toBuilder().setField("type_arguments").build();
    CompilationUnitTree unit = translate(mutate(recorded("Unresolved"), where(kind("object_creation_expression").and(node -> node.getStartLine() == 14),
      node -> node.toBuilder().addChildren(1, typeArguments).build())), 21);

    NewClassTree creation = all(unit, NewClassTree.class).get(0);

    assertThat(creation.typeArguments()).hasSize(1);
    assertThat(((TypeTree) creation.typeArguments().get(0)).symbolType().name()).isEqualTo("Other");
  }

  private static Node replaceBody(Node lambda, Node body) {
    Node.Builder builder = lambda.toBuilder().clearChildren();
    lambda.getChildrenList().forEach(child -> builder.addChildren("body".equals(child.getField()) ? body : child));
    return builder.build();
  }
}
