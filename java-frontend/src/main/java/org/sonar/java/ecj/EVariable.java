package org.sonar.java.ecj;

import com.google.common.collect.Iterators;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.sonar.java.resolve.Symbols;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodsAreNonnullByDefault;
import org.sonar.plugins.java.api.tree.ModifiersTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.VariableTree;

import javax.annotation.Nullable;
import java.util.Iterator;

@MethodsAreNonnullByDefault
class EVariable extends ETree implements VariableTree {
  Ctx ast;
  IVariableBinding binding;

  EModifiers modifiers = new EModifiers();
  TypeTree type;
  EIdentifier simpleName;
  SyntaxToken equalToken;
  ExpressionTree initializer;
  SyntaxToken endToken;

  @Override
  public ModifiersTree modifiers() {
    return modifiers;
  }

  @Override
  public TypeTree type() {
    return type;
  }

  @Override
  public IdentifierTree simpleName() {
    return simpleName;
  }

  @Nullable
  @Override
  public SyntaxToken equalToken() {
    return equalToken;
  }

  @Nullable
  @Override
  public ExpressionTree initializer() {
    return initializer;
  }

  @Override
  public Symbol symbol() {
    if (binding == null) {
      return Symbols.unknownSymbol;
    }
    return ast.variableSymbol(binding);
  }

  @Nullable
  @Override
  public SyntaxToken endToken() {
    return endToken;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitVariable(this);
  }

  @Override
  public Kind kind() {
    return Kind.VARIABLE;
  }

  @Nullable
  @Override
  public SyntaxToken firstToken() {
    if (!modifiers.isEmpty()) {
      return modifiers.get(0).firstToken();
    }
    if (type.is(Kind.INFERED_TYPE)) {
      // for LambdaTypeParameterCheckTest
      // TODO do everywhere?
      return simpleName.firstToken();
    }
    return type.firstToken();
  }

  @Override
  Iterator<? extends Tree> childrenIterator() {
    return Iterators.forArray(
      modifiers(),
      type(),
      simpleName(),
      initializer(),
      endToken()
    );
  }
}
