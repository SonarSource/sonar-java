/*
 * SonarQube Java
 * Copyright (C) 2012-2021 SonarSource SA
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
package org.sonar.java.checks.unused;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.UnresolvedIdentifiersVisitor;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.PatternInstanceOfTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import static org.sonar.java.model.JUtils.isLocalVariable;

@Rule(key = "S1481")
public class UnusedLocalVariableCheck extends IssuableSubscriptionVisitor {

  private static final Tree.Kind[] INCREMENT_KINDS = {
    Tree.Kind.POSTFIX_DECREMENT,
    Tree.Kind.POSTFIX_INCREMENT,
    Tree.Kind.PREFIX_DECREMENT,
    Tree.Kind.PREFIX_INCREMENT
  };

  private final Map<Symbol, VariableTree> unusedVariables = new HashMap<>();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.COMPILATION_UNIT, Tree.Kind.VARIABLE, Tree.Kind.PATTERN_INSTANCE_OF, Tree.Kind.IDENTIFIER);
  }

  private static final UnresolvedIdentifiersVisitor UNRESOLVED_IDENTIFIERS_VISITOR = new UnresolvedIdentifiersVisitor();

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Tree.Kind.COMPILATION_UNIT)) {
      UNRESOLVED_IDENTIFIERS_VISITOR.check(tree);
      unusedVariables.clear();
    }
  }

  @Override
  public void leaveNode(Tree tree) {
    switch (tree.kind()) {
      case IDENTIFIER:
        IdentifierTree ident = (IdentifierTree) tree;
        if (isRValue(ident)) {
          unusedVariables.remove(ident.symbol());
        }
        break;
      case VARIABLE:
        addVariable((VariableTree) tree);
        break;
      case PATTERN_INSTANCE_OF:
        addVariable(((PatternInstanceOfTree) tree).variable());
        break;
      default: // COMPILATION_UNIT
        for (Map.Entry<Symbol, VariableTree> entry : unusedVariables.entrySet()) {
          VariableTree variable = entry.getValue();
          reportIssue(variable.simpleName(), "Remove this unused \"" + variable.symbol().name() + "\" local variable.");
        }
        break;
    }
  }

  /**
   * An identifier is being used as an r-value if it is not used as the left operand of an assignment nor as the
   * operand of a stand-alone increment
   */
  private static boolean isRValue(IdentifierTree tree) {
    if (tree.parent() instanceof AssignmentExpressionTree) {
      AssignmentExpressionTree assignment = (AssignmentExpressionTree) tree.parent();
      return assignment.variable() != tree;
    }
    return !(tree.parent().is(INCREMENT_KINDS) && tree.parent().parent().is(Tree.Kind.EXPRESSION_STATEMENT));
  }

  private void addVariable(VariableTree variable) {
    if (isProperLocalVariable(variable) && !UNRESOLVED_IDENTIFIERS_VISITOR.isUnresolved(variable.simpleName().name())) {
      unusedVariables.put(variable.symbol(), variable);
    }
  }

  private boolean isProperLocalVariable(VariableTree variable) {
    return isLocalVariable(variable.symbol())
      && !variable.parent().is(Tree.Kind.METHOD, Tree.Kind.CONSTRUCTOR, Tree.Kind.LAMBDA_EXPRESSION, Tree.Kind.CATCH)
      && !isTryResource(variable);
  }

  private static boolean isTryResource(VariableTree variable) {
    return variable.parent().is(Tree.Kind.LIST) && variable.parent().parent().is(Tree.Kind.TRY_STATEMENT);
  }

}
