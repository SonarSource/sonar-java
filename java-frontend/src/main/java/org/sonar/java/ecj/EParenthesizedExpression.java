package org.sonar.java.ecj;

import com.google.common.collect.Iterators;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodsAreNonnullByDefault;
import org.sonar.plugins.java.api.tree.ParenthesizedTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;

import javax.annotation.Nullable;
import java.util.Iterator;

@MethodsAreNonnullByDefault
class EParenthesizedExpression extends EExpression implements ParenthesizedTree {
  SyntaxToken openParenToken;
  ExpressionTree expression;
  SyntaxToken closeParenToken;

  @Override
  public SyntaxToken openParenToken() {
    return openParenToken;
  }

  @Override
  public ExpressionTree expression() {
    return expression;
  }

  @Override
  public SyntaxToken closeParenToken() {
    return closeParenToken;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitParenthesized(this);
  }

  @Override
  public Kind kind() {
    return Kind.PARENTHESIZED_EXPRESSION;
  }

  @Nullable
  @Override
  public SyntaxToken firstToken() {
    return openParenToken();
  }

  @Nullable
  @Override
  public SyntaxToken lastToken() {
    return closeParenToken();
  }

  @Override
  Iterator<? extends Tree> childrenIterator() {
    return Iterators.singletonIterator(
      expression()
    );
  }
}
