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
  TypeArguments typeArguments;
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

  @Nullable
  @Override
  public TypeArguments typeArguments() {
    return typeArguments;
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
    return ast.methodSymbol(binding);
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

  @Override
  Iterator<? extends Tree> childrenIterator() {
    return Iterators.forArray(
      identifier(),
      arguments(),
      classBody()
    );
  }

  static class ETypeArguments extends EList<Tree> implements TypeArguments {
    SyntaxToken openBracketToken;
    SyntaxToken closeBracketToken;

    @Override
    public SyntaxToken openBracketToken() {
      return openBracketToken;
    }

    @Override
    public SyntaxToken closeBracketToken() {
      return closeBracketToken;
    }

    @Override
    public Kind kind() {
      return Kind.TYPE_ARGUMENTS;
    }

    @Nullable
    @Override
    public SyntaxToken firstToken() {
      return openBracketToken;
    }

    @Nullable
    @Override
    public SyntaxToken lastToken() {
      return closeBracketToken;
    }
  }
}
