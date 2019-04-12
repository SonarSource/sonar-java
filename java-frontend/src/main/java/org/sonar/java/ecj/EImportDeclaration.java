package org.sonar.java.ecj;

import com.google.common.collect.Iterators;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.ImportTree;
import org.sonar.plugins.java.api.tree.MethodsAreNonnullByDefault;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;

import javax.annotation.Nullable;
import java.util.Iterator;

@MethodsAreNonnullByDefault
class EImportDeclaration extends ETree implements ImportTree {
  boolean isStatic;
  SyntaxToken importKeyword;
  ExpressionTree qualifiedIdentifier;
  SyntaxToken semicolonToken;

  @Override
  public boolean isStatic() {
    return isStatic;
  }

  @Override
  public SyntaxToken importKeyword() {
    return importKeyword;
  }

  @Nullable
  @Override
  public SyntaxToken staticKeyword() {
    throw new UnexpectedAccessException();
  }

  @Override
  public Tree qualifiedIdentifier() {
    return qualifiedIdentifier;
  }

  @Override
  public SyntaxToken semicolonToken() {
    return semicolonToken;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitImport(this);
  }

  @Override
  public Kind kind() {
    return Kind.IMPORT;
  }

  @Nullable
  @Override
  public SyntaxToken firstToken() {
    return importKeyword();
  }

  @Nullable
  @Override
  public SyntaxToken lastToken() {
    return semicolonToken();
  }

  @Override
  Iterator<? extends Tree> childrenIterator() {
    return Iterators.forArray(
      qualifiedIdentifier()
    );
  }
}
