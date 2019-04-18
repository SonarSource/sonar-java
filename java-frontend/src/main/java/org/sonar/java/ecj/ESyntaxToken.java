package org.sonar.java.ecj;

import com.google.common.collect.Iterators;
import org.sonar.plugins.java.api.tree.MethodsAreNonnullByDefault;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.SyntaxTrivia;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@MethodsAreNonnullByDefault
class ESyntaxToken extends ETree implements SyntaxToken {
  private int line;
  private int column;
  private String text;
  List<SyntaxTrivia> trivias = new ArrayList<>();

  ESyntaxToken(int line, int column, String text) {
    this.line = line;
    this.column = column;
    this.text = text;
  }

  @Override
  public String text() {
    return text;
  }

  @Override
  public List<SyntaxTrivia> trivias() {
    return trivias;
  }

  @Override
  public int line() {
    return line;
  }

  @Override
  public int column() {
    return column;
  }

  /**
   * @see org.sonar.java.model.InternalSyntaxToken#accept(TreeVisitor)
   */
  @Override
  public void accept(TreeVisitor visitor) {
    // nop
  }

  @Override
  public Kind kind() {
    return Kind.TOKEN;
  }

  @Nullable
  @Override
  public SyntaxToken firstToken() {
    return this;
  }

  @Nullable
  @Override
  public SyntaxToken lastToken() {
    return this;
  }

  @Override
  Iterator<? extends Tree> childrenIterator() {
    return Iterators.forArray();
  }
}
