package org.sonar.java.ecj;

import com.google.common.collect.Iterators;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.MethodsAreNonnullByDefault;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.WildcardTree;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

@MethodsAreNonnullByDefault
class EWildcard extends ETypeTree implements WildcardTree {
  SyntaxToken queryToken;
  SyntaxToken extendsOrSuperToken;
  TypeTree bound;

  @Override
  public List<AnnotationTree> annotations() {
    // FIXME
    return Collections.emptyList();
  }

  @Override
  public SyntaxToken queryToken() {
    return queryToken;
  }

  @Nullable
  @Override
  public SyntaxToken extendsOrSuperToken() {
    return extendsOrSuperToken;
  }

  @Nullable
  @Override
  public TypeTree bound() {
    return bound;
  }

  @Override
  public Kind kind() {
    if (extendsOrSuperToken == null) {
      return Kind.UNBOUNDED_WILDCARD;
    } else if ("extends".equals(extendsOrSuperToken.text())) {
      return Kind.EXTENDS_WILDCARD;
    } else {
      return Kind.SUPER_WILDCARD;
    }
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitWildcard(this);
  }

  @Override
  Iterator<? extends Tree> childrenIterator() {
    return Iterators.concat(
      annotations().iterator(),
      Iterators.forArray(
        queryToken(),
        extendsOrSuperToken(),
        bound()
      )
    );
  }
}
