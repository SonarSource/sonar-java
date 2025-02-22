/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.checks;

import java.util.Collections;
import java.util.List;
import javax.annotation.CheckForNull;
import org.sonar.check.Rule;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.SynchronizedStatementTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S2445")
public class SynchronizedFieldAssignmentCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.SYNCHRONIZED_STATEMENT);
  }

  @Override
  public void visitNode(Tree tree) {
    SynchronizedStatementTree sst = (SynchronizedStatementTree) tree;
    if(sst.expression().is(Tree.Kind.NEW_CLASS)) {
      reportIssue(tree, "Synchronizing on a new instance is a no-op.");
      return;
    }
    Symbol field = getField(sst.expression());
    if (field != null) {
      sst.block().accept(new AssignmentVisitor(field, sst.expression()));
    } else {
      Symbol parameter = getParam(sst.expression());
      if(parameter != null) {
        reportIssue(tree, String.format("\"%s\" is a method parameter, and should not be used for synchronization.", parameter.name()));
      }
    }
  }

  @CheckForNull
  private static Symbol getParam(ExpressionTree tree) {
    if (tree.is(Tree.Kind.IDENTIFIER)) {
      Symbol reference = ((IdentifierTree) tree).symbol();
      if (reference.isParameter()) {
        return reference;
      }
    }
    return null;
  }

  @CheckForNull
  private static Symbol getField(ExpressionTree tree) {
    if (tree.is(Tree.Kind.IDENTIFIER)) {
      Symbol reference = ((IdentifierTree) tree).symbol();
      if (!reference.isUnknown() && reference.owner().isTypeSymbol()) {
        return reference;
      }
    } else if (tree.is(Tree.Kind.MEMBER_SELECT)) {
      MemberSelectExpressionTree mse = (MemberSelectExpressionTree) tree;
      if (isField(mse)) {
        return getField(mse.identifier());
      }
    }
    return null;
  }

  private static boolean isField(ExpressionTree tree) {
    if (tree.is(Tree.Kind.IDENTIFIER)) {
      Symbol reference = ((IdentifierTree) tree).symbol();
      return !reference.isUnknown() && reference.owner().isTypeSymbol();
    }
    if (tree.is(Tree.Kind.MEMBER_SELECT)) {
      MemberSelectExpressionTree mse = (MemberSelectExpressionTree) tree;
      ExpressionTree mseExpression = mse.expression();
      if (ExpressionUtils.isThis(mseExpression)) {
        return isField(mse.identifier());
      } else {
        return isField(mseExpression);
      }
    }
    return false;
  }

  private class AssignmentVisitor extends BaseTreeVisitor {

    private final Symbol field;
    private final Tree synchronizedStatement;

    AssignmentVisitor(Symbol field, Tree tree) {
      this.field = field;
      this.synchronizedStatement = tree;
    }

    @Override
    public void visitAssignmentExpression(AssignmentExpressionTree tree) {
      checkSymbolAssignment(tree.variable());
    }

    private void checkSymbolAssignment(Tree variable) {
      if (variable.is(Tree.Kind.IDENTIFIER)) {
        if (field.equals(((IdentifierTree) variable).symbol())) {
          reportIssue(
            synchronizedStatement,
            String.format("\"%s\" is not \"private final\", and should not be used for synchronization. ", field.name()));
        }
      } else if (variable.is(Tree.Kind.MEMBER_SELECT)) {
        checkSymbolAssignment(((MemberSelectExpressionTree) variable).identifier());
      }
    }

  }
}
