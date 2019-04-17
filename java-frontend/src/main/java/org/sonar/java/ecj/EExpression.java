package org.sonar.java.ecj;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.sonar.java.resolve.Symbols;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodsAreNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsAreNonnullByDefault
@ParametersAreNonnullByDefault
abstract class EExpression extends ETree implements ExpressionTree {
  Ctx ast;
  ITypeBinding typeBinding;

  @Override
  public final Type symbolType() {
    if (typeBinding == null) {
      return Symbols.unknownType;
    }
    return new EType(ast, typeBinding);
  }
}
