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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.java.cfg.CFG;
import org.sonar.java.se.CheckerContext;
import org.sonar.java.se.Flow;
import org.sonar.java.se.constraint.Constraint;
import org.sonar.java.se.constraint.ConstraintManager;
import org.sonar.java.se.constraint.ConstraintsByDomain;
import org.sonar.java.se.symbolicvalues.SymbolicValue;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeTree;

@Rule(key = "S3516")
public class InvariantReturnCheck extends SECheck {

  private static class MethodInvariantContext {
    private final MethodTree methodTree;
    private final Set<SymbolicValue> symbolicValues = new HashSet<>();
    private final Multimap<Class<? extends Constraint>, Constraint> methodConstraints = ArrayListMultimap.create();
    private final List<ReturnStatementTree> returnStatementTrees;
    private final boolean methodToCheck;
    private final boolean returnImmutableType;
    private int endPaths = 0;
    private boolean avoidRaisingConstraintIssue;

    MethodInvariantContext(MethodTree methodTree) {
      this.methodTree = methodTree;
      TypeTree returnType = methodTree.returnType();
      this.returnStatementTrees = extractReturnStatements(methodTree);
      methodToCheck = !isConstructorOrVoid(methodTree, returnType) && returnStatementTrees.size() > 1;
      returnImmutableType = methodToCheck && (returnType.symbolType().isPrimitive() || returnType.symbolType().is("java.lang.String"));
    }

    private static boolean isConstructorOrVoid(MethodTree methodTree, @Nullable TypeTree returnType) {
      return methodTree.is(Tree.Kind.CONSTRUCTOR)
        || returnType.symbolType().isVoid()
        || returnType.symbolType().is("java.lang.Void");
    }

    private static List<ReturnStatementTree> extractReturnStatements(MethodTree methodTree) {
      ReturnExtractor visitor = new ReturnExtractor();
      methodTree.accept(visitor);
      return visitor.returns;
    }
  }

  private Deque<MethodInvariantContext> methodInvariantContexts = new LinkedList<>();

  @Override
  public void init(MethodTree methodTree, CFG cfg) {
    methodInvariantContexts.push(new MethodInvariantContext(methodTree));
  }

  @Override
  public void scanFile(JavaFileScannerContext context) {
    for (SEIssue seIssue : issues) {
      context.reportIssueWithFlow(this, seIssue.getTree(), seIssue.getMessage(), seIssue.getFlows(), seIssue.getFlows().iterator().next().size());
    }
    issues.clear();
  }

  @Override
  public void checkEndOfExecutionPath(CheckerContext context, ConstraintManager constraintManager) {
    if (context.getState().exitingOnRuntimeException()) {
      return;
    }
    MethodInvariantContext methodInvariantContext = methodInvariantContexts.peek();
    if (!methodInvariantContext.methodToCheck) {
      return;
    }
    SymbolicValue exitValue = context.getState().exitValue();

    if (exitValue != null) {
      methodInvariantContext.endPaths++;
      methodInvariantContext.symbolicValues.add(exitValue);
      ConstraintsByDomain constraints = context.getState().getConstraints(exitValue);
      if (constraints != null) {
        constraints.forEach(methodInvariantContext.methodConstraints::put);
      } else {
        // Relational SV or NOT SV : we can't say anything.
        methodInvariantContext.avoidRaisingConstraintIssue = true;
      }
    }
  }

  @Override
  public void checkEndOfExecution(CheckerContext context) {
    reportIssues();
  }

  @Override
  public void interruptedExecution(CheckerContext context) {
    // pop but do nothing with it : can't report on incomplete execution
    methodInvariantContexts.pop();
  }

  private void reportIssues() {
    MethodInvariantContext methodInvariantContext = methodInvariantContexts.pop();
    if (!methodInvariantContext.methodToCheck) {
      return;
    }
    if (methodInvariantContext.returnImmutableType && methodInvariantContext.symbolicValues.size() == 1 && methodInvariantContext.endPaths > 1) {
      report(methodInvariantContext);
    } else if (!methodInvariantContext.avoidRaisingConstraintIssue) {
      for (Class<? extends Constraint> constraintClass : methodInvariantContext.methodConstraints.keys()) {
        Collection<Constraint> constraints = methodInvariantContext.methodConstraints.get(constraintClass);
        Constraint firstConstraint = constraints.iterator().next();
        if (constraints.size() == methodInvariantContext.endPaths && firstConstraint.hasPreciseValue() && constraints.stream().allMatch(firstConstraint::equals)) {
          report(methodInvariantContext);
          return;
        }
      }
    }
  }

  private void report(MethodInvariantContext methodInvariantContext) {
    Flow.Builder flowBuilder = Flow.builder();
    methodInvariantContext.returnStatementTrees.stream().map(r -> new JavaFileScannerContext.Location("", r)).forEach(flowBuilder::add);
    reportIssue(
      methodInvariantContext.methodTree.simpleName(),
      "Refactor this method to not always return the same value.",
      Collections.singleton(flowBuilder.build()));
  }

  private static class ReturnExtractor extends BaseTreeVisitor {
    List<ReturnStatementTree> returns = new ArrayList<>();

    @Override
    public void visitReturnStatement(ReturnStatementTree tree) {
      returns.add(tree);
    }

    @Override
    public void visitClass(ClassTree tree) {
      // cut visit of inner class to not count returns
    }

    @Override
    public void visitLambdaExpression(LambdaExpressionTree lambdaExpressionTree) {
      // cut visit of lambdas to not count returns
    }
  }
}
