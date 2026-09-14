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
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.JavaVersionAwareVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.ForEachStatement;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.IfStatementTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S9391")
public class ForLoopStreamSuggestionCheck extends IssuableSubscriptionVisitor implements JavaVersionAwareVisitor {

  private static final String MESSAGE = "Use a stream instead of this loop.";
  private static final Set<String> ADD_METHODS = Set.of("add", "addFirst", "addLast", "offer", "offerFirst", "offerLast");

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

    BlockTree loopBody = toBlock(body);
    if (loopBody == null || loopBody.body().size() != 1) {
      return;
    }

    StatementTree singleStmt = loopBody.body().get(0);
    Set<Symbol> collectionSymbols = collectCollectionSymbols(forEach);
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
    StatementTree thenBranch = ifStmt.thenStatement();
    if (thenBranch == null) {
      return false;
    }

    ExpressionTree addExpr = extractSingleExpression(thenBranch);
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
    if (!isAddMethod(mit)) {
      return false;
    }
    return isCollectionTarget(collectionSymbols, mit);
  }

  private Set<Symbol> collectCollectionSymbols(ForEachStatement forEach) {
    Set<Symbol> symbols = new HashSet<>();
    Tree parent = forEach.parent();
    if (!(parent instanceof BlockTree parentBlock)) {
      return symbols;
    }
    Set<Tree> loopBodyTrees = collectLoopBodyVariables(forEach);
    for (StatementTree stmt : parentBlock.body()) {
      if (stmt == forEach) {
        break;
      }
      if (stmt.is(Tree.Kind.VARIABLE)) {
        VariableTree varTree = (VariableTree) stmt;
        addIfCollection(symbols, varTree, loopBodyTrees);
      }
    }
    return symbols;
  }

  private static Set<Tree> collectLoopBodyVariables(ForEachStatement forEach) {
    Set<Tree> vars = new HashSet<>();
    forEach.statement().accept(new BaseTreeVisitor() {
      @Override
      public void visitVariable(VariableTree tree) {
        vars.add(tree);
        super.visitVariable(tree);
      }
    });
    return vars;
  }

  private static void addIfCollection(Set<Symbol> symbols, VariableTree varTree, Set<Tree> loopBodyTrees) {
    if (loopBodyTrees.contains(varTree)) {
      return;
    }
    ExpressionTree initializer = varTree.initializer();
    if (initializer == null) {
      return;
    }
    Symbol symbol = varTree.symbol();
    Type type = initializer.symbolType();
    if (isCollectionType(type)) {
      symbols.add(symbol);
    }
  }

  private static BlockTree toBlock(StatementTree stmt) {
    if (stmt.is(Tree.Kind.BLOCK)) {
      return (BlockTree) stmt;
    }
    return null;
  }

  private static boolean isCollectionType(Type type) {
    if (type == null || type.isUnknown()) {
      return false;
    }
    return type.isSubtypeOf("java.util.Collection");
  }

  private static boolean isAddMethod(MethodInvocationTree mit) {
    String methodName = ExpressionUtils.methodName(mit).name();
    return ADD_METHODS.contains(methodName);
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
