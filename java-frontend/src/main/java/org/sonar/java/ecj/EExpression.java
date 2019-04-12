package org.sonar.java.ecj;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.sonar.java.resolve.JavaType;
import org.sonar.java.resolve.Symbols;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.MethodsAreNonnullByDefault;
import org.sonar.plugins.java.api.tree.VariableTree;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@MethodsAreNonnullByDefault
@ParametersAreNonnullByDefault
abstract class EExpression extends ETree implements ExpressionTree {

  AST ast;
  ITypeBinding typeBinding;

  @Override
  public final Type symbolType() {
    if (typeBinding == null) {
      return Symbols.unknownType;
    }
    return new EType(ast, typeBinding);
  }

}
