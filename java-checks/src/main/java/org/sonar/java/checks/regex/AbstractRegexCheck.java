/*
 * SonarQube Java
 * Copyright (C) 2012-2020 SonarSource SA
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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Streams;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.model.JUtils;
import org.sonar.java.regex.RegexCheck;
import org.sonar.java.regex.RegexParseResult;
import org.sonar.java.regex.RegexScannerContext;
import org.sonar.java.regex.ast.FlagSet;
import org.sonar.java.regex.ast.RegexSyntaxElement;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.VariableTree;

public abstract class AbstractRegexCheck extends AbstractMethodDetection implements RegexCheck {

  private static final String JAVA_LANG_STRING = "java.lang.String";
  private static final MethodMatchers REGEX_METHODS = MethodMatchers.or(
    MethodMatchers.create()
      .ofTypes(JAVA_LANG_STRING)
      .names("matches")
      .addParametersMatcher(JAVA_LANG_STRING)
      .build(),
    MethodMatchers.create()
      .ofTypes(JAVA_LANG_STRING)
      .names("replaceAll", "replaceFirst")
      .withAnyParameters()
      .build(),
    MethodMatchers.create()
      .ofTypes("java.util.regex.Pattern")
      .names("compile", "matches")
      .withAnyParameters()
      .build());

  private RegexScannerContext regexContext;

  @Override
  public final void setContext(JavaFileScannerContext context) {
    this.regexContext = (RegexScannerContext) context;
    super.setContext(context);
  }

  @Override
  protected final MethodMatchers getMethodInvocationMatchers() {
    return REGEX_METHODS;
  }

  @Override
  protected final void onMethodInvocationFound(MethodInvocationTree mit) {
    Arguments args = mit.arguments();
    if (args.isEmpty()) {
      return;
    }
    FlagSet flags = getFlags(mit);
    if (!flags.contains(Pattern.LITERAL)) {
      getLiterals(args.get(0))
        .map(literals -> regexContext.regexForLiterals(flags, literals))
        .ifPresent(result -> checkRegex(result, mit));
    }
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
        return Optional.of(new LiteralTree[] {(LiteralTree) expr});
      default:
        return Optional.empty();
    }
  }

  private static Optional<LiteralTree[]> getLiteralsFromStringConcatenation(BinaryExpressionTree expr) {
    Optional<LiteralTree[]> leftLiterals = getLiterals(expr.leftOperand());
    if (!leftLiterals.isPresent()) {
      return Optional.empty();
    }
    Optional<LiteralTree[]> rightLiterals = getLiterals(expr.rightOperand());
    if (!rightLiterals.isPresent()) {
      return Optional.empty();
    }
    LiteralTree[] combined = Streams.concat(Arrays.stream(leftLiterals.get()), Arrays.stream(rightLiterals.get())).toArray(LiteralTree[]::new);
    return Optional.of(combined);
  }

  private static Optional<LiteralTree[]> getLiteralsFromFinalVariables(IdentifierTree identifier) {
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
    return getLiterals(initializer);
  }

  public abstract void checkRegex(RegexParseResult regexForLiterals, MethodInvocationTree mit);

  public final void reportIssue(RegexSyntaxElement regexTree, String message, @Nullable Integer cost, List<RegexCheck.RegexIssueLocation> secondaries) {
    regexContext.reportIssue(this, regexTree, message, cost, secondaries);
  }

  /**
   * @param mit A method call constructing a regex.
   * @return An optional containing the expression used to set the regex's flag if the regex is created using
   *         Pattern.compile with an argument to set the flags. An empty optional otherwise.
   */
  protected static Optional<ExpressionTree> getFlagsTree(MethodInvocationTree mit) {
    if (mit.symbol().name().equals("compile") && mit.arguments().size() == 2) {
      return Optional.of(mit.arguments().get(1));
    }
    return Optional.empty();
  }

  /**
   * @param mit A method call constructing a regex.
   * @return A FlagSet containing the flags with which the regex is created if flags are supplied and can be determined
   *         statically. An empty FlagSet otherwise.
   */
  private static FlagSet getFlags(MethodInvocationTree mit) {
    int flags = getFlagsTree(mit).flatMap(tree -> tree.asConstant(Integer.class)).orElse(0);
    return new FlagSet(flags);
  }

}
