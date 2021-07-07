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
package org.sonar.java.checks;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S6207")
public class RedundantConstructorsAndMethodsShouldBeAvoidedCheck extends IssuableSubscriptionVisitor {
  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.RECORD);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree targetRecord = (ClassTree) tree;

    Map<String, VariableTree> componentsByName = new HashMap<>();
    for (VariableTree component : targetRecord.recordComponents()) {
      componentsByName.put(component.symbol().name(), component);
    }

    for (Tree member : targetRecord.members()) {
      if (member.is(Tree.Kind.CONSTRUCTOR)) {
        MethodTree constructor = (MethodTree) member;
        // Report if the constructor is empty
        if (constructor.block().body().isEmpty()) {
          reportIssue(member, "BOOM");
        }
      } else if (member.is(Tree.Kind.METHOD)) {
        MethodTree method = (MethodTree) member;
        String methodName = method.symbol().name();
        if (componentsByName.containsKey(methodName)) {
          VariableTree component = componentsByName.get(methodName);
          Type methodType = method.returnType().symbolType();
          Type componentType = component.symbol().type();
          if (methodType.equals(componentType) && onlyReturnsRawValue(method, componentsByName.values())) {
            reportIssue(member, "BOOM");
          }
        }
      }
    }
  }

  public static boolean onlyReturnsRawValue(MethodTree method, Collection<VariableTree> components) {
    List<ReturnStatementTree> returnStatements = method.block().body().stream()
      .filter(statement -> statement.is(Tree.Kind.RETURN_STATEMENT))
      .map(ReturnStatementTree.class::cast)
      .collect(Collectors.toList());
    boolean onlyReturnsComponents = false;
    for (ReturnStatementTree returnStatement : returnStatements) {
      ExpressionTree expression = returnStatement.expression();
      if (expression.is(Tree.Kind.IDENTIFIER)) {
        IdentifierTree identifier = (IdentifierTree) expression;
        Symbol symbolIdentifier = identifier.symbol();
        boolean identifierMatchesComponent = false;
        for (VariableTree component : components) {
          if (symbolIdentifier.equals(component.symbol())) {
            identifierMatchesComponent = true;
            break;
          }
        }
        onlyReturnsComponents |= identifierMatchesComponent;
      }
    }
    return onlyReturnsComponents;
  }
}
