package org.sonar.java.ecj;

import com.google.common.collect.Iterators;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodsAreNonnullByDefault;
import org.sonar.plugins.java.api.tree.SynchronizedStatementTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;

import javax.annotation.Nullable;
import java.util.Iterator;

@MethodsAreNonnullByDefault
class ESynchronizedStatement extends EStatement implements SynchronizedStatementTree {
  SyntaxToken synchronizedKeyword;
  ExpressionTree expression;
  SyntaxToken closeParenToken;
  BlockTree block;

  @Override
  public SyntaxToken synchronizedKeyword() {
    return synchronizedKeyword;
  }

  @Override
  public SyntaxToken openParenToken() {
    throw new UnexpectedAccessException();
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
  public BlockTree block() {
    return block;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitSynchronizedStatement(this);
  }

  @Override
  public Kind kind() {
    return Kind.SYNCHRONIZED_STATEMENT;
  }

  @Nullable
  @Override
  public SyntaxToken firstToken() {
    return synchronizedKeyword();
  }

  @Nullable
  @Override
  public SyntaxToken lastToken() {
    return block.lastToken();
  }

  @Override
  Iterator<? extends Tree> childrenIterator() {
    return Iterators.forArray(
      expression(),
      block()
    );
  }
}
