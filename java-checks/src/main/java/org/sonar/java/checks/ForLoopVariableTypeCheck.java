/*
 * SonarQube Java
 * Copyright (C) 2012-2018 SonarSource SA
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
import org.sonar.java.ast.visitors.SubscriptionVisitor;
import org.sonar.java.resolve.JavaType;
import org.sonar.java.resolve.ParametrizedTypeJavaType;
import org.sonar.java.resolve.WildCardType;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.ForEachStatement;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeCastTree;
import org.sonar.plugins.java.api.tree.VariableTree;

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
    if (!hasSemantic()) {
      return;
    }

    ForEachStatement actualStatement = (ForEachStatement) tree;
    Type variableType = actualStatement.variable().type().symbolType();
    Type collectionItemType = getCollectionItemType(actualStatement.expression());

    if (collectionItemType != null && !isMostPreciseType(variableType, collectionItemType)) {
      // Second pass: check if the variable is down-cast in the statement block
      DownCastVisitor downCastVisitor = new DownCastVisitor(actualStatement, actualStatement.variable(), collectionItemType);
      downCastVisitor.setContext(context);
      downCastVisitor.scanTree(((ForEachStatement) tree).statement());
    }
  }

  @CheckForNull
  private static JavaType getCollectionItemType(ExpressionTree expression) {
    if (((JavaType) expression.symbolType()).isParameterized()) {
      ParametrizedTypeJavaType paramType = (ParametrizedTypeJavaType) expression.symbolType();
      return paramType.substitution(paramType.typeParameters().get(0));
    } else {
      return null;
    }
  }

  private static boolean isMostPreciseType(Type variableType, Type collectionItemType) {
    if (collectionItemType instanceof WildCardType) {
      return ((WildCardType) collectionItemType).isSubtypeOfBound((JavaType) variableType);
    } else if (collectionItemType instanceof ParametrizedTypeJavaType) {
      return ((ParametrizedTypeJavaType) collectionItemType).erasure().equals(variableType.erasure());
    } else {
      return variableType.equals(collectionItemType);
    }
  }

  private class DownCastVisitor extends SubscriptionVisitor {

    private final ForEachStatement forEachStatement;
    private final VariableTree variable;
    private final Type collectionItemType;
    private boolean blockAlreadyFlagged;

    private DownCastVisitor(ForEachStatement forEachStatement, VariableTree variable, Type collectionItemType) {
      this.forEachStatement = forEachStatement;
      this.variable = variable;
      this.collectionItemType = collectionItemType;
      this.blockAlreadyFlagged = false;
    }

    @Override
    public List<Tree.Kind> nodesToVisit() {
      return Collections.singletonList(Tree.Kind.TYPE_CAST);
    }

    @Override
    public void visitNode(Tree tree) {
      if (blockAlreadyFlagged) {
        return;
      }
      ExpressionTree expression = ((TypeCastTree) tree).expression();
      if (expression.is(Tree.Kind.IDENTIFIER) && ((IdentifierTree) expression).symbol().equals(variable.symbol())) {
        ForLoopVariableTypeCheck.this.reportIssue(forEachStatement.variable().type(), String.format(PRIMARY_MESSAGE, variable.type().symbolType().name()),
          getSecondaryLocations(), 0);
        blockAlreadyFlagged = true;
      }
    }

    @Override
    public void scanTree(Tree tree) {
      super.scanTree(tree);
    }

    private List<JavaFileScannerContext.Location> getSecondaryLocations() {
      return Collections.singletonList(new JavaFileScannerContext.Location(String.format(SECONDARY_MESSAGE, collectionItemType.name()), forEachStatement.expression()));
    }
  }
}
