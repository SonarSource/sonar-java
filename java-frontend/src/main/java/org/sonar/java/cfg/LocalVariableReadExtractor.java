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
package org.sonar.java.cfg;

import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.ArrayList;
import java.util.List;

public class LocalVariableReadExtractor extends BaseTreeVisitor {

  private final Symbol.MethodSymbol methodSymbol;
  private final List<Symbol> used;

  public LocalVariableReadExtractor(Symbol.MethodSymbol methodSymbol) {
    this.methodSymbol = methodSymbol;
    used = new ArrayList<>();
  }

  public List<Symbol> usedVariables() {
    return used;
  }

  @Override
  public void visitAssignmentExpression(AssignmentExpressionTree tree) {
    //skip writing to a variable.
    if(!tree.variable().is(Tree.Kind.IDENTIFIER)) {
      scan(tree.variable());
    }
    scan(tree.expression());
  }

  @Override
  public void visitIdentifier(IdentifierTree tree) {
    if(methodSymbol.equals(tree.symbol().owner())) {
      used.add(tree.symbol());
    }
    super.visitIdentifier(tree);
  }

}
