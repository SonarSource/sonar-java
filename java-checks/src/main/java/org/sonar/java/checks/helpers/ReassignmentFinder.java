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
package org.sonar.java.checks.helpers;

import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.EnumConstantTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Helper class to be used to find the latest {@link ExpressionTree} used as initializer (for a {@link VariableTree}) 
 * or expression used in assignment (for a {@link AssignmentExpressionTree}) for a given variable.
 */
public final class ReassignmentFinder {

  private ReassignmentFinder() {
  }

  @CheckForNull
  public static ExpressionTree getClosestReassignmentOrDeclarationExpression(Tree startingPoint, Symbol referenceSymbol) {
    Tree result = referenceSymbol.declaration();
    List<IdentifierTree> usages = referenceSymbol.usages();
    if (usages.size() != 1) {
      List<AssignmentExpressionTree> reassignments = getReassignments(referenceSymbol.owner().declaration(), usages);

      SyntaxToken startPointToken = startingPoint.firstToken();
      Tree lastReassignment = getClosestReassignment(startPointToken, reassignments);
      if (lastReassignment != null) {
        result = lastReassignment;
      }
    }

    ExpressionTree initializerOrExpression = getInitializerOrExpression(result);
    if (initializerOrExpression == startingPoint) {
      return getClosestReassignmentOrDeclarationExpression(result, referenceSymbol);
    }
    return initializerOrExpression;
  }

  @CheckForNull
  public static ExpressionTree getInitializerOrExpression(@Nullable Tree tree) {
    if (tree == null) {
      return null;
    }
    if (tree.is(Tree.Kind.VARIABLE)) {
      return ((VariableTree) tree).initializer();
    } else if (tree.is(Tree.Kind.ENUM_CONSTANT)) {
      return ((EnumConstantTree) tree).initializer();
    }
    return ((AssignmentExpressionTree) tree).expression();
  }

  public static List<AssignmentExpressionTree> getReassignments(@Nullable Tree ownerDeclaration, List<IdentifierTree> usages) {
    if (ownerDeclaration != null) {
      List<AssignmentExpressionTree> assignments = new ArrayList<>();
      for (IdentifierTree usage : usages) {
        checkAssignment(usage).ifPresent(assignments::add);
      }
      return assignments;
    }
    return new ArrayList<>();
  }

  private static Optional<AssignmentExpressionTree> checkAssignment(IdentifierTree usage) {
    Tree previousTree = usage;
    Tree nonParenthesisParent = previousTree.parent();

    while (nonParenthesisParent.is(Tree.Kind.PARENTHESIZED_EXPRESSION)) {
      previousTree = nonParenthesisParent;
      nonParenthesisParent = previousTree.parent();
    }

    if (nonParenthesisParent instanceof AssignmentExpressionTree) {
      AssignmentExpressionTree assignment = (AssignmentExpressionTree) nonParenthesisParent;
      if (assignment.variable().equals(previousTree)) {
        return Optional.of(assignment);
      }
    }
    return Optional.empty();
  }

  @CheckForNull
  private static Tree getClosestReassignment(SyntaxToken startToken, List<AssignmentExpressionTree> reassignments) {
    Tree result = null;
    List<Tree> assignmentsBeforeStartToken = reassignments.stream()
      .sorted(ReassignmentFinder::isBefore)
      .filter(a -> isBefore(startToken, a) > 0)
      .collect(Collectors.toList());

    if (!assignmentsBeforeStartToken.isEmpty()) {
      return assignmentsBeforeStartToken.get(assignmentsBeforeStartToken.size() - 1);
    }
    return result;
  }

  private static int isBefore(Tree t1, Tree t2) {
    SyntaxToken firstTokenT1 = t1.firstToken();
    SyntaxToken firstTokenT2 = t2.firstToken();
    int line = Integer.compare(firstTokenT1.line(), firstTokenT2.line());
    return line != 0 ? line : Integer.compare(firstTokenT1.column(), firstTokenT2.column());
  }
}
