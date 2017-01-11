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
package org.sonar.java.checks;

import com.google.common.collect.ImmutableList;
import org.sonar.check.Rule;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

@Rule(key = "S2444")
public class StaticFieldInitializationCheck extends AbstractInSynchronizeChecker {

  private Deque<Boolean> withinStaticInitializer = new LinkedList<>();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.ASSIGNMENT, Tree.Kind.METHOD, Tree.Kind.METHOD_INVOCATION, Tree.Kind.SYNCHRONIZED_STATEMENT, Tree.Kind.STATIC_INITIALIZER);
  }

  @Override
  public void scanFile(JavaFileScannerContext context) {
    withinStaticInitializer.push(false);
    super.scanFile(context);
    withinStaticInitializer.clear();
  }

  @Override
  public void visitNode(Tree tree) {
    if (hasSemantic() && tree.is(Tree.Kind.ASSIGNMENT)) {
      AssignmentExpressionTree aet = (AssignmentExpressionTree) tree;
      if (aet.variable().is(Tree.Kind.IDENTIFIER) && !isInSyncBlock() && !withinStaticInitializer.peek()) {
        IdentifierTree variable = (IdentifierTree) aet.variable();
        if (isStaticNotVolatileObject(variable)) {
          reportIssue(variable, "Synchronize this lazy initialization of '" + variable.name() + "'");
        }
      }
    }
    if (tree.is(Tree.Kind.STATIC_INITIALIZER)) {
      withinStaticInitializer.push(true);
    }
    super.visitNode(tree);
  }

  @Override
  public void leaveNode(Tree tree) {
    if (tree.is(Tree.Kind.STATIC_INITIALIZER)) {
      withinStaticInitializer.pop();
    }
    super.leaveNode(tree);
  }

  private static boolean isStaticNotVolatileObject(IdentifierTree variable) {
    Symbol symbol = variable.symbol();
    if (symbol.isUnknown()) {
      return false;
    }
    return isStaticNotFinalNotVolatile(symbol) && !symbol.type().isPrimitive();
  }

  private static boolean isStaticNotFinalNotVolatile(Symbol symbol) {
    return symbol.isStatic() && !symbol.isVolatile() && !symbol.isFinal();
  }

  @Override
  protected List<MethodMatcher> getMethodInvocationMatchers() {
    return ImmutableList.of();
  }

}
