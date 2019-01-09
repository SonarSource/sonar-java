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

import org.sonar.check.Rule;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.ArrayAccessExpressionTree;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.SynchronizedStatementTree;
import org.sonar.plugins.java.api.tree.Tree;

import javax.annotation.CheckForNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Rule(key = "S3010")
public class StaticFieldUpdateInConstructorCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.CONSTRUCTOR);
  }

  @Override
  public void visitNode(Tree tree) {
    if (!hasSemantic()) {
      return;
    }
    MethodTree constructor = (MethodTree) tree;

    Symbol.TypeSymbol owner = constructor.symbol().enclosingClass();
    Set<Symbol> staticFields = owner.memberSymbols().stream()
      .filter(Symbol::isVariableSymbol)
      .filter(Symbol::isStatic)
      .collect(Collectors.toSet());

    StaticFieldUpdateVisitor visitor = new StaticFieldUpdateVisitor(staticFields);
    constructor.block().accept(visitor);
    visitor.assignedStaticFields().forEach(identifierTree -> {
      Symbol staticField = identifierTree.symbol();
      reportIssue(identifierTree,
        "Remove this assignment of \"" + staticField.name() + "\".",
        Collections.singletonList(new JavaFileScannerContext.Location("", staticField.declaration())),
        null);
    });

  }

  private static class StaticFieldUpdateVisitor extends BaseTreeVisitor {

    private final Set<Symbol> staticFields;
    private final List<IdentifierTree> assignedStaticFields;

    StaticFieldUpdateVisitor(Set<Symbol> staticFields) {
      this.staticFields = staticFields;
      this.assignedStaticFields = new ArrayList<>();
    }

    Stream<IdentifierTree> assignedStaticFields() {
      return assignedStaticFields.stream();
    }

    @Override
    public void visitSynchronizedStatement(SynchronizedStatementTree tree) {
      // skip synchronized blocks
    }

    @Override
    public void visitAssignmentExpression(AssignmentExpressionTree tree) {
      checkExpression(tree.variable());
      super.visitAssignmentExpression(tree);
    }

    private void checkExpression(ExpressionTree expressionTree) {
      IdentifierTree variable = getVariable(expressionTree);
      if (variable != null && staticFields.contains(variable.symbol())) {
        assignedStaticFields.add(variable);
      }
    }

    @CheckForNull
    private static IdentifierTree getVariable(ExpressionTree expressionTree) {
      Tree variable = ExpressionUtils.skipParentheses(expressionTree);
      if (variable.is(Tree.Kind.ARRAY_ACCESS_EXPRESSION)) {
        return getVariable(((ArrayAccessExpressionTree) variable).expression());
      }
      if (variable.is(Tree.Kind.MEMBER_SELECT)) {
        return getVariable(((MemberSelectExpressionTree) variable).identifier());
      }
      if (variable.is(Tree.Kind.IDENTIFIER)) {
        return (IdentifierTree) variable;
      }
      return null;
    }
  }

}
