package org.sonar.java.ecj;

import com.google.common.collect.Iterators;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodsAreNonnullByDefault;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;
import org.sonar.plugins.java.api.tree.WhileStatementTree;

import javax.annotation.Nullable;
import java.util.Iterator;

@MethodsAreNonnullByDefault
class EWhileStatement extends EStatement implements WhileStatementTree {
  SyntaxToken whileKeyword;
  SyntaxToken openParenToken;
  ExpressionTree condition;
  SyntaxToken closeParenToken;
  StatementTree statement;

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
  public StatementTree statement() {
    return statement;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitWhileStatement(this);
  }

  @Override
  public Kind kind() {
    return Kind.WHILE_STATEMENT;
  }

  @Nullable
  @Override
  public SyntaxToken firstToken() {
    return whileKeyword();
  }

  @Nullable
  @Override
  public SyntaxToken lastToken() {
    return statement.lastToken();
  }

  @Override
  Iterator<? extends Tree> childrenIterator() {
    return Iterators.forArray(
      whileKeyword(),
      openParenToken(),
      condition(),
      closeParenToken(),
      statement()
    );
  }
}
