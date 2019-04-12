package org.sonar.java.ecj;

import com.google.common.collect.Iterators;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LabeledStatementTree;
import org.sonar.plugins.java.api.tree.MethodsAreNonnullByDefault;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;

import javax.annotation.Nullable;
import java.util.Iterator;

@MethodsAreNonnullByDefault
class ELabeledStatement extends EStatement implements LabeledStatementTree {
  IdentifierTree label;
  SyntaxToken colonToken;
  StatementTree statement;

  @Override
  public IdentifierTree label() {
    return label;
  }

  @Override
  public SyntaxToken colonToken() {
    return colonToken;
  }

  @Override
  public StatementTree statement() {
    return statement;
  }

  @Override
  public Symbol.LabelSymbol symbol() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitLabeledStatement(this);
  }

  @Override
  public Kind kind() {
    return Kind.LABELED_STATEMENT;
  }

  @Nullable
  @Override
  public SyntaxToken firstToken() {
    return label().firstToken();
  }

  @Nullable
  @Override
  public SyntaxToken lastToken() {
    return statement().lastToken();
  }

  @Override
  Iterator<? extends Tree> childrenIterator() {
    return Iterators.forArray(
      label(),
      statement()
    );
  }
}
