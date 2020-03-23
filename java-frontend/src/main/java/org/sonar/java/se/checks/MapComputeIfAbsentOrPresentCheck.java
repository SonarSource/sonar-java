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
package org.sonar.java.se.checks;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
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

  private final Multimap<SymbolicValue, MapGetInvocation> mapGetInvocations = LinkedListMultimap.create();
  private final List<CheckIssue> checkIssues = new ArrayList<>();

  @Override
  public boolean isCompatibleWithJavaVersion(JavaVersion version) {
    return version.isJava8Compatible();
  }

  @Override
  public void init(MethodTree methodTree, CFG cfg) {
    mapGetInvocations.clear();
    checkIssues.clear();
  }

  @Override
  public ProgramState checkPostStatement(CheckerContext context, Tree syntaxNode) {
    if (syntaxNode.is(Tree.Kind.METHOD_INVOCATION)) {
      MethodInvocationTree mit = (MethodInvocationTree) syntaxNode;
      if (MAP_GET.matches(mit)) {
        ProgramState psBeforeInvocation = context.getNode().programState;
        ProgramState psAfterInvocation = context.getState();

        SymbolicValue keySV = psBeforeInvocation.peekValue(0);
        SymbolicValue mapSV = psBeforeInvocation.peekValue(1);
        SymbolicValue valueSV = psAfterInvocation.peekValue();

        mapGetInvocations.put(mapSV, new MapGetInvocation(valueSV, keySV, mit));
      }
    }
    return super.checkPostStatement(context, syntaxNode);
  }

  @Override
  public ProgramState checkPreStatement(CheckerContext context, Tree syntaxNode) {
    if (syntaxNode.is(Tree.Kind.METHOD_INVOCATION)) {
      MethodInvocationTree mit = (MethodInvocationTree) syntaxNode;
      if (MAP_PUT.matches(mit) && !isMethodInvocationThrowingCheckedException(mit.arguments().get(1))) {
        ProgramState ps = context.getState();

        SymbolicValue keySV = ps.peekValue(1);
        SymbolicValue mapSV = ps.peekValue(2);
        mapGetInvocations.get(mapSV).stream()
          .filter(getOnSameMap -> getOnSameMap.withSameKey(keySV))
          .findAny()
          .ifPresent(getOnSameMap -> {
            ObjectConstraint constraint = ps.getConstraint(getOnSameMap.value, ObjectConstraint.class);
            if (constraint != null && isInsideIfStatementWithNullCheckWithoutElse(mit)) {
              checkIssues.add(new CheckIssue(context.getNode(), getOnSameMap.mit, mit, getOnSameMap.value, constraint));
            }
          });
      }
    }
    return super.checkPreStatement(context, syntaxNode);
  }

  private static boolean isMethodInvocationThrowingCheckedException(ExpressionTree expr) {
    if (!expr.is(Tree.Kind.METHOD_INVOCATION)) {
      return false;
    }
    Symbol symbol = ((MethodInvocationTree) expr).symbol();
    if (!symbol.isMethodSymbol()) {
      // assume a checked exception could be returned
      return true;
    }
    return ((Symbol.MethodSymbol) symbol).thrownTypes().stream().anyMatch(t -> !t.isSubtypeOf("java.lang.RuntimeException"));
  }

  private static boolean isInsideIfStatementWithNullCheckWithoutElse(MethodInvocationTree mit) {
    Tree parent = mit.parent();
    while (parent != null && !parent.is(Tree.Kind.IF_STATEMENT)) {
      parent = parent.parent();
    }
    if (parent == null) {
      return false;
    }
    IfStatementTree ifStatementTree = (IfStatementTree) parent;
    return ifStatementTree.elseStatement() == null && isNullCheck(ExpressionUtils.skipParentheses(ifStatementTree.condition()));
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

  private static class CheckIssue {
    private final ExplodedGraph.Node node;

    private final MethodInvocationTree getInvocation;
    private final MethodInvocationTree putInvocation;

    private final SymbolicValue value;
    private final ObjectConstraint valueConstraint;

    private CheckIssue(Node node, MethodInvocationTree getInvocation, MethodInvocationTree putInvocation, SymbolicValue value, ObjectConstraint valueConstraint) {
      this.node = node;

      this.getInvocation = getInvocation;
      this.putInvocation = putInvocation;

      this.value = value;
      this.valueConstraint = valueConstraint;
    }

    private boolean isOnlyPossibleIssueForReportTree(List<CheckIssue> otherIssues) {
      return otherIssues.stream().noneMatch(this::differentIssueOnSameTree);
    }

    private boolean differentIssueOnSameTree(CheckIssue otherIssue) {
      return this != otherIssue && getInvocation.equals(otherIssue.getInvocation) && valueConstraint != otherIssue.valueConstraint;
    }

    private void report(CheckerContext context, SECheck check) {
      context.reportIssue(getInvocation, check, issueMsg(), flows());
    }

    private String issueMsg() {
      return String.format("Replace this \"Map.get()\" and condition with a call to \"Map.%s()\".",
        valueConstraint == ObjectConstraint.NULL ? "computeIfAbsent" : "computeIfPresent");
    }

    private Set<Flow> flows() {
      // build nullness flows for value constraint
      Set<Flow> flows = FlowComputation.flow(node, value, Collections.singletonList(ObjectConstraint.class));
      // enrich each flow with both map method invocations
      return flows.stream().map(flow -> Flow.builder()
        .add(new JavaFileScannerContext.Location("'Map.put()' is invoked with same key.", putInvocation.methodSelect()))
        .addAll(flow)
        .add(new JavaFileScannerContext.Location("'Map.get()' is invoked.", getInvocation.methodSelect()))
        .build()).collect(Collectors.toSet());
    }
  }

  private static class MapGetInvocation {
    private final SymbolicValue value;
    private final SymbolicValue key;
    private final MethodInvocationTree mit;

    private MapGetInvocation(SymbolicValue value, SymbolicValue key, MethodInvocationTree mit) {
      this.value = value;
      this.key = key;
      this.mit = mit;
    }

    private boolean withSameKey(SymbolicValue key) {
      return this.key.equals(key);
    }
  }
}
