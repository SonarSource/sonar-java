/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
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
package org.sonar.java.npe;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.sonar.java.resolve.JavaSymbol;
import org.sonar.java.symexecengine.DataFlowVisitor;
import org.sonar.java.symexecengine.State;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.ArrayAccessExpressionTree;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.SwitchStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

public class NpeVisitor extends DataFlowVisitor {

  private Map<Tree, String> parameterErrorsByMethodName = Maps.newHashMap();

  public NpeVisitor(List<VariableTree> parameters) {
    for (VariableTree parameter : parameters) {
      super.visitVariable(parameter);
      State state;
      if (parameter.symbol().metadata().isAnnotatedWith("javax.annotation.CheckForNull") || parameter.symbol().metadata().isAnnotatedWith("javax.annotation.Nullable")) {
        state = new NPEState.Null(parameter);
      } else {
        state = new NPEState.NotNull(parameter);
      }
      executionState.markValueAs(parameter.symbol(), state);
    }
  }

  @Override
  protected boolean isSymbolRelevant(Symbol symbol) {
    return true;
  }

  @Override
  public void visitAssignmentExpression(AssignmentExpressionTree tree) {
    super.visitAssignmentExpression(tree);
    Symbol symbol = getSymbol(tree.variable());
    if (symbol != null && symbol.owner().isMethodSymbol()) {
      setStateOfValue(symbol, tree.expression());
    }
  }

  @Override
  public void visitVariable(VariableTree tree) {
    super.visitVariable(tree);
    setStateOfValue(tree.symbol(), tree.initializer());
  }

  @Override
  public void visitArrayAccessExpression(ArrayAccessExpressionTree tree) {
    super.visitArrayAccessExpression(tree);
    if (!tree.expression().is(Tree.Kind.MEMBER_SELECT) && isNullTree(tree.expression())) {
      executionState.reportIssue(tree);
    }
  }

  private void setStateOfValue(Symbol symbol, @Nullable ExpressionTree tree) {
    if (tree == null || isNullTree(tree)) {
      executionState.markValueAs(symbol, new NPEState.Null(tree));
    } else {
      executionState.markValueAs(symbol, new NPEState.NotNull(tree));
    }
  }

  private boolean isNullTree(ExpressionTree tree) {
    Symbol symbol = getSymbol(tree);
    return (symbol != null && executionState.hasState(symbol, NPEState.Null.class)) ||
      tree.is(Tree.Kind.NULL_LITERAL) ||
      (tree.is(Tree.Kind.METHOD_INVOCATION) && ((MethodInvocationTree) tree).symbol().metadata().isAnnotatedWith("javax.annotation.CheckForNull"));
  }

  @Override
  public void visitMemberSelectExpression(MemberSelectExpressionTree tree) {
    super.visitMemberSelectExpression(tree);
    if (isNullTree(tree.expression())) {
      executionState.reportIssue(tree.expression());
    }
  }

  @Override
  public void visitMethodInvocation(MethodInvocationTree tree) {
    super.visitMethodInvocation(tree);
    if (tree.symbol().isMethodSymbol()) {
      JavaSymbol.MethodJavaSymbol methodSymbol = (JavaSymbol.MethodJavaSymbol) tree.symbol();
      List<JavaSymbol> parameters = methodSymbol.getParameters().scopeSymbols();
      if (!parameters.isEmpty()) {
        for (int i = 0; i < tree.arguments().size(); i += 1) {
          if (parameters.get(i < parameters.size() ? i : parameters.size() - 1).metadata().isAnnotatedWith("javax.annotation.Nonnull")
            && isNullTree(tree.arguments().get(i))) {
            executionState.reportIssue(tree.arguments().get(i));
            parameterErrorsByMethodName.put(tree.arguments().get(i), methodSymbol.name());
          }
        }
      }
    }
  }

  @Override
  public void visitSwitchStatement(SwitchStatementTree tree) {
    if (isNullTree(tree.expression())) {
      executionState.reportIssue(tree.expression());
    }
    super.visitSwitchStatement(tree);
  }

  @Override
  protected void evaluateConditionToTrue(ExpressionTree condition) {
    if (condition.is(Tree.Kind.EQUAL_TO, Tree.Kind.NOT_EQUAL_TO)) {
      BinaryExpressionTree equal = (BinaryExpressionTree) condition;
      Symbol symbol = null;
      if (isNullTree(equal.leftOperand())) {
        symbol = getSymbol(equal.rightOperand());
      }
      if (symbol == null && isNullTree(equal.rightOperand())) {
        symbol = getSymbol(equal.leftOperand());
      }
      if (symbol != null) {
        State state;
        if (condition.is(Tree.Kind.EQUAL_TO)) {
          state = new NPEState.Null(condition);
        } else {
          state = new NPEState.NotNull(condition);
        }
        executionState.markValueAs(symbol, state);
      }
    }

  }

  @Override
  protected void evaluateConditionToFalse(ExpressionTree condition) {
    if (condition.is(Tree.Kind.EQUAL_TO, Tree.Kind.NOT_EQUAL_TO)) {
      BinaryExpressionTree equal = (BinaryExpressionTree) condition;
      Symbol symbol = null;
      if (isNullTree(equal.leftOperand())) {
        symbol = getSymbol(equal.rightOperand());
      }
      if (symbol == null && isNullTree(equal.rightOperand())) {
        symbol = getSymbol(equal.leftOperand());
      }
      if (symbol != null) {
        State state;
        if (condition.is(Tree.Kind.EQUAL_TO)) {
          state = new NPEState.NotNull(condition);
        } else {
          state = new NPEState.Null(condition);
        }
        executionState.markValueAs(symbol, state);
      }
    }
  }

  @CheckForNull
  public String isTreeMethodParam(Tree issueTree) {
    return parameterErrorsByMethodName.get(issueTree);
  }

  private abstract static class NPEState extends State {
    public NPEState(Tree tree) {
      super(tree);
    }

    public NPEState(List<Tree> trees) {
      super(trees);
    }

    private static class Null extends NPEState {

      public Null(Tree tree) {
        super(tree);
      }

      public Null(List<Tree> trees) {
        super(trees);
      }

      @Override
      public State merge(State s) {
        if (s instanceof Null) {
          List<Tree> trees = Lists.newArrayList(s.reportingTrees());
          trees.addAll(reportingTrees());
          return new Null(trees);
        }
        return new Unknown(s.reportingTrees());
      }
    }

    private static class NotNull extends NPEState {

      public NotNull(Tree tree) {
        super(tree);
      }

      public NotNull(List<Tree> trees) {
        super(trees);
      }

      @Override
      public State merge(State s) {
        if (s instanceof NotNull) {
          List<Tree> trees = Lists.newArrayList(s.reportingTrees());
          trees.addAll(reportingTrees());
          return new NotNull(trees);
        }
        return new Unknown(s.reportingTrees());
      }
    }

    private static class Unknown extends NPEState {

      public Unknown(List<Tree> trees) {
        super(trees);
      }

      @Override
      public State merge(State s) {
        return this;
      }
    }

    @Override
    public String toString() {
      return getClass().getSimpleName() + " From : " + Joiner.on(",").join(reportingTrees()) + " ";
    }
  }
}
