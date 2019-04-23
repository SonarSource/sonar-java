package org.sonar.java.ecj;

import com.google.common.collect.Iterators;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.ForStatementTree;
import org.sonar.plugins.java.api.tree.ListTree;
import org.sonar.plugins.java.api.tree.MethodsAreNonnullByDefault;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;

import javax.annotation.Nullable;
import java.util.Iterator;

@MethodsAreNonnullByDefault
class EForStatement extends EStatement implements ForStatementTree {
  SyntaxToken forKeyword;
  EList<StatementTree> initializer = new EList<>();
  ExpressionTree condition;
  EList<StatementTree> update = new EList<>();
  SyntaxToken closeParenToken;
  StatementTree statement;

  @Override
  public SyntaxToken forKeyword() {
    return forKeyword;
  }

  @Override
  public SyntaxToken openParenToken() {
    throw new UnexpectedAccessException();
  }

  @Override
  public ListTree<StatementTree> initializer() {
    return initializer;
  }

  @Override
  public SyntaxToken firstSemicolonToken() {
    throw new UnexpectedAccessException();
  }

  @Nullable
  @Override
  public ExpressionTree condition() {
    return condition;
  }

  @Override
  public SyntaxToken secondSemicolonToken() {
    throw new UnexpectedAccessException();
  }

  @Override
  public ListTree<StatementTree> update() {
    return update;
  }

  @Override
  public SyntaxToken closeParenToken() {
    return closeParenToken;
  }

  @Override
  public StatementTree statement() {
    return statement;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitForStatement(this);
  }

  @Override
  public Kind kind() {
    return Kind.FOR_STATEMENT;
  }

  @Nullable
  @Override
  public SyntaxToken firstToken() {
    return forKeyword();
  }

  @Nullable
  @Override
  public SyntaxToken lastToken() {
    return statement().lastToken();
  }

  @Override
  Iterator<? extends Tree> childrenIterator() {
    return Iterators.forArray(
      forKeyword(),
      initializer(),
      condition(),
      update(),
      statement()
    );
  }
}
