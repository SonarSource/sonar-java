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

import org.sonar.check.Rule;
import org.sonar.java.cfg.CFG;
import org.sonar.java.se.CheckerContext;
import org.sonar.java.se.constraint.ConstraintManager;
import org.sonar.java.se.symbolicvalues.SymbolicValue;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.TypeTree;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Rule(key = "S3516")
public class InvariantReturnCheck extends SECheck {

  private final Set<SymbolicValue> symbolicValues = new HashSet<>();
  private int endPaths = 0;
  private boolean methodToCheck = false;
  private boolean returnImmutableType = false;

  @Override
  public void init(MethodTree methodTree, CFG cfg) {
    symbolicValues.clear();
    endPaths = 0;
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

  @Override
  public void checkEndOfExecutionPath(CheckerContext context, ConstraintManager constraintManager) {
    if (!methodToCheck) {
      return;
    }
    SymbolicValue exitValue = context.getState().exitValue();
    if (exitValue != null) {
      endPaths++;
      symbolicValues.add(exitValue);
    }
  }

  @Override
  public void checkEndOfExecution(CheckerContext context) {
    if (!methodToCheck) {
      return;
    }
    if (returnImmutableType && symbolicValues.size() == 1 && endPaths > 1) {
      context.getNode().edges().stream()
        .findFirst()
        .map(e -> e.parent().programPoint.syntaxTree())
        .ifPresent(t -> reportIssue(t, "Refactor this method to not always return the same value.\n", Collections.emptySet()));
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
