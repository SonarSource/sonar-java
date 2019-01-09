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
package org.sonar.java.cfg;

import java.util.HashSet;
import java.util.Set;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Symbol.MethodSymbol;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.VariableTree;

public class VariableReadExtractor extends BaseTreeVisitor {

  private final Symbol.MethodSymbol methodSymbol;
  private final Set<Symbol> used;
  private final boolean includeFields;

  public VariableReadExtractor(MethodSymbol methodSymbol, boolean includeFields) {
    this.methodSymbol = methodSymbol;
    this.includeFields = includeFields;
    used = new HashSet<>();
  }

  public Set<Symbol> usedVariables() {
    return used;
  }

  @Override
  public void visitAssignmentExpression(AssignmentExpressionTree tree) {
    //skip writing to a variable or field.
    if(!tree.variable().is(Tree.Kind.IDENTIFIER) && !tree.variable().is(Kind.MEMBER_SELECT)) {
      scan(tree.variable());
    }
    scan(tree.expression());
  }

  @Override
  public void visitVariable(VariableTree tree) {
    // skip variable modifiers and simple name
    scan(tree.initializer());
  }

  @Override
  public void visitClass(ClassTree tree) {
    // skip modifiers, parameters, simple name and superclass/interface
    scan(tree.members());
  }

  @Override
  public void visitLambdaExpression(LambdaExpressionTree lambdaExpressionTree) {
    // skip variable declaration
    scan(lambdaExpressionTree.body());
  }

  @Override
  public void visitIdentifier(IdentifierTree tree) {
    Symbol owner = tree.symbol().owner();
    if(methodSymbol.equals(owner) || (includeFields && isField(tree.symbol(), methodSymbol.owner()))) {
      used.add(tree.symbol());
    }
    super.visitIdentifier(tree);
  }

  private static boolean isField(Symbol identifierSymbol, Symbol methodOwnerSymbol) {
    return methodOwnerSymbol.equals(identifierSymbol.owner()) && !"this".equals(identifierSymbol.name());
  }

}
