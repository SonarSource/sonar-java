package org.sonar.java.ecj;

import com.google.common.collect.Iterators;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.sonar.java.resolve.Symbols;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.ArrayTypeTree;
import org.sonar.plugins.java.api.tree.ListTree;
import org.sonar.plugins.java.api.tree.MethodsAreNonnullByDefault;
import org.sonar.plugins.java.api.tree.ParameterizedTypeTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;
import org.sonar.plugins.java.api.tree.TypeArguments;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.UnionTypeTree;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

@MethodsAreNonnullByDefault
abstract class ETypeTree extends ETree implements TypeTree {
  AST ast;
  ITypeBinding binding;

  @Override
  public final Type symbolType() {
    if (binding == null) {
      return Symbols.unknownType;
    }
    return new EType(ast, binding);
  }
}

/**
 * {@link org.sonar.plugins.java.api.tree.InferedTypeTree}
 */
@MethodsAreNonnullByDefault
@ParametersAreNonnullByDefault
class EInferedType extends ETypeTree {
  @Override
  public List<AnnotationTree> annotations() {
    return Collections.emptyList();
  }

  @Override
  public void accept(TreeVisitor visitor) {
    // nop
  }

  @Override
  public Kind kind() {
    return Kind.INFERED_TYPE;
  }

  @Override
  Iterator<? extends Tree> childrenIterator() {
    return Collections.emptyIterator();
  }

  @Nullable
  @Override
  public SyntaxToken firstToken() {
    return null;
  }

  @Nullable
  @Override
  public SyntaxToken lastToken() {
    return null;
  }
}

@MethodsAreNonnullByDefault
class EUnionType extends ETypeTree implements UnionTypeTree {
  EList<TypeTree> typeAlternatives = new EList<>();

  @Override
  public ListTree<TypeTree> typeAlternatives() {
    return typeAlternatives;
  }

  @Override
  public List<AnnotationTree> annotations() {
    return Collections.emptyList();
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitUnionType(this);
  }

  @Override
  public Kind kind() {
    return Kind.UNION_TYPE;
  }

  @Nullable
  @Override
  public SyntaxToken firstToken() {
    return null;
  }

  @Nullable
  @Override
  public SyntaxToken lastToken() {
    return null;
  }

  @Override
  Iterator<? extends Tree> childrenIterator() {
    return Iterators.forArray(typeAlternatives());
  }
}

@MethodsAreNonnullByDefault
class EArrayType extends ETypeTree implements ArrayTypeTree {
  // TODO are next two mutually exclusive?
  TypeTree type;
  SyntaxToken ellipsisToken;
  SyntaxToken openBracketToken;

  @Override
  public TypeTree type() {
    return type;
  }

  @Nullable
  @Override
  public SyntaxToken openBracketToken() {
    return openBracketToken;
  }

  @Nullable
  @Override
  public SyntaxToken closeBracketToken() {
    throw new UnsupportedOperationException();
  }

  @Nullable
  @Override
  public SyntaxToken ellipsisToken() {
    return ellipsisToken;
  }

  @Override
  public List<AnnotationTree> annotations() {
    return Collections.emptyList();
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitArrayType(this);
  }

  @Nullable
  @Override
  public SyntaxToken firstToken() {
    return type.firstToken();
  }

  @Override
  public Kind kind() {
    return Kind.ARRAY_TYPE;
  }

  @Override
  Iterator<? extends Tree> childrenIterator() {
    return Iterators.forArray(type);
  }
}

@MethodsAreNonnullByDefault
class EParameterizedType extends ETypeTree implements ParameterizedTypeTree {
  TypeTree type;
  TypeArguments typeArguments;

  @Override
  public TypeTree type() {
    return type;
  }

  @Override
  public TypeArguments typeArguments() {
    return typeArguments;
  }

  @Override
  public List<AnnotationTree> annotations() {
    return Collections.emptyList();
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitParameterizedType(this);
  }

  @Override
  public Kind kind() {
    return Kind.PARAMETERIZED_TYPE;
  }

  @Nullable
  @Override
  public SyntaxToken firstToken() {
    return type.firstToken();
  }

  @Nullable
  @Override
  public SyntaxToken lastToken() {
    return typeArguments.lastToken();
  }

  @Override
  Iterator<? extends Tree> childrenIterator() {
    return Iterators.forArray(
      type(),
      typeArguments()
    );
  }
}
