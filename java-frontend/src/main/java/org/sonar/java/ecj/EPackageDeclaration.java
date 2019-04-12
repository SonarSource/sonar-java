package org.sonar.java.ecj;

import com.google.common.collect.Iterators;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodsAreNonnullByDefault;
import org.sonar.plugins.java.api.tree.PackageDeclarationTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

@MethodsAreNonnullByDefault
class EPackageDeclaration extends ETree implements PackageDeclarationTree {
  ExpressionTree name;

  @Override
  public List<AnnotationTree> annotations() {
    return Collections.emptyList();
  }

  @Override
  public SyntaxToken packageKeyword() {
    throw new UnexpectedAccessException();
  }

  @Override
  public ExpressionTree packageName() {
    return name;
  }

  @Override
  public SyntaxToken semicolonToken() {
    throw new UnexpectedAccessException();
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitPackage(this);
  }

  @Override
  public Kind kind() {
    return Kind.PACKAGE;
  }

  @Override
  Iterator<? extends Tree> childrenIterator() {
    return Iterators.singletonIterator(
      packageName()
    );
  }
}
