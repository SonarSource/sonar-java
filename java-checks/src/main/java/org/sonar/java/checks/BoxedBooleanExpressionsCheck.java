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
package org.sonar.java.checks;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.CheckForNull;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.QuickFixHelper;
import org.sonar.java.reporting.JavaQuickFix;
import org.sonar.java.reporting.JavaTextEdit;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.semantic.Type.Primitives;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ConditionalExpressionTree;
import org.sonar.plugins.java.api.tree.DoWhileStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.ForStatementTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.IfStatementTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.TypeCastTree;
import org.sonar.plugins.java.api.tree.UnaryExpressionTree;
import org.sonar.plugins.java.api.tree.WhileStatementTree;

import static org.sonar.plugins.java.api.semantic.MethodMatchers.ANY;

@Rule(key = "S5411")
public class BoxedBooleanExpressionsCheck extends BaseTreeVisitor implements JavaFileScanner {

  private static final String MESSAGE = "Use a primitive boolean expression here.";
  private static final String MESSAGE_QUICKFIX = "Use a primitive boolean expression";

  private static final MethodMatchers OPTIONAL_OR_ELSE = MethodMatchers.create()
    .ofTypes("java.util.Optional").names("orElse").addParametersMatcher(ANY).build();

  private static final String BOOLEAN = "java.lang.Boolean";
  private JavaFileScannerContext context;

  private static final Map<Tree, IfStatementTree> ifStatementCache = new HashMap<>();
  private static final Map<Symbol, ExpressionTree> firstNullCheckCache = new HashMap<>();

  @Override
  public void scanFile(JavaFileScannerContext context) {
    ifStatementCache.clear();
    firstNullCheckCache.clear();
    this.context = context;
    if (context.getSemanticModel() != null) {
      scan(context.getTree());
    }
  }

  @Override
  public void visitForStatement(ForStatementTree tree) {
    if (tree.condition() != null && !isSafeBooleanExpression(tree.condition())) {
      scan(tree.initializer());
      scan(tree.update());
      scan(tree.statement());
    } else {
      super.visitForStatement(tree);
    }
  }

  @Override
  public void visitWhileStatement(WhileStatementTree tree) {
    if (!isSafeBooleanExpression(tree.condition())) {
      scan(tree.statement());
    } else {
      super.visitWhileStatement(tree);
    }
  }

  @Override
  public void visitDoWhileStatement(DoWhileStatementTree tree) {
    if (!isSafeBooleanExpression(tree.condition())) {
      scan(tree.statement());
    } else {
      super.visitDoWhileStatement(tree);
    }
  }

  @Override
  public void visitIfStatement(IfStatementTree tree) {
    if (!isSafeBooleanExpression(tree.condition())) {
      scan(tree.thenStatement());
      scan(tree.elseStatement());
    } else {
      super.visitIfStatement(tree);
    }
  }

  @Override
  public void visitConditionalExpression(ConditionalExpressionTree tree) {
    if (!isSafeBooleanExpression(tree.condition())) {
      scan(tree.trueExpression());
      scan(tree.falseExpression());
    } else {
      super.visitConditionalExpression(tree);
    }
  }

  private boolean isSafeBooleanExpression(ExpressionTree tree) {
    ExpressionTree boxedBoolean = findBoxedBoolean(tree);
    if (boxedBoolean != null) {
      // The rule is relaxed if the first usage of the variable is a test against nullness.
      // A more thorough approach would require tracing all possible paths to lookup the test using symbolic execution.
      if (isFirstUsageANullCheck(boxedBoolean)) {
        return true;
      }
      QuickFixHelper.newIssue(context)
        .forRule(this)
        .onTree(boxedBoolean)
        .withMessage(MESSAGE)
        .withQuickFixes(() -> getQuickFix(tree, boxedBoolean))
        .report();
      return false;
    }
    return true;
  }

  private static boolean isFirstUsageANullCheck(ExpressionTree boxedBoolean) {
    if (boxedBoolean.is(Kind.IDENTIFIER)) {
      IdentifierTree identifier = (IdentifierTree) boxedBoolean;
      // Usages are not guaranteed to be ordered
      List<IdentifierTree> usages = identifier.symbol().usages();
      Tree firstUsage = usages.get(0).parent();
      // Test if the first usage in our list is a null check
      if (firstUsage.is(Kind.EQUAL_TO, Kind.NOT_EQUAL_TO) && isNullCheck((ExpressionTree) firstUsage)) {
        return true;
      }
      // Return false if the only usage is not a null check
      if (usages.size() == 1) {
        return false;
      }
      // Fetch the first null check in the usages list
      Optional<ExpressionTree> firstNullCheck = getFirstNullCheck(identifier.symbol());
      if (!firstNullCheck.isPresent()) {
        return false;
      }
      // Test if the first null check and the first usage are part of the same higher if structure
      Optional<IfStatementTree> ifStatementWithNullCheck = getParentConditionalBranch(firstNullCheck.get());
      Optional<IfStatementTree> ifStatementWithFirstUsage = getParentConditionalBranch(firstUsage);
      return ifStatementWithNullCheck.equals(ifStatementWithFirstUsage);
    }
    if (boxedBoolean.is(Kind.TYPE_CAST)) {
      TypeCastTree typeCast = (TypeCastTree) boxedBoolean;
      return isFirstUsageANullCheck(typeCast.expression());
    }
    return false;
  }

  private static Optional<ExpressionTree> getFirstNullCheck(Symbol symbol) {
    if (firstNullCheckCache.containsKey(symbol)) {
      return Optional.ofNullable(firstNullCheckCache.get(symbol));
    }
    Optional<ExpressionTree> firstNullCheck = symbol.usages().stream()
      .map(IdentifierTree::parent)
      .filter(tree -> tree.is(Kind.EQUAL_TO, Kind.NOT_EQUAL_TO) && isNullCheck((ExpressionTree) tree))
      .map(ExpressionTree.class::cast)
      .findFirst();
    firstNullCheckCache.put(symbol, firstNullCheck.orElse(null));
    return firstNullCheck;
  }


  private static Optional<IfStatementTree> getParentConditionalBranch(Tree tree) {
    Deque<Tree> trees = new ArrayDeque<>();
    Tree current = tree;
    IfStatementTree ifStatementTree = null;

    while (current != null && ifStatementTree == null) {
      if (ifStatementCache.containsKey(tree)) {
        ifStatementTree = ifStatementCache.get(tree);
      } else if (current.is(Kind.IF_STATEMENT)) {
        ifStatementTree = (IfStatementTree) current;
      }
      trees.add(current);
      current = current.parent();
    }

    while (!trees.isEmpty()) {
      ifStatementCache.put(trees.pop(), ifStatementTree);
    }

    return Optional.ofNullable(ifStatementTree);
  }

  @CheckForNull
  private static ExpressionTree findBoxedBoolean(ExpressionTree tree) {
    if (tree.symbolType().is(BOOLEAN) && !isValidMethodInvocation(tree)) {
      return tree;
    }
    if (tree.is(Kind.LOGICAL_COMPLEMENT)) {
      return findBoxedBoolean(((UnaryExpressionTree) tree).expression());
    }
    if (tree instanceof BinaryExpressionTree) {
      BinaryExpressionTree expr = (BinaryExpressionTree) tree;
      if (findBoxedBoolean(expr.leftOperand()) != null && expr.rightOperand().symbolType().isPrimitive(Primitives.BOOLEAN)) {
        return expr.leftOperand();
      }
      if (findBoxedBoolean(expr.rightOperand()) != null && expr.leftOperand().symbolType().isPrimitive(Primitives.BOOLEAN) && !isNullCheck(expr.leftOperand())) {
        return expr.rightOperand();
      }
    }
    return null;
  }

  private static boolean isNullCheck(ExpressionTree tree) {
    if (tree.is(Kind.NOT_EQUAL_TO, Kind.EQUAL_TO)) {
      BinaryExpressionTree expr = (BinaryExpressionTree) tree;
      return expr.leftOperand().is(Kind.NULL_LITERAL) || expr.rightOperand().is(Kind.NULL_LITERAL);
    }
    return false;
  }

  private static boolean isValidMethodInvocation(ExpressionTree tree) {
    if (tree.is(Kind.METHOD_INVOCATION)) {
      MethodInvocationTree mit = (MethodInvocationTree) tree;
      return isOptionalInvocation(mit) || isAnnotatedNonnull(mit);
    }
    return false;
  }

  private static boolean isOptionalInvocation(MethodInvocationTree mit) {
    return OPTIONAL_OR_ELSE.matches(mit) && !mit.arguments().get(0).is(Kind.NULL_LITERAL);
  }

  private static boolean isAnnotatedNonnull(MethodInvocationTree mit) {
    return mit.methodSymbol().metadata()
      .annotations()
      .stream()
      .map(SymbolMetadata.AnnotationInstance::symbol)
      .map(Symbol::name)
      .anyMatch(name -> "nonNull".equalsIgnoreCase(name) || "notNull".equalsIgnoreCase(name));
  }

  private static List<JavaQuickFix> getQuickFix(ExpressionTree tree, ExpressionTree boxedBoolean) {
    if (tree.is(Kind.METHOD_INVOCATION) && OPTIONAL_OR_ELSE.matches((MethodInvocationTree) tree)) {
      // We do not suggest a quick fix when we have an optional
      return Collections.emptyList();
    }
    List<JavaTextEdit> edits = new ArrayList<>(2);
    if (tree.is(Kind.LOGICAL_COMPLEMENT)) {
      edits.add(JavaTextEdit.replaceTree(((UnaryExpressionTree) tree).operatorToken(), "Boolean.FALSE.equals("));
    } else {
      edits.add(JavaTextEdit.insertBeforeTree(boxedBoolean, "Boolean.TRUE.equals("));
    }
    edits.add(JavaTextEdit.insertAfterTree(boxedBoolean, ")"));

    return Collections.singletonList(JavaQuickFix.newQuickFix(MESSAGE_QUICKFIX)
      .addTextEdits(edits)
      .build());
  }
}
