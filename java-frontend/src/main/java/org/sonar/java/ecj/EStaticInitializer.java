package org.sonar.java.ecj;

import com.google.common.collect.Iterators;
import org.sonar.plugins.java.api.tree.MethodsAreNonnullByDefault;
import org.sonar.plugins.java.api.tree.StaticInitializerTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;

import javax.annotation.Nullable;
import java.util.Iterator;

@MethodsAreNonnullByDefault
class EStaticInitializer extends EBlock implements StaticInitializerTree {
  SyntaxToken staticKeyword;

  @Override
  public SyntaxToken staticKeyword() {
    return staticKeyword;
  }

  @Override
  public Kind kind() {
    return Kind.STATIC_INITIALIZER;
  }

  @Nullable
  @Override
  public SyntaxToken firstToken() {
    return staticKeyword;
  }

  @Override
  Iterator<? extends Tree> childrenIterator() {
    return Iterators.concat(
      Iterators.singletonIterator(staticKeyword()),
      super.childrenIterator()
    );
  }
}
