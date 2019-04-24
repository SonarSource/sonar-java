package org.sonar.java.ecj;

import com.google.common.collect.Iterators;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.CatchTree;
import org.sonar.plugins.java.api.tree.MethodsAreNonnullByDefault;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;
import org.sonar.plugins.java.api.tree.VariableTree;

import javax.annotation.Nullable;
import java.util.Iterator;

@MethodsAreNonnullByDefault
class ECatchClause extends ETree implements CatchTree {
  SyntaxToken catchKeyword;
  SyntaxToken openParenToken;
  VariableTree parameter;
  SyntaxToken closeParenToken;
  BlockTree block;

  @Override
  public SyntaxToken catchKeyword() {
    return catchKeyword;
  }

  @Override
  public SyntaxToken openParenToken() {
    return openParenToken;
  }

  @Override
  public VariableTree parameter() {
    return parameter;
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
    visitor.visitCatch(this);
  }

  @Override
  public Kind kind() {
    return Kind.CATCH;
  }

  @Nullable
  @Override
  public SyntaxToken firstToken() {
    return catchKeyword;
  }

  @Nullable
  @Override
  public SyntaxToken lastToken() {
    return block.lastToken();
  }

  @Override
  Iterator<? extends Tree> childrenIterator() {
    return Iterators.forArray(
      catchKeyword,
      openParenToken,
      parameter,
      closeParenToken,
      block
    );
  }
}
