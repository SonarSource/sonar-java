/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.checks.unused;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.QuickFixHelper;
import org.sonar.java.checks.helpers.UnresolvedIdentifiersVisitor;
import org.sonar.java.reporting.AnalyzerMessage;
import org.sonar.java.reporting.JavaQuickFix;
import org.sonar.java.reporting.JavaTextEdit;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.CaseLabelTree;
import org.sonar.plugins.java.api.tree.ForEachStatement;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.ListTree;
import org.sonar.plugins.java.api.tree.RecordPatternTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import static org.sonar.java.model.ExpressionUtils.skipParenthesesUpwards;

@Rule(key = "S1481")
public class UnusedLocalVariableCheck extends IssuableSubscriptionVisitor {

  private static final Tree.Kind[] INCREMENT_KINDS = {
    Tree.Kind.POSTFIX_DECREMENT,
    Tree.Kind.POSTFIX_INCREMENT,
    Tree.Kind.PREFIX_DECREMENT,
    Tree.Kind.PREFIX_INCREMENT
  };

  private static final String MESSAGE = "Remove this unused \"%s\" local variable.";

  private static final IdentifierProperties IDENTIFIER_PROPERTIES = new IdentifierProperties();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.COMPILATION_UNIT, Tree.Kind.VARIABLE);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Tree.Kind.COMPILATION_UNIT)) {
      IDENTIFIER_PROPERTIES.check(tree);
    }
  }

  @Override
  public void leaveNode(Tree tree) {
    if (tree.is(Tree.Kind.VARIABLE)) {
      VariableTree variable = (VariableTree) tree;
      IdentifierTree simpleName = variable.simpleName();
      if (!simpleName.isUnnamedVariable()) {
        // If a variable with the same name as this is unresolved, we can't be sure it's not a usage of this. So we can't raise an issue.
        boolean unresolved = IDENTIFIER_PROPERTIES.isUnresolved(simpleName.name());
        if (!unresolved && isProperLocalVariable(variable) && isUnused(variable.symbol()) && canBeReplaced(variable)) {
          QuickFixHelper.newIssue(context)
            .forRule(this)
            .onTree(simpleName)
            .withMessage(String.format(MESSAGE, simpleName.name()))
            .withQuickFixes(() -> computeQuickFix(variable))
            .report();
        }
      }
    }

  }

  /**
   * Before Java 22 it was not possible to remove the variable in a foreach statement or try with resources even it is unused.
   * For instance in {@code for (String element : list) {}}, it is only since Java 22 that it can be rewritten {@code for (var _ : list) {}}.
   */
  private boolean canBeReplaced(VariableTree variable) {
    return context.getJavaVersion().isJava22Compatible()
      || (!isForeachVariable(variable)
        && !isTryResource(variable)
        && !isTypePatternWithinCaseOrRecord(variable));
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
    if (parent instanceof AssignmentExpressionTree assignment) {
      return assignment.variable() != tree;
    }
    // Note that an expression statement can't be a parenthesized expression, so we don't need to skip parentheses here
    return !(parent.is(INCREMENT_KINDS) && parent.parent().is(Tree.Kind.EXPRESSION_STATEMENT));
  }

  private static boolean isProperLocalVariable(VariableTree variable) {
    Symbol symbol = variable.symbol();
    return symbol.isLocalVariable()
      && !symbol.isParameter()
      && !isDefinedInCatchClause(variable);
  }

  private static boolean isDefinedInCatchClause(VariableTree variable) {
    return variable.parent().is(Tree.Kind.CATCH);
  }

  private static boolean isTryResource(VariableTree variable) {
    return variable.parent().is(Tree.Kind.LIST) && variable.parent().parent().is(Tree.Kind.TRY_STATEMENT);
  }

  private static boolean isForeachVariable(VariableTree variable) {
    return variable.parent() instanceof ForEachStatement;
  }

  private static List<JavaQuickFix> computeQuickFix(VariableTree variable) {
    if (isForeachVariable(variable)
      || isTryResource(variable)
      || isTypePatternWithinCaseOrRecord(variable)) {
      return List.of(makeQuickFixReplacingWithUnnamedVariable(variable));
    }
    return getQuickFixTextSpan(variable).map(textSpan -> Collections.singletonList(
      JavaQuickFix.newQuickFix("Remove unused local variable")
        .addTextEdit(JavaTextEdit.removeTextSpan(textSpan))
        .build()))
      .orElseGet(Collections::emptyList);
  }

  private static boolean isTypePatternWithinCaseOrRecord(VariableTree variable) {
    return IDENTIFIER_PROPERTIES.isVariableInsideCase(variable)
      || IDENTIFIER_PROPERTIES.isVariableInsideRecordPattern(variable);
  }

  private static JavaQuickFix makeQuickFixReplacingWithUnnamedVariable(VariableTree variable) {
    return JavaQuickFix.newQuickFix("Replace unused local variable with _")
      // This works with both enhanced for loop and try-with-resources.
      // In the latter case we keep the initializer:
      // `for(int elem: elems)` turns into `for(var _: elems)`
      // `try(Resource res = initializer())` turns into `try(var _ = initializer ())`
      .addTextEdit(JavaTextEdit.replaceBetweenTree(variable.type(), true, variable.simpleName(), true, "var _"))
      .build();
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

  private static class IdentifierProperties extends UnresolvedIdentifiersVisitor {
    private final Set<VariableTree> patternVariables = new HashSet<>();
    private final Set<VariableTree> caseVariables = new HashSet<>();
    private int nestingRecordLevel = 0;
    private boolean withinCaseLabel = false;

    @Override
    public Set<String> check(Tree tree) {
      patternVariables.clear();
      caseVariables.clear();
      nestingRecordLevel = 0;
      withinCaseLabel = false;
      return super.check(tree);
    }

    public boolean isVariableInsideRecordPattern(VariableTree variable) {
      return patternVariables.contains(variable);
    }

    public boolean isVariableInsideCase(VariableTree variable) {
      return caseVariables.contains(variable);
    }

    @Override
    public void visitRecordPattern(RecordPatternTree tree) {
      nestingRecordLevel += 1;
      super.visitRecordPattern(tree);
      nestingRecordLevel -= 1;
    }

    @Override
    public void visitCaseLabel(CaseLabelTree tree) {
      withinCaseLabel = true;
      super.visitCaseLabel(tree);
      withinCaseLabel = false;
    }

    @Override
    public void visitVariable(VariableTree tree) {
      if (nestingRecordLevel > 0) {
        patternVariables.add(tree);
      }
      if (withinCaseLabel) {
        caseVariables.add(tree);
      }
      super.visitVariable(tree);
    }
  }
}
