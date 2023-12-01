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
import java.util.function.ObjIntConsumer;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.sonar.check.Rule;
import org.sonar.java.model.DefaultJavaFileScannerContext;
import org.sonar.java.reporting.AnalyzerMessage;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.location.Position;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.springframework.expression.ParseException;
import org.springframework.expression.spel.standard.SpelExpressionParser;

@Rule(key = "S6857")
public class SpelExpressionCheck extends IssuableSubscriptionVisitor {

  private static final String SPRING_PREFIX = "org.springframework";

  private static final Pattern PROPERTY_PLACEHOLDER_PATTERN = Pattern.compile(
    "[a-zA-Z_]\\w*+(\\[\\d++])*+(\\.[a-zA-Z_]\\w*+(\\[\\d++])*+)*+"
  );

  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.CLASS, Tree.Kind.INTERFACE, Tree.Kind.ANNOTATION_TYPE);
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
    annotation.arguments().stream()
      .filter(argument -> argument.is(Tree.Kind.STRING_LITERAL))
      .forEach(argument -> checkSpringAnnotationStringArgument((LiteralTree) argument));
  }

  private void checkSpringAnnotationStringArgument(LiteralTree argument) {
    try {
      parseStringArgument(argument.value());
    } catch (SyntaxError e) {
      reportIssue(argument, e);
    }
  }

  private void reportIssue(LiteralTree stringToken, SyntaxError error) {
    var tokenStart = Position.startOf(stringToken);
    var textSpan = new AnalyzerMessage.TextSpan(
      tokenStart.line(),
      tokenStart.columnOffset() + error.startIndex + 1,
      tokenStart.line(),
      tokenStart.columnOffset() + error.endIndex + 1
    );

    var analyzerMessage = new AnalyzerMessage(this, context.getInputFile(), textSpan, error.getMessage(), 0);
    ((DefaultJavaFileScannerContext) context).reportIssue(analyzerMessage);
  }

  private static void parseStringArgument(String value) {
    value = value.substring(1, value.length() - 1);
    var i = 0;
    while (i < value.length()) {
      var c = value.charAt(i);
      i++;
      switch (c) {
        case '$':
          i = parsePropertyPlaceholder(value, i);
          break;
        case '#':
          i = parseSpelExpression(value, i);
          break;
        default:
          break;
      }
    }
  }

  private static int parsePropertyPlaceholder(String value, int startIndex) {
    return parseDelimitersAndContents(value, startIndex, (contents, endIndex) -> {
      if (!isValidPropertyPlaceholder(contents)) {
        throw new SyntaxError("Correct this malformed property placeholder.", startIndex - 1, endIndex);
      }
    });
  }

  private static int parseSpelExpression(String value, int startIndex) {
    return parseDelimitersAndContents(value, startIndex, (contents, endIndex) -> {
      if (!isValidSpelExpression(contents)) {
        throw new SyntaxError("Correct this malformed SpEL expression.", startIndex - 1, endIndex);
      }
    });
  }

  private static int parseDelimitersAndContents(String value, int startIndex, ObjIntConsumer<String> parseContents) {
    if (startIndex == value.length()) {
      return startIndex;
    }
    var endIndex = parseDelimiterBraces(value, startIndex);
    if (endIndex == startIndex) {
      return endIndex;
    }
    var contents = value.substring(startIndex + 1, endIndex - 1).strip();
    parseContents.accept(contents, endIndex);
    return endIndex;
  }

  private static int parseDelimiterBraces(String value, int startIndex) throws SyntaxError {
    if (value.charAt(startIndex) != '{') {
      return startIndex;
    }
    var i = startIndex + 1;
    var openCount = 1;

    while (i < value.length()) {
      var c = value.charAt(i);
      i++;
      switch (c) {
        case '{':
          openCount++;
          break;
        case '}':
          openCount--;
          if (openCount == 0) {
            return i;
          }
          break;
        default:
          break;
      }
    }
    throw new SyntaxError("Add missing '}' for this property placeholder or SpEL expression.", startIndex - 1, i);
  }

  private static boolean isValidPropertyPlaceholder(String placeholder) {
    return PROPERTY_PLACEHOLDER_PATTERN.matcher(placeholder).matches();
  }

  private static boolean isValidSpelExpression(String expressionString) {
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

    SyntaxError(String message, int startIndex, int endIndex) {
      super(message);
      this.startIndex = startIndex;
      this.endIndex = endIndex;
    }

    public final int startIndex;

    public final int endIndex;
  }
}
