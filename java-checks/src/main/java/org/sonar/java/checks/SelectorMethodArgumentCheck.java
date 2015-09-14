/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
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
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.model.ModifiersUtils;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.ConditionalExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.IfStatementTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.List;

@Rule(
  key = "S2301",
  name = "Public methods should not contain selector arguments",
  tags = {"design"},
  priority = Priority.MAJOR)
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.MODULARITY)
@SqaleConstantRemediation("15min")
public class SelectorMethodArgumentCheck extends SubscriptionBaseVisitor {

  @Override
  public List<Kind> nodesToVisit() {
    return ImmutableList.of(Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    if (!hasSemantic()) {
      return;
    }
    MethodTree methodTree = (MethodTree) tree;
    List<Symbol> booleanParameterSymbols = getBooleanParametersAsSymbol(methodTree.parameters());
    BlockTree blockTree = methodTree.block();

    if (isPublic(methodTree) && blockTree != null && !booleanParameterSymbols.isEmpty()) {
      for (Symbol variable : booleanParameterSymbols) {
        Collection<IdentifierTree> usages = variable.usages();
        if (usages.size() == 1) {
          blockTree.accept(new ConditionalStatementVisitor(variable.name(), Iterables.get(usages, 0), tree));
        }
      }
    }
  }

  private static boolean isPublic(MethodTree methodTree) {
    return ModifiersUtils.hasModifier(methodTree.modifiers(), Modifier.PUBLIC);
  }

  private static List<Symbol> getBooleanParametersAsSymbol(List<VariableTree> parameters) {
    List<Symbol> booleanParameters = Lists.newLinkedList();
    for (VariableTree variableTree : parameters) {
      if (isBooleanVariable(variableTree)) {
        booleanParameters.add(variableTree.symbol());
      }
    }
    return booleanParameters;
  }

  private static boolean isBooleanVariable(VariableTree variableTree) {
    return variableTree.type().symbolType().isPrimitive(Type.Primitives.BOOLEAN);
  }

  private class ConditionalStatementVisitor extends BaseTreeVisitor {

    private final String variableName;
    private final Tree method;
    private IdentifierTree usage;

    public ConditionalStatementVisitor(String variableName, IdentifierTree usage, Tree method) {
      this.variableName = variableName;
      this.usage = usage;
      this.method = method;
    }

    @Override
    public void visitIfStatement(IfStatementTree tree) {
      checkParameterUsage(tree.condition());
    }

    @Override
    public void visitConditionalExpression(ConditionalExpressionTree tree) {
      checkParameterUsage(tree.condition());
    }

    private void checkParameterUsage(ExpressionTree condition) {
      if (usage.equals(condition)) {
        addIssue(method, MessageFormat.format("Provide multiple methods instead of using \"{0}\" to determine which action to take.", variableName));
      }
    }
  }
}
