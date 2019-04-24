package org.sonar.java.ecj;

import com.google.common.collect.Iterators;
import org.sonar.plugins.java.api.tree.DoWhileStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodsAreNonnullByDefault;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;

import javax.annotation.Nullable;
import java.util.Iterator;

@MethodsAreNonnullByDefault
class EDoStatement extends EStatement implements DoWhileStatementTree {
  SyntaxToken doKeyword;
  StatementTree statement;
  SyntaxToken whileKeyword;
  SyntaxToken openParenToken;
  ExpressionTree condition;
  SyntaxToken closeParenToken;
  SyntaxToken semicolonToken;

  @Override
  public SyntaxToken doKeyword() {
    return doKeyword;
  }

  @Override
  public StatementTree statement() {
    return statement;
  }

  @Override
  public SyntaxToken whileKeyword() {
    return whileKeyword;
  }

  @Override
  public SyntaxToken openParenToken() {
    return openParenToken;
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
  public SyntaxToken semicolonToken() {
    return semicolonToken;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitDoWhileStatement(this);
  }

  @Override
  public Kind kind() {
    return Kind.DO_STATEMENT;
  }

  @Nullable
  @Override
  public SyntaxToken firstToken() {
    return doKeyword();
  }

  @Nullable
  @Override
  public SyntaxToken lastToken() {
    return semicolonToken();
  }

  @Override
  Iterator<? extends Tree> childrenIterator() {
    return Iterators.forArray(
      doKeyword(),
      statement(),
      whileKeyword(),
      openParenToken(),
      condition(),
      closeParenToken(),
      semicolonToken()
    );
  }
}
