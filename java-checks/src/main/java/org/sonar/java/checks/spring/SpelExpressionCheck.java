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
    "[a-zA-Z0-9/_-]++(\\[\\d++])*+(\\.[a-zA-Z0-9/_-]++(\\[\\d++])*+)*+"
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

  //TODO est-ce que ça fait du sens de levé sur toutes les annotations spring?
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
    // je vais merge des boucles
    List<Expr> rootExpr = new java.util.ArrayList<>();
    int idx = 0;
    validateRootExpr: while (idx < content.length()) {
      if(PropertyPlaceholder.matchPrefix(content, idx)) {
        var expr = parseDelimiterBraces(""+PropertyPlaceholder.prefix, content, idx+1, startColumn);
        PropertyPlaceholder.validPlaceholder(expr);
        idx = expr.endIdx();
      } else if (SpEL.matchPrefix(content, idx)) {
        var expr = parseDelimiterBraces(""+SpEL.prefix, content, idx+1, startColumn);
        SpEL.validSpEL(expr);
        idx = expr.endIdx();
      }
      ++idx;
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

  // doit tout prendre
  private record Expr(String prefix, String expr, int startIdx, int endIdx, int offset) {
  }
  private static Expr parseDelimiterBraces(String prefix, String content, int firstBraceIdx, int offset) throws SyntaxError {
    if (content.charAt(firstBraceIdx) != '{') {
      throw  new IllegalArgumentException();
    }

    int openCount = 1;
    for (var i = firstBraceIdx + 1; i < content.length(); i++) {
      var c = content.charAt(i);
      if (c == '{') {
        openCount++;
      } else if (c == '}') {
        openCount--;
        if (openCount == 0) {
          return new Expr(prefix, content.substring(firstBraceIdx-prefix.length(), i+1), firstBraceIdx-prefix.length(), i+1, offset);
        }
      }
    }

    throw new SyntaxError("Add missing '}' for this property placeholder or SpEL expression.", offset+firstBraceIdx-prefix.length(), offset+content.length());
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

  // je veux utiliser une liste de règle
  private static class PropertyPlaceholder {
    private static char prefix = '$';
    record Placeholder(int startIdx, int endIdx, String expr, @Nullable String defaultValue) {
    }

    private static boolean matchPrefix(String stream, int idx){
      return idx + 1 < stream.length() && stream.charAt(idx)==prefix && stream.charAt(idx+1)=='{';
    }

    // quelle datatstructure je veux utiliser?
    private static void validPlaceholder(Expr placeholder){
      // utiliser regex
      String mainPart = placeholder.expr.substring(2, placeholder.expr.length()-1);
      String expr = mainPart.split(":")[0].trim();
      if(!PROPERTY_PLACEHOLDER_PATTERN.asMatchPredicate().test(expr)){
        throw new SyntaxError("Correct this malformed property placeholder.", placeholder.offset+placeholder.startIdx(), placeholder.offset+placeholder.endIdx());
      }
    }


    private static String evaluate(String expr) {
      List<Placeholder> placeholders = parse(expr);

      for (Placeholder placeholder : placeholders) {

        //parseValidPropertyPlaceholder(placeholder.expr, placeholder.startIdx);

        for(SubstitutionRule rule : substitutionRules) {
          Optional<String> value = rule.substitute(expr, placeholder);
          if (value.isPresent()) {
            expr = expr.substring(0, placeholder.startIdx) + value.get() + expr.substring(placeholder.endIdx);
            break;
          }
        }
      }
      return expr;
    }



    // a placeholder has the following format ${...:..}
    private static List<Placeholder> parse(String expr) {
      List<Placeholder> placeholders = new java.util.ArrayList<>();

      ParseStates state = new OUTSIDE_PLACEHOLDER();

      for (int idx = 0; idx < expr.length(); idx++) {
        if (idx + 1 < expr.length() && state instanceof OUTSIDE_PLACEHOLDER && expr.charAt(idx) == '$' && expr.charAt(idx + 1) == '{') {
          state = new INSIDE_PLACEHOLDER(idx);
        } else if (state instanceof INSIDE_PLACEHOLDER s && expr.charAt(idx) == '}') {
          state = new OUTSIDE_PLACEHOLDER();
          placeholders.add(new Placeholder(s.start, idx, expr.substring(s.start + 2, idx - 1), null));
        } else if (state instanceof INSIDE_PLACEHOLDER s && expr.charAt(idx) == ':') {
          state = new INSIDE_DEFAULT_VALUE(s.start, expr.substring(s.start + 2, idx - 1), idx);
        } else if (state instanceof INSIDE_DEFAULT_VALUE s && expr.charAt(idx) == '}') {
          state = new OUTSIDE_PLACEHOLDER();
          placeholders.add(new Placeholder(s.start, idx, s.expr, expr.substring(s.startDefault + 1, idx - 1)));
        }
      }

      boolean inPlaceholder = state instanceof INSIDE_PLACEHOLDER || state instanceof INSIDE_DEFAULT_VALUE;
      if(inPlaceholder && expr.charAt(expr.length()-1)!='}') {
        throw new SyntaxError("", 0, expr.length());
      }

      return placeholders;
    }
    
    sealed interface ParseStates {}
    record INSIDE_PLACEHOLDER(int start) implements ParseStates {}
    record INSIDE_DEFAULT_VALUE(int start, String expr, int startDefault) implements ParseStates {}
    record OUTSIDE_PLACEHOLDER() implements ParseStates {}

    @FunctionalInterface
    interface SubstitutionRule {
      Optional<String> substitute(String expr, Placeholder placeholder);
    }
    private static List<SubstitutionRule> substitutionRules = List.of(
      (expr, placeholder) -> {
        if(placeholder.startIdx>0 && expr.charAt(placeholder.startIdx-1)=='@') {
          return Optional.of("bean");
        }
        return Optional.empty();
      },
      (expr, placeholder) -> {
        return Optional.of("#aVar");
      }
    );

  }
  private  static class SpEL {
    private static char prefix = '#';

    record SpELExpr(String expr, int startIdx, int endIdx) {
    }

    private static boolean matchPrefix(String stream, int idx){
      return idx + 1 < stream.length() && stream.charAt(idx)==prefix && stream.charAt(idx+1)=='{';
    }

    private static void validSpEL(Expr expr) throws SyntaxError {
      String placeholderEvaluated = PropertyPlaceholder.evaluate(expr.expr);
      String prefix = placeholderEvaluated.substring(0,2);
      String mainPart = 3<placeholderEvaluated.length()? placeholderEvaluated.substring(2,placeholderEvaluated.length()-1): "";
      String suffix = placeholderEvaluated.substring(placeholderEvaluated.length()-1);

      if(!prefix.equals("#{")||!suffix.equals("}")) {
        throw new SyntaxError("Correct this malformed SpEL expression.",expr.offset+expr.startIdx, expr.offset+expr.endIdx());
      }
      parseValidSpelExpression(mainPart, expr.offset+expr.startIdx, expr.offset+expr.endIdx);
    }


    private static void parseValidSpelExpression(String expressionString, int startColumn, int endColumn) throws SyntaxError {
      if (!isValidSpelExpression(expressionString)) {
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
  }
}
