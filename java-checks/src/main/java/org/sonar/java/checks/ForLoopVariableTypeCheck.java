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
import org.sonar.java.resolve.JavaType;
import org.sonar.java.resolve.ParametrizedTypeJavaType;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.ForEachStatement;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S4838")
public class ForLoopVariableTypeCheck extends IssuableSubscriptionVisitor {

  private static final String PRIMARY_MESSAGE = "Change \"%s\" by the type handled by the Collection.";
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

    if (collectionItemType != null && !variableType.equals(collectionItemType)) {
      reportIssue(actualStatement.variable().type(), String.format(PRIMARY_MESSAGE, variableType.name()),
        getFlow(actualStatement, collectionItemType), 0);
    }
  }

  @CheckForNull
  private static Type getCollectionItemType(ExpressionTree expression) {
    if (((JavaType) expression.symbolType()).isParameterized()) {
      ParametrizedTypeJavaType paramType = (ParametrizedTypeJavaType) expression.symbolType();
      return paramType.substitution(paramType.typeParameters().get(0));
    } else {
      return null;
    }
  }

  private static List<JavaFileScannerContext.Location> getFlow(ForEachStatement actualStatement, Type collectionItemType) {
    return Collections.singletonList(new JavaFileScannerContext.Location(String.format(SECONDARY_MESSAGE, collectionItemType.name()), actualStatement.expression()));
  }
}
