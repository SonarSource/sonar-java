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
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewArrayTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import java.util.List;

@Rule(key = "S2076")
public class OSCommandInjectionCheck extends AbstractInjectionChecker {

  private static final MethodMatcher RUNTIME_EXEC_MATCHER = MethodMatcher.create()
      .typeDefinition("java.lang.Runtime")
      .name("exec")
      .withAnyParameters();

  private static final MethodMatcher PROCESS_BUILDER_COMMAND_MATCHER = MethodMatcher.create()
      .typeDefinition("java.lang.ProcessBuilder")
      .name("command")
      .withAnyParameters();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.METHOD_INVOCATION, Tree.Kind.NEW_CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    if (hasSemantic()) {
      if (tree.is(Tree.Kind.METHOD_INVOCATION)) {
        MethodInvocationTree mit = (MethodInvocationTree) tree;
        Arguments arguments = mit.arguments();
        if (RUNTIME_EXEC_MATCHER.matches(mit)) {
          checkForIssue(tree, arguments.get(0));
        } else if (PROCESS_BUILDER_COMMAND_MATCHER.matches(mit) && !arguments.isEmpty()) {
          checkForIssue(tree, arguments);
        }
      } else if (((NewClassTree) tree).symbolType().is("java.lang.ProcessBuilder")) {
        checkForIssue(tree, ((NewClassTree) tree).arguments());
      }
    }
  }

  private void checkForIssue(Tree tree, Arguments arguments) {
    for (ExpressionTree arg : arguments) {
      checkForIssue(tree, arg);
    }
  }

  private void checkForIssue(Tree tree, ExpressionTree arg) {
    if (isDynamicArray(arg, tree) && !arg.symbolType().isSubtypeOf("java.util.List")) {
      reportIssue(arg, "Make sure \"" + parameterName + "\" is properly sanitized before use in this OS command.");
    }
  }

  private boolean isDynamicArray(ExpressionTree argument, Tree mit) {
    ExpressionTree arg = getDeclaration(argument);
    if (arg.is(Tree.Kind.NEW_ARRAY)) {
      NewArrayTree nat = (NewArrayTree) arg;
      for (ExpressionTree expressionTree : nat.initializers()) {
        if (isDynamicString(mit, expressionTree, null)) {
          return true;
        }
      }
      return false;
    }

    setParameterNameFromArgument(arg);
    boolean argIsString = arg.symbolType().is("java.lang.String");
    return !argIsString || isDynamicString(mit, arg, null);
  }

  private static ExpressionTree getDeclaration(ExpressionTree arg) {
    if (arg.symbolType().is("java.lang.String[]") && arg.is(Tree.Kind.IDENTIFIER)) {
      Tree declaration = ((IdentifierTree) arg).symbol().declaration();
      if (declaration != null) {
        ExpressionTree result = ((VariableTree) declaration).initializer();
        if (result != null) {
          return result;
        }
      }
    }
    return arg;
  }
}
