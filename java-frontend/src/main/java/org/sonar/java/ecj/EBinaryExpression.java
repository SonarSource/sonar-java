package org.sonar.java.ecj;

import com.google.common.collect.Iterators;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodsAreNonnullByDefault;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;

import javax.annotation.Nullable;
import java.util.Iterator;

@MethodsAreNonnullByDefault
class EBinaryExpression extends EExpression implements BinaryExpressionTree {
  Kind kind;
  ExpressionTree leftOperand;
  SyntaxToken operatorToken;
  ExpressionTree rightOperand;

  @Override
  public ExpressionTree leftOperand() {
    return leftOperand;
  }

  @Override
  public SyntaxToken operatorToken() {
    return operatorToken;
  }

  @Override
  public ExpressionTree rightOperand() {
    return rightOperand;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitBinaryExpression(this);
  }

  @Override
  public Kind kind() {
    return kind;
  }

  @Nullable
  @Override
  public SyntaxToken firstToken() {
    return leftOperand.firstToken();
  }

  @Nullable
  @Override
  public SyntaxToken lastToken() {
    return rightOperand.lastToken();
  }

  @Override
  Iterator<? extends Tree> childrenIterator() {
    return Iterators.forArray(
      leftOperand(),
      operatorToken(),
      rightOperand()
    );
  }
}
