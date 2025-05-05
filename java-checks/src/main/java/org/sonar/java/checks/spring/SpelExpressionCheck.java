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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.ObjIntConsumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
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
        tokenStart.columnOffset() + error.startColumn,
        tokenStart.line(),
        tokenStart.columnOffset() + error.endColumn);

      var analyzerMessage = new AnalyzerMessage(this, context.getInputFile(), textSpan, error.getMessage(), 0);
      ((DefaultJavaFileScannerContext) context).reportIssue(analyzerMessage);
    } else {
      reportIssue(expression, error.getMessage());
    }
  }

  private static void checkStringContents(String content, int offset) throws SyntaxError {
    int idx = 0;
    validateRootExpr: while (idx < content.length()) {
      if (PropertyPlaceholder.matchPrefix(content, idx)) {
        var placeholder = PropertyPlaceholder.parse(content, idx, offset);
        idx = placeholder.endIdx();
        continue;
      } else if (SpEL.matchPrefix(content, idx)) {
        var expr = SpEL.parse(content, idx, offset);
        idx = expr.endIdx();
        continue;
      }
      ++idx;
    }
  }

  private static class SyntaxError extends RuntimeException {

    SyntaxError(String message, int startColumn, int endColumn) {
      super(message);
      this.startColumn = startColumn;
      this.endColumn = endColumn;
    }

    public final int startColumn;
    public final int endColumn;
  }

  private static class PropertyPlaceholder {
    private static char prefix = '$';

    // range are always inclusive, exclusive
    record Placeholder(int offset, int startIdx, int endIdx, String expr, @Nullable String defaultValue) {
      public Placeholder {
        if (!PROPERTY_PLACEHOLDER_PATTERN.asMatchPredicate().test(expr)) {
          throw new SyntaxError("Correct this malformed property placeholder.", offset + startIdx, offset + endIdx);
        }
      }
    }

    private static boolean matchPrefix(String stream, int idx) {
      return idx + 1 < stream.length() && stream.charAt(idx) == prefix && stream.charAt(idx + 1) == '{';
    }

    // parse a placeholder starting from idx in the stream
    private static Placeholder parse(String stream, int startIdx, int offset) {
      if (!PropertyPlaceholder.matchPrefix(stream, startIdx)) {
        throw new IllegalArgumentException();
      }

      ParseStates state = new EXPR(startIdx);
      int startExpr = startIdx + 2;

      for (int idx = startExpr; idx < stream.length(); idx++) {
        char c = stream.charAt(idx);
        if (state instanceof EXPR e && (c == '}' || c == ':')) {
          String expr = stream.substring(startExpr, idx).trim();
          if (c == '}') {
            return new Placeholder(offset, e.start, idx + 1, expr, null);
          } else {
            state = new DEFAULT_VALUE(e.start, 0, expr, idx + 1);
          }
        } else if (state instanceof DEFAULT_VALUE d && c == '}' && d.nestingLevel == 0) {
          return new Placeholder(offset, startExpr, idx + 1, d.expr, stream.substring(d.startDefault, idx).trim());
        } else if (state instanceof DEFAULT_VALUE d && c == '{') {
          state = d.increaseNestingLevel();
        } else if (state instanceof DEFAULT_VALUE d && c == '}') {
          state = d.decreaseNestingLevel();
        }
      }

      throw new SyntaxError("Add missing '}' for this property placeholder or SpEL expression.", startIdx + offset, stream.length() + offset);
    }

    sealed interface ParseStates {
    }
    record EXPR(int start) implements ParseStates {
    }
    record DEFAULT_VALUE(int start, int nestingLevel, String expr, int startDefault) implements ParseStates {
      DEFAULT_VALUE increaseNestingLevel() {
        return new DEFAULT_VALUE(start, nestingLevel + 1, expr, startDefault);
      }

      DEFAULT_VALUE decreaseNestingLevel() {
        return new DEFAULT_VALUE(start, nestingLevel - 1, expr, startDefault);
      }
    }
  }
  private static class SpEL {
    private static char prefix = '#';

    // range is inclusive, exclusive
    record SpELExpr(String expr, int startIdx, int endIdx, int offset) {
      public SpELExpr {
        try {
          new SpelExpressionParser().parseExpression(expr);
        } catch (ParseException | IllegalStateException | IllegalArgumentException e) {
          throw new SyntaxError("Correct this malformed SpEL expression.", offset + startIdx, offset + endIdx);
        }
      }
    }

    private static boolean matchPrefix(String stream, int startIdx) {
      return startIdx + 1 < stream.length() && stream.charAt(startIdx) == prefix && stream.charAt(startIdx + 1) == '{';
    }

    private static SpELExpr parse(String stream, int startIdx, int offset) {
      if (!SpEL.matchPrefix(stream, startIdx)) {
        throw new IllegalArgumentException();
      }

      int startExpr = startIdx + 2;
      int startText = startExpr;
      int nestingLevel = 0;
      int idx = startExpr;
      boolean finished = false;
      List<Token> tokens = new java.util.ArrayList<>();

      while (idx < stream.length()) {
        if (SpelExpressionCheck.PropertyPlaceholder.matchPrefix(stream, idx)) {
          tokens.add(new Text(stream.substring(startText, idx)));
          var placeholder = SpelExpressionCheck.PropertyPlaceholder.parse(stream, idx, offset);
          tokens.add(new SpEL.PropertyPlaceholder(placeholder));
          idx = placeholder.endIdx();
          startText = idx;
          continue;
        } else if (stream.charAt(idx) == '}' && nestingLevel == 0) {
          if (startText < idx) {
            tokens.add(new Text(stream.substring(startText, idx)));
          }
          finished = true;
          break;
        } else if (stream.charAt(idx) == '{') {
          nestingLevel++;
        } else if (stream.charAt(idx) == '}') {
          nestingLevel--;
        }
        idx++;
      }

      if (nestingLevel > 0 || !finished) {
        throw new SyntaxError("Add missing '}' for this property placeholder or SpEL expression.", startIdx + offset, stream.length() + offset);
      }

      String expr = tokens.stream().map(t -> {
        if (t instanceof Text text) {
          return text.text();
        } else if (t instanceof SpEL.PropertyPlaceholder p) {
          return "#aVar";
        }
        return "";
      }).collect(Collectors.joining(""));

      return new SpELExpr(expr, startIdx, idx + 1, offset);

    }

    sealed interface Token {
    }
    record PropertyPlaceholder(SpelExpressionCheck.PropertyPlaceholder.Placeholder p) implements Token {
    }
    record Text(String text) implements Token {
    }

  }
}
