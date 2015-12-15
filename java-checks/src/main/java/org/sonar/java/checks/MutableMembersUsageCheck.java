/*
 * SonarQube Java
 * Copyright (C) 2012-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import com.google.common.collect.ImmutableList;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.tag.Tag;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

@Rule(
  key = "S2384",
  name = "Mutable members should not be stored or returned directly",
  priority = Priority.CRITICAL,
  tags = {Tag.CERT, Tag.CWE, Tag.SECURITY, Tag.UNPREDICTABLE})
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.DATA_RELIABILITY)
@SqaleConstantRemediation("5min")
public class MutableMembersUsageCheck extends BaseTreeVisitor implements JavaFileScanner {

  private static final List<String> MUTABLE_TYPES = ImmutableList.of(
    "java.util.Collection",
    "java.util.Date",
    "java.util.Hashtable"
  );
  private static final List<String> IMMUTABLE_TYPES = ImmutableList.of(
    "java.util.Collections.UnmodifiableCollection",
    "java.util.Collections.UnmodifiableMap",
    "com.google.common.collect.ImmutableCollection"
  );

  private JavaFileScannerContext context;
  private Deque<List<Symbol>> parametersStack = new LinkedList<>();

  @Override
  public void scanFile(final JavaFileScannerContext context) {
    this.context = context;
    scan(context.getTree());
  }

  @Override
  public void visitMethod(MethodTree tree) {
    ArrayList<Symbol> parameters = new ArrayList<>();
    for (VariableTree variableTree : tree.parameters()) {
      parameters.add(variableTree.symbol());
    }
    parametersStack.push(parameters);
    super.visitMethod(tree);
    parametersStack.pop();
  }

  @Override
  public void visitAssignmentExpression(AssignmentExpressionTree tree) {
    super.visitAssignmentExpression(tree);
    if (!isMutableType(tree.expression().symbolType())) {
      return;
    }
    ExpressionTree variable = tree.variable();
    Symbol leftSymbol = null;
    if (variable.is(Tree.Kind.IDENTIFIER)) {
      IdentifierTree identifierTree = (IdentifierTree) variable;
      leftSymbol = identifierTree.symbol();
    } else if(variable.is(Tree.Kind.MEMBER_SELECT)) {
      MemberSelectExpressionTree mit = (MemberSelectExpressionTree) variable;
      leftSymbol = mit.identifier().symbol();
    }
    if (leftSymbol != null && leftSymbol.isPrivate()) {
      checkStore(tree.expression());
    }
  }

  @Override
  public void visitVariable(VariableTree tree) {
    super.visitVariable(tree);
    ExpressionTree initializer = tree.initializer();
    if (initializer == null || !isMutableType(initializer.symbolType())) {
      return;
    }
    checkStore(initializer);
  }

  private void checkStore(ExpressionTree expression) {
    if (expression.is(Tree.Kind.IDENTIFIER)) {
      IdentifierTree identifierTree = (IdentifierTree) expression;
      if (!parametersStack.isEmpty() && parametersStack.peek().contains(identifierTree.symbol())) {
        context.addIssue(expression, this, "Store a copy of \"" + identifierTree.name() + "\".");
      }
    }
  }

  @Override
  public void visitReturnStatement(ReturnStatementTree tree) {
    super.visitReturnStatement(tree);
    ExpressionTree expressionTree = tree.expression();
    if (expressionTree == null || !isMutableType(expressionTree.symbolType())) {
      return;
    }
    if (expressionTree.is(Tree.Kind.IDENTIFIER)) {
      IdentifierTree identifierTree = (IdentifierTree) expressionTree;
      if (identifierTree.symbol().isPrivate()) {
        context.addIssue(expressionTree, this, "Return a copy of \"" + identifierTree.name() + "\".");
      }
    }
  }

  private static boolean isMutableType(Type type) {
    if (type.isArray()) {
      return true;
    }
    for (String mutableType : MUTABLE_TYPES) {
      if (type.isSubtypeOf(mutableType) && isNotImmutable(type)) {
        return true;
      }
    }
    return false;
  }

  private static boolean isNotImmutable(Type type) {
    for (String immutableType : IMMUTABLE_TYPES) {
      if (type.isSubtypeOf(immutableType)) {
        return false;
      }
    }
    return true;
  }

}
