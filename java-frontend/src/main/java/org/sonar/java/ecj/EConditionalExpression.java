package org.sonar.java.ecj;

import com.google.common.collect.Iterators;
import org.sonar.plugins.java.api.tree.ConditionalExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodsAreNonnullByDefault;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;

import javax.annotation.Nullable;
import java.util.Iterator;

@MethodsAreNonnullByDefault
class EConditionalExpression extends EExpression implements ConditionalExpressionTree {
  ExpressionTree condition;
  SyntaxToken questionToken;
  ExpressionTree trueExpression;
  SyntaxToken colonToken;
  ExpressionTree falseExpression;

  @Override
  public ExpressionTree condition() {
    return condition;
  }

  @Override
  public SyntaxToken questionToken() {
    return questionToken;
  }

  @Override
  public ExpressionTree trueExpression() {
    return trueExpression;
  }

  @Override
  public SyntaxToken colonToken() {
    return colonToken;
  }

  @Override
  public ExpressionTree falseExpression() {
    return falseExpression;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitConditionalExpression(this);
  }

  @Override
  public Kind kind() {
    return Kind.CONDITIONAL_EXPRESSION;
  }

  @Nullable
  @Override
  public SyntaxToken firstToken() {
    return condition.firstToken();
  }

  @Nullable
  @Override
  public SyntaxToken lastToken() {
    return falseExpression.lastToken();
  }

  @Override
  Iterator<? extends Tree> childrenIterator() {
    return Iterators.forArray(
      condition(),
      trueExpression(),
      falseExpression()
    );
  }
}
