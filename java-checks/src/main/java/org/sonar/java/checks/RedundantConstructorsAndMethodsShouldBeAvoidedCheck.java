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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.StatementTree;
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

    List<VariableTree> components = targetRecord.recordComponents();
    Map<String, VariableTree> componentsByName = components.stream()
      .collect(Collectors.toMap(component -> component.symbol().name(), component -> component));

    for (Tree member : targetRecord.members()) {
      if (member.is(Tree.Kind.CONSTRUCTOR)) {
        MethodTree constructor = (MethodTree) member;
        // Report if the constructor is empty
        if (constructor.block().body().isEmpty() || onlyDoesSimpleAssignments(constructor, components)) {
          reportIssue(member, "BOOM");
        }
      } else if (member.is(Tree.Kind.METHOD)) {
        MethodTree method = (MethodTree) member;
        String methodName = method.symbol().name();
        if (!componentsByName.containsKey(methodName)) {
          continue;
        }
        VariableTree component = componentsByName.get(methodName);
        Type methodType = method.returnType().symbolType();
        Type componentType = component.symbol().type();
        if (methodType.equals(componentType) && onlyReturnsRawValue(method, components)) {
          reportIssue(member, "BOOM");
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
      IdentifierTree identifier;
      if (expression.is(Tree.Kind.IDENTIFIER)) {
        identifier = (IdentifierTree) expression;
      } else if (expression.is(Tree.Kind.MEMBER_SELECT)) {
        identifier = ((MemberSelectExpressionTree) expression).identifier();
      } else {
        continue;
      }
      Symbol identifierSymbol = identifier.symbol();
      boolean identifierMatchesComponent = components.stream().anyMatch(component -> component.symbol().equals(identifierSymbol));
      onlyReturnsComponents |= identifierMatchesComponent;
    }
    return onlyReturnsComponents;
  }

  public static boolean onlyDoesSimpleAssignments(MethodTree constructor, List<VariableTree> components) {
    List<VariableTree> parameters = constructor.parameters();
    List<StatementTree> statements = constructor.block().body();
    Set<Symbol> componentsAssignedInConstructor = new HashSet<>();
    for (StatementTree statement : statements) {
      if (!statement.is(Tree.Kind.EXPRESSION_STATEMENT)) {
        continue;
      }
      ExpressionStatementTree initialStatement = (ExpressionStatementTree) statement;
      if (!initialStatement.expression().is(Tree.Kind.ASSIGNMENT)) {
        continue;
      }
      AssignmentExpressionTree assignment = (AssignmentExpressionTree) initialStatement.expression();
      ExpressionTree leftHandSide = assignment.variable();
      if (!leftHandSide.is(Tree.Kind.MEMBER_SELECT)) {
        continue;
      }
      Symbol variableSymbol = ((MemberSelectExpressionTree) leftHandSide).identifier().symbol();
      boolean variableIsAComponent = components.stream().anyMatch(component -> component.symbol().equals(variableSymbol));
      if (!variableIsAComponent) {
        continue;
      }
      ExpressionTree rightHandSide = assignment.expression();
      if (!rightHandSide.is(Tree.Kind.IDENTIFIER)) {
        continue;
      }
      Symbol valueSymbol = ((IdentifierTree) rightHandSide).symbol();
      boolean valueIsAParameter = parameters.stream().anyMatch(parameter -> parameter.symbol().equals(valueSymbol));
      if (valueIsAParameter && variableSymbol.name().equals(valueSymbol.name()) && variableSymbol.type().equals(valueSymbol.type())) {
        componentsAssignedInConstructor.add(variableSymbol);
      }
    }
    return componentsAssignedInConstructor.size() == components.size();
  }
}
