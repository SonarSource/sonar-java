package org.sonar.java.ecj;

import com.google.common.collect.Iterators;
import org.sonar.plugins.java.api.tree.ArrayDimensionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.ListTree;
import org.sonar.plugins.java.api.tree.MethodsAreNonnullByDefault;
import org.sonar.plugins.java.api.tree.NewArrayTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;
import org.sonar.plugins.java.api.tree.TypeTree;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

@MethodsAreNonnullByDefault
class EArrayCreation extends EExpression implements NewArrayTree {
  SyntaxToken newKeyword;
  TypeTree type;
  SyntaxToken openBraceToken;
  List<ArrayDimensionTree> dimensions = new ArrayList<>();
  EList<ExpressionTree> initializers = new EList<>();
  SyntaxToken closeBraceToken;

  // TODO Godin: I guess that nullable to support array initializers?
  @Nullable
  @Override
  public SyntaxToken newKeyword() {
    return newKeyword;
  }

  @Nullable
  @Override
  public TypeTree type() {
    return type;
  }

  @Override
  public List<ArrayDimensionTree> dimensions() {
    return dimensions;
  }

  @Nullable
  @Override
  public SyntaxToken openBraceToken() {
    return openBraceToken;
  }

  @Override
  public ListTree<ExpressionTree> initializers() {
    return initializers;
  }

  @Nullable
  @Override
  public SyntaxToken closeBraceToken() {
    return closeBraceToken;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitNewArray(this);
  }

  @Override
  public Kind kind() {
    return Kind.NEW_ARRAY;
  }

  @Nullable
  @Override
  public SyntaxToken firstToken() {
    if (newKeyword != null) {
      return newKeyword;
    }
    return Objects.requireNonNull(openBraceToken);
  }

  @Nullable
  @Override
  public SyntaxToken lastToken() {
    if (closeBraceToken != null) {
      return closeBraceToken;
    }
    return type.lastToken();
  }

  @Override
  Iterator<? extends Tree> childrenIterator() {
    return Iterators.concat(
      Iterators.forArray(
        newKeyword(),
        type()
      ),
      dimensions().iterator(),
      Iterators.forArray(
        openBraceToken(),
        initializers(),
        closeBraceToken()
      )
    );
  }
}
