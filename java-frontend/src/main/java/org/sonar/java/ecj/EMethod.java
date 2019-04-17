package org.sonar.java.ecj;

import com.google.common.collect.Iterators;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.sonar.java.model.declaration.MethodTreeImpl;
import org.sonar.java.resolve.Symbols;
import org.sonar.plugins.java.api.cfg.ControlFlowGraph;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.ListTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.MethodsAreNonnullByDefault;
import org.sonar.plugins.java.api.tree.ModifiersTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;
import org.sonar.plugins.java.api.tree.TypeParameters;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.VariableTree;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@MethodsAreNonnullByDefault
class EMethod extends ETree implements MethodTree {
  Ctx ast;
  IMethodBinding binding;

  EModifiers modifiers = new EModifiers();
  TypeTree returnType;
  IdentifierTree simpleName;
  List<VariableTree> parameters = new ArrayList<>();
  SyntaxToken closeParenToken;
  SyntaxToken throwsToken;
  EList<TypeTree> throwsClauses = new EList<>();
  BlockTree block;

  @Override
  public ModifiersTree modifiers() {
    return modifiers;
  }

  @Override
  public TypeParameters typeParameters() {
    // FIXME
    return new ETypeParameters();
  }

  @Nullable
  @Override
  public TypeTree returnType() {
    return returnType;
  }

  @Override
  public IdentifierTree simpleName() {
    return simpleName;
  }

  @Override
  public SyntaxToken openParenToken() {
    throw new UnexpectedAccessException();
  }

  @Override
  public List<VariableTree> parameters() {
    return parameters;
  }

  @Override
  public SyntaxToken closeParenToken() {
    return closeParenToken;
  }

  @Override
  public SyntaxToken throwsToken() {
    return throwsToken;
  }

  @Override
  public ListTree<TypeTree> throwsClauses() {
    return throwsClauses;
  }

  @Nullable
  @Override
  public BlockTree block() {
    return block;
  }

  @Nullable
  @Override
  public SyntaxToken semicolonToken() {
    throw new UnexpectedAccessException();
  }

  @Nullable
  @Override
  public SyntaxToken defaultToken() {
    throw new UnexpectedAccessException();
  }

  @Nullable
  @Override
  public ExpressionTree defaultValue() {
    // FIXME
    return null;
  }

  @Override
  public Symbol.MethodSymbol symbol() {
    if (binding == null) {
      return Symbols.unknownMethodSymbol;
    }
    return new EMethodSymbol(ast, binding);
  }

  /**
   * @see MethodTreeImpl#isOverriding()
   */
  @Nullable
  @Override
  public Boolean isOverriding() {
    // TODO what about unresolved?
    if (binding != null) {
      for (IAnnotationBinding annotation : binding.getAnnotations()) {
        if ("java.lang.Override".equals(
          annotation.getAnnotationType().getQualifiedName()
        )) {
          return true;
        }
      }
      return EMethodSymbol.find(binding::overrides, binding.getDeclaringClass()) != null;
    }
    return false;
  }

  @Nullable
  @Override
  public ControlFlowGraph cfg() {
    throw new NotImplementedException();
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitMethod(this);
  }

  @Override
  public Kind kind() {
    return returnType == null ? Kind.CONSTRUCTOR : Kind.METHOD;
  }

  @Nullable
  @Override
  public SyntaxToken firstToken() {
    // FIXME depends
    return simpleName().firstToken();
  }

  @Override
  Iterator<? extends Tree> childrenIterator() {
    return Iterators.concat(
      Iterators.forArray(
        modifiers(),
        typeParameters(),
        returnType(),
        simpleName()
      ),
      parameters.iterator(),
      Iterators.forArray(
        block()
      )
    );
  }
}
