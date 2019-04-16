package org.sonar.java.ecj;

import com.google.common.collect.Iterators;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.sonar.java.resolve.Symbols;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodsAreNonnullByDefault;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

@MethodsAreNonnullByDefault
class EIdentifier extends EExpression implements IdentifierTree {
  IBinding binding;

  SyntaxToken identifierToken;

  @Override
  public SyntaxToken identifierToken() {
    return identifierToken;
  }

  @Override
  public String name() {
    return identifierToken.text();
  }

  @Override
  public Symbol symbol() {
    if (binding == null) {
      return Symbols.unknownSymbol;
    }
    switch (binding.getKind()) {
      case IBinding.TYPE:
        return new ETypeSymbol(ast, (ITypeBinding) binding);
      case IBinding.VARIABLE:
        return new EVariableSymbol(ast, (IVariableBinding) binding);
      case IBinding.METHOD:
        return new EMethodSymbol(ast, (IMethodBinding) binding);
      default:
        return Symbols.unknownSymbol;
    }
  }

  @Override
  public List<AnnotationTree> annotations() {
    // FIXME
    return Collections.emptyList();
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitIdentifier(this);
  }

  @Nullable
  @Override
  public SyntaxToken firstToken() {
    return identifierToken;
  }

  @Nullable
  @Override
  public SyntaxToken lastToken() {
    return identifierToken;
  }

  @Override
  public Kind kind() {
    return Kind.IDENTIFIER;
  }

  @Override
  Iterator<? extends Tree> childrenIterator() {
    return Iterators.forArray(
      identifierToken()
    );
  }

  /**
   * {@link org.sonar.java.model.expression.IdentifierTreeImpl}
   */
  @Override
  public String toString() {
    return name();
  }
}
