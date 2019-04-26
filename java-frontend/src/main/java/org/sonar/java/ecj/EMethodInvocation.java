package org.sonar.java.ecj;

import com.google.common.collect.Iterators;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.sonar.java.resolve.Symbols;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodsAreNonnullByDefault;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;
import org.sonar.plugins.java.api.tree.TypeArguments;

import javax.annotation.Nullable;
import java.util.Iterator;

@MethodsAreNonnullByDefault
class EMethodInvocation extends EExpression implements MethodInvocationTree {
  Ctx ast;
  @Nullable
  IMethodBinding binding;

  ExpressionTree methodSelect;
  EArguments arguments = new EArguments();

  @Nullable
  @Override
  public TypeArguments typeArguments() {
    // FIXME
    return null;
  }

  @Override
  public ExpressionTree methodSelect() {
    return methodSelect;
  }

  @Override
  public Arguments arguments() {
    return arguments;
  }

  @Override
  public Symbol symbol() {
    if (binding == null) {
      return Symbols.unknownSymbol;
    }
    return ast.methodSymbol(binding);
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitMethodInvocation(this);
  }

  @Override
  public Kind kind() {
    return Kind.METHOD_INVOCATION;
  }

  @Nullable
  @Override
  public SyntaxToken firstToken() {
    return methodSelect.firstToken();
  }

  @Nullable
  @Override
  public SyntaxToken lastToken() {
    return arguments.lastToken();
  }

  @Override
  Iterator<? extends Tree> childrenIterator() {
    return Iterators.forArray(
      methodSelect(),
      arguments()
    );
  }

  static class EArguments extends EList<ExpressionTree> implements Arguments {
    SyntaxToken openParenToken;
    SyntaxToken closeParenToken;

    @Nullable
    @Override
    public SyntaxToken openParenToken() {
      return openParenToken;
    }

    @Nullable
    @Override
    public SyntaxToken closeParenToken() {
      return closeParenToken;
    }

    @Nullable
    @Override
    public SyntaxToken firstToken() {
      return openParenToken;
    }

    @Nullable
    @Override
    public SyntaxToken lastToken() {
      return closeParenToken();
    }

    @Override
    public Kind kind() {
      return Kind.ARGUMENTS;
    }

    @Override
    Iterator<? extends Tree> childrenIterator() {
      return Iterators.concat(
        Iterators.singletonIterator(openParenToken()),
        super.childrenIterator(),
        Iterators.singletonIterator(closeParenToken())
      );
    }
  }
}
