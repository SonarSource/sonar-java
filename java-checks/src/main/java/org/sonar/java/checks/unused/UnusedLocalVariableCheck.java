/*
 * SonarQube Java
 * Copyright (C) 2012-2023 SonarSource SA
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
package org.sonar.java.checks.unused;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.QuickFixHelper;
import org.sonar.java.checks.helpers.UnresolvedIdentifiersVisitor;
import org.sonar.java.reporting.AnalyzerMessage;
import org.sonar.java.reporting.JavaQuickFix;
import org.sonar.java.reporting.JavaTextEdit;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.ListTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import static org.sonar.java.model.JUtils.isParameter;

@Rule(key = "S1481")
public class UnusedLocalVariableCheck extends IssuableSubscriptionVisitor {

  private static final Tree.Kind[] INCREMENT_KINDS = {
    Tree.Kind.POSTFIX_DECREMENT,
    Tree.Kind.POSTFIX_INCREMENT,
    Tree.Kind.PREFIX_DECREMENT,
    Tree.Kind.PREFIX_INCREMENT
  };

  private static final String MESSAGE = "Remove this unused \"%s\" local variable.";

  private static final UnresolvedIdentifiersVisitor UNRESOLVED_IDENTIFIERS_VISITOR = new UnresolvedIdentifiersVisitor();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.COMPILATION_UNIT, Tree.Kind.VARIABLE);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Tree.Kind.COMPILATION_UNIT)) {
      UNRESOLVED_IDENTIFIERS_VISITOR.check(tree);
    }
  }

  @Override
  public void leaveNode(Tree tree) {
    if (tree.is(Tree.Kind.VARIABLE)) {
      VariableTree variable = (VariableTree) tree;
      String name = variable.simpleName().name();
      boolean unresolved = UNRESOLVED_IDENTIFIERS_VISITOR.isUnresolved(name);
      if (!unresolved && isProperLocalVariable(variable) && isUnused(variable.symbol())) {
        QuickFixHelper.newIssue(context)
          .forRule(this)
          .onTree(variable.simpleName())
          .withMessage(String.format(MESSAGE, name))
          .withQuickFixes(() -> computeQuickFix(variable))
          .report();
      }
    }

  }

  private static boolean isUnused(Symbol symbol) {
    return symbol.usages().stream().noneMatch(UnusedLocalVariableCheck::isRValue);
  }

  /**
   * An identifier is being used as an r-value if it is not used as the left operand of an assignment nor as the
   * operand of a stand-alone increment
   */
  private static boolean isRValue(IdentifierTree tree) {
    Tree parent = skipParenthesesUpwards(tree.parent());
    if (parent instanceof AssignmentExpressionTree) {
      AssignmentExpressionTree assignment = (AssignmentExpressionTree) parent;
      return assignment.variable() != tree;
    }
    // Note that an expression statement can't be a parenthesized expression, so we don't need to skip parentheses here
    return !(parent.is(INCREMENT_KINDS) && parent.parent().is(Tree.Kind.EXPRESSION_STATEMENT));
  }

  private static Tree skipParenthesesUpwards(Tree tree) {
    while (tree.is(Tree.Kind.PARENTHESIZED_EXPRESSION)) {
      tree = tree.parent();
    }
    return tree;
  }

  private static boolean isProperLocalVariable(VariableTree variable) {
    Symbol symbol = variable.symbol();
    return symbol.isLocalVariable()
      && !isParameter(symbol)
      && !isDefinedInCatchClause(variable)
      && !isTryResource(variable);
  }

  private static boolean isDefinedInCatchClause(VariableTree variable) {
    return variable.parent().is(Tree.Kind.CATCH);
  }

  private static boolean isTryResource(VariableTree variable) {
    return variable.parent().is(Tree.Kind.LIST) && variable.parent().parent().is(Tree.Kind.TRY_STATEMENT);
  }

  private static List<JavaQuickFix> computeQuickFix(VariableTree variable) {
    return getQuickFixTextSpan(variable).map(textSpan -> Collections.singletonList(
        JavaQuickFix.newQuickFix("Remove unused local variable")
          .addTextEdit(JavaTextEdit.removeTextSpan(textSpan))
          .build()
      )
    ).orElseGet(Collections::emptyList);
  }

  private static Optional<AnalyzerMessage.TextSpan> getQuickFixTextSpan(VariableTree variable) {
    if (!variable.symbol().usages().isEmpty()) {
      return Optional.empty();
    }
    Tree parent = variable.parent();
    SyntaxToken lastToken = variable.lastToken();
    if (parent.is(Tree.Kind.BLOCK, Tree.Kind.INITIALIZER, Tree.Kind.STATIC_INITIALIZER)) {
      // If the variable is in the declaration list but is not the last one, we also need to include the following comma
      Optional<VariableTree> followingVariable = QuickFixHelper.nextVariable(variable);
      if (followingVariable.isPresent()) {
        return Optional.of(AnalyzerMessage.textSpanBetween(variable.simpleName(), true, followingVariable.get().simpleName(), false));
      }
      // If the variable is last in the declaration, we need to retrieve the preceding comma
      Optional<SyntaxToken> precedingComma = getPrecedingComma(variable);
      if (precedingComma.isPresent()) {
        AnalyzerMessage.TextSpan value = AnalyzerMessage.textSpanBetween(precedingComma.get(), true, lastToken, false);
        return Optional.of(value);
      }
      return Optional.of(AnalyzerMessage.textSpanBetween(variable.firstToken(), lastToken));
    } else if (parent.is(Tree.Kind.LIST)) {
      ListTree<VariableTree> variables = (ListTree<VariableTree>) parent;
      // If the variable is the only one in the list we can include the entire list
      if (variables.size() == 1) {
        return Optional.of(AnalyzerMessage.textSpanFor(variable));
      }
      // If the variable is not the last one in the list we can include the following comma
      if (",".equals(lastToken.text())) {
        return Optional.of(AnalyzerMessage.textSpanBetween(variable.simpleName(), lastToken));
      }
      // If the variable is last in the list, we need to retrieve the preceding comma
      SyntaxToken precedingComma = variables.get(variables.indexOf(variable) - 1).lastToken();
      return Optional.of(AnalyzerMessage.textSpanBetween(precedingComma, lastToken));
    } else if (parent.is(Tree.Kind.TYPE_PATTERN)) {
      return Optional.of(AnalyzerMessage.textSpanFor(lastToken));
    }
    return Optional.empty();
  }

  private static Optional<SyntaxToken> getPrecedingComma(VariableTree variable) {
    return QuickFixHelper.previousVariable(variable).map(VariableTree::lastToken);
  }
}
