package org.sonar.java.ecj;

import com.google.common.collect.Iterators;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.ArrayAccessExpressionTree;
import org.sonar.plugins.java.api.tree.ArrayDimensionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodsAreNonnullByDefault;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

@MethodsAreNonnullByDefault
class EArrayAccess extends EExpression implements ArrayAccessExpressionTree {

  ExpressionTree expression;
  EArrayDimension dimension = new EArrayDimension();

  @Override
  public ExpressionTree expression() {
    return expression;
  }

  @Override
  public ArrayDimensionTree dimension() {
    return dimension;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitArrayAccessExpression(this);
  }

  @Override
  public Kind kind() {
    return Kind.ARRAY_ACCESS_EXPRESSION;
  }

  @Override
  Iterator<? extends Tree> childrenIterator() {
    return Iterators.forArray(
      expression(),
      dimension()
    );
  }

  @Nullable
  @Override
  public SyntaxToken firstToken() {
    return expression().firstToken();
  }

  @Nullable
  @Override
  public SyntaxToken lastToken() {
    return dimension().lastToken();
  }

  static class EArrayDimension extends ETree implements ArrayDimensionTree {
    ExpressionTree expression;
    SyntaxToken closeBracketToken;

    @Override
    public List<AnnotationTree> annotations() {
      // FIXME
      return Collections.emptyList();
    }

    @Override
    public SyntaxToken openBracketToken() {
      throw new UnexpectedAccessException();
    }

    @Nullable
    @Override
    public ExpressionTree expression() {
      return expression;
    }

    @Override
    public SyntaxToken closeBracketToken() {
      return closeBracketToken;
    }

    @Override
    public void accept(TreeVisitor visitor) {
      visitor.visitArrayDimension(this);
    }

    @Override
    public Kind kind() {
      return Kind.ARRAY_DIMENSION;
    }

    @Nullable
    @Override
    public SyntaxToken firstToken() {
      return expression.firstToken();
    }

    @Nullable
    @Override
    public SyntaxToken lastToken() {
      return closeBracketToken();
    }

    @Override
    Iterator<? extends Tree> childrenIterator() {
      return Iterators.singletonIterator(
        expression()
      );
    }
  }

}
