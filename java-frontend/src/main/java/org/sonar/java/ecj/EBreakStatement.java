package org.sonar.java.ecj;

import com.google.common.collect.Iterators;
import org.sonar.plugins.java.api.tree.BreakStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodsAreNonnullByDefault;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;

import javax.annotation.Nullable;
import java.util.Iterator;

@MethodsAreNonnullByDefault
class EBreakStatement extends EStatement implements BreakStatementTree {
  SyntaxToken breakKeyword;
  ExpressionTree labelOrValue;
  SyntaxToken semicolonToken;

  @Override
  public SyntaxToken breakKeyword() {
    return breakKeyword;
  }

  @Nullable
  @Override
  public IdentifierTree label() {
    return labelOrValue instanceof IdentifierTree ? (IdentifierTree) labelOrValue : null;
  }

  @Nullable
  @Override
  public ExpressionTree value() {
    return labelOrValue;
  }

  @Override
  public SyntaxToken semicolonToken() {
    return semicolonToken;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitBreakStatement(this);
  }

  @Override
  public Kind kind() {
    return Kind.BREAK_STATEMENT;
  }

  @Nullable
  @Override
  public SyntaxToken firstToken() {
    return breakKeyword();
  }

  @Nullable
  @Override
  public SyntaxToken lastToken() {
    return semicolonToken();
  }

  @Override
  Iterator<? extends Tree> childrenIterator() {
    return Iterators.forArray(
      breakKeyword,
      labelOrValue,
      semicolonToken()
    );
  }
}
