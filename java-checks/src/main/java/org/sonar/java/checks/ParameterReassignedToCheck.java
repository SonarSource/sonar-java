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
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.model.declaration.VariableTreeImpl;
import org.sonar.java.resolve.SemanticModel;
import org.sonar.java.resolve.Symbol;
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
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.Set;

@Rule(
  key = ParameterReassignedToCheck.RULE_KEY,
  name = "Method parameters, caught exceptions and foreach variables should not be reassigned",
  tags = {"pitfall", "misra"},
  priority = Priority.MAJOR)
@ActivatedByDefault
@SqaleSubCharacteristic(value = RulesDefinition.SubCharacteristics.ARCHITECTURE_RELIABILITY)
@SqaleConstantRemediation(value = "5min")
public class ParameterReassignedToCheck extends BaseTreeVisitor implements JavaFileScanner {

  public static final String RULE_KEY = "S1226";
  private final RuleKey ruleKey = RuleKey.of(CheckList.REPOSITORY_KEY, RULE_KEY);

  private final Set<Symbol.VariableSymbol> variables = Sets.newHashSet();

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
      variables.add(((VariableTreeImpl) parameterTree).getSymbol());
    }
    super.visitMethod(tree);
    for (VariableTree parameterTree : tree.parameters()) {
      variables.remove(((VariableTreeImpl) parameterTree).getSymbol());
    }
  }

  @Override
  public void visitCatch(CatchTree tree) {
    variables.add(((VariableTreeImpl) tree.parameter()).getSymbol());
    super.visitCatch(tree);
    variables.remove(((VariableTreeImpl) tree.parameter()).getSymbol());
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
      Symbol reference = semanticModel.getReference(identifier);
      if (reference != null && reference.isKind(Symbol.VAR) && variables.contains(reference)) {
        context.addIssue(identifier, ruleKey, "Introduce a new variable instead of reusing the parameter \"" + identifier.name() + "\".");
      }
    }
  }

}
