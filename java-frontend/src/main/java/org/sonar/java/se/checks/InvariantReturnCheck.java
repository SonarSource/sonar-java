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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.sonar.check.Rule;
import org.sonar.java.cfg.CFG;
import org.sonar.java.collections.PMap;
import org.sonar.java.se.CheckerContext;
import org.sonar.java.se.constraint.Constraint;
import org.sonar.java.se.constraint.ConstraintManager;
import org.sonar.java.se.symbolicvalues.SymbolicValue;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.TypeTree;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

@Rule(key = "S3516")
public class InvariantReturnCheck extends SECheck {

  private static class MethodInvariantContext {
    private final Set<SymbolicValue> symbolicValues = new HashSet<>();
    private final Multimap<Class<? extends Constraint>, Constraint> methodConstraints = ArrayListMultimap.create();
    private int endPaths = 0;
    private boolean methodToCheck = false;
    private boolean returnImmutableType = false;
    private boolean avoidRaisingConstraintIssue = false;

    public MethodInvariantContext(MethodTree methodTree) {
      TypeTree returnType = methodTree.returnType();
      methodToCheck = !isConstructorOrVoid(returnType) && hasAtLeastTwoReturn(methodTree);
      returnImmutableType = methodToCheck && (returnType.symbolType().isPrimitive() || returnType.symbolType().is("java.lang.String"));
    }

    private static boolean isConstructorOrVoid(@Nullable TypeTree returnType) {
      return returnType == null || returnType.symbolType().isVoid();
    }

    private static boolean hasAtLeastTwoReturn(MethodTree methodTree) {
      ReturnCounter visitor = new ReturnCounter();
      methodTree.accept(visitor);
      return visitor.returnCount > 1;
    }
  }

  private Deque<MethodInvariantContext> methodInvariantContexts = new LinkedList<>();

  @Override
  public void init(MethodTree methodTree, CFG cfg) {
    methodInvariantContexts.push(new MethodInvariantContext(methodTree));
  }

  @Override
  public void checkEndOfExecutionPath(CheckerContext context, ConstraintManager constraintManager) {
    MethodInvariantContext methodInvariantContext = methodInvariantContexts.peek();
    if (!methodInvariantContext.methodToCheck) {
      return;
    }
    SymbolicValue exitValue = context.getState().exitValue();

    if (exitValue != null) {
      methodInvariantContext.endPaths++;
      methodInvariantContext.symbolicValues.add(exitValue);
      PMap<Class<? extends Constraint>, Constraint> constraints = context.getState().getConstraints(exitValue);
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
    reportIssues(context);
  }

  @Override
  public void interruptedExecution(CheckerContext context) {
    reportIssues(context);
  }

  private void reportIssues(CheckerContext context) {
    MethodInvariantContext methodInvariantContext = methodInvariantContexts.pop();
    if (!methodInvariantContext.methodToCheck) {
      return;
    }
    if (methodInvariantContext.returnImmutableType && methodInvariantContext.symbolicValues.size() == 1 && methodInvariantContext.endPaths > 1) {
      context.getNode().edges().stream()
        .findFirst()
        .map(e -> e.parent().programPoint.syntaxTree())
        .ifPresent(t -> reportIssue(t, "Refactor this method to not always return the same value.\n", Collections.emptySet()));
    }
    if(!methodInvariantContext.avoidRaisingConstraintIssue) {
      for (Class<? extends Constraint> aClass : methodInvariantContext.methodConstraints.keys()) {
        Collection<Constraint> constraints = methodInvariantContext.methodConstraints.get(aClass);
        if(constraints.size() == methodInvariantContext.endPaths
          && constraints.stream().allMatch(c -> constraints.iterator().next().hasPreciseValue() && constraints.iterator().next().equals(c))) {
          context.getNode().edges().stream()
            .findFirst()
            .map(e -> e.parent().programPoint.syntaxTree())
            .ifPresent(t -> reportIssue(t, "Refactor this method to not always return the same value.\n", Collections.emptySet()));
          return;
        }
      }
    }
  }

  private static class ReturnCounter extends BaseTreeVisitor {
    int returnCount = 0;

    @Override
    public void visitReturnStatement(ReturnStatementTree tree) {
      returnCount++;
    }

    @Override
    public void visitClass(ClassTree tree) {
      // cut visit of inner class to not count returns
    }
  }
}
