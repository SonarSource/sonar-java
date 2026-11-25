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
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonarsource.semsitter.wire.Node;
import org.sonarsource.semsitter.wire.Ref;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.java.semsitter.TranslationStateFixtures.all;
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

  private static Node replaceBody(Node lambda, Node body) {
    Node.Builder builder = lambda.toBuilder().clearChildren();
    lambda.getChildrenList().forEach(child -> builder.addChildren("body".equals(child.getField()) ? body : child));
    return builder.build();
  }
}
