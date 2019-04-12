package org.sonar.java.ecj;

import com.google.common.collect.Iterators;
import org.sonar.plugins.java.api.tree.AssertStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodsAreNonnullByDefault;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;

import javax.annotation.Nullable;
import java.util.Iterator;

@MethodsAreNonnullByDefault
class EAssertStatement extends EStatement implements AssertStatementTree {
  SyntaxToken assertKeyword;
  ExpressionTree condition;
  ExpressionTree detail;
  SyntaxToken semicolonToken;

  @Override
  public SyntaxToken assertKeyword() {
    return assertKeyword;
  }

  @Override
  public ExpressionTree condition() {
    return condition;
  }

  @Nullable
  @Override
  public SyntaxToken colonToken() {
    throw new UnexpectedAccessException();
  }

  @Nullable
  @Override
  public ExpressionTree detail() {
    return detail;
  }

  @Override
  public SyntaxToken semicolonToken() {
    return semicolonToken;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitAssertStatement(this);
  }

  @Override
  public Kind kind() {
    return Kind.ASSERT_STATEMENT;
  }

  @Nullable
  @Override
  public SyntaxToken firstToken() {
    return assertKeyword();
  }

  @Nullable
  @Override
  public SyntaxToken lastToken() {
    return semicolonToken;
  }

  @Override
  Iterator<? extends Tree> childrenIterator() {
    return Iterators.forArray(
      condition()
    );
  }
}
