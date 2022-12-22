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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.model.JUtils;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonarsource.analyzer.commons.regex.RegexParseResult;

public abstract class AbstractRegexCheckTrackingMatchers extends AbstractRegexCheck {

  /**
   * The methods for which we want to know whether they're invoked on the given regex. These should either be methods of
   * the Pattern or Matcher class or part of REGEX_METHODS.
   */
  protected abstract MethodMatchers trackedMethodMatchers();

  /**
   * @param regexForLiterals The regex to be checked
   * @param methodInvocationOrAnnotation The method invocation or annotation that the regex string is passed to
   * @param trackedMethodsCalled The list of method invocations performed on the Pattern or Matcher object associated
   *                             with the regex (only taking into account methods returned by trackedMethodMatchers)
   * @param didEscape Whether or not the regex escaped the method in which it was created (via return, being assigned to
   *                  a non-local variable or being passed to a different method). If true, trackedMethodsCalled may not
   *                  be exhaustive.
   */
  protected abstract void checkRegex(RegexParseResult regexForLiterals, ExpressionTree methodInvocationOrAnnotation,
    List<MethodInvocationTree> trackedMethodsCalled, boolean didEscape);

  private static final String JAVA_UTIL_REGEX_PATTERN = "java.util.regex.Pattern";
  private static final String JAVA_UTIL_REGEX_MATCHER = "java.util.regex.Matcher";

  private static final MethodMatchers PATTERN_MATCHER = MethodMatchers.create()
    .ofTypes(JAVA_UTIL_REGEX_PATTERN)
    .names("matcher")
    .addParametersMatcher("java.lang.CharSequence")
    .build();

  private static final MethodMatchers PATTERN_COMPILE = MethodMatchers.create()
    .ofTypes(JAVA_UTIL_REGEX_PATTERN)
    .names("compile")
    .withAnyParameters()
    .build();

  private static final MethodMatchers PATTERN_OR_MATCHER_ARGUMENT = MethodMatchers.create()
    .ofAnyType()
    .anyName()
    .addParametersMatcher(types -> types.stream().anyMatch(AbstractRegexCheckTrackingMatchers::isPatternOrMatcher))
    .build();

  private static boolean isPatternOrMatcher(Type type) {
    String name = type.fullyQualifiedName();
    return name.equals(JAVA_UTIL_REGEX_PATTERN) || name.equals(JAVA_UTIL_REGEX_MATCHER);
  }

  private final MethodMatchers matchers = MethodMatchers.or(REGEX_METHODS, PATTERN_MATCHER, PATTERN_OR_MATCHER_ARGUMENT, trackedMethodMatchers());

  /**
   * Maps a variable containing either a Pattern or a Matcher associated with a pattern to the RegexParseResult of
   * the corresponding regex.
   */
  private final Map<Symbol, RegexParseResult> variableToRegex = new HashMap<>();

  private final Map<MethodInvocationTree, RegexParseResult> methodInvocationToRegex = new HashMap<>();

  private final Map<RegexParseResult, List<MethodInvocationTree>> methodsCalledOnRegex = new LinkedHashMap<>();

  private final Map<RegexParseResult, ExpressionTree> regexCreations = new HashMap<>();

  /**
   * This sets contains all regexes whose Pattern and/or Matcher object escapes, meaning it is used in such a way that
   * we no longer know which methods are called on it.
   */
  private final Set<RegexParseResult> escapingRegexes = new HashSet<>();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    List<Tree.Kind> nodes = new ArrayList<>(super.nodesToVisit());
    // visit more nodes than method invocations
    nodes.add(Tree.Kind.NEW_CLASS);
    nodes.add(Tree.Kind.RETURN_STATEMENT);
    nodes.add(Tree.Kind.COMPILATION_UNIT);
    return nodes;
  }

  @Override
  public void leaveNode(Tree tree) {
    if (tree.is(Tree.Kind.RETURN_STATEMENT)) {
      collectReturnedVariables(((ReturnStatementTree) tree).expression());
    } else if (tree.is(Tree.Kind.COMPILATION_UNIT)) {
      methodsCalledOnRegex.forEach((regex, invocation) ->
        checkRegex(regex, regexCreations.get(regex), invocation, escapingRegexes.contains(regex))
      );
      // clear all the structures used during analysis to start fresh in next file
      variableToRegex.clear();
      methodInvocationToRegex.clear();
      methodsCalledOnRegex.clear();
      escapingRegexes.clear();
      regexCreations.clear();
    } else if (tree.is(Tree.Kind.METHOD_INVOCATION) && matchers.matches((MethodInvocationTree) tree)) {
      onMethodInvocationFound((MethodInvocationTree) tree);
    } else if (tree.is(Tree.Kind.NEW_CLASS)) {
      if (PATTERN_OR_MATCHER_ARGUMENT.matches((NewClassTree) tree)) {
        onConstructorFound((NewClassTree) tree);
      }
    } else {
      super.visitNode(tree);
    }
  }

  private void collectReturnedVariables(@Nullable ExpressionTree returnedExpression) {
    if (returnedExpression == null) {
      return;
    }
    getRegex(returnedExpression).ifPresent(escapingRegexes::add);
  }

  @Override
  public void visitNode(Tree tree) {
    // Do nothing because we want to visit method invocations inside-out instead of outside-in (so we call
    // onMethodInvocationFound when exiting a method invocation, not when entering it)
  }

  private void onConstructorFound(NewClassTree tree) {
    for (ExpressionTree argument : tree.arguments()) {
      getRegex(argument).ifPresent(escapingRegexes::add);
    }
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    if (PATTERN_MATCHER.matches(mit)) {
      getRegex(mit).ifPresent(regex -> handleAssignment(mit, regex));
    } else if (REGEX_METHODS.matches(mit)) {
      super.onMethodInvocationFound(mit);
    } else if (trackedMethodMatchers().matches(mit)) {
      getRegex(mit).ifPresent(regex -> methodsCalledOnRegex.get(regex).add(mit));
    } else {
      for (ExpressionTree argument : mit.arguments()) {
        getRegex(argument).ifPresent(escapingRegexes::add);
      }
    }
  }

  private Optional<RegexParseResult> getRegex(ExpressionTree tree) {
    if (tree.is(Tree.Kind.METHOD_INVOCATION)) {
      MethodInvocationTree mit = (MethodInvocationTree) tree;
      if ((PATTERN_MATCHER.matches(mit) || trackedMethodMatchers().matches(mit))) {
        return getRegexOperand(mit);
      }
      return Optional.ofNullable(methodInvocationToRegex.get(tree));
    }
    return ExpressionUtils.extractIdentifierSymbol(tree)
      .flatMap(symbol -> Optional.ofNullable(variableToRegex.get(symbol)));
  }

  private Optional<RegexParseResult> getRegexOperand(MethodInvocationTree mit) {
    if (mit.methodSelect().is(Tree.Kind.MEMBER_SELECT)) {
      ExpressionTree object = ((MemberSelectExpressionTree) mit.methodSelect()).expression();
      if (isPatternOrMatcher(object.symbolType())) {
        return getRegex(object);
      }
    }
    return mit.arguments()
      .stream()
      .filter(arg -> isPatternOrMatcher(arg.symbolType()))
      .map(this::getRegex)
      .filter(Optional::isPresent)
      .map(Optional::get)
      .findFirst();
  }

  @Override
  public void checkRegex(RegexParseResult regexForLiterals, ExpressionTree methodInvocationOrAnnotation) {
    regexCreations.put(regexForLiterals, methodInvocationOrAnnotation);
    if (methodInvocationOrAnnotation.is(Tree.Kind.ANNOTATION)) {
      methodsCalledOnRegex.put(regexForLiterals, new ArrayList<>());
      return;
    }
    MethodInvocationTree mit = (MethodInvocationTree) methodInvocationOrAnnotation;
    if (PATTERN_COMPILE.matches(mit)) {
      handleAssignment(mit, regexForLiterals);
      methodInvocationToRegex.put(mit, regexForLiterals);
      methodsCalledOnRegex.put(regexForLiterals, new ArrayList<>());
    } else if (trackedMethodMatchers().matches(mit)) {
      methodsCalledOnRegex.put(regexForLiterals, new ArrayList<>(Collections.singletonList(mit)));
    } else {
      methodsCalledOnRegex.put(regexForLiterals, new ArrayList<>());
    }
  }

  private void handleAssignment(MethodInvocationTree mit, RegexParseResult regex) {
    Tree parent = mit.parent();
    if (parent.is(Tree.Kind.VARIABLE, Tree.Kind.ASSIGNMENT)) {
      Optional<Symbol> assignedVariable = getAssignedPrivateEffectivelyFinalVariable(mit);
      if (assignedVariable.isPresent()) {
        variableToRegex.put(assignedVariable.get(), regex);
      } else {
        escapingRegexes.add(regex);
      }
    } else if (parent.is(Tree.Kind.ARGUMENTS)) {
      Tree grandParent = parent.parent();
      if (!grandParent.is(Tree.Kind.METHOD_INVOCATION) || !trackedMethodMatchers().matches((MethodInvocationTree) grandParent)) {
        escapingRegexes.add(regex);
      }
    }
  }

  private static boolean isPrivateEffectivelyFinalVariable(Symbol symbol) {
    return (symbol.isPrivate() || symbol.owner().isMethodSymbol())
      && symbol.isVariableSymbol()
      && (symbol.isFinal() || JUtils.isEffectivelyFinal((Symbol.VariableSymbol) symbol));
  }

  private static Optional<Symbol> getAssignedPrivateEffectivelyFinalVariable(MethodInvocationTree mit) {
    Tree parent = mit.parent();
    Symbol symbol = null;
    if (parent.is(Tree.Kind.VARIABLE)) {
      symbol = ((VariableTree) parent).symbol();
    } else if (parent.is(Tree.Kind.ASSIGNMENT)) {
      ExpressionTree variable = ((AssignmentExpressionTree) parent).variable();
      if (variable.is(Tree.Kind.IDENTIFIER)) {
        symbol = ((IdentifierTree) variable).symbol();
      } else if (variable.is(Tree.Kind.MEMBER_SELECT)) {
        MemberSelectExpressionTree mset = (MemberSelectExpressionTree) variable;
        if (ExpressionUtils.isSelectOnThisOrSuper(mset)) {
          symbol = mset.identifier().symbol();
        }
      }
    }
    return Optional.ofNullable(symbol).filter(AbstractRegexCheckTrackingMatchers::isPrivateEffectivelyFinalVariable);
  }
}
