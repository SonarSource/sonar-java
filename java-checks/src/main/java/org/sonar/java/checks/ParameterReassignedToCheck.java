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

import com.google.common.collect.Sets;
import org.sonar.api.rule.RuleKey;
import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.CatchTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.UnaryExpressionTree;
import org.sonar.plugins.java.api.tree.VariableTree;

import java.util.Set;

@Rule(
  key = ParameterReassignedToCheck.RULE_KEY,
  priority = Priority.MAJOR,
  tags={"pitfall"})
@BelongsToProfile(title = "Sonar way", priority = Priority.MAJOR)
public class ParameterReassignedToCheck extends BaseTreeVisitor implements JavaFileScanner {

  public static final String RULE_KEY = "S1226";
  private final RuleKey ruleKey = RuleKey.of(CheckList.REPOSITORY_KEY, RULE_KEY);

  private final Set<String> variables = Sets.newHashSet();

  private JavaFileScannerContext context;

  @Override
  public void scanFile(final JavaFileScannerContext context) {
    this.context = context;
    variables.clear();
    scan(context.getTree());
  }

  @Override
  public void visitMethod(MethodTree tree) {
    for (VariableTree parameterTree : tree.parameters()) {
      variables.add(parameterTree.simpleName().name());
    }
    super.visitMethod(tree);
    for (VariableTree parameterTree : tree.parameters()) {
      variables.remove(parameterTree.simpleName().name());
    }
  }

  @Override
  public void visitCatch(CatchTree tree) {
    variables.add(tree.parameter().simpleName().name());
    super.visitCatch(tree);
    variables.remove(tree.parameter().simpleName().name());
  }

  @Override
  public void visitAssignmentExpression(AssignmentExpressionTree tree) {
    checkExpression(tree.variable());
  }

  @Override
  public void visitUnaryExpression(UnaryExpressionTree tree) {
    if (isIncrementOrDecrement(tree) && tree.expression().is(Tree.Kind.IDENTIFIER)) {
      checkExpression(tree.expression());
    }
  }

  private static boolean isIncrementOrDecrement(Tree tree) {
    return tree.is(Tree.Kind.PREFIX_INCREMENT) ||
      tree.is(Tree.Kind.PREFIX_DECREMENT) ||
      tree.is(Tree.Kind.POSTFIX_INCREMENT) ||
      tree.is(Tree.Kind.POSTFIX_DECREMENT);
  }

  private void checkExpression(ExpressionTree tree) {
    if (tree.is(Tree.Kind.IDENTIFIER)) {
      IdentifierTree identifier = (IdentifierTree) tree;
      if (variables.contains(identifier.name())) {
        context.addIssue(identifier, ruleKey, "Introduce a new variable instead of reusing the parameter \"" + identifier.name() + "\".");
      }
    }
  }

}
