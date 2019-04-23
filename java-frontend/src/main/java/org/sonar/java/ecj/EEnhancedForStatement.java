package org.sonar.java.ecj;

import com.google.common.collect.Iterators;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.ForEachStatement;
import org.sonar.plugins.java.api.tree.MethodsAreNonnullByDefault;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;
import org.sonar.plugins.java.api.tree.VariableTree;

import javax.annotation.Nullable;
import java.util.Iterator;

@MethodsAreNonnullByDefault
class EEnhancedForStatement extends EStatement implements ForEachStatement {
  SyntaxToken forKeyword;
  VariableTree variable;
  ExpressionTree expression;
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
  public VariableTree variable() {
    return variable;
  }

  @Override
  public SyntaxToken colonToken() {
    throw new UnexpectedAccessException();
  }

  @Override
  public ExpressionTree expression() {
    return expression;
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
    visitor.visitForEachStatement(this);
  }

  @Override
  public Kind kind() {
    return Kind.FOR_EACH_STATEMENT;
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
      variable(),
      expression(),
      statement()
    );
  }
}
