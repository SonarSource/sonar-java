/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.java.checks;

import com.google.common.collect.ImmutableList;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.MethodInvocationMatcher;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewArrayTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import javax.annotation.Nullable;

import java.util.List;

@Rule(
  key = "S2076",
  name = "Values passed to OS commands should be sanitized",
  tags = {"cwe", "owasp-top10", "sans-top25", "security"},
  priority = Priority.CRITICAL)
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.INPUT_VALIDATION_AND_REPRESENTATION)
@SqaleConstantRemediation("30min")
public class OSCommandInjectionCheck extends AbstractInjectionChecker {

  private static final MethodInvocationMatcher RUNTIME_EXEC_MATCHER = MethodInvocationMatcher.create()
    .typeDefinition("java.lang.Runtime")
    .name("exec").withNoParameterConstraint();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.METHOD_INVOCATION, Tree.Kind.NEW_CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    if (hasSemantic()) {
      if (tree.is(Tree.Kind.METHOD_INVOCATION) && RUNTIME_EXEC_MATCHER.matches((MethodInvocationTree) tree)) {
        MethodInvocationTree mit = (MethodInvocationTree) tree;
        checkForIssue(tree, mit.arguments().get(0));
      } else if (tree.is(Tree.Kind.NEW_CLASS) && ((NewClassTree) tree).symbolType().is("java.lang.ProcessBuilder")) {
        for (ExpressionTree expressionTree : ((NewClassTree) tree).arguments()) {
          checkForIssue(tree, expressionTree);
        }
      }
    }
  }

  private void checkForIssue(Tree tree, ExpressionTree arg) {
    if (isDynamicArray(arg, tree)) {
      addIssue(arg, "Make sure \"" + parameterName + "\" is properly sanitized before use in this OS command.");
    }
  }

  private boolean isDynamicArray(@Nullable ExpressionTree arg, Tree mit) {
    if (arg == null) {
      return false;
    }
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

}
