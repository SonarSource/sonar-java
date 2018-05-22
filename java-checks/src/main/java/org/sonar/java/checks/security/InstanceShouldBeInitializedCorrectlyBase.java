package org.sonar.java.checks.security;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.util.List;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

public abstract class InstanceShouldBeInitializedCorrectlyBase extends IssuableSubscriptionVisitor {
  private final List<Symbol.VariableSymbol> variablesToFlag = Lists.newArrayList();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.VARIABLE, Tree.Kind.METHOD_INVOCATION);
  }

  protected abstract String getMessage();

  //protected abstract boolean constructorArgumentsHaveExpectedValue(Arguments arguments);

  protected abstract String getMethodName();

  protected abstract boolean methodArgumentsHaveExpectedValue(Arguments arguments);

  protected abstract int getMethodArity();

  protected abstract List<String> getClasses();

  @Override
  public void scanFile(JavaFileScannerContext context) {
    variablesToFlag.clear();
    super.scanFile(context);
    for (Symbol.VariableSymbol var : variablesToFlag) {
      reportIssue(var.declaration().simpleName(), getMessage());
    }
  }

  @Override
  public void visitNode(Tree tree) {
    if (hasSemantic()) {
      if (tree.is(Tree.Kind.VARIABLE)) {
        VariableTree variableTree = (VariableTree) tree;
        addToVariablesToFlag(variableTree);
      } else if (tree.is(Tree.Kind.METHOD_INVOCATION)) {
        MethodInvocationTree mit = (MethodInvocationTree) tree;
        removeFromVariablesToFlagIfInitializedWithMethod(mit);
      }
    }
  }

  private void addToVariablesToFlag(VariableTree variableTree) {
    Type type = variableTree.type().symbolType();
    if (getClasses().stream().anyMatch(type::isSubtypeOf) && isConstructorInitialized(variableTree)) {
      Symbol variableSymbol = variableTree.symbol();
      //Ignore field variables
      if (variableSymbol.isVariableSymbol() && variableSymbol.owner().isMethodSymbol()) {
        variablesToFlag.add((Symbol.VariableSymbol) variableSymbol);
      }
    }
  }

  // TODO for certain classes, must check the parameters of the constructor as well
  private static boolean isConstructorInitialized(VariableTree variableTree) {
    ExpressionTree initializer = variableTree.initializer();
    return initializer != null && initializer.is(Tree.Kind.NEW_CLASS);
  }

  // TODO should check the method vs constructor parameters
  private void removeFromVariablesToFlagIfInitializedWithMethod(MethodInvocationTree mit) {
    if (isInitializedWithMethod(mit) && mit.methodSelect().is(Tree.Kind.MEMBER_SELECT)) {
      MemberSelectExpressionTree mse = (MemberSelectExpressionTree) mit.methodSelect();
      if (mse.expression().is(Tree.Kind.IDENTIFIER)) {
        Symbol reference = ((IdentifierTree) mse.expression()).symbol();
        variablesToFlag.remove(reference);
      }
    }
  }

  private boolean isInitializedWithMethod(MethodInvocationTree mit) {
    Symbol methodSymbol = mit.symbol();
    boolean hasMethodArity = mit.arguments().size() == getMethodArity();
    if (hasMethodArity && isWantedClassMethod(methodSymbol) && methodArgumentsHaveExpectedValue(mit.arguments())) {
      return getMethodName().equals(getIdentifier(mit).name());
    }
    return false;
  }

  private boolean isWantedClassMethod(Symbol methodSymbol) {
    return methodSymbol.isMethodSymbol() && getClasses().stream().anyMatch(methodSymbol.owner().type()::isSubtypeOf);
  }

  private static IdentifierTree getIdentifier(MethodInvocationTree mit) {
    IdentifierTree id;
    if (mit.methodSelect().is(Tree.Kind.IDENTIFIER)) {
      id = (IdentifierTree) mit.methodSelect();
    } else {
      id = ((MemberSelectExpressionTree) mit.methodSelect()).identifier();
    }
    return id;
  }
}
