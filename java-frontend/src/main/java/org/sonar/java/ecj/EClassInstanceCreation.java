package org.sonar.java.ecj;

import com.google.common.collect.Iterators;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.sonar.java.resolve.Symbols;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodsAreNonnullByDefault;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;
import org.sonar.plugins.java.api.tree.TypeArguments;
import org.sonar.plugins.java.api.tree.TypeTree;

import javax.annotation.Nullable;
import java.util.Iterator;

@MethodsAreNonnullByDefault
class EClassInstanceCreation extends EExpression implements NewClassTree {
  IMethodBinding binding;

  SyntaxToken newKeyword;
  TypeTree identifier;
  EMethodInvocation.EArguments arguments = new EMethodInvocation.EArguments();
  EClass classBody;

  @Nullable
  @Override
  public ExpressionTree enclosingExpression() {
    // FIXME
    return null;
  }

  @Nullable
  @Override
  public SyntaxToken dotToken() {
    throw new UnexpectedAccessException();
  }

  @Nullable
  @Override
  public SyntaxToken newKeyword() {
    return newKeyword;
  }

  @Override
  public TypeArguments typeArguments() {
    // FIXME
    return new ETypeArguments();
  }

  @Override
  public TypeTree identifier() {
    return identifier;
  }

  @Override
  public Arguments arguments() {
    return arguments;
  }

  @Nullable
  @Override
  public ClassTree classBody() {
    return classBody;
  }

  @Override
  public Symbol constructorSymbol() {
    if (binding == null) {
      return Symbols.unknownSymbol;
    }
    return new EMethodSymbol(ast, binding);
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitNewClass(this);
  }

  @Override
  public Kind kind() {
    return Kind.NEW_CLASS;
  }

  @Nullable
  @Override
  public SyntaxToken firstToken() {
    return enclosingExpression() != null ? enclosingExpression().firstToken() : newKeyword();
  }

  @Nullable
  @Override
  public SyntaxToken lastToken() {
    return classBody() != null ? classBody().lastToken() : arguments().lastToken();
  }

  @Override
  Iterator<? extends Tree> childrenIterator() {
    return Iterators.forArray(
      identifier(),
      arguments(),
      classBody()
    );
  }

  static class ETypeArguments extends EList<Tree> implements TypeArguments {
    @Override
    public SyntaxToken openBracketToken() {
      throw new UnexpectedAccessException();
    }

    @Override
    public SyntaxToken closeBracketToken() {
      throw new NotImplementedException();
    }

    @Override
    public Kind kind() {
      return Kind.TYPE_ARGUMENTS;
    }

    @Override
    Iterator<? extends Tree> childrenIterator() {
      return Iterators.forArray();
    }
  }
}
