package org.sonar.java.ecj;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.LookupEnvironment;
import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;
import org.sonar.plugins.java.api.tree.MethodsAreNonnullByDefault;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.reflect.Method;

@ParametersAreNonnullByDefault
@MethodsAreNonnullByDefault
final class Hack {

  private Hack() {
  }

  @Nullable
  static ITypeBinding resolveType(AST ast, String name) {
    // for example in InstanceOfAlwaysTrueCheck
    int i = name.length();
    while (name.charAt(i - 1) == ']') {
      i -= 2;
    }
    int dimensions = (name.length() - i) / 2;
    String nameWithoutDimensions = name.substring(0, i);

    // for example "byte" in IntegerToHexStringCheck
    ITypeBinding result = ast.resolveWellKnownType(nameWithoutDimensions);

    if (result == null) {
      result = findNonPrimitiveType(ast, nameWithoutDimensions);
    }

    if (result == null) {
      return null;
    }

    return dimensions > 0 ? result.createArrayType(dimensions) : result;
  }

  @Nullable
  private static ITypeBinding findNonPrimitiveType(AST ast, String name) {
    // BindingResolver bindingResolver = ast.getBindingResolver();
    // ReferenceBinding referenceBinding = bindingResolver
    //   .lookupEnvironment()
    //   .getType(CharOperation.splitOn('.', fqn.toCharArray()));
    // return bindingResolver.getTypeBinding(referenceBinding);
    try {
      Method methodGetBindingResolver = ast.getClass().getDeclaredMethod("getBindingResolver");
      methodGetBindingResolver.setAccessible(true);
      Object bindingResolver = methodGetBindingResolver.invoke(ast);

      Method methodLookupEnvironment = bindingResolver.getClass().getDeclaredMethod("lookupEnvironment");
      methodLookupEnvironment.setAccessible(true);
      LookupEnvironment lookupEnvironment = (LookupEnvironment) methodLookupEnvironment.invoke(bindingResolver);

      ReferenceBinding referenceBinding = lookupEnvironment.getType(CharOperation.splitOn('.', name.toCharArray()));

      Method methodGetTypeBinding = bindingResolver.getClass().getDeclaredMethod("getTypeBinding", TypeBinding.class);
      methodGetTypeBinding.setAccessible(true);
      return (ITypeBinding) methodGetTypeBinding.invoke(bindingResolver, referenceBinding);

    } catch (ReflectiveOperationException e) {
      throw new RuntimeException(e);
    }
  }

}
