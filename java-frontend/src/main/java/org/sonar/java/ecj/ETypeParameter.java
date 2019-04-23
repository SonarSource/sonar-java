package org.sonar.java.ecj;

import com.google.common.collect.Iterators;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.ListTree;
import org.sonar.plugins.java.api.tree.MethodsAreNonnullByDefault;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;
import org.sonar.plugins.java.api.tree.TypeParameterTree;

import javax.annotation.Nullable;
import java.util.Iterator;

@MethodsAreNonnullByDefault
class ETypeParameter extends ETree implements TypeParameterTree {
  EIdentifier identifier;
  EList<Tree> bounds = new EList<>();

  @Override
  public IdentifierTree identifier() {
    return identifier;
  }

  @Nullable
  @Override
  public SyntaxToken extendToken() {
    return null;
  }

  @Override
  public ListTree<Tree> bounds() {
    return bounds;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitTypeParameter(this);
  }

  @Override
  public Kind kind() {
    return Kind.TYPE_PARAMETER;
  }

  @Override
  Iterator<? extends Tree> childrenIterator() {
    return Iterators.forArray(
      identifier(),
      extendToken(),
      bounds().isEmpty() ? null : bounds()
    );
  }
}
