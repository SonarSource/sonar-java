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
import org.sonar.java.model.AbstractTypedTree;
import org.sonar.java.resolve.Symbol;
import org.sonar.java.resolve.Symbol.TypeSymbol;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.SynchronizedStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.text.MessageFormat;
import java.util.List;

@Rule(
  key = "S2445",
  name = "Blocks synchronized on fields should not contain assignments new objects to those fields",
  tags = {"multi-threading", "bug"},
  priority = Priority.BLOCKER)
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.SYNCHRONIZATION_RELIABILITY)
@SqaleConstantRemediation("15min")
public class SynchronizedFieldAssignmentCheck extends SubscriptionBaseVisitor {

  @Override
  public List<Kind> nodesToVisit() {
    return ImmutableList.of(Kind.SYNCHRONIZED_STATEMENT);
  }

  @Override
  public void visitNode(Tree tree) {
    if (!hasSemantic()) {
      return;
    }
    SynchronizedStatementTree sst = (SynchronizedStatementTree) tree;
    ExpressionTree synchronizedExpression = sst.expression();
    if (isField(synchronizedExpression)) {
      Symbol field = getSemanticModel().getReference((IdentifierTree) synchronizedExpression);
      sst.block().accept(new AssignmentVisitor(field, tree));
    }
  }

  private boolean isField(ExpressionTree tree) {
    TypeSymbol enclosingClass = (TypeSymbol) getSemanticModel().getEnclosingClass(tree);
    if (tree.is(Kind.IDENTIFIER)) {
      IdentifierTree identifier = (IdentifierTree) tree;
      List<Symbol> lookup = ((TypeSymbol) enclosingClass).members().lookup(identifier.name());
      Symbol reference = getSemanticModel().getReference(identifier);
      return lookup.contains(reference);
    }
    return false;
  }

  private class AssignmentVisitor extends BaseTreeVisitor {

    private final Symbol field;
    private final Tree synchronizedStatement;

    public AssignmentVisitor(Symbol field, Tree tree) {
      this.field = field;
      this.synchronizedStatement = tree;
    }

    @Override
    public void visitAssignmentExpression(AssignmentExpressionTree tree) {
      AbstractTypedTree variable = (AbstractTypedTree) tree.variable();
      if (variable.is(Kind.IDENTIFIER)) {
        Symbol variableSymbol = getSemanticModel().getReference((IdentifierTree) variable);
        if (field.equals(variableSymbol)) {
          addIssue(synchronizedStatement,
            MessageFormat.format("Don''t synchronize on \"{0}\" or remove its reassignment on line {1}.", field.getName(), String.valueOf(variable.getLine())));
        }
      }
    }
  }
}
