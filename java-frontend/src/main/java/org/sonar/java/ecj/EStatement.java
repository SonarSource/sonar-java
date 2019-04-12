package org.sonar.java.ecj;

import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;

import javax.annotation.Nullable;

abstract class EStatement extends ETree implements StatementTree {

  @Nullable
  @Override
  public abstract SyntaxToken firstToken();

  @Nullable
  @Override
  public abstract SyntaxToken lastToken();

}
