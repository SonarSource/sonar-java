package org.sonar.java.ecj;

import com.google.common.collect.Iterators;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodsAreNonnullByDefault;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.ThrowStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;

import javax.annotation.Nullable;
import java.util.Iterator;

@MethodsAreNonnullByDefault
class EThrowStatement extends EStatement implements ThrowStatementTree {
  SyntaxToken throwKeyword;
  ExpressionTree expression;
  SyntaxToken semicolonToken;

  @Override
  public SyntaxToken throwKeyword() {
    return throwKeyword;
  }

  @Override
  public ExpressionTree expression() {
    return expression;
  }

  @Override
  public SyntaxToken semicolonToken() {
    return semicolonToken;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitThrowStatement(this);
  }

  @Override
  public Kind kind() {
    return Kind.THROW_STATEMENT;
  }

  @Nullable
  @Override
  public SyntaxToken firstToken() {
    return throwKeyword();
  }

  @Nullable
  @Override
  public SyntaxToken lastToken() {
    return semicolonToken();
  }

  @Override
  Iterator<? extends Tree> childrenIterator() {
    return Iterators.forArray(
      throwKeyword(),
      expression(),
      semicolonToken()
    );
  }
}
