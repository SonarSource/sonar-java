package org.sonar.java.ecj;

import com.google.common.collect.Iterators;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IfStatementTree;
import org.sonar.plugins.java.api.tree.MethodsAreNonnullByDefault;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;

import javax.annotation.Nullable;
import java.util.Iterator;

@MethodsAreNonnullByDefault
class EIfStatement extends EStatement implements IfStatementTree {
  SyntaxToken ifKeyword;
  ExpressionTree condition;
  SyntaxToken closeParenToken;
  StatementTree thenStatement;
  SyntaxToken elseKeyword;
  StatementTree elseStatement;

  @Override
  public SyntaxToken ifKeyword() {
    return ifKeyword;
  }

  @Override
  public SyntaxToken openParenToken() {
    throw new UnexpectedAccessException();
  }

  @Override
  public ExpressionTree condition() {
    return condition;
  }

  @Override
  public SyntaxToken closeParenToken() {
    return closeParenToken;
  }

  @Override
  public StatementTree thenStatement() {
    return thenStatement;
  }

  @Nullable
  @Override
  public SyntaxToken elseKeyword() {
    return elseKeyword;
  }

  @Nullable
  @Override
  public StatementTree elseStatement() {
    return elseStatement;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitIfStatement(this);
  }

  @Override
  public Kind kind() {
    return Kind.IF_STATEMENT;
  }

  @Nullable
  @Override
  public SyntaxToken firstToken() {
    return ifKeyword;
  }

  @Nullable
  @Override
  public SyntaxToken lastToken() {
    if (elseStatement != null) {
      return elseStatement.lastToken();
    }
    return thenStatement.lastToken();
  }

  @Override
  Iterator<? extends Tree> childrenIterator() {
    return Iterators.forArray(
      condition,
      thenStatement,
      elseStatement
    );
  }
}
