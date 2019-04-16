package org.sonar.java.ecj;

import org.sonar.plugins.java.api.tree.MethodsAreNonnullByDefault;
import org.sonar.plugins.java.api.tree.StaticInitializerTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;

import javax.annotation.Nullable;

@MethodsAreNonnullByDefault
public class EStaticInitializer extends EBlock implements StaticInitializerTree {
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
}
