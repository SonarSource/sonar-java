package org.sonar.java.ecj;

import com.google.common.collect.Iterators;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.ListTree;
import org.sonar.plugins.java.api.tree.MethodsAreNonnullByDefault;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;
import org.sonar.plugins.java.api.tree.TypeCastTree;
import org.sonar.plugins.java.api.tree.TypeTree;

import javax.annotation.Nullable;
import java.util.Iterator;

@MethodsAreNonnullByDefault
class ECastExpression extends EExpression implements TypeCastTree {
  SyntaxToken openParenToken;
  TypeTree type;
  ExpressionTree expression;

  @Override
  public SyntaxToken openParenToken() {
    return openParenToken;
  }

  @Override
  public TypeTree type() {
    return type;
  }

  @Nullable
  @Override
  public SyntaxToken andToken() {
    throw new UnexpectedAccessException();
  }

  @Override
  public ListTree<Tree> bounds() {
    throw new UnexpectedAccessException();
  }

  @Override
  public SyntaxToken closeParenToken() {
    throw new UnexpectedAccessException();
  }

  @Override
  public ExpressionTree expression() {
    return expression;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitTypeCast(this);
  }

  @Override
  public Kind kind() {
    return Kind.TYPE_CAST;
  }

  @Nullable
  @Override
  public SyntaxToken firstToken() {
    return openParenToken();
  }

  @Nullable
  @Override
  public SyntaxToken lastToken() {
    return expression().lastToken();
  }

  @Override
  Iterator<? extends Tree> childrenIterator() {
    return Iterators.forArray(
      openParenToken(),
      type(),
      // TODO closeParenToken(),
      expression()
    );
  }
}
