package org.sonar.java.ecj;

import com.google.common.collect.Iterators;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodsAreNonnullByDefault;
import org.sonar.plugins.java.api.tree.PackageDeclarationTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@MethodsAreNonnullByDefault
class EPackageDeclaration extends ETree implements PackageDeclarationTree {
  List<AnnotationTree> annotations = new ArrayList<>();
  SyntaxToken packageKeyword;
  ExpressionTree name;
  SyntaxToken semicolonToken;

  @Override
  public List<AnnotationTree> annotations() {
    return annotations;
  }

  @Override
  public SyntaxToken packageKeyword() {
    return packageKeyword;
  }

  @Override
  public ExpressionTree packageName() {
    return name;
  }

  @Override
  public SyntaxToken semicolonToken() {
    return semicolonToken;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitPackage(this);
  }

  @Override
  public Kind kind() {
    return Kind.PACKAGE;
  }

  @Nullable
  @Override
  public SyntaxToken lastToken() {
    return semicolonToken;
  }

  @Override
  Iterator<? extends Tree> childrenIterator() {
    return Iterators.concat(
      annotations().iterator(),
      Iterators.forArray(
        packageKeyword(),
        packageName(),
        semicolonToken()
      )
    );
  }
}
