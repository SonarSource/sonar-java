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
package org.sonar.java.se.checks;

import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.sonar.check.Rule;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.model.JUtils;
import org.sonar.java.se.CheckerContext;
import org.sonar.java.se.Flow;
import org.sonar.java.se.ProgramState;
import org.sonar.java.se.constraint.ObjectConstraint;
import org.sonar.java.se.symbolicvalues.SymbolicValue;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonarsource.analyzer.commons.collections.ListUtils;

import static org.sonar.java.se.ExplodedGraphWalker.EQUALS_METHODS;
import static org.sonar.plugins.java.api.semantic.SymbolMetadata.NullabilityLevel.PACKAGE;
import static org.sonar.plugins.java.api.semantic.SymbolMetadata.NullabilityLevel.VARIABLE;

@Rule(key = "S4449")
public class ParameterNullnessCheck extends SECheck {

  private static final MethodMatchers AUTHORIZED_METHODS = MethodMatchers.or(
    MethodMatchers.create().ofTypes("com.google.common.base.Preconditions").names("checkNotNull").withAnyParameters().build(),
    EQUALS_METHODS
  );

  @Override
  public ProgramState checkPreStatement(CheckerContext context, Tree syntaxNode) {
    ProgramState state = context.getState();
    if (syntaxNode.is(Tree.Kind.METHOD_INVOCATION)) {
      MethodInvocationTree mit = (MethodInvocationTree) syntaxNode;
      checkParameters(mit, mit.symbol(), mit.arguments(), state);
    } else if (syntaxNode.is(Tree.Kind.NEW_CLASS)) {
      NewClassTree nct = (NewClassTree) syntaxNode;
      checkParameters(nct, nct.constructorSymbol(), nct.arguments(), state);
    }
    return state;
  }

  private void checkParameters(Tree syntaxNode, Symbol.MethodSymbol symbol, Arguments arguments, ProgramState state) {
    if (!symbol.isMethodSymbol() || arguments.isEmpty()) {
      return;
    }
    if (AUTHORIZED_METHODS.matches(symbol)) {
      return;
    }
    int nbArguments = arguments.size();
    List<SymbolicValue> argumentSVs = getArgumentSVs(state, syntaxNode, nbArguments);
    int nbArgumentToCheck = Math.min(nbArguments, symbol.parameterTypes().size() - (JUtils.isVarArgsMethod(symbol) ? 1 : 0));
    List<Symbol> parameterSymbols = symbol.declarationParameters();
    for (int i = 0; i < nbArgumentToCheck; i++) {
      ObjectConstraint constraint = state.getConstraint(argumentSVs.get(i), ObjectConstraint.class);
      if (constraint != null && constraint.isNull() && parameterIsNonNullIndirectly(parameterSymbols.get(i))) {
        reportIssue(syntaxNode, arguments.get(i), symbol);
      }
    }
  }

  private void reportIssue(Tree syntaxNode, ExpressionTree argument, Symbol.MethodSymbol methodSymbol) {
    String declarationMessage = "constructor declaration";
    if (!"<init>".equals(methodSymbol.name())) {
      declarationMessage = "method '" + methodSymbol.name() + "' declaration";
    }
    String message = String.format("Annotate the parameter with @javax.annotation.Nullable in %s, or make sure that null can not be passed as argument.", declarationMessage);

    Tree reportTree;
    if (syntaxNode.is(Tree.Kind.METHOD_INVOCATION)) {
      reportTree = ExpressionUtils.methodName((MethodInvocationTree) syntaxNode);
    } else {
      reportTree = ((NewClassTree) syntaxNode).identifier();
    }

    Flow.Builder secondaryBuilder = Flow.builder();
    MethodTree declarationTree = methodSymbol.declaration();
    if (declarationTree != null) {
      secondaryBuilder.add(new JavaFileScannerContext.Location(StringUtils.capitalize(declarationMessage) + ".", declarationTree.simpleName()));
    }
    secondaryBuilder.add(new JavaFileScannerContext.Location("Argument can be null.", argument));

    reportIssue(reportTree, message, Collections.singleton(secondaryBuilder.build()));
  }

  private static List<SymbolicValue> getArgumentSVs(ProgramState state, Tree syntaxNode, int nbArguments) {
    if (syntaxNode.is(Tree.Kind.METHOD_INVOCATION)) {
      return ListUtils.reverse(state.peekValues(nbArguments + 1).subList(0, nbArguments));
    }
    return ListUtils.reverse(state.peekValues(nbArguments));
  }

  private static boolean parameterIsNonNullIndirectly(Symbol symbol) {
    SymbolMetadata.NullabilityData nullabilityData = symbol.metadata().nullabilityData();
    return nullabilityData.isNonNull(PACKAGE, false, false) && nullabilityData.level() != VARIABLE;
  }
}
