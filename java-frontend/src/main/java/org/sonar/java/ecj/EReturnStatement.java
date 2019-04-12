package org.sonar.java.ecj;

import com.google.common.collect.Iterators;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodsAreNonnullByDefault;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;

import javax.annotation.Nullable;
import java.util.Iterator;

@MethodsAreNonnullByDefault
class EReturnStatement extends EStatement implements ReturnStatementTree {

  SyntaxToken returnKeyword;
  ExpressionTree expression;
  SyntaxToken semicolonToken;

  @Override
  public SyntaxToken returnKeyword() {
    return returnKeyword;
  }

  @Nullable
  @Override
  public ExpressionTree expression() {
    return expression;
  }

  @Override
  public SyntaxToken semicolonToken() {
    return semicolonToken;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitReturnStatement(this);
  }

  @Override
  public Kind kind() {
    return Kind.RETURN_STATEMENT;
  }

  @Nullable
  @Override
  public SyntaxToken firstToken() {
    return returnKeyword();
  }

  @Nullable
  @Override
  public SyntaxToken lastToken() {
    return semicolonToken();
  }

  @Override
  Iterator<? extends Tree> childrenIterator() {
    return Iterators.singletonIterator(
      expression()
    );
  }
}
