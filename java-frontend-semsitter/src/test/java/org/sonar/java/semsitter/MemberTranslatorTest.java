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
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonarsource.semsitter.wire.Node;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.java.semsitter.TranslationStateFixtures.all;
import static org.sonar.java.semsitter.TranslationStateFixtures.inner;
import static org.sonar.java.semsitter.TranslationStateFixtures.kind;
import static org.sonar.java.semsitter.TranslationStateFixtures.leaf;
import static org.sonar.java.semsitter.TranslationStateFixtures.mutate;
import static org.sonar.java.semsitter.TranslationStateFixtures.recorded;
import static org.sonar.java.semsitter.TranslationStateFixtures.text;
import static org.sonar.java.semsitter.TranslationStateFixtures.translate;
import static org.sonar.java.semsitter.TranslationStateFixtures.unresolved;
import static org.sonar.java.semsitter.TranslationStateFixtures.where;

class MemberTranslatorTest {

  @Test
  void unresolved_element_names_are_the_methods_of_the_annotation_type() {
    Predicate<Node> elements = text("since").or(text("forRemoval"));
    CompilationUnitTree unit = translate(mutate(recorded("Annotations"), where(elements, TranslationStateFixtures::unresolved)), 21);

    List<IdentifierTree> names = all(unit, AssignmentExpressionTree.class).stream()
      .map(AssignmentExpressionTree::variable).filter(IdentifierTree.class::isInstance).map(IdentifierTree.class::cast)
      .filter(name -> name.name().equals("since") || name.name().equals("forRemoval"))
      .toList();

    assertThat(names).extracting(name -> name.symbol().owner().name() + "#" + name.symbol().name() + " " + name.symbolType().fullyQualifiedName())
      .containsExactly("Deprecated#since java.lang.String", "Deprecated#forRemoval boolean");
  }

  @Test
  void an_unresolved_qualified_annotation_keeps_its_written_name() {
    Node qualified = inner("scoped_identifier",
      field(inner("scoped_identifier", field(unresolved(leaf("identifier", "com", 50, 3)), "scope"), leaf(".", ".", 50, 6), field(unresolved(leaf("identifier", "example", 50, 7)), "name")), "scope"),
      leaf(".", ".", 50, 14),
      field(unresolved(leaf("identifier", "Missing", 50, 15)), "name"));
    CompilationUnitTree unit = translate(mutate(recorded("Annotations"), where(text("Override"), node -> field(qualified, "name"))), 21);

    AnnotationTree annotation = all(unit, AnnotationTree.class).stream().filter(tree -> tree.annotationType() instanceof MemberSelectExpressionTree).findFirst().orElseThrow();

    assertThat(annotation.symbolType().fullyQualifiedName()).isEqualTo("com.example.Missing");
    assertThat(annotation.annotationType().symbolType().isUnknown()).isTrue();
  }

  @Test
  void initializers_of_an_undeclared_class_are_translated() {
    CompilationUnitTree unit = translate(mutate(recorded("Initializers"), where(kind("identifier").and(text("Initializers")), node -> node.toBuilder().clearDecls().build())), 21);

    List<BlockTree> initializers = all(unit, BlockTree.class).stream().filter(block -> block.is(Tree.Kind.INITIALIZER, Tree.Kind.STATIC_INITIALIZER)).toList();

    assertThat(((ClassTree) unit.types().get(0)).symbol().isUnknown()).isTrue();
    assertThat(initializers).extracting(Tree::kind).containsExactly(Tree.Kind.STATIC_INITIALIZER, Tree.Kind.INITIALIZER, Tree.Kind.INITIALIZER);
  }

  private static Node field(Node node, String name) {
    return node.toBuilder().setField(name).build();
  }
}
