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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.JavaVersionAwareVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.ForEachStatement;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.IfStatementTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S9391")
public class ForLoopStreamSuggestionCheck extends IssuableSubscriptionVisitor implements JavaVersionAwareVisitor {

  private static final String MESSAGE = "Use a stream instead of this loop.";
  private static final String JAVA_UTIL_COLLECTION = "java.util.Collection";
  private static final Set<String> COLLECTION_API_TYPES = Set.of(
    JAVA_UTIL_COLLECTION,
    "java.util.List",
    "java.util.Set",
    "java.util.Queue",
    "java.util.Deque",
    "java.util.concurrent.BlockingQueue",
    "java.util.concurrent.BlockingDeque");
  private static final MethodMatchers COLLECTION_ADD_MATCHERS = MethodMatchers.create()
    .ofSubTypes(JAVA_UTIL_COLLECTION)
    .names("add", "addLast", "offer", "offerLast")
    .addParametersMatcher(MethodMatchers.ANY)
    .build();

  @Override
  public boolean isCompatibleWithJavaVersion(JavaVersion version) {
    return version.isJava8Compatible();
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.FOR_EACH_STATEMENT);
  }

  @Override
  public void visitNode(Tree tree) {
    ForEachStatement forEach = (ForEachStatement) tree;
    StatementTree body = forEach.statement();
    if (body == null) {
      return;
    }
    StatementTree singleStmt;
    if (body.is(Tree.Kind.BLOCK)) {
      BlockTree loopBody = (BlockTree) body;
      if (loopBody.body().size() != 1) {
        return;
      }
      singleStmt = loopBody.body().get(0);
    } else {
      singleStmt = body;
    }
    Set<Symbol> collectionSymbols = collectCollectionSymbols(forEach);
    if (collectionSymbols.isEmpty()) {
      return;
    }
    excludeSourceSymbol(forEach, collectionSymbols);
    if (collectionSymbols.isEmpty()) {
      return;
    }

    if (singleStmt.is(Tree.Kind.EXPRESSION_STATEMENT)) {
      ExpressionTree expr = ((ExpressionStatementTree) singleStmt).expression();
      if (isCollectionAdd(collectionSymbols, expr)) {
        reportIssue(forEach.forKeyword(), MESSAGE);
      }
    } else if (singleStmt.is(Tree.Kind.IF_STATEMENT)) {
      IfStatementTree ifStmt = (IfStatementTree) singleStmt;
      if (isSimpleFilterCollect(collectionSymbols, ifStmt)) {
        reportIssue(forEach.forKeyword(), MESSAGE);
      }
    }
  }

  private static boolean isSimpleFilterCollect(Set<Symbol> collectionSymbols, IfStatementTree ifStmt) {
    if (ifStmt.elseStatement() != null) {
      return false;
    }
    ExpressionTree addExpr = extractSingleExpression(ifStmt.thenStatement());
    if (addExpr == null) {
      return false;
    }
    return isCollectionAdd(collectionSymbols, addExpr);
  }

  private static ExpressionTree extractSingleExpression(StatementTree stmt) {
    if (stmt.is(Tree.Kind.EXPRESSION_STATEMENT)) {
      return ((ExpressionStatementTree) stmt).expression();
    }
    if (stmt.is(Tree.Kind.BLOCK)) {
      BlockTree block = (BlockTree) stmt;
      if (block.body().size() == 1 && block.body().get(0).is(Tree.Kind.EXPRESSION_STATEMENT)) {
        return ((ExpressionStatementTree) block.body().get(0)).expression();
      }
    }
    return null;
  }

  private static boolean isCollectionAdd(Set<Symbol> collectionSymbols, ExpressionTree expr) {
    if (!expr.is(Tree.Kind.METHOD_INVOCATION)) {
      return false;
    }
    MethodInvocationTree mit = (MethodInvocationTree) expr;
    if (!COLLECTION_ADD_MATCHERS.matches(mit)) {
      return false;
    }
    if (!isInheritedCollectionMethod(mit)) {
      return false;
    }
    return isCollectionTarget(collectionSymbols, mit);
  }

  private static boolean isInheritedCollectionMethod(MethodInvocationTree mit) {
    Symbol.MethodSymbol methodSymbol = mit.methodSymbol();
    if (isCollectionApiType(methodSymbol.owner().type())) {
      return true;
    }
    return methodSymbol.overriddenSymbols().stream()
      .anyMatch(sym -> isCollectionApiType(sym.owner().type()));
  }

  private static boolean isCollectionApiType(Type type) {
    return COLLECTION_API_TYPES.stream().anyMatch(type::is);
  }

  private static Set<Symbol> collectCollectionSymbols(ForEachStatement forEach) {
    Set<Symbol> symbols = new HashSet<>();
    Tree parent = forEach.parent();
    if (!(parent instanceof BlockTree parentBlock)) {
      return symbols;
    }
    for (StatementTree stmt : parentBlock.body()) {
      if (stmt == forEach) {
        break;
      }
      if (stmt.is(Tree.Kind.VARIABLE)) {
        addIfCollection(symbols, (VariableTree) stmt);
      } else {
        excludeMutatedSymbols(symbols, stmt);
      }
    }
    return symbols;
  }

  private static void excludeMutatedSymbols(Set<Symbol> symbols, StatementTree stmt) {
    if (symbols.isEmpty() || !stmt.is(Tree.Kind.EXPRESSION_STATEMENT)) {
      return;
    }
    ExpressionTree expr = ((ExpressionStatementTree) stmt).expression();
    if (!expr.is(Tree.Kind.METHOD_INVOCATION)) {
      return;
    }
    MethodInvocationTree mit = (MethodInvocationTree) expr;
    ExpressionTree methodSelect = mit.methodSelect();
    if (methodSelect.is(Tree.Kind.MEMBER_SELECT)) {
      ExpressionTree target = ((MemberSelectExpressionTree) methodSelect).expression();
      if (target.is(Tree.Kind.IDENTIFIER)) {
        symbols.remove(((IdentifierTree) target).symbol());
      }
    }
  }

  private static void addIfCollection(Set<Symbol> symbols, VariableTree varTree) {
    ExpressionTree initializer = varTree.initializer();
    if (initializer == null) {
      return;
    }
    if (!isFreshEmptyCollection(initializer)) {
      return;
    }
    symbols.add(varTree.symbol());
  }

  private static boolean isFreshEmptyCollection(ExpressionTree expr) {
    if (!expr.is(Tree.Kind.NEW_CLASS)) {
      return false;
    }
    NewClassTree newClass = (NewClassTree) expr;
    if (!expr.symbolType().isSubtypeOf(JAVA_UTIL_COLLECTION)) {
      return false;
    }
    return newClass.arguments().stream()
      .noneMatch(arg -> {
        Type argType = arg.symbolType();
        return argType.isUnknown() || argType.isSubtypeOf(JAVA_UTIL_COLLECTION);
      });
  }

  private static void excludeSourceSymbol(ForEachStatement forEach, Set<Symbol> collectionSymbols) {
    ExpressionTree source = forEach.expression();
    if (source.is(Tree.Kind.IDENTIFIER)) {
      collectionSymbols.remove(((IdentifierTree) source).symbol());
    }
  }

  private static boolean isCollectionTarget(Set<Symbol> collectionSymbols, MethodInvocationTree mit) {
    ExpressionTree methodSelect = mit.methodSelect();
    if (methodSelect.is(Tree.Kind.MEMBER_SELECT)) {
      ExpressionTree target = ((MemberSelectExpressionTree) methodSelect).expression();
      if (target.is(Tree.Kind.IDENTIFIER)) {
        IdentifierTree id = (IdentifierTree) target;
        return collectionSymbols.contains(id.symbol());
      }
    }
    return false;
  }
}
