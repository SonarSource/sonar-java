package org.sonar.java.ecj;

import com.google.common.collect.Iterators;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.CatchTree;
import org.sonar.plugins.java.api.tree.ListTree;
import org.sonar.plugins.java.api.tree.MethodsAreNonnullByDefault;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;
import org.sonar.plugins.java.api.tree.TryStatementTree;
import org.sonar.plugins.java.api.tree.VariableTree;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@MethodsAreNonnullByDefault
class ETryStatement extends EStatement implements TryStatementTree {
  SyntaxToken tryKeyword;
  BlockTree body;
  List<CatchTree> catches = new ArrayList<>();
  BlockTree finallyBlock;

  @Override
  public SyntaxToken tryKeyword() {
    return tryKeyword;
  }

  @Nullable
  @Override
  public SyntaxToken openParenToken() {
    throw new UnexpectedAccessException();
  }

  @Override
  public ListTree<VariableTree> resources() {
    throw new UnsupportedOperationException();
  }

  @Override
  public ListTree<Tree> resourceList() {
    // FIXME
    return new EList<>();
  }

  @Nullable
  @Override
  public SyntaxToken closeParenToken() {
    throw new UnsupportedOperationException();
  }

  @Override
  public BlockTree block() {
    return body;
  }

  @Override
  public List<CatchTree> catches() {
    return catches;
  }

  @Nullable
  @Override
  public SyntaxToken finallyKeyword() {
    throw new UnsupportedOperationException();
  }

  @Nullable
  @Override
  public BlockTree finallyBlock() {
    return finallyBlock;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitTryStatement(this);
  }

  @Override
  public Kind kind() {
    return Kind.TRY_STATEMENT;
  }

  @Nullable
  @Override
  public SyntaxToken firstToken() {
    return tryKeyword();
  }

  @Nullable
  @Override
  public SyntaxToken lastToken() {
    if (finallyBlock != null) {
      return finallyBlock.lastToken();
    }
    if (!catches.isEmpty()) {
      return catches.get(catches.size() - 1).lastToken();
    }
    return body.lastToken();
  }

  @Override
  Iterator<? extends Tree> childrenIterator() {
    return Iterators.concat(
      Iterators.singletonIterator(block()),
      catches().iterator(),
      Iterators.singletonIterator(finallyBlock())
    );
  }
}
