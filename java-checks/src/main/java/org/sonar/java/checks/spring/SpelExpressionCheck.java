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
package org.sonar.java.checks.spring;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.ExpressionsHelper;
import org.sonar.java.model.DefaultJavaFileScannerContext;
import org.sonar.java.reporting.AnalyzerMessage;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.location.Position;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.springframework.expression.ParseException;
import org.springframework.expression.spel.standard.SpelExpressionParser;

@Rule(key = "S6857")
public class SpelExpressionCheck extends IssuableSubscriptionVisitor {

  private static final String SPRING_PREFIX = "org.springframework";

  /**
   * Regular expression for a property placeholder segment that is not a SpEL expression.
   * It implements the following grammar with possessive quantifiers:
   * <p>
   * <pre>
   * PropertyPlaceholder ::= Identifier IndexExpression* ("." Identifier IndexExpression*)*
   * Identifier          ::= [a-zA-Z0-9_-]+
   * IndexExpression     ::= "[" [0-9]+ "]"
   * </pre>
   * <p>
   * Some examples for accepted inputs:
   * <p>
   * <pre>
   * foo
   * foo.bar
   * foo[42].bar23
   * bar[23][42]
   * </pre>
   */
  private static final Pattern PROPERTY_PLACEHOLDER_PATTERN = Pattern.compile(
    "[a-zA-Z0-9/_-]++(\\[\\d++])*+(\\.[a-zA-Z0-9/_-]++(\\[\\d++])*+)*+");

  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.CLASS, Tree.Kind.INTERFACE);
  }

  @Override
  public void visitNode(Tree tree) {
    getClassAndMemberAnnotations((ClassTree) tree)
      .filter(SpelExpressionCheck::isSpringAnnotation)
      .forEach(this::checkSpringAnnotationArguments);
  }

  private static Stream<AnnotationTree> getClassAndMemberAnnotations(ClassTree cls) {
    return Stream.concat(
      Stream.of(cls.modifiers().annotations()),
      cls.members().stream().map(SpelExpressionCheck::getMemberAnnotations)).flatMap(Collection::stream);
  }

  private static List<AnnotationTree> getMemberAnnotations(Tree member) {
    if (member.is(Tree.Kind.METHOD)) {
      return ((MethodTree) member).modifiers().annotations();
    } else if (member.is(Tree.Kind.VARIABLE)) {
      return ((VariableTree) member).modifiers().annotations();
    } else {
      return Collections.emptyList();
    }
  }

  private static boolean isSpringAnnotation(AnnotationTree annotation) {
    return annotation.symbolType().fullyQualifiedName().startsWith(SPRING_PREFIX);
  }

  private void checkSpringAnnotationArguments(AnnotationTree annotation) {
    annotation.arguments().stream().map(SpelExpressionCheck::extractArgumentValue).filter(Objects::nonNull)
      .forEach(this::checkSpringExpressionsInString);
  }

  @CheckForNull
  private static Map.Entry<Tree, String> extractArgumentValue(ExpressionTree expression) {
    expression = getExpressionOrAssignmentRhs(expression);
    var stringValue = ExpressionsHelper.getConstantValueAsString(expression).value();
    if (stringValue == null) {
      return null;
    }
    return Map.entry(expression, stringValue);
  }

  private static ExpressionTree getExpressionOrAssignmentRhs(ExpressionTree expression) {
    return expression.is(Tree.Kind.ASSIGNMENT) ? ((AssignmentExpressionTree) expression).expression() : expression;
  }

  private void checkSpringExpressionsInString(Map.Entry<Tree, String> entry) {
    var expression = entry.getKey();
    try {
      var argValue = entry.getValue();
      if (expression.is(Tree.Kind.STRING_LITERAL)) {
        checkStringContents(argValue, 1);
      } else {
        checkStringContents(argValue, 0);
      }
    } catch (SyntaxError e) {
      reportIssue(expression, e);
    }
  }

  private void reportIssue(Tree expression, SyntaxError error) {
    if (expression.is(Tree.Kind.STRING_LITERAL)) {
      // For string literals, report exact issue location within the string.
      var tokenStart = Position.startOf(expression);
      var textSpan = new AnalyzerMessage.TextSpan(
        tokenStart.line(),
        tokenStart.columnOffset() + error.range.start,
        tokenStart.line(),
        tokenStart.columnOffset() + error.range.end);

      var analyzerMessage = new AnalyzerMessage(this, context.getInputFile(), textSpan, error.getMessage(), 0);
      ((DefaultJavaFileScannerContext) context).reportIssue(analyzerMessage);
    } else {
      reportIssue(expression, error.getMessage());
    }
  }

  private static void checkStringContents(String content, int offset) throws SyntaxError {
    int idx = 0;
    while (idx < content.length()) {
      if (PropertyPlaceholder.matchPrefix(content, idx)) {
        var placeholder = PropertyPlaceholder.parse(content, idx, offset);
        idx = placeholder.range.end;
      } else if (SpEL.matchPrefix(content, idx)) {
        var expr = SpEL.parse(content, idx, offset);
        idx = expr.range.end;
      } else {
        ++idx;
      }
    }
  }

  private static class SyntaxError extends RuntimeException {

    SyntaxError(String message, Range range) {
      super(message);
      this.range = range;
    }

    public final Range range;
  }

  /**
   * Represents a non-empty range
   *
   * @param start the inclusive start index of the range
   * @param end   the exclusive end index of the range
   */
  private record Range(int start, int end) implements Serializable {
    public Range {
      if (start >= end) {
        throw new IllegalArgumentException("a range is must be non empty, this imply start must be less than end");
      }
    }

    public Range addOffset(int offset) {
      return new Range(start + offset, end + offset);
    }
  }

  private static class PropertyPlaceholder {
    private final static char prefix = '$';

    static boolean matchPrefix(String expressionSource, int idx) {
      return idx + 1 < expressionSource.length() && expressionSource.charAt(idx) == prefix && expressionSource.charAt(idx + 1) == '{';
    }

    /**
     * Parses a property placeholder from the given string starting at the specified index.
     * Assumes that the prefix for a property placeholder is already matched.
     *
     * For example, given the inputs:
     * - expressionSource="1234${foo.property:default}"
     * - startIdx=4
     * - offset=0
     * parse will return: Placeholder(offset=0, range=Range(4, 27), expr="foo.property", defaultValue="default")
     *
     * @param expressionSource The input string containing the property placeholder.
     * @param startIdx The starting index of the placeholder in the string.
     * @param offset The offset to be added to the range for error reporting.
     * @return A {@link Placeholder} object representing the parsed placeholder.
     * @throws IllegalArgumentException If the prefix is not matched.
     * @throws SyntaxError If the placeholder is malformed or if matching braces are not closed.
     *
     */
    static Placeholder parse(String expressionSource, int startIdx, int offset) {
      if (!PropertyPlaceholder.matchPrefix(expressionSource, startIdx)) {
        throw new IllegalArgumentException();
      }

      ParseStates state = new Expr();
      int startExpr = startIdx + 2;

      for (int idx = startExpr; idx < expressionSource.length(); idx++) {
        char current = expressionSource.charAt(idx);

        if (state instanceof Expr && (current == '}' || current == ':')) {
          String expr = expressionSource.substring(startExpr, idx).trim();
          if (current == '}') {
            return new Placeholder(offset, new Range(startIdx, idx + 1), expr, null);
          } else {
            state = new DefaultValue(0, expr, idx + 1);
          }
        } else if (state instanceof DefaultValue d && current == '}' && d.nestingLevel == 0) {
          return new Placeholder(offset, new Range(startIdx, idx + 1), d.expr, expressionSource.substring(d.startDefault, idx).trim());
        } else if (state instanceof DefaultValue d) {
          if (current == '{') {
            state = d.increaseNestingLevel();
          } else if (current == '}') {
            state = d.decreaseNestingLevel();
          }
        }
      }

      Range range = new Range(startIdx, expressionSource.length());
      throw new SyntaxError("Add missing '}' for this property placeholder or SpEL expression.", range.addOffset(offset));
    }

    sealed interface ParseStates {
    }
    record Expr() implements ParseStates {
    }
    record DefaultValue(int nestingLevel, String expr, int startDefault) implements ParseStates {
      DefaultValue increaseNestingLevel() {
        return new DefaultValue(nestingLevel + 1, expr, startDefault);
      }

      DefaultValue decreaseNestingLevel() {
        return new DefaultValue(nestingLevel - 1, expr, startDefault);
      }
    }
  }

  record Placeholder(int offset, Range range, String expr, @Nullable String defaultValue) {
    public Placeholder {
      if (!PROPERTY_PLACEHOLDER_PATTERN.asMatchPredicate().test(expr)) {
        throw new SyntaxError("Correct this malformed property placeholder.", range.addOffset(offset));
      }
    }

    /**
     * Evaluates the placeholder and returns its evaluation using a heuristic.
     * We cannot return the true value without access to the context.
     *
     * @param expressionSource The original string containing the placeholder.
     * @return The evaluated string value of the placeholder.
     */
    public String evaluate(String expressionSource) {
      char before = range.start > 0 ? expressionSource.charAt(range.start - 1) : ' ';
      if (before == '@') {
        return "bean";
      }
      return "#var";
    }
  }

  
  private static class SpEL {
    private final static char prefix = '#';

    static boolean matchPrefix(String expressionSource, int startIdx) {
      return startIdx + 1 < expressionSource.length() && expressionSource.charAt(startIdx) == prefix && expressionSource.charAt(startIdx + 1) == '{';
    }

    /**
     * Parses a SpEL (Spring Expression Language) expression from the given string starting at the specified index.
     * Assumes that the prefix for a SpEL expression is already matched.
     *
     * For example, given the inputs:
     * - expressionSource="1234#{1 + 2}"
     * - startIdx=4
     * - offset=0
     * parse will return: SpELExpr(offset=0, range=Range(4, 12), expr="1 + 2")
     *
     * @param expressionSource The input string containing the SpEL expression.
     * @param startIdx The starting index of the SpEL expression in the string.
     * @param offset The offset to be added to the range for error reporting.
     * @return A {@link SpELExpr} object representing the parsed SpEL expression.
     * @throws IllegalArgumentException If the prefix is not matched.
     * @throws SyntaxError If the SpEL expression is malformed or if matching braces are not closed.
     */
    static SpELExpr parse(String expressionSource, int startIdx, int offset) {
      if (!SpEL.matchPrefix(expressionSource, startIdx)) {
        throw new IllegalArgumentException();
      }

      int startExpr = startIdx + 2;
      StringBuilder evaluated = new StringBuilder();
      int nestingLevel = 0;
      int idx = startExpr;

      while (idx < expressionSource.length()) {
        char current = expressionSource.charAt(idx);

        if (SpelExpressionCheck.PropertyPlaceholder.matchPrefix(expressionSource, idx)) {
          var placeholder = SpelExpressionCheck.PropertyPlaceholder.parse(expressionSource, idx, offset);
          evaluated.append(placeholder.evaluate(expressionSource));
          idx = placeholder.range.end;
        } else if (current == '}' && nestingLevel == 0) {

          return new SpELExpr(evaluated.toString(), new Range(startIdx, idx + 1), offset);
        } else {
          evaluated.append(expressionSource.charAt(idx));
          if (current == '{') {
            nestingLevel++;
          } else if (current == '}') {
            nestingLevel--;
          }
          idx++;
        }
      }

      Range range = new Range(startIdx, expressionSource.length());
      throw new SyntaxError("Add missing '}' for this property placeholder or SpEL expression.", range.addOffset(offset));
    }

  }

  record SpELExpr(String expr, Range range, int offset) {
    public SpELExpr {
      try {
        new SpelExpressionParser().parseExpression(expr);
      } catch (ParseException | IllegalStateException | IllegalArgumentException e) {
        throw new SyntaxError("Correct this malformed SpEL expression.", range.addOffset(offset));
      }
    }
  }
}
