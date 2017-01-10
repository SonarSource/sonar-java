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

import com.google.common.collect.Sets;
import org.sonar.check.Rule;
import org.sonar.java.resolve.SemanticModel;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
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

@Rule(key = "S1226")
public class ParameterReassignedToCheck extends BaseTreeVisitor implements JavaFileScanner {

  private final Set<Symbol> variables = Sets.newHashSet();

  private JavaFileScannerContext context;
  private SemanticModel semanticModel;

  @Override
  public void scanFile(final JavaFileScannerContext context) {
    this.context = context;
    variables.clear();
    semanticModel = (SemanticModel) context.getSemanticModel();
    scan(context.getTree());
  }

  private boolean hasSemanticModel() {
    return semanticModel != null;
  }

  @Override
  public void visitMethod(MethodTree tree) {
    for (VariableTree parameterTree : tree.parameters()) {
      variables.add(parameterTree.symbol());
    }
    super.visitMethod(tree);
    for (VariableTree parameterTree : tree.parameters()) {
      variables.remove(parameterTree.symbol());
    }
  }

  @Override
  public void visitCatch(CatchTree tree) {
    variables.add(tree.parameter().symbol());
    super.visitCatch(tree);
    variables.remove(tree.parameter().symbol());
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
    if (hasSemanticModel() && tree.is(Tree.Kind.IDENTIFIER)) {
      IdentifierTree identifier = (IdentifierTree) tree;
      Symbol reference = identifier.symbol();
      if (reference.isVariableSymbol() && variables.contains(reference)) {
        context.reportIssue(this, identifier, "Introduce a new variable instead of reusing the parameter \"" + identifier.name() + "\".");
      }
    }
  }

}
