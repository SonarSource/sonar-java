/*
 * SonarQube Java
 * Copyright (C) 2012-2023 SonarSource SA
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
package org.sonar.java.model.pattern;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.sonar.java.model.JParserTestUtils;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.CaseGroupTree;
import org.sonar.plugins.java.api.tree.CaseLabelTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.ConditionalExpressionTree;
import org.sonar.plugins.java.api.tree.DefaultPatternTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.GuardedPatternTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NullPatternTree;
import org.sonar.plugins.java.api.tree.ParenthesizedTree;
import org.sonar.plugins.java.api.tree.PatternInstanceOfTree;
import org.sonar.plugins.java.api.tree.PatternTree;
import org.sonar.plugins.java.api.tree.RecordPatternTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.SwitchExpressionTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypePatternTree;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.VariableTree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.java.model.assertions.SymbolAssert.assertThat;
import static org.sonar.java.model.assertions.TreeAssert.assertThat;
import static org.sonar.java.model.assertions.TypeAssert.assertThat;

class AbstractPatternTreeTest {

  private static final String BASE_SOURCE_CODE = "class A {\n"
    + "  static Object method(%s) {\n"
    + "    return %s;\n"
    + "  }\n"
    + "  public sealed interface Shape permits Rectangle,Triangle {\n"
    + "    default int volume() { return 0; }\n"
    + "  }\n"
    + "  public static non-sealed class Rectangle implements Shape {\n"
    + "    private int base, height;\n"
    + "    Rectangle(int base, int height) { this.base = base; this.height = height; }\n"
    + "  }\n"
    + "  public static final class Square extends Rectangle {\n"
    + "    Square(int side) { super(side, side); }\n"
    + "  }\n"
    + "  public static record Triangle(int a, int b, int c) implements Shape {}\n"
    + "}\n";

  @Test
  void test_default_pattern() {
    String code = "switch (o) {\n"
      + "  case default -> o;\n"
      + "}";
    SwitchExpressionTree s = switchExpressionTree("Object o", code);
    List<CaseLabelTree> labels = s.cases().get(0).labels();
    assertThat(labels).hasSize(1);
    List<ExpressionTree> defaultExpressions = labels.get(0).expressions();
    assertThat(defaultExpressions).hasSize(1);
    ExpressionTree patternExpression = defaultExpressions.get(0);
    assertThat(patternExpression).is(Tree.Kind.DEFAULT_PATTERN);
    assertThat(((DefaultPatternTree) patternExpression).defaultToken()).is("default");
    assertThat(patternExpression.symbolType()).isUnknown();
    assertThat(patternExpression.asConstant()).isEmpty();
    assertThat(patternExpression.asConstant(Object.class)).isEmpty();
  }

  @Test
  void test_null_pattern() {
    String code = "switch (o) {\n"
      + "  case null -> -1;\n"
      + "  default -> o;\n"
      + "}";
    SwitchExpressionTree s = switchExpressionTree("Object o", code);
    List<CaseGroupTree> cases = s.cases();
    List<CaseLabelTree> nullLabels = cases.get(0).labels();
    assertThat(nullLabels).hasSize(1);
    List<ExpressionTree> nullExpressions = nullLabels.get(0).expressions();
    assertThat(nullExpressions).hasSize(1);
    ExpressionTree patternExpression = nullExpressions.get(0);
    assertThat(patternExpression).is(Tree.Kind.NULL_PATTERN);
    NullPatternTree nullPattern = (NullPatternTree) patternExpression;
    assertThat(patternExpression.symbolType()).isSameAs(nullPattern.nullLiteral().symbolType());
    assertThat(patternExpression.asConstant()).isSameAs(nullPattern.nullLiteral().asConstant());
    assertThat(patternExpression.asConstant(Object.class)).isSameAs(nullPattern.nullLiteral().asConstant(Object.class));

    CaseGroupTree defaultCase = cases.get(1);
    List<CaseLabelTree> defaultLabels = defaultCase.labels();
    assertThat(defaultLabels).hasSize(1);
    assertThat(defaultLabels.get(0).expressions()).isEmpty();
    assertThat(defaultLabels.get(0).caseOrDefaultKeyword()).is("default");
  }

  @Test
  void test_default_null_combined_pattern() {
    String code = "switch (o) {\n"
      + "  case null, default -> -1;\n"
      + "}";
    SwitchExpressionTree s = switchExpressionTree("Object o", code);
    List<CaseLabelTree> labels = s.cases().get(0).labels();
    assertThat(labels).hasSize(1);
    List<ExpressionTree> expressions = labels.get(0).expressions();
    assertThat(expressions).hasSize(2);
    assertThat(expressions.get(0)).is(Tree.Kind.NULL_PATTERN);
    assertThat(expressions.get(1)).is(Tree.Kind.DEFAULT_PATTERN);
  }

  @Test
  void test_type_pattern() {
    String code = "switch (o) {\n"
      + "  case Integer i -> -1;\n"
      + "  default -> o;\n"
      + "}";
    SwitchExpressionTree s = switchExpressionTree("Object o", code);
    List<CaseLabelTree> labels = s.cases().get(0).labels();
    assertThat(labels).hasSize(1);
    List<ExpressionTree> expressions = labels.get(0).expressions();
    assertThat(expressions).hasSize(1);
    ExpressionTree expression = expressions.get(0);
    assertThat(expression).is(Tree.Kind.TYPE_PATTERN);
    TypePatternTree typePattern = (TypePatternTree) expression;
    VariableTree patternVariable = typePattern.patternVariable();
    assertThat(patternVariable.symbol()).isOfType("java.lang.Integer");
    assertThat(typePattern.symbolType()).isSameAs(patternVariable.symbol().type());
    assertThat(typePattern.asConstant()).isEmpty();
    assertThat(typePattern.asConstant(Object.class)).isEmpty();
  }

  @Test
  void test_array_type_pattern() {
    String code = "switch (o) {\n"
      + "  case Integer[] i -> -1;\n"
      + "  default -> o;\n"
      + "}";
    SwitchExpressionTree s = switchExpressionTree("Object o", code);
    List<CaseLabelTree> labels = s.cases().get(0).labels();
    assertThat(labels).hasSize(1);
    List<ExpressionTree> expressions = labels.get(0).expressions();
    assertThat(expressions).hasSize(1);
    ExpressionTree expression = expressions.get(0);
    assertThat(expression).is(Tree.Kind.TYPE_PATTERN);
    VariableTree patternVariable = ((TypePatternTree) expression).patternVariable();
    assertThat(patternVariable.symbol()).isOfType("java.lang.Integer[]");
  }

  @Test
  void test_guarded_pattern() {
    String code = "switch (shape) {\n"
      + "    case Rectangle r when r.volume() > 42 -> String.format(\"big rectangle of volume %d!\", r.volume());\n"
      + "    default -> \"default case\";\n"
      + "  }";
    SwitchExpressionTree s = switchExpressionTree("Shape shape", code);
    List<CaseLabelTree> labels = s.cases().get(0).labels();
    assertThat(labels).hasSize(1);
    List<ExpressionTree> expressions = labels.get(0).expressions();
    assertThat(expressions).hasSize(1);
    ExpressionTree expression = expressions.get(0);
    assertThat(expression).is(Tree.Kind.GUARDED_PATTERN);
    GuardedPatternTree guardedPattern = (GuardedPatternTree) expression;
    assertThat(guardedPattern.pattern()).is(Tree.Kind.TYPE_PATTERN);
    assertThat(guardedPattern.whenOperator()).is("when");
    assertThat(guardedPattern.expression()).is(Tree.Kind.GREATER_THAN);
    assertThat(guardedPattern.symbolType()).isUnknown();
    assertThat(guardedPattern.asConstant()).isEmpty();
    assertThat(guardedPattern.asConstant(Object.class)).isEmpty();
  }

  @Test
  void test_guarded_pattern_parenthesized() {
    String code = "switch (shape) {\n"
      + "    case (Rectangle r) when (r.volume() > 42) -> String.format(\"big rectangle of volume %d!\", r.volume());\n"
      + "    default -> \"default case\";\n"
      + "  }";
    SwitchExpressionTree s = switchExpressionTree("Shape shape", code);
    List<CaseLabelTree> labels = s.cases().get(0).labels();
    assertThat(labels).hasSize(1);
    List<ExpressionTree> expressions = labels.get(0).expressions();
    assertThat(expressions).hasSize(1);
    ExpressionTree expression = expressions.get(0);
    assertThat(expression).is(Tree.Kind.GUARDED_PATTERN);
    GuardedPatternTree guardedPattern = (GuardedPatternTree) expression;
    assertThat(guardedPattern.pattern()).is(Tree.Kind.TYPE_PATTERN);
    ExpressionTree guardedExpression = guardedPattern.expression();
    assertThat(guardedExpression).is(Tree.Kind.PARENTHESIZED_EXPRESSION);
    assertThat(((ParenthesizedTree) guardedExpression).expression()).is(Tree.Kind.GREATER_THAN);
  }

  @Test
  void test_guarded_pattern_parenthesized_nested() {
    String code = "switch (shape) {\n"
      + "    case Rectangle r when r.volume() > 42 && false -> String.format(\"big rectangle of volume %d!\", r.volume());\n"
      + "    default -> \"default case\";\n"
      + "  }";
    SwitchExpressionTree s = switchExpressionTree("Shape shape", code);
    List<CaseLabelTree> labels = s.cases().get(0).labels();
    assertThat(labels).hasSize(1);
    List<ExpressionTree> expressions = labels.get(0).expressions();
    assertThat(expressions).hasSize(1);
    ExpressionTree expression = expressions.get(0);
    assertThat(expression).is(Tree.Kind.GUARDED_PATTERN);
    GuardedPatternTree guardedPattern = (GuardedPatternTree) expression;
    assertThat(guardedPattern.pattern()).is(Tree.Kind.TYPE_PATTERN);
    // ECJ transform the guarded pattern in and drop the parenthesis
    assertThat(guardedPattern.expression()).is(Tree.Kind.CONDITIONAL_AND);
    BinaryExpressionTree and = (BinaryExpressionTree) guardedPattern.expression();
    assertThat(and.leftOperand()).is(Tree.Kind.GREATER_THAN);
    assertThat(and.rightOperand()).is(Tree.Kind.BOOLEAN_LITERAL);
  }

  @Test
  void test_guarded_pattern_mixed() {
    String code = "switch (shape) {\n"
      + "      case null -> \"null case\";\n"
      + "      case Triangle t -> String.format(\"triangle (%d,%d,%d)\", t.a(), t.b(), t.c());\n"
      + "      case Rectangle r when r.volume() > 42 -> String.format(\"big rectangle of volume %d!\", r.volume());\n"
      + "      case Square s -> \"Square!\";\n"
      + "      case Rectangle r -> String.format(\"Rectangle (%d,%d)\", r.base, r.height);\n"
      + "      case default -> \"default case\";\n"
      + "    }";
    SwitchExpressionTree s = switchExpressionTree("Shape shape", code);
    List<CaseGroupTree> cases = s.cases();
    assertThat(cases).hasSize(6);
    assertThat(cases.stream()
      .map(CaseGroupTree::labels)
      .map(labels -> labels.get(0)))
      .map(CaseLabelTree::expressions)
      .map(expressions -> expressions.get(0))
      .map(Tree::kind)
      .containsExactly(
        Tree.Kind.NULL_PATTERN,
        Tree.Kind.TYPE_PATTERN,
        Tree.Kind.GUARDED_PATTERN,
        Tree.Kind.TYPE_PATTERN,
        Tree.Kind.TYPE_PATTERN,
        Tree.Kind.DEFAULT_PATTERN);
  }

  @Test
  void test_base_tree_visitor() {
    String code = "switch (shape) {\n"
      + "      case null -> \"null case\";\n"
      + "      case Triangle t -> String.format(\"triangle (%d,%d,%d)\", t.a(), t.b(), t.c());\n"
      + "      case Rectangle r when r.volume() > 42 -> String.format(\"big rectangle of volume %d!\", r.volume());\n"
      + "      case default -> \"default case\";\n"
      + "    }";
    SwitchExpressionTree s = switchExpressionTree("Shape shape", code);
    List<Tree.Kind> patternKinds = new ArrayList<>();
    s.accept(new BaseTreeVisitor() {
      @Override
      public void visitNullPattern(NullPatternTree tree) {
        patternKinds.add(tree.kind());
        super.visitNullPattern(tree);
      }

      @Override
      public void visitDefaultPattern(DefaultPatternTree tree) {
        patternKinds.add(tree.kind());
        super.visitDefaultPattern(tree);
      }

      @Override
      public void visitTypePattern(TypePatternTree tree) {
        patternKinds.add(tree.kind());
        super.visitTypePattern(tree);
      }

      @Override
      public void visitGuardedPattern(GuardedPatternTree tree) {
        patternKinds.add(tree.kind());
        super.visitGuardedPattern(tree);
      }
    });

    assertThat(patternKinds).containsExactly(
        Tree.Kind.NULL_PATTERN,
        Tree.Kind.TYPE_PATTERN,
        Tree.Kind.GUARDED_PATTERN,
        // from the guarded pattern child node
        Tree.Kind.TYPE_PATTERN,
        Tree.Kind.DEFAULT_PATTERN);
  }

  @Test
  void test_record_pattern_in_instanceof() {
    String code = "(shape instanceof Triangle(int a, int b, int c) t) ? new Object() : new Object()";
    ConditionalExpressionTree conditionalExpressionTree = conditionalExpressionTree("Shape shape", code);
    assertThat(conditionalExpressionTree.is(Tree.Kind.CONDITIONAL_EXPRESSION)).isTrue();
    ExpressionTree instanceOfexpression = ((ParenthesizedTree) conditionalExpressionTree.condition()).expression();
    assertThat(instanceOfexpression).is(Tree.Kind.PATTERN_INSTANCE_OF);
    PatternTree pattern = ((PatternInstanceOfTree) instanceOfexpression).pattern();
    assertThat(pattern).is(Tree.Kind.TYPE_PATTERN);
  }

  @Test
  void test_record_pattern_in_switch() {
    String code = "switch (shape) { case Triangle(int a, int b, int c) t -> new Object(); default -> null; }";
    SwitchExpressionTree switchCase = switchExpressionTree("Shape shape", code);
    assertThat(switchCase).is(Tree.Kind.SWITCH_EXPRESSION);
    var firstCase = switchCase.cases().stream().findFirst().get();
    var label = firstCase.labels().stream().findFirst().get();
    var expression = label.expressions().stream().findFirst().get();
    assertThat(expression).is(Tree.Kind.RECORD_PATTERN);
    RecordPatternTree recordPattern = (RecordPatternTree) expression;
    assertThat(recordPattern.patterns())
      .hasSize(3)
      .allMatch(pattern -> {
        if (!pattern.is(Tree.Kind.TYPE_PATTERN)) {
          return false;
        }
        TypePatternTree typePattern = (TypePatternTree) pattern;
        VariableTree variableTree = typePattern.patternVariable();
        return variableTree.type().symbolType().is("int") && variableTree.simpleName() != null;
      });
    TypeTree type = recordPattern.type();
    assertThat(type.annotations()).isEmpty();
    assertThat(type.toString()).isEqualTo("Triangle");
    assertThat(recordPattern.name().name()).isEqualTo("t");
  }

  private static SwitchExpressionTree switchExpressionTree(String methodParametersDeclaration, String switchExpressionCode) {
    CompilationUnitTree cut = JParserTestUtils.parse(String.format(BASE_SOURCE_CODE, methodParametersDeclaration, switchExpressionCode));
    ClassTree classTree = (ClassTree) cut.types().get(0);
    MethodTree methodTree = (MethodTree) classTree.members().get(0);
    ReturnStatementTree returnStatementTree = (ReturnStatementTree) methodTree.block().body().get(0);
    return (SwitchExpressionTree) returnStatementTree.expression();
  }

  private static ConditionalExpressionTree conditionalExpressionTree(String methodParametersDeclaration, String ifStatementCode) {
    CompilationUnitTree cut = JParserTestUtils.parse(String.format(BASE_SOURCE_CODE, methodParametersDeclaration, ifStatementCode));
    ClassTree classTree = (ClassTree) cut.types().get(0);
    MethodTree methodTree = (MethodTree) classTree.members().get(0);
    ReturnStatementTree returnStatementTree = (ReturnStatementTree) methodTree.block().body().get(0);
    return (ConditionalExpressionTree) returnStatementTree.expression();
  }
}
