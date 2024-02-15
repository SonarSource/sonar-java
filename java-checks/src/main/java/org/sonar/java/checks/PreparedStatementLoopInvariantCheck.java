/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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
package org.sonar.java.checks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.sonar.check.Rule;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.DoWhileStatementTree;
import org.sonar.plugins.java.api.tree.ForEachStatement;
import org.sonar.plugins.java.api.tree.ForStatementTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonar.plugins.java.api.tree.WhileStatementTree;

@Rule(key = "S6909")
public class PreparedStatementLoopInvariantCheck extends IssuableSubscriptionVisitor {

  private static final MethodMatchers matchers = MethodMatchers.create()
    .ofSubTypes("java.sql.PreparedStatement")
    .name(it -> it.startsWith("set"))
    .withAnyParameters()
    .build();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    var invocvationCollector = new MethodInvocationCollector(matchers);
    tree.accept(invocvationCollector);

    var candidatesByLoopBody = new HashMap<StatementTree, List<Candidate>>();
    invocvationCollector.invocations.stream()
      .map(PreparedStatementLoopInvariantCheck::getCandidate)
      .filter(Objects::nonNull)
      .forEach(candidate -> {
        var candidates = candidatesByLoopBody.computeIfAbsent(candidate.enclosingLoopBody, key -> new ArrayList<>());
        candidates.add(candidate);
      });

    candidatesByLoopBody.forEach((loopBody, candidates) -> {
      var localsCollector = new DeclaredOrAssignedLocalsCollector();
      loopBody.accept(localsCollector);
      candidates.forEach(it -> reportIfLoopInvariant(localsCollector.declaredOrAssignedLocals, it));
    });
  }

  private void reportIfLoopInvariant(Set<String> declaredOrAssignedLocals, Candidate candidate) {
    if (isLoopInvariant(declaredOrAssignedLocals, candidate)) {
      var secondaryLocation = new JavaFileScannerContext.Location(
        "Enclosing loop",
        Objects.requireNonNull(candidate.enclosingLoopBody.parent())
      );
      reportIssue(
        candidate.invocation,
        "Move this loop-invariant setter invocation out of this loop.",
        List.of(secondaryLocation),
        null
      );
    }
  }

  private static boolean isLoopInvariant(Set<String> declaredOrAssignedLocals, Candidate candidate) {
    return (candidate.identifierArguments.stream().noneMatch(declaredOrAssignedLocals::contains));
  }

  private static Candidate getCandidate(MethodInvocationTree invocation) {
    var identifierArguments = new ArrayList<String>();
    for (var arg : invocation.arguments()) {
      if (arg.is(Tree.Kind.IDENTIFIER)) {
        identifierArguments.add(((IdentifierTree) arg).name());
      } else {
        var argValue = ExpressionUtils.resolveAsConstant(arg);
        if (argValue == null) return null;
      }
    }

    var loopBody = findEnclosingLoopBody(invocation);
    if (loopBody == null) {
      return null;
    }
    return new Candidate(invocation, identifierArguments, loopBody);
  }

  private static StatementTree findEnclosingLoopBody(Tree tree) {
    while (!Objects.requireNonNull(tree).is(Tree.Kind.METHOD)) {
      var loopBody = getLoopBodyIfLoop(tree);
      if (loopBody != null) {
        return loopBody;
      }
      tree = tree.parent();
    }
    return null;
  }

  private static StatementTree getLoopBodyIfLoop(Tree tree) {
    switch (tree.kind()) {
      case FOR_STATEMENT:
        return ((ForStatementTree) tree).statement();
      case FOR_EACH_STATEMENT:
        return ((ForEachStatement) tree).statement();
      case WHILE_STATEMENT:
        return ((WhileStatementTree) tree).statement();
      case DO_STATEMENT:
        return ((DoWhileStatementTree) tree).statement();
      default:
        return null;
    }
  }

  private static class Candidate {
    public final MethodInvocationTree invocation;
    public final List<String> identifierArguments;
    public final StatementTree enclosingLoopBody;

    private Candidate(MethodInvocationTree invocation, List<String> argumentVariables, StatementTree enclosingLoopBody) {
      this.invocation = invocation;
      this.identifierArguments = argumentVariables;
      this.enclosingLoopBody = enclosingLoopBody;
    }
  }

  private static class MethodInvocationCollector extends BaseTreeVisitor {

    public final List<MethodInvocationTree> invocations = new ArrayList<>();
    private final MethodMatchers matchers;

    public MethodInvocationCollector(MethodMatchers matchers) {
      this.matchers = matchers;
    }

    @Override
    public void visitMethodInvocation(MethodInvocationTree tree) {
      if (matchers.matches(tree)) {
        invocations.add(tree);
      }
      super.visitMethodInvocation(tree);
    }
  }

  private static class DeclaredOrAssignedLocalsCollector extends BaseTreeVisitor {

    public final Set<String> declaredOrAssignedLocals = new HashSet<>();

    @Override
    public void visitAssignmentExpression(AssignmentExpressionTree tree) {
      super.visitAssignmentExpression(tree);
      var variable = tree.variable();
      if (variable.is(Tree.Kind.IDENTIFIER)) {
        declaredOrAssignedLocals.add(((IdentifierTree) variable).name());
      }
    }

    @Override
    public void visitVariable(VariableTree tree) {
      super.visitVariable(tree);
      declaredOrAssignedLocals.add(tree.simpleName().name());
    }
  }
}
