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
package org.sonar.java.se.checks;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.sonar.java.se.ExplodedGraph;
import org.sonar.java.se.Flow;
import org.sonar.java.se.FlowComputation;
import org.sonar.java.se.ProgramState;
import org.sonar.java.se.constraint.Constraint;
import org.sonar.java.se.constraint.ConstraintsByDomain;
import org.sonar.java.se.constraint.ObjectConstraint;
import org.sonar.java.se.symbolicvalues.SymbolicValue;
import org.sonar.java.se.xproc.ExceptionalCheckBasedYield;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonarsource.analyzer.commons.collections.ListUtils;

public class ExceptionalYieldChecker {

  private final String message;

  ExceptionalYieldChecker(String message) {
    this.message = message;
  }

  void reportOnExceptionalYield(ExplodedGraph.Node node, SECheck check) {
    node.edges().stream().forEach(edge -> edge.yields().stream()
      .filter(methodYield -> methodYield.generatedByCheck(check))
      .forEach(methodYield -> reportIssue(edge.parent(), (ExceptionalCheckBasedYield) methodYield, check))
    );
  }

  private void reportIssue(ExplodedGraph.Node node, ExceptionalCheckBasedYield exceptionalYield, SECheck check) {
    MethodInvocationTree mit = (MethodInvocationTree) node.programPoint.syntaxTree();
    ExpressionTree methodSelect = mit.methodSelect();
    String methodName = mit.methodSymbol().name();

    Tree reportTree = methodSelect;
    if (methodSelect.is(Tree.Kind.MEMBER_SELECT)) {
      reportTree = ((MemberSelectExpressionTree) methodSelect).identifier();
    }

    JavaFileScannerContext.Location methodInvocationMessage;
    int parameterCausingExceptionIndex = exceptionalYield.parameterCausingExceptionIndex();
    IdentifierTree identifierTree = FlowComputation.getArgumentIdentifier(mit, parameterCausingExceptionIndex);
    if (identifierTree != null) {
      methodInvocationMessage = new JavaFileScannerContext.Location(String.format("'%s' is passed to '%s()'.", identifierTree.name(), methodName), identifierTree);
    } else {
      methodInvocationMessage = new JavaFileScannerContext.Location(String.format("'%s()' is invoked.", methodName), reportTree);
    }

    Flow argumentChangingNameFlows = flowsForArgumentsChangingName(exceptionalYield, mit);
    Set<Flow> argumentsFlows = flowsForMethodArguments(node, mit, parameterCausingExceptionIndex, FlowComputation.MAX_REPORTED_FLOWS);
    Set<Flow> exceptionFlows = exceptionalYield.exceptionFlows(FlowComputation.MAX_REPORTED_FLOWS);

    Set<Flow> flows = new HashSet<>();
    for (Flow argumentFlow : argumentsFlows) {
      for (Flow exceptionFlow : exceptionFlows) {
        flows.add(Flow.builder()
          .addAll(exceptionFlow)
          .addAll(argumentChangingNameFlows)
          .add(methodInvocationMessage)
          .addAll(argumentFlow)
          .build());
      }
    }

    check.reportIssue(reportTree, String.format(message, methodName), Collections.unmodifiableSet(flows));
  }

  private static Set<Flow> flowsForMethodArguments(ExplodedGraph.Node node, MethodInvocationTree mit, int parameterCausingExceptionIndex, int maxReturnedFlows) {
    ProgramState programState = node.programState;
    List<ProgramState.SymbolicValueSymbol> arguments = ListUtils.reverse(programState.peekValuesAndSymbols(mit.arguments().size()));
    SymbolicValue parameterCausingExceptionSV = arguments.get(parameterCausingExceptionIndex).symbolicValue();

    Set<SymbolicValue> argSymbolicValues = new LinkedHashSet<>();
    Set<Symbol> argSymbols = new LinkedHashSet<>();
    arguments.stream()
      .filter(svs -> parameterCausingExceptionSV == svs.symbolicValue() || hasConstraintOtherThanNonNull(svs, programState))
      .forEach(svs -> {
        argSymbolicValues.add(svs.symbolicValue());
        Symbol symbol = svs.symbol();
        if (symbol != null) {
          argSymbols.add(symbol);
        }
      });

    List<Class<? extends Constraint>> domains = domainsFromArguments(programState, argSymbolicValues);
    return FlowComputation.flow(node, argSymbolicValues, c -> true, c -> false, domains, argSymbols, maxReturnedFlows);
  }

  private static boolean hasConstraintOtherThanNonNull(ProgramState.SymbolicValueSymbol svs, ProgramState ps) {
    SymbolicValue sv = svs.symbolicValue();
    ConstraintsByDomain constraints = ps.getConstraints(sv);
    return constraints != null && !hasOnlyNonNullConstraint(constraints);
  }

  private static boolean hasOnlyNonNullConstraint(ConstraintsByDomain constraints) {
    return constraints.domains().count() == 1 && constraints.get(ObjectConstraint.class) == ObjectConstraint.NOT_NULL;
  }

  private static List<Class<? extends Constraint>> domainsFromArguments(ProgramState programState, Collection<SymbolicValue> arguments) {
    return arguments.stream()
      .map(programState::getConstraints)
      .filter(Objects::nonNull)
      .flatMap(ConstraintsByDomain::domains)
      .distinct()
      .toList();
  }

  private static Flow flowsForArgumentsChangingName(ExceptionalCheckBasedYield exceptionalYield, MethodInvocationTree mit) {
    return FlowComputation.flowsForArgumentsChangingName(Collections.singletonList(exceptionalYield.parameterCausingExceptionIndex()), mit);
  }
}
