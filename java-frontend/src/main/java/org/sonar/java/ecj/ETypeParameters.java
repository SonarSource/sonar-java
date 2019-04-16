package org.sonar.java.ecj;

import org.sonar.plugins.java.api.tree.MethodsAreNonnullByDefault;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.TypeParameterTree;
import org.sonar.plugins.java.api.tree.TypeParameters;

import javax.annotation.Nullable;

@MethodsAreNonnullByDefault
class ETypeParameters extends EList<TypeParameterTree> implements TypeParameters {
  @Nullable
  @Override
  public SyntaxToken openBracketToken() {
    throw new NotImplementedException();
  }

  @Nullable
  @Override
  public SyntaxToken closeBracketToken() {
    throw new NotImplementedException();
  }

  @Override
  public Kind kind() {
    return Kind.TYPE_PARAMETERS;
  }
}
