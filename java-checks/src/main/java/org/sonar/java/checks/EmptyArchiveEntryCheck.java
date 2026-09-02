/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.checks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.CheckForNull;
import org.sonar.check.Rule;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeCastTree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S9342")
public class EmptyArchiveEntryCheck extends IssuableSubscriptionVisitor {

  private static final MethodMatchers PUT_NEXT_ENTRY = MethodMatchers.create()
    .ofSubTypes("java.util.zip.ZipOutputStream")
    .names("putNextEntry")
    .addParametersMatcher("java.util.zip.ZipEntry")
    .build();

  private static final MethodMatchers CLOSE_ENTRY = MethodMatchers.create()
    .ofSubTypes("java.util.zip.ZipOutputStream")
    .names("closeEntry")
    .addWithoutParametersMatcher()
    .build();

  private static final MethodMatchers WRITE = MethodMatchers.create()
    .ofSubTypes("java.io.OutputStream")
    .names("write")
    .withAnyParameters()
    .build();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.BLOCK);
  }

  @Override
  public void visitNode(Tree tree) {
    if (context.getSemanticModel() == null) {
      return;
    }
    Map<Symbol, MethodInvocationTree> pendingEntries = new HashMap<>();
    for (StatementTree statement : ((BlockTree) tree).body()) {
      MethodInvocationTree mit = extractMethodInvocation(statement);
      if (mit != null) {
        handleMethodInvocation(mit, pendingEntries);
      } else {
        scanForTrackedSymbolUsage(statement, pendingEntries);
      }
    }
  }

  private void handleMethodInvocation(MethodInvocationTree mit, Map<Symbol, MethodInvocationTree> pendingEntries) {
    Symbol receiver = getReceiverSymbol(mit);
    if (receiver == null) {
      clearTrackedSymbolsUsedAsArguments(mit, pendingEntries);
      return;
    }
    if (PUT_NEXT_ENTRY.matches(mit)) {
      if (!isDirectoryEntry(mit)) {
        pendingEntries.put(receiver, mit);
      }
    } else if (CLOSE_ENTRY.matches(mit)) {
      MethodInvocationTree putNextEntry = pendingEntries.remove(receiver);
      if (putNextEntry != null) {
        reportIssue(ExpressionUtils.methodName(mit), "Write content to this archive entry; it is empty.",
          Collections.singletonList(new JavaFileScannerContext.Location("Entry opened here", ExpressionUtils.methodName(putNextEntry))), null);
      }
    } else if (WRITE.matches(mit)) {
      pendingEntries.remove(receiver);
    } else {
      clearTrackedSymbolsUsedAsArguments(mit, pendingEntries);
    }
  }

  private static void scanForTrackedSymbolUsage(StatementTree statement, Map<Symbol, MethodInvocationTree> pendingEntries) {
    if (pendingEntries.isEmpty()) {
      return;
    }
    removeUsedSymbols(statement, pendingEntries);
  }

  private static void removeUsedSymbols(Tree tree, Map<Symbol, MethodInvocationTree> pendingEntries) {
    TrackedSymbolVisitor visitor = new TrackedSymbolVisitor(pendingEntries);
    tree.accept(visitor);
    visitor.usedSymbols.forEach(pendingEntries::remove);
  }

  @CheckForNull
  private static MethodInvocationTree extractMethodInvocation(StatementTree statement) {
    if (statement.is(Tree.Kind.EXPRESSION_STATEMENT)) {
      ExpressionTree expression = ((ExpressionStatementTree) statement).expression();
      if (expression.is(Tree.Kind.METHOD_INVOCATION)) {
        return (MethodInvocationTree) expression;
      }
    }
    return null;
  }

  private static boolean isDirectoryEntry(MethodInvocationTree putNextEntry) {
    if (putNextEntry.arguments().size() != 1) {
      return false;
    }
    ExpressionTree argument = putNextEntry.arguments().get(0);
    NewClassTree newClass = resolveNewClass(argument);
    if (newClass != null && newClass.arguments().size() == 1 && newClass.arguments().get(0).is(Tree.Kind.STRING_LITERAL)) {
      String value = ((LiteralTree) newClass.arguments().get(0)).value();
      // value includes quotes, e.g. "\"dir/\""
      return value.endsWith("/\"");
    }
    return false;
  }

  @CheckForNull
  private static NewClassTree resolveNewClass(ExpressionTree expression) {
    if (expression.is(Tree.Kind.NEW_CLASS)) {
      return (NewClassTree) expression;
    }
    if (expression.is(Tree.Kind.IDENTIFIER)) {
      Symbol symbol = ((IdentifierTree) expression).symbol();
      Tree declaration = symbol.declaration();
      if (declaration != null && declaration.is(Tree.Kind.VARIABLE)) {
        ExpressionTree initializer = ((VariableTree) declaration).initializer();
        if (initializer != null && initializer.is(Tree.Kind.NEW_CLASS)) {
          return (NewClassTree) initializer;
        }
      }
    }
    return null;
  }

  @CheckForNull
  private static Symbol extractIdentifierSymbol(ExpressionTree expression) {
    ExpressionTree unwrapped = ExpressionUtils.skipParentheses(expression);
    while (unwrapped.is(Tree.Kind.TYPE_CAST)) {
      unwrapped = ExpressionUtils.skipParentheses(((TypeCastTree) unwrapped).expression());
    }
    if (unwrapped.is(Tree.Kind.IDENTIFIER)) {
      return ((IdentifierTree) unwrapped).symbol();
    }
    return null;
  }

  private static void clearTrackedSymbolsUsedAsArguments(MethodInvocationTree mit, Map<Symbol, MethodInvocationTree> pendingEntries) {
    if (pendingEntries.isEmpty()) {
      return;
    }
    for (ExpressionTree arg : mit.arguments()) {
      Symbol symbol = extractIdentifierSymbol(arg);
      if (symbol != null) {
        pendingEntries.remove(symbol);
      } else {
        ExpressionTree unwrapped = ExpressionUtils.skipParentheses(arg);
        while (unwrapped.is(Tree.Kind.TYPE_CAST)) {
          unwrapped = ExpressionUtils.skipParentheses(((TypeCastTree) unwrapped).expression());
        }
        removeUsedSymbols(unwrapped, pendingEntries);
      }
    }
  }

  @CheckForNull
  private static Symbol getReceiverSymbol(MethodInvocationTree mit) {
    ExpressionTree methodSelect = mit.methodSelect();
    if (methodSelect.is(Tree.Kind.MEMBER_SELECT)) {
      ExpressionTree expression = ((MemberSelectExpressionTree) methodSelect).expression();
      if (expression.is(Tree.Kind.IDENTIFIER) && !((IdentifierTree) expression).symbol().isUnknown()) {
        return ((IdentifierTree) expression).symbol();
      }
    }
    return null;
  }

  private static class TrackedSymbolVisitor extends BaseTreeVisitor {
    private final Map<Symbol, MethodInvocationTree> pendingEntries;
    private final List<Symbol> usedSymbols = new ArrayList<>();

    TrackedSymbolVisitor(Map<Symbol, MethodInvocationTree> pendingEntries) {
      this.pendingEntries = pendingEntries;
    }

    @Override
    public void visitMethodInvocation(MethodInvocationTree mit) {
      Symbol receiver = getReceiverSymbol(mit);
      if (receiver != null && pendingEntries.containsKey(receiver) && WRITE.matches(mit)) {
        usedSymbols.add(receiver);
      }
      collectTrackedArgumentSymbols(mit.arguments());
      super.visitMethodInvocation(mit);
    }

    @Override
    public void visitNewClass(NewClassTree tree) {
      collectTrackedArgumentSymbols(tree.arguments());
      super.visitNewClass(tree);
    }

    private void collectTrackedArgumentSymbols(List<ExpressionTree> arguments) {
      for (ExpressionTree arg : arguments) {
        Symbol argSymbol = extractIdentifierSymbol(arg);
        if (argSymbol != null && pendingEntries.containsKey(argSymbol)) {
          usedSymbols.add(argSymbol);
        }
      }
    }

    @Override
    public void visitIdentifier(IdentifierTree tree) {
      // Intentionally not clearing state for simple identifier references;
      // only method calls (write or passing as argument) should clear state.
    }
  }
}
