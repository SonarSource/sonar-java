/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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

import java.util.Collections;
import java.util.List;
import javax.annotation.CheckForNull;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.ForEachStatement;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeCastTree;

@Rule(key = "S4838")
public class ForLoopVariableTypeCheck extends IssuableSubscriptionVisitor {

  private static final String PRIMARY_MESSAGE = "Change \"%s\" to the type handled by the Collection.";
  private static final String SECONDARY_MESSAGE = "Collection item type is \"%s\"";

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.FOR_EACH_STATEMENT);
  }

  @Override
  public void visitNode(Tree tree) {
    ForEachStatement actualStatement = (ForEachStatement) tree;
    Type variableType = actualStatement.variable().type().symbolType();
    Type collectionItemType = getCollectionItemType(actualStatement.expression());

    if (collectionItemType == null || collectionItemType.isUnknown() || variableType.isUnknown()) {
      return;
    }

    if (!isMostPreciseType(variableType, collectionItemType)) {
      // Second pass: check if the variable is down-cast in the statement block
      DownCastVisitor downCastVisitor = new DownCastVisitor(actualStatement.variable().symbol());
      actualStatement.statement().accept(downCastVisitor);

      if (downCastVisitor.hasDownCastOfLoopVariable) {
        List<JavaFileScannerContext.Location> locations = Collections.singletonList(
          new JavaFileScannerContext.Location(String.format(SECONDARY_MESSAGE, collectionItemType.name()), actualStatement.expression()));
        reportIssue(actualStatement.variable().type(), String.format(PRIMARY_MESSAGE, variableType.name()),
          locations, 0);
      }
    }
  }

  @CheckForNull
  private static Type getCollectionItemType(ExpressionTree expression) {
    Type expressionType = expression.symbolType();
    if (expressionType.isSubtypeOf("java.util.Collection") && !expressionType.isParameterized()) {
      // Ignoring raw collections (too many FP)
      return null;
    }
    if (expressionType.isArray()) {
      return ((Type.ArrayType) expressionType).elementType();
    }
    if(expressionType.isClass()) {
      return expressionType.symbol().superTypes().stream()
        .filter(t -> t.is("java.lang.Iterable") && t.isParameterized())
        .findFirst()
        .map(iter -> iter.typeArguments().get(0))
        .orElse(null);
    }
    return null;
  }

  private static boolean isMostPreciseType(Type variableType, Type collectionItemType) {
    return variableType.erasure().equals(collectionItemType.erasure());
  }

  private static class DownCastVisitor extends BaseTreeVisitor {

    private final Symbol symbol;
    private boolean hasDownCastOfLoopVariable;

    private DownCastVisitor(Symbol symbol) {
      this.symbol = symbol;
      this.hasDownCastOfLoopVariable = false;
    }

    @Override
    public void visitTypeCast(TypeCastTree tree) {
      if (hasDownCastOfLoopVariable) {
        return;
      }
      ExpressionTree expression = tree.expression();
      if (expression.is(Tree.Kind.IDENTIFIER) && ((IdentifierTree) expression).symbol().equals(symbol)) {
        hasDownCastOfLoopVariable = true;
      } else {
        super.visitTypeCast(tree);
      }
    }
  }
}
