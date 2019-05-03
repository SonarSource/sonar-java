/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
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

import com.google.common.collect.Iterables;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.model.ModifiersUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
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

@Rule(key = "S2301")
public class SelectorMethodArgumentCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Kind> nodesToVisit() {
    return Collections.singletonList(Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    if (!hasSemantic()) {
      return;
    }
    MethodTree methodTree = (MethodTree) tree;
    if (Boolean.TRUE.equals(methodTree.isOverriding())) {
      return;
    }
    List<Symbol> booleanParameterSymbols = getBooleanParametersAsSymbol(methodTree.parameters());
    BlockTree blockTree = methodTree.block();

    if (isPublic(methodTree) && blockTree != null && !booleanParameterSymbols.isEmpty()) {
      for (Symbol variable : booleanParameterSymbols) {
        Collection<IdentifierTree> usages = variable.usages();
        if (usages.size() == 1) {
          blockTree.accept(new ConditionalStatementVisitor(variable.name(), Iterables.get(usages, 0), methodTree));
        }
      }
    }
  }

  private static boolean isPublic(MethodTree methodTree) {
    return ModifiersUtils.hasModifier(methodTree.modifiers(), Modifier.PUBLIC);
  }

  private static List<Symbol> getBooleanParametersAsSymbol(List<VariableTree> parameters) {
    List<Symbol> booleanParameters = new LinkedList<>();
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
    private final MethodTree method;
    private IdentifierTree usage;

    public ConditionalStatementVisitor(String variableName, IdentifierTree usage, MethodTree method) {
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
        reportIssue(method.simpleName(), MessageFormat.format("Provide multiple methods instead of using \"{0}\" to determine which action to take.", variableName));
      }
    }
  }
}
