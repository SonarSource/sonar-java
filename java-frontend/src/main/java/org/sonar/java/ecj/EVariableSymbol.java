package org.sonar.java.ecj;

import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.MethodsAreNonnullByDefault;
import org.sonar.plugins.java.api.tree.VariableTree;

import javax.annotation.Nullable;

@MethodsAreNonnullByDefault
public class EVariableSymbol extends ESymbol implements Symbol.VariableSymbol {
  /**
   * Use {@link Ctx#variableSymbol(IVariableBinding)}
   */
  EVariableSymbol(Ctx ast, IVariableBinding binding) {
    super(ast, binding);
  }

  @Nullable
  @Override
  public TypeSymbol enclosingClass() {
    IVariableBinding b = (IVariableBinding) binding;
    ITypeBinding declaringClass = b.getDeclaringClass();
    if (declaringClass == null) {
      // local variable
      return ast.typeSymbol(b.getDeclaringMethod().getDeclaringClass());
    }
    return ast.typeSymbol(declaringClass);
  }

  @Nullable
  @Override
  public VariableTree declaration() {
    return (VariableTree) super.declaration();
  }
}
