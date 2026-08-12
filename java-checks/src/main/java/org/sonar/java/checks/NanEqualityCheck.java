/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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

import java.util.List;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S9147")
public class NanEqualityCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.EQUAL_TO, Tree.Kind.NOT_EQUAL_TO);
  }

  @Override
  public void visitNode(Tree tree) {
    BinaryExpressionTree binaryExpression = (BinaryExpressionTree) tree;
    String typeName = getNanTypeName(binaryExpression.leftOperand());
    if (typeName == null) {
      typeName = getNanTypeName(binaryExpression.rightOperand());
    }
    if (typeName != null) {
      reportIssue(binaryExpression.operatorToken(),
        String.format("Use \"%s.isNaN()\" instead of comparison with \"%s.NaN\".", typeName, typeName));
    }
  }

  @Nullable
  private static String getNanTypeName(ExpressionTree expression) {
    ExpressionTree expr = ExpressionUtils.skipParentheses(expression);
    if (expr.is(Tree.Kind.MEMBER_SELECT)) {
      MemberSelectExpressionTree memberSelect = (MemberSelectExpressionTree) expr;
      if ("NaN".equals(memberSelect.identifier().name())) {
        return resolveNanOwnerType(memberSelect.identifier().symbol());
      }
    } else if (expr.is(Tree.Kind.IDENTIFIER)) {
      IdentifierTree identifier = (IdentifierTree) expr;
      if ("NaN".equals(identifier.name())) {
        return resolveNanOwnerType(identifier.symbol());
      }
    }
    return null;
  }

  @Nullable
  private static String resolveNanOwnerType(Symbol symbol) {
    if (symbol.isUnknown()) {
      return null;
    }
    Symbol owner = symbol.owner();
    if (owner == null || owner.type() == null) {
      return null;
    }
    Type ownerType = owner.type();
    if (ownerType.is("java.lang.Double") || ownerType.is("java.lang.Float")) {
      return ownerType.name();
    }
    return null;
  }

}
