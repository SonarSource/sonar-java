/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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
package org.sonar.java.checks.regex;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.JParserTestUtils;
import org.sonar.java.checks.verifier.CheckVerifier;
import org.sonarsource.analyzer.commons.regex.RegexParseResult;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.ParenthesizedTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.java.checks.verifier.TestUtils.mainCodeSourcesPath;
import static org.sonar.java.checks.regex.AbstractRegexCheck.getLiterals;

class AbstractRegexCheckTest {

  private static final String JAVA_CODE = ""
    + "package org.foo;\n"
    + "import static java.util.regex.Pattern.MULTILINE;"
    + "class A {\n"
    + "  void m(Object arg) {\n"
    + "    %s\n"
    + "    m(%s);\n"
    + "  }\n"
    + "  public static final String A_CONST = \"a_constant_value\";\n"
    + "}";

  @Test
  void test_GetLiterals_string_literals() {
    ExpressionTree expr = getArg("\"hello\"").expr;

    Optional<LiteralTree[]> result = getLiterals(expr);
    assertThat(result).isPresent();

    assertThat(result.get())
      .hasSize(1)
      .allMatch(t -> t.is(Tree.Kind.STRING_LITERAL))
      .containsExactly((LiteralTree) expr);
  }

  @ParameterizedTest(name = "[{index}] getLiterals({0}) should be empty")
  @ValueSource(strings = {/* not a String */ "42", "unknown", "\"a\" + unknown", "unknown + \"a\""})
  void test_GetLiterals_is_empty(String arg) {
    ExpressionTree expr = getArg(arg).expr;

    Optional<LiteralTree[]> result = getLiterals(expr);
    assertThat(result).isEmpty();
  }

  @Test
  void test_GetLiterals_effectively_final_variable() {
    TestCase testcase = getArg("a", "String a = \"abc\";");
    ExpressionTree expr = testcase.expr;
    ExpressionTree initializer = ((VariableTree) testcase.preStatements.get(0)).initializer();

    Optional<LiteralTree[]> result = getLiterals(expr);
    assertThat(result).isPresent();

    assertThat(result.get())
      .hasSize(1)
      .allMatch(t -> t.is(Tree.Kind.STRING_LITERAL))
      .containsExactly((LiteralTree) initializer);
  }

  @Test
  void test_GetLiterals_effectively_non_final_variable() {
    TestCase testcase = getArg("a", "String a = \"abc\";", "a = \"bcd\";");
    ExpressionTree expr = testcase.expr;

    Optional<LiteralTree[]> result = getLiterals(expr);
    assertThat(result).isEmpty();
  }

  @Test
  void test_GetLiterals_final_variable() {
    TestCase testcase = getArg("a", "final String a = \"abc\";");
    ExpressionTree expr = testcase.expr;
    ExpressionTree initializer = ((VariableTree) testcase.preStatements.get(0)).initializer();

    Optional<LiteralTree[]> result = getLiterals(expr);
    assertThat(result).isPresent();

    assertThat(result.get())
      .hasSize(1)
      .allMatch(t -> t.is(Tree.Kind.STRING_LITERAL))
      .containsExactly((LiteralTree) initializer);
  }

  @Test
  void test_GetLiterals_constant_outside_file() {
    TestCase testcase = getArg("MULTILINE");
    ExpressionTree expr = testcase.expr;

    Optional<LiteralTree[]> result = getLiterals(expr);
    assertThat(result).isEmpty();
  }

  @Test
  void test_GetLiterals_constant_within_file() {
    TestCase testcase = getArg("A_CONST");
    ExpressionTree expr = testcase.expr;
    ExpressionTree initializer = ((VariableTree) (((IdentifierTree) expr).symbol().declaration())).initializer();

    Optional<LiteralTree[]> result = getLiterals(expr);
    assertThat(result).isPresent();

    assertThat(result.get())
      .hasSize(1)
      .allMatch(t -> t.is(Tree.Kind.STRING_LITERAL))
      .containsExactly((LiteralTree) initializer);
  }

  @Test
  void test_GetLiterals_final_variable_not_directly_initialized() {
    TestCase testcase = getArg("a", "final String a;", "a = \"abc\";");
    ExpressionTree expr = testcase.expr;

    Optional<LiteralTree[]> result = getLiterals(expr);
    assertThat(result).isEmpty();
  }

  @Test
  void test_GetLiterals_string_concatenation() {
    TestCase testcase = getArg("\"a\" + \"b\" + \"c\"");
    ExpressionTree expr = testcase.expr;

    BinaryExpressionTree ab = (BinaryExpressionTree) ((BinaryExpressionTree) expr).leftOperand();
    ExpressionTree a = ab.leftOperand();
    ExpressionTree b = ab.rightOperand();
    ExpressionTree c = ((BinaryExpressionTree) expr).rightOperand();

    Optional<LiteralTree[]> result = getLiterals(expr);
    assertThat(result).isPresent();

    assertThat(result.get())
      .hasSize(3)
      .allMatch(t -> t.is(Tree.Kind.STRING_LITERAL))
      .containsExactly((LiteralTree) a, (LiteralTree) b, (LiteralTree) c);
  }

  @Test
  void test_GetLiterals_string_concatenation_with_constant() {
    TestCase testcase = getArg("\"a\" + b + \"c\"", "final String b = \"q\";");
    ExpressionTree expr = testcase.expr;

    BinaryExpressionTree ab = (BinaryExpressionTree) ((BinaryExpressionTree) expr).leftOperand();
    ExpressionTree a = ab.leftOperand();
    ExpressionTree b = ((VariableTree) (testcase.preStatements.get(0))).initializer();
    ExpressionTree c = ((BinaryExpressionTree) expr).rightOperand();

    Optional<LiteralTree[]> result = getLiterals(expr);
    assertThat(result).isPresent();

    assertThat(result.get())
      .hasSize(3)
      .allMatch(t -> t.is(Tree.Kind.STRING_LITERAL))
      .containsExactly((LiteralTree) a, (LiteralTree) b, (LiteralTree) c);
  }

  @Test
  void test_GetLiterals_string_concatenation_with_constant_and_parenthesis() {
    TestCase testcase = getArg("\"a\" + (b + \"c\")", "final String b = \"q\";");
    ExpressionTree expr = testcase.expr;
    ExpressionTree a = ((BinaryExpressionTree) expr).leftOperand();
    BinaryExpressionTree bc = (BinaryExpressionTree) ((ParenthesizedTree) ((BinaryExpressionTree) expr).rightOperand()).expression();
    ExpressionTree b = ((VariableTree) (testcase.preStatements.get(0))).initializer();
    ExpressionTree c = bc.rightOperand();

    Optional<LiteralTree[]> result = getLiterals(expr);
    assertThat(result).isPresent();

    assertThat(result.get())
      .hasSize(3)
      .allMatch(t -> t.is(Tree.Kind.STRING_LITERAL))
      .containsExactly((LiteralTree) a, (LiteralTree) b, (LiteralTree) c);
  }

  @Test
  void match_regex_methods() {
    @Rule(key = "S000")
    class IssueOnAllRegexCheck extends AbstractRegexCheck {
      @Override
      public void checkRegex(RegexParseResult regexForLiterals, ExpressionTree methodInvocationOrAnnotation) {
        if (regexForLiterals.getSyntaxErrors().isEmpty()) {
          int mask = regexForLiterals.getInitialFlags().getMask();
          String flags = mask == 0 ? "" : ",initialFlags=" + mask;
          reportIssue(methodInvocationOrAnnotation, regexForLiterals.getResult().getText() + flags);
        } else {
          reportIssue(methodInvocationOrAnnotation, regexForLiterals.getSyntaxErrors().get(0).getMessage());
        }
      }
    }
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/regex/AbstractRegexCheck.java"))
      .withCheck(new IssueOnAllRegexCheck())
      .verifyIssues();
  }

  private static TestCase getArg(String expression, String... preStatements) {
    CompilationUnitTree cut = JParserTestUtils.parse(String.format(JAVA_CODE, Arrays.stream(preStatements).collect(Collectors.joining("\n")), expression));
    ClassTree a = (ClassTree) cut.types().get(0);
    MethodTree m = (MethodTree) a.members().get(0);
    List<StatementTree> statements = m.block().body();
    ExpressionStatementTree lastStatement = (ExpressionStatementTree) statements.get(statements.size() - 1);

    ExpressionTree expr = ((MethodInvocationTree) lastStatement.expression()).arguments().get(0);

    if (statements.size() > 1) {
      statements.remove(statements.size() - 1);
      return new TestCase(expr, statements);
    }
    return new TestCase(expr);
  }

  private static class TestCase {
    final ExpressionTree expr;
    final List<StatementTree> preStatements;

    public TestCase(ExpressionTree expr) {
      this(expr, Collections.emptyList());
    }

    public TestCase(ExpressionTree expr, List<StatementTree> preStatements) {
      this.expr = expr;
      this.preStatements = preStatements;
    }

  }
}
