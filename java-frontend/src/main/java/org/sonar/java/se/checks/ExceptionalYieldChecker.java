/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

import org.sonar.java.collections.PMap;
import org.sonar.java.se.ExplodedGraph;
import org.sonar.java.se.FlowComputation;
import org.sonar.java.se.ProgramState;
import org.sonar.java.se.constraint.Constraint;
import org.sonar.java.se.symbolicvalues.SymbolicValue;
import org.sonar.java.se.xproc.ExceptionalCheckBasedYield;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.ArrayList;
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
    JavaFileScannerContext.Location methodInvocationMessage = new JavaFileScannerContext.Location(String.format("'%s()' is invoked.", methodName), reportTree);

    Set<List<JavaFileScannerContext.Location>> argumentsFlows = flowsForMethodArguments(node, mit);
    Set<List<JavaFileScannerContext.Location>> exceptionFlows = yield.exceptionFlows();

    ImmutableSet.Builder<List<JavaFileScannerContext.Location>> flows = ImmutableSet.builder();
    for (List<JavaFileScannerContext.Location> argumentFlow : argumentsFlows) {
      for (List<JavaFileScannerContext.Location> exceptionFlow : exceptionFlows) {
        flows.add(ImmutableList.<JavaFileScannerContext.Location>builder()
          .addAll(Lists.reverse(argumentFlow))
          .add(methodInvocationMessage)
          .addAll(Lists.reverse(exceptionFlow))
          .build());
      }
    }

    check.reportIssue(reportTree, String.format(message, methodName), flows.build());
  }

  private static Set<List<JavaFileScannerContext.Location>> flowsForMethodArguments(ExplodedGraph.Node node, MethodInvocationTree mit) {
    ProgramState programState = node.programState;
    List<SymbolicValue> arguments = Lists.reverse(programState.peekValues(mit.arguments().size()));
    List<Class<? extends Constraint>> domains = domainsFromArguments(programState, arguments);
    return FlowComputation.flow(node, new LinkedHashSet<>(arguments), c -> true, c -> false, domains, programState.getLastEvaluated());
  }

  private static List<Class<? extends Constraint>> domainsFromArguments(ProgramState programState, List<SymbolicValue> arguments) {
    return arguments.stream()
      .map(programState::getConstraints)
      .filter(Objects::nonNull)
      .map(ExceptionalYieldChecker::domainsFromConstraints)
      .flatMap(List::stream)
      .distinct()
      .collect(Collectors.toList());
  }

  private static List<Class<? extends Constraint>> domainsFromConstraints(PMap<Class<? extends Constraint>, Constraint> constraints) {
    List<Class<? extends Constraint>> domains = new ArrayList<>();
    constraints.forEach((d, c) -> domains.add(d));
    return domains;
  }
}
