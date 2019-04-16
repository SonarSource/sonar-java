package org.sonar.java.ecj;

import com.google.common.collect.Iterators;
import org.sonar.plugins.java.api.tree.MethodsAreNonnullByDefault;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Iterator;
import java.util.Objects;

@ParametersAreNonnullByDefault
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

  /**
   * @return excluding tokens
   */
  abstract Iterator<? extends Tree> childrenIterator();
}
