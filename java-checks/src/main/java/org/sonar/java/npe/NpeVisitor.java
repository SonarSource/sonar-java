package org.sonar.java.npe;

import com.google.common.collect.Lists;
import org.sonar.java.resolve.JavaSymbol;
import org.sonar.java.symexecengine.DataFlowVisitor;
import org.sonar.java.symexecengine.State;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.ArrayAccessExpressionTree;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import javax.annotation.Nullable;

import java.util.List;

public class NpeVisitor extends DataFlowVisitor {

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
      executionState.reportIssue(tree);
    }
  }

  @Override
  public void visitMethodInvocation(MethodInvocationTree tree) {
    super.visitMethodInvocation(tree);
    if(tree.symbol().isMethodSymbol()) {
      JavaSymbol.MethodJavaSymbol methodSymbol = (JavaSymbol.MethodJavaSymbol) tree.symbol();
      List<JavaSymbol> parameters = methodSymbol.getParameters().scopeSymbols();
      if (!parameters.isEmpty()) {
        for (int i = 0; i < tree.arguments().size(); i += 1) {
          if(parameters.get(i < parameters.size() ? i : parameters.size() - 1).metadata().isAnnotatedWith("javax.annotation.Nonnull")
              && isNullTree(tree.arguments().get(i))) {
            executionState.reportIssue(tree.arguments().get(i));
          }
        }
      }
    }
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
        if (s instanceof com.sun.istack.internal.NotNull) {
          return new Unknown(s.reportingTrees());
        }
        if (s instanceof Null) {
          List<Tree> trees = Lists.newArrayList(s.reportingTrees());
          trees.addAll(reportingTrees());
          return new Null(trees);
        }
        return s;
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
        if (s instanceof Null) {
          return new Unknown(s.reportingTrees());
        }
        return s;
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

  }
}
