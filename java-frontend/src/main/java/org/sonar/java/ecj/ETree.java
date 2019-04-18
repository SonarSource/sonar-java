package org.sonar.java.ecj;

import com.google.common.collect.Iterators;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodsAreNonnullByDefault;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.Objects;

@MethodsAreNonnullByDefault
public abstract class ETree implements Tree {
  Tree parent;

  @Override
  public final boolean is(Kind... kinds) {
    Kind kind = kind();
    for (Kind k : kinds) {
      if (k == kind) {
        return true;
      }
    }
    return false;
  }

  @Nullable
  @Override
  public final Tree parent() {
    return parent;
  }

  @Nullable
  @Override
  public SyntaxToken firstToken() {
    throw new NotImplementedException(getClass().getCanonicalName());
  }

  @Nullable
  @Override
  public SyntaxToken lastToken() {
    // FIXME
    return firstToken();
  }

  public final Iterator<? extends Tree> children() {
    return Iterators.filter(
      childrenIterator(),
      Objects::nonNull
    );
  }

  abstract Iterator<? extends Tree> childrenIterator();

  @Override
  public final boolean equals(Object obj) {
    return super.equals(obj);
  }

  @Override
  public final int hashCode() {
    return super.hashCode();
  }

  /**
   * @see org.sonar.java.model.expression.IdentifierTreeImpl
   */
  @Override
  public final String toString() {
    if (kind() == Kind.IDENTIFIER) {
      return ((IdentifierTree) this).name();
    }
    return super.toString();
  }
}
