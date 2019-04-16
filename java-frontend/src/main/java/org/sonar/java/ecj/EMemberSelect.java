package org.sonar.java.ecj;

import com.google.common.collect.Iterators;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodsAreNonnullByDefault;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

@MethodsAreNonnullByDefault
class EMemberSelect extends EExpression implements MemberSelectExpressionTree {
  ExpressionTree lhs;
  EIdentifier rhs;

  @Override
  public ExpressionTree expression() {
    return lhs;
  }

  @Override
  public SyntaxToken operatorToken() {
    throw new NotImplementedException();
  }

  @Override
  public IdentifierTree identifier() {
    return rhs;
  }

  @Override
  public List<AnnotationTree> annotations() {
    // FIXME
    return Collections.emptyList();
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitMemberSelectExpression(this);
  }

  @Override
  public Kind kind() {
    return Kind.MEMBER_SELECT;
  }

  @Nullable
  @Override
  public SyntaxToken firstToken() {
    return lhs.firstToken();
  }

  @Nullable
  @Override
  public SyntaxToken lastToken() {
    return rhs.lastToken();
  }

  @Override
  Iterator<? extends Tree> childrenIterator() {
    return Iterators.forArray(
      expression(),
      identifier()
    );
  }
}
