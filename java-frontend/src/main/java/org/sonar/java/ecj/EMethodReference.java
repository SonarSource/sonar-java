package org.sonar.java.ecj;

import com.google.common.collect.Iterators;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodReferenceTree;
import org.sonar.plugins.java.api.tree.MethodsAreNonnullByDefault;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;
import org.sonar.plugins.java.api.tree.TypeArguments;

import javax.annotation.Nullable;
import java.util.Iterator;

@MethodsAreNonnullByDefault
public class EMethodReference extends EExpression implements MethodReferenceTree {

  Tree expression;
  IdentifierTree method;

  @Override
  public Tree expression() {
    return expression;
  }

  @Override
  public SyntaxToken doubleColon() {
    throw new UnsupportedOperationException();
  }

  @Nullable
  @Override
  public TypeArguments typeArguments() {
    // FIXME
    return new EClassInstanceCreation.ETypeArguments();
  }

  @Override
  public IdentifierTree method() {
    return method;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitMethodReference(this);
  }

  @Override
  public Kind kind() {
    return Kind.METHOD_REFERENCE;
  }

  @Override
  Iterator<? extends Tree> childrenIterator() {
    return Iterators.forArray(
      expression(),
      method()
    );
  }
}
