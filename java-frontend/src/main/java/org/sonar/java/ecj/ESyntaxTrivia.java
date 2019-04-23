package org.sonar.java.ecj;

import com.google.common.collect.Iterators;
import org.sonar.plugins.java.api.tree.MethodsAreNonnullByDefault;
import org.sonar.plugins.java.api.tree.SyntaxTrivia;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;

import java.util.Iterator;

@MethodsAreNonnullByDefault
class ESyntaxTrivia extends ETree implements SyntaxTrivia {
  String comment;
  int line;
  int column;

  @Override
  public String comment() {
    return comment;
  }

  @Override
  public int startLine() {
    return line;
  }

  @Override
  public int column() {
    return column;
  }

  /**
   * @see org.sonar.java.model.InternalSyntaxTrivia#accept(TreeVisitor)
   */
  @Override
  public void accept(TreeVisitor visitor) {
    // nop
  }

  @Override
  public Kind kind() {
    return Kind.TRIVIA;
  }

  @Override
  Iterator<? extends Tree> childrenIterator() {
    return Iterators.forArray();
  }
}
