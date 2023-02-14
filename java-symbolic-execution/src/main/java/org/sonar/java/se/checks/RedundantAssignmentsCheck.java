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
package org.sonar.java.se.checks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.java.cfg.CFG;
import org.sonar.java.collections.CollectionUtils;
import org.sonarsource.analyzer.commons.collections.SetUtils;
import org.sonar.java.se.CheckerContext;
import org.sonar.java.se.ExplodedGraph;
import org.sonar.java.se.Flow;
import org.sonar.java.se.FlowComputation;
import org.sonar.java.se.ProgramState;
import org.sonar.java.se.ProgramState.SymbolicValueSymbol;
import org.sonar.java.se.symbolicvalues.SymbolicValue;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S4165")
public class RedundantAssignmentsCheck extends SECheck {

  private static final Set<String> STREAM_TYPES = SetUtils.immutableSetOf(
    "java.util.stream.Stream",
    "java.util.stream.IntStream",
    "java.util.stream.LongStream",
    "java.util.stream.DoubleStream");
  private final Deque<Map<AssignmentExpressionTree, List<AssignmentDataHolder>>> assignmentsByMethod = new LinkedList<>();

  @Override
  public void scanFile(JavaFileScannerContext context) {
    assignmentsByMethod.clear();
    super.scanFile(context);
  }

  @Override
  public void init(MethodTree methodTree, CFG cfg) {
    assignmentsByMethod.push(new HashMap<>());
  }

  @Override
  public ProgramState checkPostStatement(CheckerContext context, Tree syntaxNode) {
    if (syntaxNode.is(Tree.Kind.ASSIGNMENT)) {
      handleAssignment(context, (AssignmentExpressionTree) syntaxNode);
    }
    return super.checkPostStatement(context, syntaxNode);
  }

  private void handleAssignment(CheckerContext context, AssignmentExpressionTree assignmentExpressionTree) {
    SymbolicValueSymbol assignedVariable = context.getState().peekValueSymbol();
    Symbol assignedSymbol = assignedVariable.symbol();
    if (assignedSymbol == null
      // Rule S3959 returns the same SV after each intermediate operations,
      // meaning that 'stream = stream.map(...);' would be detected as redundant assignment if not explicitly excluded
      || STREAM_TYPES.stream().anyMatch(assignedSymbol.type()::is)) {
      return;
    }
    ExplodedGraph.Node node = context.getNode();
    ProgramState previousState = node.programState;
    SymbolicValue oldValue = previousState.getValue(assignedSymbol);
    SymbolicValue newValue = assignedVariable.symbolicValue();
    Symbol fromSymbol = previousState.peekValueSymbol().symbol();
    assignmentsByMethod.peek().computeIfAbsent(assignmentExpressionTree, 
      k -> new ArrayList<>()).add(new AssignmentDataHolder(assignedSymbol, oldValue, newValue, fromSymbol, node));
  }

  @Override
  public void interruptedExecution(CheckerContext context) {
    this.assignmentsByMethod.pop();
  }

  @Override
  public void checkEndOfExecution(CheckerContext context) {
    for (Map.Entry<AssignmentExpressionTree, List<AssignmentDataHolder>> assignmentForTree : assignmentsByMethod.pop().entrySet()) {
      Collection<AssignmentDataHolder> allAssignments = assignmentForTree.getValue();
      if (allAssignments.stream().allMatch(AssignmentDataHolder::isRedundant)) {
        Set<Flow> flows = allAssignments.stream().map(AssignmentDataHolder::flows).flatMap(Set::stream).collect(Collectors.toSet());
        reportIssue(assignmentForTree.getKey(),
          String.format("Remove this useless assignment; \"%s\" already holds the assigned value along all execution paths.",
            CollectionUtils.getFirst(allAssignments, null).assignedSymbol.name()),
          flows);
      }
    }
  }

  private static class AssignmentDataHolder {

    private final Symbol assignedSymbol;
    @Nullable
    private final Symbol fromSymbol;
    private final SymbolicValue oldValue;
    private final SymbolicValue newValue;
    private final ExplodedGraph.Node node;

    public AssignmentDataHolder(Symbol assignedSymbol, @Nullable SymbolicValue oldValue, SymbolicValue newValue, @Nullable Symbol fromSymbol, ExplodedGraph.Node node) {
      this.assignedSymbol = assignedSymbol;
      this.fromSymbol = fromSymbol;
      this.oldValue = oldValue;
      this.newValue = newValue;
      this.node = node;
    }

    public boolean isRedundant() {
      return oldValue == newValue;
    }

    public Set<Flow> flows() {
      return FlowComputation.flow(node, newValue, Collections.emptyList(), fromSymbol, FlowComputation.MAX_REPORTED_FLOWS);
    }
  }

}
