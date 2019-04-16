package org.sonar.java.ecj;

import com.google.common.collect.Iterators;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MethodsAreNonnullByDefault;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;

import javax.annotation.Nullable;
import java.util.Iterator;

@MethodsAreNonnullByDefault
class ELiteral extends EExpression implements LiteralTree {
  Kind kind;
  SyntaxToken token;

  @Override
  public String value() {
    return token().text();
  }

  @Override
  public SyntaxToken token() {
    return token;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitLiteral(this);
  }

  @Override
  public Kind kind() {
    return kind;
  }

  @Nullable
  @Override
  public SyntaxToken firstToken() {
    return token();
  }

  @Nullable
  @Override
  public SyntaxToken lastToken() {
    return token();
  }

  @Override
  Iterator<? extends Tree> childrenIterator() {
    return Iterators.forArray(
      token()
    );
  }
}
