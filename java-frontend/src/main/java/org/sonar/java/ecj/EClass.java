package org.sonar.java.ecj;

import com.google.common.collect.Iterators;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.sonar.java.resolve.Symbols;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.ListTree;
import org.sonar.plugins.java.api.tree.MethodsAreNonnullByDefault;
import org.sonar.plugins.java.api.tree.ModifiersTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;
import org.sonar.plugins.java.api.tree.TypeParameters;
import org.sonar.plugins.java.api.tree.TypeTree;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@MethodsAreNonnullByDefault
class EClass extends ETree implements ClassTree {
  Ctx ast;
  ITypeBinding binding;

  Kind kind;
  EModifiers modifiers = new EModifiers();
  SyntaxToken declarationKeyword;
  EIdentifier simpleName;
  ETypeParameters typeParameters = new ETypeParameters();
  TypeTree superClass;
  EList<TypeTree> superInterfaces = new EList<>();
  SyntaxToken openBraceToken;
  List<Tree> members = new ArrayList<>();
  SyntaxToken closeBraceToken;

  @Override
  public ModifiersTree modifiers() {
    return modifiers;
  }

  @Nullable
  @Override
  public SyntaxToken declarationKeyword() {
    return declarationKeyword;
  }

  @Nullable
  @Override
  public IdentifierTree simpleName() {
    return simpleName;
  }

  @Override
  public TypeParameters typeParameters() {
    return typeParameters;
  }

  @Nullable
  @Override
  public TypeTree superClass() {
    return superClass;
  }

  @Override
  public ListTree<TypeTree> superInterfaces() {
    return superInterfaces;
  }

  @Override
  public SyntaxToken openBraceToken() {
    return openBraceToken;
  }

  @Override
  public List<Tree> members() {
    return members;
  }

  @Override
  public SyntaxToken closeBraceToken() {
    return closeBraceToken;
  }

  @Override
  public Symbol.TypeSymbol symbol() {
    if (binding == null) {
      return Symbols.unknownSymbol;
    }
    return ast.typeSymbol(binding);
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitClass(this);
  }

  @Override
  public Kind kind() {
    return kind;
  }

  @Nullable
  @Override
  public SyntaxToken firstToken() {
    // TODO suboptimal
    Iterator<? extends Tree> childrenIterator = childrenIterator();
    while (childrenIterator.hasNext()) {
      Tree child = childrenIterator.next();
      if (child != null && !child.is(Kind.TYPE_PARAMETERS) && !child.is(Kind.LIST) && child.firstToken() != null) {
        return child.firstToken();
      }
    }
    throw new IllegalStateException();
  }

  @Nullable
  @Override
  public SyntaxToken lastToken() {
    return closeBraceToken();
  }

  @Override
  Iterator<? extends Tree> childrenIterator() {
    return Iterators.concat(
      Iterators.forArray(
        modifiers(),
        declarationKeyword(),
        simpleName(),
        typeParameters(),
        superClass(),
        superInterfaces(),
        openBraceToken()
      ),
      members().iterator(),
      Iterators.forArray(closeBraceToken)
    );
  }
}
