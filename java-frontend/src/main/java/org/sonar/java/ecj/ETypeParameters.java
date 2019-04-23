package org.sonar.java.ecj;

import com.google.common.collect.Iterators;
import org.sonar.plugins.java.api.tree.MethodsAreNonnullByDefault;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeParameterTree;
import org.sonar.plugins.java.api.tree.TypeParameters;

import javax.annotation.Nullable;
import java.util.Iterator;

@MethodsAreNonnullByDefault
class ETypeParameters extends EList<TypeParameterTree> implements TypeParameters {
  SyntaxToken openBracketToken;
  SyntaxToken closeBracketToken;

  @Nullable
  @Override
  public SyntaxToken openBracketToken() {
    return openBracketToken;
  }

  @Nullable
  @Override
  public SyntaxToken closeBracketToken() {
    return closeBracketToken;
  }

  @Override
  public Kind kind() {
    return Kind.TYPE_PARAMETERS;
  }

  @Nullable
  @Override
  public SyntaxToken firstToken() {
    return openBracketToken;
  }

  @Nullable
  @Override
  public SyntaxToken lastToken() {
    return closeBracketToken;
  }

  @Override
  Iterator<? extends Tree> childrenIterator() {
    return Iterators.concat(
      Iterators.singletonIterator(openBracketToken()),
      super.iterator(),
      Iterators.singletonIterator(closeBracketToken())
    );
  }
}
