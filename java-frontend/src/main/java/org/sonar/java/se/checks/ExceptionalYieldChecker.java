/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

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

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class ExceptionalYieldChecker {

  private final String message;

  ExceptionalYieldChecker(String message) {
    this.message = message;
  }

  void reportOnExceptionalYield(ExplodedGraph.Node node, SECheck check) {
    node.edges().stream().forEach(edge -> edge.yields().stream()
      .filter(yield -> yield.generatedByCheck(check))
      .forEach(yield -> reportIssue(edge.parent(), (ExceptionalCheckBasedYield) yield, check))
    );
  }

  private void reportIssue(ExplodedGraph.Node node, ExceptionalCheckBasedYield yield, SECheck check) {
    MethodInvocationTree mit = (MethodInvocationTree) node.programPoint.syntaxTree();
    ExpressionTree methodSelect = mit.methodSelect();
    String methodName = mit.symbol().name();

    Tree reportTree = methodSelect;
    if (methodSelect.is(Tree.Kind.MEMBER_SELECT)) {
      reportTree = ((MemberSelectExpressionTree) methodSelect).identifier();
    }

    JavaFileScannerContext.Location methodInvocationMessage;
    int parameterCausingExceptionIndex = yield.parameterCausingExceptionIndex();
    IdentifierTree identifierTree = FlowComputation.getArgumentIdentifier(mit, parameterCausingExceptionIndex);
    if (identifierTree != null) {
      methodInvocationMessage = new JavaFileScannerContext.Location(String.format("'%s' is passed to '%s()'.", identifierTree.name(), methodName), identifierTree);
    } else {
      methodInvocationMessage = new JavaFileScannerContext.Location(String.format("'%s()' is invoked.", methodName), reportTree);
    }

    Flow argumentChangingNameFlows = flowsForArgumentsChangingName(yield, mit);
    Set<Flow> argumentsFlows = flowsForMethodArguments(node, mit, parameterCausingExceptionIndex);
    Set<Flow> exceptionFlows = yield.exceptionFlows();

    ImmutableSet.Builder<Flow> flows = ImmutableSet.builder();
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

    check.reportIssue(reportTree, String.format(message, methodName), flows.build());
  }

  private static Set<Flow> flowsForMethodArguments(ExplodedGraph.Node node, MethodInvocationTree mit, int parameterCausingExceptionIndex) {
    ProgramState programState = node.programState;
    List<ProgramState.SymbolicValueSymbol> arguments = Lists.reverse(programState.peekValuesAndSymbols(mit.arguments().size()));
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
    return FlowComputation.flow(node, argSymbolicValues, c -> true, c -> false, domains, argSymbols);
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
      .collect(Collectors.toList());
  }

  private static Flow flowsForArgumentsChangingName(ExceptionalCheckBasedYield yield, MethodInvocationTree mit) {
    return FlowComputation.flowsForArgumentsChangingName(Collections.singletonList(yield.parameterCausingExceptionIndex()), mit);
  }
}
