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
package org.sonar.java.checks.spring;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.ObjIntConsumer;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
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
    "[a-zA-Z0-9_-]++(\\[\\d++])*+(\\.[a-zA-Z0-9_-]++(\\[\\d++])*+)*+"
  );

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
      cls.members().stream().map(SpelExpressionCheck::getMemberAnnotations)
    ).flatMap(Collection::stream);
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
        tokenStart.columnOffset() + error.endColumn
      );

      var analyzerMessage = new AnalyzerMessage(this, context.getInputFile(), textSpan, error.getMessage(), 0);
      ((DefaultJavaFileScannerContext) context).reportIssue(analyzerMessage);
    } else {
      reportIssue(expression, error.getMessage());
    }
  }

  private static void checkStringContents(String content, int startColumn) throws SyntaxError {
    var i = 0;
    while (i < content.length()) {
      var c = content.charAt(i);
      switch (c) {
        case '$':
          i = parseDelimitersAndContents(content, i + 1, startColumn + i, SpelExpressionCheck::parseValidPropertyPlaceholder);
          break;
        case '#':
          i = parseDelimitersAndContents(content, i + 1, startColumn + i, SpelExpressionCheck::parseValidSpelExpression);
          break;
        default:
          i++;
          break;
      }
    }
  }

  /**
   * Parses the following grammatical expression, starting at <code>startIndex</code> in `value`:
   *
   * <pre>
   * ('{' contents '}')?
   * </pre>
   * <p>
   * Where correct bracing is checked and then <code>contents</code> is parsed using the given <code>parseContents</code> function.
   *
   * @param value         string containing the character sequence to parse
   * @param startIndex    index of the opening delimiter we start from in <code>value</code>
   * @param startColumn   offset with the position of <code>value</code> within a potentially longer original string (used for reporting)
   * @param parseContents function to parse <code>contents</code>
   * @throws SyntaxError when the input does not comply with the expected grammatical expression
   */
  private static int parseDelimitersAndContents(
    String value,
    int startIndex,
    int startColumn,
    ObjIntConsumer<String> parseContents
  ) throws SyntaxError {
    if (startIndex == value.length()) {
      return startIndex;
    }
    var endIndex = parseDelimiterBraces(value, startIndex, startColumn);
    if (endIndex == startIndex) {
      return endIndex;
    }
    var contents = value.substring(startIndex + 1, endIndex - 1);
    parseContents.accept(contents, startColumn);
    return endIndex;
  }

  private static int parseDelimiterBraces(String value, int startIndex, int startColumn) throws SyntaxError {
    if (value.charAt(startIndex) != '{') {
      return startIndex;
    }

    int openCount = 1;
    for (var i = startIndex + 1; i < value.length(); i++) {
      var c = value.charAt(i);
      if (c == '{') {
        openCount++;
      } else if (c == '}') {
        openCount--;
        if (openCount == 0) {
          return i + 1;
        }
      }
    }

    // +1 because of prefix `$` or `#`
    var endColumn = startColumn + value.length() - startIndex + 1;
    throw new SyntaxError("Add missing '}' for this property placeholder or SpEL expression.", startColumn, endColumn);
  }

  private static void parseValidPropertyPlaceholder(String placeholder, int startColumn) throws SyntaxError {
    if (!isValidPropertyPlaceholder(placeholder, startColumn)) {
      // +3 because of delimiter `#{` and `}`
      var endColumn = startColumn + placeholder.length() + 3;
      throw new SyntaxError("Correct this malformed property placeholder.", startColumn, endColumn);
    }
  }

  private static boolean isValidPropertyPlaceholder(String placeholder, int startColumn) throws SyntaxError {
    var startIndex = 0;
    var endIndex = placeholder.indexOf(':');

    while (endIndex != -1) {
      var segment = placeholder.substring(startIndex, endIndex);
      if (!isValidPropertyPlaceholderSegment(segment, startColumn + startIndex)) {
        return false;
      }
      startIndex = endIndex + 1;
      endIndex = placeholder.indexOf(':', startIndex);
    }
    var segment = placeholder.substring(startIndex);
    return isValidPropertyPlaceholderSegment(segment, startColumn + startIndex);
  }

  private static boolean isValidPropertyPlaceholderSegment(String segment, int startColumn) throws SyntaxError {
    var stripped = segment.stripLeading();
    startColumn += segment.length() - stripped.length();
    stripped = stripped.stripTrailing();

    if (stripped.startsWith("#{")) {
      parseDelimitersAndContents(stripped, 1, startColumn + 2, SpelExpressionCheck::parseValidSpelExpression);
      return true;
    } else {
      return PROPERTY_PLACEHOLDER_PATTERN.matcher(stripped).matches();
    }
  }

  private static void parseValidSpelExpression(String expressionString, int startColumn) throws SyntaxError {
    if (!isValidSpelExpression(expressionString)) {
      // +3 because of delimiter `${` and `}`
      var endColumn = startColumn + expressionString.length() + 3;
      throw new SyntaxError("Correct this malformed SpEL expression.", startColumn, endColumn);
    }
  }

  private static boolean isValidSpelExpression(String expressionString) {
    expressionString = expressionString.strip();
    if (expressionString.isEmpty()) {
      return false;
    }
    try {
      new SpelExpressionParser().parseExpression(expressionString);
    } catch (ParseException | IllegalStateException e) {
      return false;
    }
    return true;
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
}
