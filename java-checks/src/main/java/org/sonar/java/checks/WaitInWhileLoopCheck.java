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
package org.sonar.java.checks;

import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.ForStatementTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S2274")
public class WaitInWhileLoopCheck extends AbstractMethodDetection {

  private Deque<Boolean> inWhileLoop = new LinkedList<>();

  @Override
  public void setContext(JavaFileScannerContext context) {
    inWhileLoop.push(false);
    super.setContext(context);
  }

  @Override
  public void leaveFile(JavaFileScannerContext context) {
    inWhileLoop.clear();
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.METHOD_INVOCATION, Tree.Kind.WHILE_STATEMENT, Tree.Kind.DO_STATEMENT, Tree.Kind.FOR_STATEMENT);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Tree.Kind.METHOD_INVOCATION)) {
      super.visitNode(tree);
    } else if (tree.is(Tree.Kind.FOR_STATEMENT)) {
      ForStatementTree fst = (ForStatementTree) tree;
      inWhileLoop.push(fst.initializer().isEmpty() && fst.condition() == null && fst.update().isEmpty());
    } else {
      inWhileLoop.push(true);
    }
  }

  @Override
  public void leaveNode(Tree tree) {
    if (tree.is(Tree.Kind.WHILE_STATEMENT, Tree.Kind.DO_STATEMENT, Tree.Kind.FOR_STATEMENT)) {
      inWhileLoop.pop();
    }
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    if (!Boolean.TRUE.equals(inWhileLoop.peek())) {
      IdentifierTree identifierTree = ExpressionUtils.methodName(mit);
      reportIssue(identifierTree, "Remove this call to \"" + identifierTree.name() + "\" or move it into a \"while\" loop.");
    }
  }

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return MethodMatchers.or(
      MethodMatchers.create().ofAnyType().names("wait")
        .addWithoutParametersMatcher()
        .addParametersMatcher("long")
        .addParametersMatcher("long", "int")
        .build(),
      MethodMatchers.create().ofTypes("java.util.concurrent.locks.Condition").name(name -> name.startsWith("await")).withAnyParameters().build()
    );
  }
}
