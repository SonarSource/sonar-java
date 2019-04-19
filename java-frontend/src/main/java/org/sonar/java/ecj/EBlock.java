package org.sonar.java.ecj;

import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.MethodsAreNonnullByDefault;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@MethodsAreNonnullByDefault
public class EBlock extends EStatement implements BlockTree {
  Kind kind = Tree.Kind.BLOCK;
  SyntaxToken openBraceToken;
  List<StatementTree> body = new ArrayList<>();
  SyntaxToken closeBraceToken;

  @Override
  public SyntaxToken openBraceToken() {
    return openBraceToken;
  }

  @Override
  public List<StatementTree> body() {
    return body;
  }

  @Override
  public SyntaxToken closeBraceToken() {
    return closeBraceToken;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitBlock(this);
  }

  @Override
  public Kind kind() {
    return kind;
  }

  @Nullable
  @Override
  public SyntaxToken firstToken() {
    return openBraceToken();
  }

  @Nullable
  @Override
  public SyntaxToken lastToken() {
    return closeBraceToken();
  }

  @Override
  Iterator<? extends Tree> childrenIterator() {
    return body.iterator();
  }
}
