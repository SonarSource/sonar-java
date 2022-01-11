/*
 * SonarQube Java
 * Copyright (C) 2012-2022 SonarSource SA
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.IntFunction;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.sonar.java.annotations.VisibleForTesting;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.model.JUtils;
import org.sonar.java.regex.RegexCheck;
import org.sonar.java.regex.RegexScannerContext;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonarsource.analyzer.commons.regex.RegexParseResult;
import org.sonarsource.analyzer.commons.regex.ast.FlagSet;
import org.sonarsource.analyzer.commons.regex.ast.RegexSyntaxElement;

public abstract class AbstractRegexCheck extends IssuableSubscriptionVisitor implements RegexCheck {

  protected static final String JAVA_LANG_STRING = "java.lang.String";
  protected static final String LANG3_REGEX_UTILS = "org.apache.commons.lang3.RegExUtils";

  protected static final MethodMatchers REGEX_ON_THE_SECOND_ARGUMENT_METHODS = MethodMatchers.create()
    .ofTypes(LANG3_REGEX_UTILS)
    .anyName()
    .addParametersMatcher(JAVA_LANG_STRING, JAVA_LANG_STRING)
    .addParametersMatcher(JAVA_LANG_STRING, JAVA_LANG_STRING, JAVA_LANG_STRING)
    .build();

  protected static final MethodMatchers METHODS_IMPLYING_DOT_ALL_FLAG = MethodMatchers.create()
    .ofTypes(LANG3_REGEX_UTILS)
    .names("removePattern", "replacePattern")
    .withAnyParameters()
    .build();

  private static final MethodMatchers PATTERN_COMPILE = MethodMatchers.create()
    .ofTypes("java.util.regex.Pattern")
    .names("compile")
    .withAnyParameters()
    .build();

  protected static final MethodMatchers REGEX_METHODS = MethodMatchers.or(
    MethodMatchers.create()
      .ofTypes(JAVA_LANG_STRING)
      .names("matches")
      .addParametersMatcher(JAVA_LANG_STRING)
      .build(),
    MethodMatchers.create()
      .ofTypes(JAVA_LANG_STRING)
      .names("replaceAll", "replaceFirst", "split")
      .withAnyParameters()
      .build(),
    PATTERN_COMPILE,
    MethodMatchers.create()
      .ofTypes("java.util.regex.Pattern")
      .names("matches")
      .withAnyParameters()
      .build(),
    MethodMatchers.create()
      .ofTypes(LANG3_REGEX_UTILS)
      .names("removeAll", "removeFirst", "removePattern")
      .addParametersMatcher(JAVA_LANG_STRING, JAVA_LANG_STRING)
      .build(),
    MethodMatchers.create()
      .ofTypes(LANG3_REGEX_UTILS)
      .names("replaceAll", "replaceFirst", "replacePattern")
      .addParametersMatcher(JAVA_LANG_STRING, JAVA_LANG_STRING, JAVA_LANG_STRING)
      .build());

  private RegexScannerContext regexContext;

  // We want to report only one issue per element for one rule.
  private final HashSet<RegexSyntaxElement> reportedRegexTrees = new HashSet<>();

  @Override
  public final void setContext(JavaFileScannerContext context) {
    this.regexContext = (RegexScannerContext) context;
    reportedRegexTrees.clear();
    super.setContext(context);
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    // ignore constructors and method references, add annotations
    return Arrays.asList(Tree.Kind.METHOD_INVOCATION, Tree.Kind.ANNOTATION);
  }

  protected MethodMatchers getMethodInvocationMatchers() {
    return REGEX_METHODS;
  }

  protected boolean filterAnnotation(AnnotationTree annotation) {
    Type type = annotation.symbolType();
    return type.is("javax.validation.constraints.Pattern") ||
      type.is("javax.validation.constraints.Email") ||
      type.is("org.hibernate.validator.constraints.URL") ||
      type.is("org.hibernate.validator.constraints.Email");
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Tree.Kind.ANNOTATION)) {
      AnnotationTree annotation = (AnnotationTree) tree;
      if (filterAnnotation(annotation)) {
        onAnnotationFound(annotation);
      }
    } else {
      MethodInvocationTree mit = (MethodInvocationTree) tree;
      if (getMethodInvocationMatchers().matches(mit)) {
        onMethodInvocationFound(mit);
      }
    }
  }

  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    ExpressionTree regexExpression = getRegexLiteralExpression(mit);
    if (regexExpression != null) {
      FlagSet flags = getFlags(mit);
      if (!flags.contains(Pattern.LITERAL)) {
        getLiterals(regexExpression)
          .map(literals -> regexForLiterals(flags, literals))
          .ifPresent(result -> checkRegex(result, mit));
      }
    }
  }

  @Nullable
  protected ExpressionTree getRegexLiteralExpression(ExpressionTree methodInvocationOrAnnotation) {
    if (methodInvocationOrAnnotation.is(Tree.Kind.METHOD_INVOCATION)) {
      MethodInvocationTree mit = (MethodInvocationTree) methodInvocationOrAnnotation;
      int regexIndex = REGEX_ON_THE_SECOND_ARGUMENT_METHODS.matches(mit) ? 1 : 0;
      return (regexIndex < mit.arguments().size()) ? mit.arguments().get(regexIndex) : null;
    } else {
      AnnotationTree annotation = (AnnotationTree) methodInvocationOrAnnotation;
      for (ExpressionTree argument : annotation.arguments()) {
        ExpressionTree expression = getAnnotationValue(argument, "regexp");
        if (expression != null) {
          return expression;
        }
      }
    }
    return null;
  }

  protected void onAnnotationFound(AnnotationTree annotation) {
    ExpressionTree regexExpression = getRegexLiteralExpression(annotation);
    if (regexExpression != null) {
      getLiterals(regexExpression)
        .map(literals -> regexForLiterals(getFlags(annotation), literals))
        .ifPresent(result -> checkRegex(result, annotation));
    }
  }

  private static class AnnotationFlagsVisitor extends BaseTreeVisitor {
    private static final Map<String, Integer> FLAG_MASK = new HashMap<>();
    static {
      FLAG_MASK.put("UNIX_LINES", 1);
      FLAG_MASK.put("CASE_INSENSITIVE", 2);
      FLAG_MASK.put("COMMENTS", 4);
      FLAG_MASK.put("MULTILINE", 8);
      FLAG_MASK.put("DOTALL", 32);
      FLAG_MASK.put("UNICODE_CASE", 64);
      FLAG_MASK.put("CANON_EQ", 128);
    }
    int mask = 0;

    @Override
    public void visitIdentifier(IdentifierTree tree) {
      Type symbolType = tree.symbolType();
      if (symbolType.is("javax.validation.constraints.Pattern$Flag") || symbolType.is("jakarta.validation.constraints.Pattern$Flag")) {
        mask |= FLAG_MASK.getOrDefault(tree.name(), 0);
      }
    }

    FlagSet extractFlags(ExpressionTree flagsExpression) {
      mask = 0;
      flagsExpression.accept(this);
      return new FlagSet(mask);
    }
  }

  protected final RegexParseResult regexForLiterals(FlagSet flags, LiteralTree... literals) {
    return regexContext.regexForLiterals(flags, literals);
  }

  @VisibleForTesting
  protected static Optional<LiteralTree[]> getLiterals(ExpressionTree expr) {
    switch (expr.kind()) {
      case PLUS:
        return getLiteralsFromStringConcatenation((BinaryExpressionTree) expr);
      case IDENTIFIER:
        return getLiteralsFromFinalVariables((IdentifierTree) expr);
      case PARENTHESIZED_EXPRESSION:
        return getLiterals(ExpressionUtils.skipParentheses(expr));
      case STRING_LITERAL:
      case TEXT_BLOCK:
        return Optional.of(new LiteralTree[] {(LiteralTree) expr});
      case METHOD_INVOCATION:
        // We do not need to consider flags or precedence issues here because Pattern.toString() does not include
        // the flags passed to Pattern.compile nor does it add any parentheses for precedence - it just returns the
        // pattern string exactly as it was given to Pattern.compile, so we can simply take that string and work with
        // it as-is.
        MethodInvocationTree mit = (MethodInvocationTree) expr;
        if (PATTERN_COMPILE.matches(mit)) {
          return getLiterals(mit.arguments().get(0));
        }
        // else fall through
      default:
        return Optional.empty();
    }
  }

  private static Optional<LiteralTree[]> getLiteralsFromStringConcatenation(BinaryExpressionTree expr) {
    return getLiterals(expr.leftOperand()).flatMap(leftLiterals ->
      getLiterals(expr.rightOperand()).map(rightLiterals ->
        concatenateArrays(leftLiterals, rightLiterals, LiteralTree[]::new)));
  }

  private static <T> T[] concatenateArrays(T[] array1, T[] array2, IntFunction<T[]> arrayConstructor) {
    return Stream.of(array1, array2).flatMap(Arrays::stream).toArray(arrayConstructor);
  }

  protected static Optional<ExpressionTree> getFinalVariableInitializer(IdentifierTree identifier) {
    Symbol symbol = identifier.symbol();
    if (!symbol.isVariableSymbol()) {
      return Optional.empty();
    }

    Symbol.VariableSymbol variableSymbol = (Symbol.VariableSymbol) symbol;
    if (!(variableSymbol.isFinal() || JUtils.isEffectivelyFinal(variableSymbol))) {
      return Optional.empty();
    }
    VariableTree declaration = variableSymbol.declaration();
    if (declaration == null) {
      return Optional.empty();
    }
    ExpressionTree initializer = declaration.initializer();
    if (initializer == null) {
      return Optional.empty();
    }
    return Optional.of(initializer);
  }

  private static Optional<LiteralTree[]> getLiteralsFromFinalVariables(IdentifierTree identifier) {
    return getFinalVariableInitializer(identifier).flatMap(AbstractRegexCheck::getLiterals);
  }

  public abstract void checkRegex(RegexParseResult regexForLiterals, ExpressionTree methodInvocationOrAnnotation);

  public final void reportIssue(RegexSyntaxElement regexTree, String message, @Nullable Integer cost, List<RegexCheck.RegexIssueLocation> secondaries) {
    if (reportedRegexTrees.add(regexTree)) {
      regexContext.reportIssue(this, regexTree, message, cost, secondaries);
    }
  }

  public Tree methodOrAnnotationName(ExpressionTree methodInvocationOrAnnotation) {
    if (methodInvocationOrAnnotation.is(Tree.Kind.METHOD_INVOCATION)) {
      return ExpressionUtils.methodName((MethodInvocationTree) methodInvocationOrAnnotation);
    } else {
      return ((AnnotationTree) methodInvocationOrAnnotation).annotationType();
    }
  }

  public final void reportIssue(Tree javaTree, String message, @Nullable Integer cost, List<RegexCheck.RegexIssueLocation> secondaries) {
    regexContext.reportIssue(this, javaTree, message, cost, secondaries);
  }

  /**
   * @param methodInvocationOrAnnotation A method call or annotation constructing a regex.
   * @return An optional containing the expression used to set the regex's flag if the regex is created using
   *         Pattern.compile or "flags" annotation parameter with an argument to set the flags. An empty optional otherwise.
   */
  protected static Optional<ExpressionTree> getFlagsTree(ExpressionTree methodInvocationOrAnnotation) {
    if (methodInvocationOrAnnotation.is(Tree.Kind.METHOD_INVOCATION)) {
      MethodInvocationTree mit = (MethodInvocationTree) methodInvocationOrAnnotation;
      if (mit.symbol().name().equals("compile") && mit.arguments().size() == 2) {
        return Optional.of(mit.arguments().get(1));
      }
    } else {
      AnnotationTree annotation = (AnnotationTree) methodInvocationOrAnnotation;
      for (ExpressionTree argument : annotation.arguments()) {
        ExpressionTree expression = getAnnotationValue(argument, "flags");
        if (expression != null) {
          return Optional.of(expression);
        }
      }
    }
    return Optional.empty();
  }

  /**
   * @param mit A method call constructing a regex.
   * @return A FlagSet containing the flags with which the regex is created if flags are supplied and can be determined
   *         statically. An empty FlagSet otherwise.
   */
  private static FlagSet getFlags(MethodInvocationTree mit) {
    if (METHODS_IMPLYING_DOT_ALL_FLAG.matches(mit)) {
      return new FlagSet(Pattern.DOTALL);
    }
    int flags = getFlagsTree(mit).flatMap(tree -> tree.asConstant(Integer.class)).orElse(0);
    return new FlagSet(flags);
  }

  private static FlagSet getFlags(AnnotationTree annotation) {
    return getFlagsTree(annotation)
      .map(expression -> new AnnotationFlagsVisitor().extractFlags(expression))
      .orElseGet(FlagSet::new);
  }

  @Nullable
  private static ExpressionTree getAnnotationValue(ExpressionTree expression, String parameterName) {
    if(expression.is(Tree.Kind.ASSIGNMENT)) {
      AssignmentExpressionTree assignment = (AssignmentExpressionTree) expression;
      ExpressionTree variable = assignment.variable();
      if (variable.is(Tree.Kind.IDENTIFIER) && ((IdentifierTree) variable).name().equals(parameterName)) {
        return assignment.expression();
      }
    }
    return null;
  }

}
