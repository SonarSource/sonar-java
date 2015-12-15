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
package org.sonar.java.checks.helpers;

import org.sonar.java.syntaxtoken.FirstSyntaxTokenFinder;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Helper Visitor to be used to find the latest {@link ExpressionTree} used as initializer (for a {@link VariableTree}) 
 * or expression used in assignment (for a {@link AssignmentExpressionTree}) for a given variable.
 */
public class ReassignmentFinder extends BaseTreeVisitor {

  private final List<IdentifierTree> usages;
  private List<Tree> reassignments;

  private ReassignmentFinder(List<IdentifierTree> usages) {
    this.usages = usages;
    this.reassignments = new LinkedList<>();
  }

  @CheckForNull
  public static ExpressionTree getClosestReassignmentOrDeclarationExpression(Tree startingPoint, Symbol referenceSymbol) {
    Tree result = referenceSymbol.declaration();
    List<IdentifierTree> usages = referenceSymbol.usages();
    if (usages.size() != 1) {
      List<Tree> reassignments = getReassignments(referenceSymbol.owner().declaration(), usages);

      SyntaxToken startPointToken = FirstSyntaxTokenFinder.firstSyntaxToken(startingPoint);
      Tree lastReassignment = getClosestReassignment(startPointToken, reassignments);
      if (lastReassignment != null) {
        result = lastReassignment;
      }
    }

    return getInitializerOrExpression(result);
  }

  @CheckForNull
  private static ExpressionTree getInitializerOrExpression(@Nullable Tree tree) {
    if (tree == null) {
      return null;
    }
    if (tree.is(Tree.Kind.VARIABLE)) {
      return ((VariableTree) tree).initializer();
    }
    return ((AssignmentExpressionTree) tree).expression();
  }

  private static List<Tree> getReassignments(@Nullable Tree ownerDeclaration, List<IdentifierTree> usages) {
    if (ownerDeclaration != null) {
      ReassignmentFinder reassignmentFinder = new ReassignmentFinder(usages);
      ownerDeclaration.accept(reassignmentFinder);
      return reassignmentFinder.reassignments;
    }
    return new ArrayList<>();
  }

  @CheckForNull
  private static Tree getClosestReassignment(SyntaxToken startToken, List<Tree> reassignments) {
    Tree result = null;
    for (Tree reassignment : reassignments) {
      SyntaxToken reassignmentFirstToken = FirstSyntaxTokenFinder.firstSyntaxToken(reassignment);
      int reassignmentLine = reassignmentFirstToken.line();
      int startLine = startToken.line();
      if (startLine > reassignmentLine ||
          (startLine == reassignmentLine &&
              startToken.column() > reassignmentFirstToken.column() &&
              !isInAssignedExpression(startToken, reassignment))) {
        result = reassignment;
      }
    }
    return result;
  }

  private static boolean isInAssignedExpression(SyntaxToken syntaxToken, Tree reassignement) {
    Tree parent = syntaxToken.parent();
    while (parent != null && !parent.is(Tree.Kind.EXPRESSION_STATEMENT)) {
      if(parent == reassignement) {
        return true;
      }
      parent = parent.parent();
    }
    return false;
  }

  @Override
  public void visitAssignmentExpression(AssignmentExpressionTree tree) {
    if (isSearchedVariable(tree.variable())) {
      reassignments.add(tree);
    }
    super.visitAssignmentExpression(tree);
  }

  private boolean isSearchedVariable(ExpressionTree variable) {
    return variable.is(Tree.Kind.IDENTIFIER) && usages.contains(variable);
  }
}
