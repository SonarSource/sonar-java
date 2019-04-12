package org.sonar.java.ecj;

import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.TypeParameterTree;
import org.sonar.plugins.java.api.tree.TypeParameters;

import javax.annotation.Nullable;

class ETypeParameters extends EList<TypeParameterTree> implements TypeParameters {
  @Nullable
  @Override
  public SyntaxToken openBracketToken() {
    throw new UnsupportedOperationException();
  }

  @Nullable
  @Override
  public SyntaxToken closeBracketToken() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Kind kind() {
    return Kind.TYPE_PARAMETERS;
  }
}
