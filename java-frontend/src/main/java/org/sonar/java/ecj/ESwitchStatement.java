package org.sonar.java.ecj;

import com.google.common.collect.Iterators;
import org.sonar.plugins.java.api.tree.CaseGroupTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodsAreNonnullByDefault;
import org.sonar.plugins.java.api.tree.SwitchExpressionTree;
import org.sonar.plugins.java.api.tree.SwitchStatementTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.List;

@MethodsAreNonnullByDefault
class ESwitchStatement extends EStatement implements SwitchStatementTree {
  ESwitchExpression switchExpression = new ESwitchExpression();

  @Override
  public SyntaxToken switchKeyword() {
    return switchExpression.switchKeyword();
  }

  @Override
  public SyntaxToken openParenToken() {
    return switchExpression.openParenToken();
  }

  @Override
  public ExpressionTree expression() {
    return switchExpression.expression();
  }

  @Override
  public SyntaxToken closeParenToken() {
    return switchExpression.closeParenToken();
  }

  @Override
  public SyntaxToken openBraceToken() {
    return switchExpression.openBraceToken();
  }

  @Override
  public List<CaseGroupTree> cases() {
    return switchExpression.cases();
  }

  @Override
  public SyntaxToken closeBraceToken() {
    return switchExpression.closeBraceToken();
  }

  @Override
  public SwitchExpressionTree asSwitchExpression() {
    return switchExpression;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitSwitchStatement(this);
  }

  @Override
  public Kind kind() {
    return Kind.SWITCH_STATEMENT;
  }

  @Nullable
  @Override
  public SyntaxToken firstToken() {
    return switchKeyword();
  }

  @Nullable
  @Override
  public SyntaxToken lastToken() {
    return closeBraceToken();
  }

  @Override
  Iterator<? extends Tree> childrenIterator() {
    return Iterators.singletonIterator(
      asSwitchExpression()
    );
  }
}
