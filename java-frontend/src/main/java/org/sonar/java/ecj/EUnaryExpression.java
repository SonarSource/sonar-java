package org.sonar.java.ecj;

import com.google.common.collect.Iterators;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodsAreNonnullByDefault;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;
import org.sonar.plugins.java.api.tree.UnaryExpressionTree;

import javax.annotation.Nullable;
import java.util.Iterator;

@MethodsAreNonnullByDefault
abstract class EUnaryExpression extends EExpression implements UnaryExpressionTree {
  Kind kind;
  SyntaxToken operatorToken;
  ExpressionTree expression;

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
    visitor.visitUnaryExpression(this);
  }

  @Override
  public Kind kind() {
    return kind;
  }

  @Override
  Iterator<? extends Tree> childrenIterator() {
    return Iterators.singletonIterator(
      expression()
    );
  }

  static class Prefix extends EUnaryExpression {
    @Nullable
    @Override
    public SyntaxToken firstToken() {
      return operatorToken();
    }

    @Nullable
    @Override
    public SyntaxToken lastToken() {
      return expression().lastToken();
    }
  }

  static class Postfix extends EUnaryExpression {
    @Nullable
    @Override
    public SyntaxToken firstToken() {
      return expression().firstToken();
    }

    @Nullable
    @Override
    public SyntaxToken lastToken() {
      return operatorToken();
    }
  }
}
