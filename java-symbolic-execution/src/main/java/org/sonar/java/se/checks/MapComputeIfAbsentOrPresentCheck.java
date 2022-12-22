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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.java.JavaVersionAwareVisitor;
import org.sonar.java.cfg.CFG;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.se.CheckerContext;
import org.sonar.java.se.ExplodedGraph;
import org.sonar.java.se.ExplodedGraph.Node;
import org.sonar.java.se.Flow;
import org.sonar.java.se.FlowComputation;
import org.sonar.java.se.ProgramState;
import org.sonar.java.se.constraint.BooleanConstraint;
import org.sonar.java.se.constraint.Constraint;
import org.sonar.java.se.constraint.ObjectConstraint;
import org.sonar.java.se.symbolicvalues.SymbolicValue;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IfStatementTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

import static org.sonar.plugins.java.api.semantic.MethodMatchers.ANY;

@Rule(key = "S3824")
public class MapComputeIfAbsentOrPresentCheck extends SECheck implements JavaVersionAwareVisitor {

  private static final MethodMatchers.NameBuilder JAVA_UTIL_MAP = MethodMatchers.create().ofSubTypes("java.util.Map");
  private static final MethodMatchers MAP_GET = JAVA_UTIL_MAP.names("get").addParametersMatcher(ANY).build();
  private static final MethodMatchers MAP_PUT = JAVA_UTIL_MAP.names("put").addParametersMatcher(ANY, ANY).build();
  private static final MethodMatchers MAP_CONTAINS_KEY = JAVA_UTIL_MAP.names("containsKey").addParametersMatcher(ANY).build();

  private final Map<SymbolicValue, List<MapMethodInvocation>> mapGetInvocations = new HashMap<>();
  private final Map<SymbolicValue, List<MapMethodInvocation>> mapContainsKeyInvocations = new HashMap<>();
  private final List<CheckIssue> checkIssues = new ArrayList<>();
  private final Map<Tree, IfStatementTree> closestIfStatements = new HashMap<>();

  @Override
  public boolean isCompatibleWithJavaVersion(JavaVersion version) {
    return version.isJava8Compatible();
  }

  @Override
  public void init(MethodTree methodTree, CFG cfg) {
    mapContainsKeyInvocations.clear();
    mapGetInvocations.clear();
    checkIssues.clear();
    closestIfStatements.clear();
  }

  @Override
  public ProgramState checkPostStatement(CheckerContext context, Tree syntaxNode) {
    if (syntaxNode.is(Tree.Kind.METHOD_INVOCATION)) {
      MethodInvocationTree mit = (MethodInvocationTree) syntaxNode;
      if (MAP_GET.matches(mit)) {
        addMapMethodInvocation(context, mit, mapGetInvocations);
      } else if (MAP_CONTAINS_KEY.matches(mit)) {
        addMapMethodInvocation(context, mit, mapContainsKeyInvocations);
      }
    }
    return super.checkPostStatement(context, syntaxNode);
  }

  private static void addMapMethodInvocation(CheckerContext context, MethodInvocationTree mit, Map<SymbolicValue, List<MapMethodInvocation>> invocations) {
    ProgramState psBeforeInvocation = context.getNode().programState;
    ProgramState psAfterInvocation = context.getState();

    SymbolicValue keySV = psBeforeInvocation.peekValue(0);
    SymbolicValue mapSV = psBeforeInvocation.peekValue(1);
    SymbolicValue valueSV = psAfterInvocation.peekValue();

    Objects.requireNonNull(valueSV);
    invocations.computeIfAbsent(mapSV, k -> new ArrayList<>()).add(new MapMethodInvocation(valueSV, keySV, mit));
  }

  @Override
  public ProgramState checkPreStatement(CheckerContext context, Tree syntaxNode) {
    if (syntaxNode.is(Tree.Kind.METHOD_INVOCATION)) {
      MethodInvocationTree mit = (MethodInvocationTree) syntaxNode;
      if (MAP_PUT.matches(mit)) {
        ExpressionTree valueArgument = mit.arguments().get(1);
        if (!isMethodInvocationThrowingCheckedException(valueArgument) && !valueArgument.is(Tree.Kind.NULL_LITERAL)) {
          checkForGetAndContainsKeyInvocations(context, mit);
        }
      }
    }
    return super.checkPreStatement(context, syntaxNode);
  }

  private void checkForGetAndContainsKeyInvocations(CheckerContext context, MethodInvocationTree mit) {
    ProgramState ps = context.getState();

    SymbolicValue keySV = ps.peekValue(1);
    SymbolicValue mapSV = ps.peekValue(2);

    sameMapAndSameKeyInvocation(keySV, mapSV, mapGetInvocations)
      .ifPresent(getOnSameMap -> {
        ObjectConstraint constraint = ps.getConstraint(getOnSameMap.value, ObjectConstraint.class);
        if (constraint != null && isInsideIfStatementWithNullCheckWithoutElse(mit)) {
          checkIssues.add(new GetMethodCheckIssue(context.getNode(), getOnSameMap.mit, mit, getOnSameMap.value, constraint));
        }
      });

    sameMapAndSameKeyInvocation(keySV, mapSV, mapContainsKeyInvocations)
      .ifPresent(containsKeyOnSameMap -> {
        BooleanConstraint constraint = ps.getConstraint(containsKeyOnSameMap.value, BooleanConstraint.class);
        if (constraint != null && isInsideIfStatementWithoutElse(mit)) {
          checkIssues.add(new ContainsKeyMethodCheckIssue(context.getNode(), containsKeyOnSameMap.mit, mit, containsKeyOnSameMap.value, constraint));
        }
      });
  }

  private static Optional<MapMethodInvocation> sameMapAndSameKeyInvocation(SymbolicValue keySV, SymbolicValue mapSV,
    Map<SymbolicValue, List<MapMethodInvocation>> mapGetInvocations) {
    return mapGetInvocations.getOrDefault(mapSV, Collections.emptyList()).stream()
      .filter(getOnSameMap -> getOnSameMap.withSameKey(keySV))
      .findAny();
  }

  private static boolean isMethodInvocationThrowingCheckedException(ExpressionTree expr) {
    if (!expr.is(Tree.Kind.METHOD_INVOCATION)) {
      return false;
    }
    Symbol.MethodSymbol symbol = ((MethodInvocationTree) expr).methodSymbol();
    if (symbol.isUnknown()) {
      // assume a checked exception could be returned
      return true;
    }
    return symbol.thrownTypes().stream().anyMatch(t -> !t.isSubtypeOf("java.lang.RuntimeException"));
  }

  private boolean isInsideIfStatementWithNullCheckWithoutElse(MethodInvocationTree mit) {
    return getIfStatementParent(mit).map(ifStatementTree -> ifStatementTree.elseStatement() == null
      && isNullCheck(ExpressionUtils.skipParentheses(ifStatementTree.condition())))
      .orElse(false);
  }

  private boolean isInsideIfStatementWithoutElse(MethodInvocationTree mit) {
    return getIfStatementParent(mit).map(ifStatementTree -> ifStatementTree.elseStatement() == null).orElse(false);
  }

  private Optional<IfStatementTree> getIfStatementParent(MethodInvocationTree mit) {
    IfStatementTree closestKnownParent = closestIfStatements.get(mit);
    if (closestKnownParent == null) {
      List<Tree> children = new ArrayList<>();
      children.add(mit);
      return doGetIfStatementParent(mit.parent(), children);
    }
    return Optional.of(closestKnownParent);
  }

  private Optional<IfStatementTree> doGetIfStatementParent(@Nullable Tree currentTree, List<Tree> children) {
    while (currentTree != null) {
      if (currentTree.is(Tree.Kind.IF_STATEMENT)) {
        IfStatementTree ifStatementTree = (IfStatementTree) currentTree;
        children.forEach(tree -> closestIfStatements.put(tree, ifStatementTree));
        return Optional.of(ifStatementTree);
      }
      IfStatementTree ifStatementTree = closestIfStatements.get(currentTree);
      if (ifStatementTree != null) {
        children.forEach(tree -> closestIfStatements.put(tree, ifStatementTree));
        return Optional.of(ifStatementTree);
      }
      children.add(currentTree);
      currentTree = currentTree.parent();
    }
    return Optional.empty();
  }

  private static boolean isNullCheck(ExpressionTree condition) {
    if (condition.is(Tree.Kind.EQUAL_TO, Tree.Kind.NOT_EQUAL_TO)) {
      BinaryExpressionTree bet = (BinaryExpressionTree) condition;
      ExpressionTree rightOperand = ExpressionUtils.skipParentheses(bet.rightOperand());
      ExpressionTree leftOperand = ExpressionUtils.skipParentheses(bet.leftOperand());
      return rightOperand.is(Tree.Kind.NULL_LITERAL) || leftOperand.is(Tree.Kind.NULL_LITERAL);
    }
    return false;
  }

  @Override
  public void checkEndOfExecution(CheckerContext context) {
    SECheck check = this;
    checkIssues.stream().filter(checkIssue -> checkIssue.isOnlyPossibleIssueForReportTree(checkIssues)).forEach(issue -> issue.report(context, check));
  }

  private abstract static class CheckIssue {
    protected final ExplodedGraph.Node node;

    private final MethodInvocationTree checkValueInvocation;
    private final MethodInvocationTree putInvocation;

    protected final SymbolicValue value;
    protected final Constraint valueConstraint;

    private CheckIssue(Node node, MethodInvocationTree checkValueInvocation, MethodInvocationTree putInvocation, SymbolicValue value, Constraint constraint) {
      this.node = node;

      this.checkValueInvocation = checkValueInvocation;
      this.putInvocation = putInvocation;

      this.value = value;
      this.valueConstraint = constraint;
    }

    private boolean isOnlyPossibleIssueForReportTree(List<CheckIssue> otherIssues) {
      return otherIssues.stream().noneMatch(this::differentIssueOnSameTree);
    }

    private boolean differentIssueOnSameTree(CheckIssue otherIssue) {
      return this != otherIssue
        && checkValueInvocation.equals(otherIssue.checkValueInvocation)
        && valueConstraint != otherIssue.valueConstraint;
    }

    protected abstract String issueMsg();

    protected abstract Set<Flow> flows();

    private void report(CheckerContext context, SECheck check) {
      context.reportIssue(checkValueInvocation, check, issueMsg(), flows());
    }

    protected Set<Flow> flows(String methodName, Set<Flow> flows) {
      // enrich each flow with both map method invocations
      return flows.stream().map(flow -> Flow.builder()
        .add(new JavaFileScannerContext.Location("'Map.put()' is invoked with same key.", putInvocation.methodSelect()))
        .addAll(flow)
        .add(new JavaFileScannerContext.Location(String.format("'%s' is invoked.", methodName), checkValueInvocation.methodSelect()))
        .build()).collect(Collectors.toSet());
    }
  }

  private static final class GetMethodCheckIssue extends CheckIssue {

    private GetMethodCheckIssue(Node node, MethodInvocationTree checkValueInvocation, MethodInvocationTree putInvocation,
      SymbolicValue value, ObjectConstraint valueConstraint) {
      super(node, checkValueInvocation, putInvocation, value, valueConstraint);
    }

    @Override
    protected String issueMsg() {
      return String.format("Replace this \"Map.get()\" and condition with a call to \"Map.%s()\".",
        valueConstraint == ObjectConstraint.NULL ? "computeIfAbsent" : "computeIfPresent");
    }

    @Override
    protected Set<Flow> flows() {
      // build nullness flows for value constraint
      Set<Flow> flows = FlowComputation.flow(node, value, Collections.singletonList(ObjectConstraint.class), FlowComputation.MAX_REPORTED_FLOWS);
      return flows("Map.get()", flows);
    }
  }

  private static final class ContainsKeyMethodCheckIssue extends CheckIssue {

    private ContainsKeyMethodCheckIssue(Node node, MethodInvocationTree checkValueInvocation, MethodInvocationTree putInvocation,
      SymbolicValue value, BooleanConstraint constraint) {
      super(node, checkValueInvocation, putInvocation, value, constraint);
    }

    @Override
    protected String issueMsg() {
      return String.format("Replace this \"Map.containsKey()\" with a call to \"Map.%s()\".",
        valueConstraint == BooleanConstraint.FALSE ? "computeIfAbsent" : "computeIfPresent");
    }

    @Override
    protected Set<Flow> flows() {
      // build boolean flows for value constraint
      Set<Flow> flows = FlowComputation.flow(node, value, Collections.singletonList(BooleanConstraint.class), FlowComputation.MAX_REPORTED_FLOWS);
      return flows("Map.containsKey()", flows);
    }
  }

  private static class MapMethodInvocation {
    private final SymbolicValue value;
    private final SymbolicValue key;
    private final MethodInvocationTree mit;

    private MapMethodInvocation(SymbolicValue value, SymbolicValue key, MethodInvocationTree mit) {
      this.value = value;
      this.key = key;
      this.mit = mit;
    }

    private boolean withSameKey(SymbolicValue key) {
      return this.key.equals(key);
    }
  }
}
