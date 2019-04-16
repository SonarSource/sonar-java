package org.sonar.java.ecj;

import com.google.common.collect.Iterators;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.InstanceOfTree;
import org.sonar.plugins.java.api.tree.MethodsAreNonnullByDefault;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;
import org.sonar.plugins.java.api.tree.TypeTree;

import javax.annotation.Nullable;
import java.util.Iterator;

@MethodsAreNonnullByDefault
class EInstanceof extends EExpression implements InstanceOfTree {
  TypeTree type;
  SyntaxToken instanceofKeyword;
  ExpressionTree expression;

  @Override
  public ExpressionTree expression() {
    return expression;
  }

  @Override
  public SyntaxToken instanceofKeyword() {
    return instanceofKeyword;
  }

  @Override
  public TypeTree type() {
    return type;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitInstanceOf(this);
  }

  @Override
  public Kind kind() {
    return Kind.INSTANCE_OF;
  }

  @Nullable
  @Override
  public SyntaxToken firstToken() {
    return expression().firstToken();
  }

  @Nullable
  @Override
  public SyntaxToken lastToken() {
    return type().lastToken();
  }

  @Override
  Iterator<? extends Tree> childrenIterator() {
    return Iterators.forArray(
      expression(),
      type()
    );
  }
}
