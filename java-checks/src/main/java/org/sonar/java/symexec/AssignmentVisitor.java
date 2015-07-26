/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.java.symexec;

import com.google.common.annotations.VisibleForTesting;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Symbol.VariableSymbol;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.HashSet;
import java.util.Set;

class AssignedSymbolExtractor {

  private final Visitor visitor = new Visitor();

  public Set<VariableSymbol> findAssignedVariables(Tree tree) {
    visitor.assignedSymbols = new HashSet<>();
    tree.accept(visitor);
    return visitor.assignedSymbols;
  }

  private static class Visitor extends BaseTreeVisitor {
    @VisibleForTesting
    Set<VariableSymbol> assignedSymbols;

    @Override
    public void visitAssignmentExpression(AssignmentExpressionTree tree) {
      if (tree.variable().is(Tree.Kind.IDENTIFIER)) {
        registerAssignedSymbol(((IdentifierTree) tree.variable()).symbol());
      }
      super.visitAssignmentExpression(tree);
    }

    @VisibleForTesting
    void registerAssignedSymbol(Symbol symbol) {
      if (symbol.isVariableSymbol()) {
        assignedSymbols.add((VariableSymbol) symbol);
      }
    }
  }

}
