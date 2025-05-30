/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.model.pattern;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.sonar.java.model.JParserTestUtils;
import org.sonar.java.model.JavaTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.CaseGroupTree;
import org.sonar.plugins.java.api.tree.CaseLabelTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.DefaultPatternTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.GuardedPatternTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NullPatternTree;
import org.sonar.plugins.java.api.tree.RecordPatternTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.SwitchExpressionTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypePatternTree;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.VariableTree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.java.model.assertions.SymbolAssert.assertThat;
import static org.sonar.java.model.assertions.TreeAssert.assertThat;
import static org.sonar.java.model.assertions.TypeAssert.assertThat;

class AbstractPatternTreeTest {

  private static final String BASE_SOURCE_CODE = """
    class A {
      static Object method(%s) {
        return %s;
      }
      public sealed interface Shape permits Rectangle,Triangle {
        default int volume() { return 0; }
      }
      public static non-sealed class Rectangle implements Shape {
        private int base, height;
        Rectangle(int base, int height) { this.base = base; this.height = height; }
      }
      public static final class Square extends Rectangle {
        Square(int side) { super(side, side); }
      }
      public static record Triangle(int a, int b, int c) implements Shape {}
    }
    """;

  @Test
  void test_default_pattern() {
    String code = """
      switch (o) {
        case default -> o;
      }
      """;
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
    String code = """
      switch (o) {
        case null -> -1;
        default -> o;
      }
      """;
    SwitchExpressionTree s = switchExpressionTree("Object o", code);
    List<CaseGroupTree> cases = s.cases();
    List<CaseLabelTree> nullLabels = cases.get(0).labels();
    assertThat(nullLabels).hasSize(1);
    List<ExpressionTree> nullExpressions = nullLabels.get(0).expressions();
    assertThat(nullExpressions).hasSize(1);
    ExpressionTree patternExpression = nullExpressions.get(0);
    assertThat(patternExpression).is(Tree.Kind.NULL_PATTERN);
    NullPatternTree nullPattern = (NullPatternTree) patternExpression;
    assertThat(nullPattern.symbolType().fullyQualifiedName()).isEqualTo("<nulltype>");
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
    String code = """
      switch (o) {
        case null, default -> -1;
      }
      """;
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
    String code = """
      switch (o) {
        case Integer i -> -1;
        default -> o;
      }
      """;
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
    assertThat(typePattern.symbolType().fullyQualifiedName()).isEqualTo("java.lang.Integer");
    assertThat(typePattern.symbolType()).isSameAs(patternVariable.symbol().type());
    assertThat(typePattern.asConstant()).isEmpty();
    assertThat(typePattern.asConstant(Object.class)).isEmpty();
  }

  @Test
  void test_array_type_pattern() {
    String code = """
      switch (o) {
        case Integer[] i -> -1;
        default -> o;
      }
      """;
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
    String code = """
        switch (shape) {
          case Rectangle r when r.volume() > 42 -> String.format("big rectangle of volume %d!", r.volume());
          default -> "default case";
        }
      """;
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
    assertThat(guardedPattern.whenOperator().range()).hasToString("(4:22)-(4:26)");
    assertThat(guardedPattern.expression()).is(Tree.Kind.GREATER_THAN);
    assertThat(guardedPattern.symbolType().fullyQualifiedName()).isEqualTo("A$Rectangle");
    assertThat(guardedPattern.asConstant()).isEmpty();
    assertThat(guardedPattern.asConstant(Object.class)).isEmpty();
  }

  static Stream<Arguments> parsing_when_token_as_identifier_or_guarded_pattern_arguments() {
    return Stream.of(
      // JDT core 3.33.0 raises RecognitionException:
      // Parse error at line 3 column 4: Syntax error on token "RestrictedIdentifierWhen", while expected
      Arguments.of("""
        public class X {
          public static void main(String argv[]) {
            when("Pass");
          }
          static void when(String arg) {
            System.out.println(arg);
          }
        }
        """,
        "IDENTIFIER,IDENTIFIER"),
      // JDT core 3.33.0 raises RecognitionException:
      // Parse error at line 3 column 4: Syntax error on token "RestrictedIdentifierWhen", delete this token
      Arguments.of("""
        public class when {
          public static void main(String argv[]) {
            when x = new when();
            System.out.println(x);
          }
          public String toString() {
            return "Pass";
          }
        }
        """,
        "IDENTIFIER,IDENTIFIER,IDENTIFIER"),
      // JDT core 3.33.0 raises RecognitionException:
      // Parse error at line 4 column 16: Syntax error on token "x", delete this token
      Arguments.of("""
        public class when {
          public String toString() {
            return switch((Object) this) {
              case when x -> "Pass";
              default -> "Fail";
            };
          }
          public static void main(String argv[]) {
            System.out.println(new when());
          }
        }
        """,
        "IDENTIFIER,IDENTIFIER,IDENTIFIER"),
      // JDT core 3.33.0 raises RecognitionException:
      // Parse error at line 3 column 34: Syntax error on token "RestrictedIdentifierWhen", delete this token
      Arguments.of("""
        public class X {
          public static void main(String argv[]) {
            System.out.println( (Boolean) when(true) );
          }
          static Object when(Object arg) {
            return arg;
          }
        }
        """,
        "IDENTIFIER,IDENTIFIER"),
      // JDT core 3.33.0 raises RecognitionException:
      // Parse error at line 5 column 6: Syntax error on token "case", BeginCaseElement expected after this token
      Arguments.of("""
        class when {
          boolean when = true;
          static boolean when(when arg) {
            return switch(arg) {
              case when when when when.when && when.when(null) -> when.when;
              case null -> true;
              default -> false;
            };
          }
          public static void main(String[] args) {
            System.out.println(when(new when()));
          }
        }
        """,
        "IDENTIFIER,IDENTIFIER,IDENTIFIER,IDENTIFIER,IDENTIFIER,IDENTIFIER,GUARDED_PATTERN," +
          "IDENTIFIER,IDENTIFIER,IDENTIFIER,IDENTIFIER,IDENTIFIER,IDENTIFIER,IDENTIFIER,IDENTIFIER"));
  }

  @ParameterizedTest
  @MethodSource("parsing_when_token_as_identifier_or_guarded_pattern_arguments")
  void parsing_when_token_as_identifier_or_guarded_pattern(String code, String expectedWhenTokenParentKinds) {
    // JDT core 3.33.0 had a bug that prevented to parse java code when the "when" token was used as an identifier
    // starting a method body. See: https://github.com/eclipse-jdt/eclipse.jdt.core/issues/456#issuecomment-1520710765
    // this test ensure this bug has been fixed by JDT core 3.36.0 and does not reappear later.
    Tree tree = JParserTestUtils.parse(code);
    String actual = recursivelyExtractWhenTokenParentKinds(tree).stream()
        .map(Enum::name)
          .collect(Collectors.joining(","));
    assertThat(actual).isEqualTo(expectedWhenTokenParentKinds);
  }

  static List<Tree.Kind> recursivelyExtractWhenTokenParentKinds(Tree parent) {
    List<Tree.Kind> kinds = new ArrayList<>();
    for (Tree child : ((JavaTree) parent).getChildren()) {
      if (child.is(Tree.Kind.TOKEN)) {
        if ("when".equals(((SyntaxToken) child).text())) {
          kinds.add(parent.kind());
        }
      } else {
        kinds.addAll(recursivelyExtractWhenTokenParentKinds(child));
      }
    }
    return kinds;
  }

  @Test
  void test_guarded_pattern_parenthesized_nested() {
    String code = """
        switch (shape) {
          case Rectangle r when r.volume() > 42 && false -> String.format("big rectangle of volume %d!", r.volume());
          default -> "default case";
        }
      """;
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
    String code = """
        switch (shape) {
          case null -> "null case";
          case Triangle t -> String.format("triangle (%d,%d,%d)", t.a(), t.b(), t.c());
          case Rectangle r when r.volume() > 42 -> String.format("big rectangle of volume %d!", r.volume());
          case Square s -> "Square!";
          case Rectangle r -> String.format("Rectangle (%d,%d)", r.base, r.height);
          case default -> "default case";
        }
      """;
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
    String code = """
        switch (shape) {
          case null -> "null case";
          case Triangle(int a, var b, int c) when a + b < 42 -> String.format("Big trangle");
          case Triangle t -> String.format("triangle (%d,%d,%d)", t.a(), t.b(), t.c());
          case Rectangle r when r.volume() > 42 -> String.format("big rectangle of volume %d!", r.volume());
          case default -> "default case";
        }
      """;
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

      @Override
      public void visitRecordPattern(RecordPatternTree tree) {
        patternKinds.add(tree.kind());
        super.visitRecordPattern(tree);
      }
    });

    assertThat(patternKinds).containsExactly(
        Tree.Kind.NULL_PATTERN,
        Tree.Kind.GUARDED_PATTERN,
        // from the record pattern guarded pattern child node
        Tree.Kind.RECORD_PATTERN,
        Tree.Kind.TYPE_PATTERN, // a
        Tree.Kind.TYPE_PATTERN, // b
        Tree.Kind.TYPE_PATTERN, // c
        Tree.Kind.TYPE_PATTERN,
        Tree.Kind.GUARDED_PATTERN,
        // from the guarded pattern child node
        Tree.Kind.TYPE_PATTERN, // r
        Tree.Kind.DEFAULT_PATTERN);
  }

  @Test
  void test_record_pattern_in_switch() {
    String code = "switch (shape) { case Triangle(int a, int b, int c) -> new Object(); default -> null; }";
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
    assertThat(type).is(Tree.Kind.IDENTIFIER);
    assertThat((IdentifierTree) type).hasName("Triangle");
  }

  @Test
  void test_record_pattern_without_identifier_in_switch() {
    String code = "switch (shape) { case Triangle(int a, int b, int c) -> new Object(); default -> null; }";
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
    assertThat(type).is(Tree.Kind.IDENTIFIER);
    assertThat((IdentifierTree) type).hasName("Triangle");
  }

  private static SwitchExpressionTree switchExpressionTree(String methodParametersDeclaration, String switchExpressionCode) {
    CompilationUnitTree cut = JParserTestUtils.parse(String.format(BASE_SOURCE_CODE, methodParametersDeclaration, switchExpressionCode));
    ClassTree classTree = (ClassTree) cut.types().get(0);
    MethodTree methodTree = (MethodTree) classTree.members().get(0);
    ReturnStatementTree returnStatementTree = (ReturnStatementTree) methodTree.block().body().get(0);
    return (SwitchExpressionTree) returnStatementTree.expression();
  }

}
