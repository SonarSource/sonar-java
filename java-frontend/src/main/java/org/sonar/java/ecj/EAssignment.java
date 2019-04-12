package org.sonar.java.ecj;

import com.google.common.collect.Iterators;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodsAreNonnullByDefault;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;

import javax.annotation.Nullable;
import java.util.Iterator;

@MethodsAreNonnullByDefault
class EAssignment extends EExpression implements AssignmentExpressionTree {

  Kind kind;
  ExpressionTree variable;
  SyntaxToken operatorToken;
  ExpressionTree expression;

  @Override
  public ExpressionTree variable() {
    return variable;
  }

  @Override
  public SyntaxToken operatorToken() {
    return operatorToken;
  }

  @Override
  public ExpressionTree expression() {
    return expression;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitAssignmentExpression(this);
  }

  @Override
  public Kind kind() {
    return kind;
  }

  @Nullable
  @Override
  public SyntaxToken firstToken() {
    return variable.firstToken();
  }

  @Nullable
  @Override
  public SyntaxToken lastToken() {
    return expression.lastToken();
  }

  @Override
  Iterator<? extends Tree> childrenIterator() {
    return Iterators.forArray(
      variable(),
      expression()
    );
  }
}
