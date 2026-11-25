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

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.ConditionalExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.NewArrayTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeCastTree;
import org.sonar.plugins.java.api.tree.UnaryExpressionTree;
import org.sonar.plugins.java.api.tree.VariableTree;
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

class ExpressionTranslatorTest {

  @Test
  void an_annotation_is_an_element_value() {
    Node nested = inner("marker_annotation", leaf("@", "@", 32, 10), leaf("identifier", "Nested", 32, 11).toBuilder().setField("name").build());
    CompilationUnitTree unit = translate(mutate(recorded("Annotations"), where(text("\"single\""), node -> nested)), 21);

    AnnotationTree config = all(unit, AnnotationTree.class).stream()
      .filter(annotation -> annotation.arguments().size() == 1 && annotation.arguments().get(0) instanceof AnnotationTree)
      .findFirst().orElseThrow();

    assertThat(((AnnotationTree) config.arguments().get(0)).annotationType().symbolType().name()).isEqualTo("Nested");
  }

  @Test
  void an_intersection_cast_separates_each_bound() {
    CompilationUnitTree unit = translate(mutate(recorded("Unresolved"), where(kind("cast_expression"), ExpressionTranslatorTest::withTwoMoreBounds)), 21);

    TypeCastTree cast = all(unit, TypeCastTree.class).get(0);

    assertThat(cast.andToken()).isNotNull();
    assertThat(cast.bounds()).hasSize(2);
    assertThat(cast.bounds().separators()).hasSize(1);
  }

  @Test
  void a_conditional_of_a_boolean_and_a_number_has_the_type_of_its_first_operand() {
    CompilationUnitTree unit = translate(mutate(recorded("Imports"), where(kind("decimal_integer_literal").and(text("1")).and(node -> node.getStartLine() == 65),
      node -> node.toBuilder().setKind("true").setText("true").build())), 21);

    ConditionalExpressionTree conditional = all(unit, ConditionalExpressionTree.class).get(0);

    assertThat(conditional.trueExpression().is(Tree.Kind.BOOLEAN_LITERAL)).isTrue();
    assertThat(conditional.symbolType().is("boolean")).isTrue();
  }

  @Test
  void a_conditional_of_a_type_variable_and_an_unrelated_type_has_the_bound_of_the_variable() {
    CompilationUnitTree unit = translate(mutate(recorded("Generics"), where(node -> node.getStartLine() == 43 && "alternative".equals(node.getField()),
      node -> leaf("string_literal", "\"x\"", 43, node.getStartCol()).toBuilder().setField("alternative").build())), 21);

    ConditionalExpressionTree conditional = all(unit, ConditionalExpressionTree.class).get(0);

    assertThat(conditional.falseExpression().is(Tree.Kind.STRING_LITERAL)).isTrue();
    assertThat(conditional.symbolType().erasure().is("java.lang.Comparable")).isTrue();
  }

  @Test
  void a_negated_unknown_operand_stays_unknown() {
    CompilationUnitTree unit = translate(mutate(recorded("Imports"), where(text("value").and(node -> node.getStartLine() == 27 && node.getRefsCount() > 0),
      node -> unresolved(node.toBuilder().setText("ghost").build()))), 21);

    UnaryExpressionTree negation = all(unit, UnaryExpressionTree.class).stream().filter(tree -> tree.is(Tree.Kind.UNARY_MINUS)).findFirst().orElseThrow();

    assertThat(negation.expression().symbolType().isUnknown()).isTrue();
    assertThat(negation.symbolType().isUnknown()).isTrue();
  }

  private static Node withTwoMoreBounds(Node cast) {
    return cast.toBuilder()
      .addChildren(2, leaf("&", "&", 35, 30))
      .addChildren(3, leaf("type_identifier", "First", 35, 32).toBuilder().setField("type").build())
      .addChildren(4, leaf("&", "&", 35, 38))
      .addChildren(5, leaf("type_identifier", "Second", 35, 40).toBuilder().setField("type").build())
      .build();
  }

  @Test
  void an_int_constant_narrows_a_conditional_only_when_it_fits(@TempDir Path baseDir) throws Exception {
    CompilationUnitTree unit = (CompilationUnitTree) Replays.parseWithEcj(baseDir, "B", "class B { int x; int[] v = {127, 128, -128, -129, 32767, 32768, 65535, -1, x}; }").get();
    List<ExpressionTree> values = ((NewArrayTree) ((VariableTree) ((ClassTree) unit.types().get(0)).members().get(1)).initializer()).initializers();
    JSema sema = new JSema();
    Type byteType = sema.primitiveType("byte");
    Type shortType = sema.primitiveType("short");
    Type charType = sema.primitiveType("char");

    assertThat(values.subList(0, 4)).extracting(value -> ExpressionTranslator.fitsIn(value, byteType)).containsExactly(true, false, true, false);
    assertThat(values.subList(4, 6)).extracting(value -> ExpressionTranslator.fitsIn(value, shortType)).containsExactly(true, false);
    assertThat(values.subList(6, 8)).extracting(value -> ExpressionTranslator.fitsIn(value, charType)).containsExactly(true, false);
    assertThat(ExpressionTranslator.fitsIn(values.get(8), byteType)).isFalse();
  }

}
