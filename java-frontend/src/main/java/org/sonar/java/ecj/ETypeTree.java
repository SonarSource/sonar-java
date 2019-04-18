package org.sonar.java.ecj;

import com.google.common.collect.Iterators;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.sonar.java.resolve.Symbols;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.ArrayTypeTree;
import org.sonar.plugins.java.api.tree.ListTree;
import org.sonar.plugins.java.api.tree.MethodsAreNonnullByDefault;
import org.sonar.plugins.java.api.tree.ParameterizedTypeTree;
import org.sonar.plugins.java.api.tree.PrimitiveTypeTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;
import org.sonar.plugins.java.api.tree.TypeArguments;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.UnionTypeTree;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

@MethodsAreNonnullByDefault
abstract class ETypeTree extends ETree implements TypeTree {
  Ctx ast;
  ITypeBinding binding;

  @Override
  public final Type symbolType() {
    if (binding == null) {
      return Symbols.unknownType;
    }
    return ast.type(binding);
  }
}

@MethodsAreNonnullByDefault
class EPrimitiveType extends ETypeTree implements PrimitiveTypeTree {
  SyntaxToken keyword;

  @Override
  public List<AnnotationTree> annotations() {
    return Collections.emptyList();
  }

  @Override
  public SyntaxToken keyword() {
    return keyword;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitPrimitiveType(this);
  }

  @Override
  public Kind kind() {
    return Kind.PRIMITIVE_TYPE;
  }

  @Nullable
  @Override
  public SyntaxToken firstToken() {
    return keyword;
  }

  @Nullable
  @Override
  public SyntaxToken lastToken() {
    return keyword;
  }

  @Override
  Iterator<? extends Tree> childrenIterator() {
    return Iterators.forArray();
  }
}

/**
 * {@link org.sonar.plugins.java.api.tree.InferedTypeTree}
 */
@MethodsAreNonnullByDefault
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
    return typeAlternatives.get(0).firstToken();
  }

  @Nullable
  @Override
  public SyntaxToken lastToken() {
    return typeAlternatives.get(typeAlternatives.size() - 1).lastToken();
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
    throw new UnexpectedAccessException();
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
  EClassInstanceCreation.ETypeArguments typeArguments;

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
