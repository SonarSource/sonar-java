package org.sonar.java.ecj;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.Tree;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class Ctx {

  final AST ast;

  final Map<IBinding, Tree> declarations = new HashMap<>();

  final Map<IBinding, List<IdentifierTree>> usages = new HashMap<>();

  Ctx(AST ast) {
    this.ast = ast;
  }

  void declaration(@Nullable IBinding binding, Tree node) {
    if (binding == null) {
      return;
    }
    declarations.put(binding, node);
  }

  void usage(@Nullable IBinding binding, EIdentifier identifier) {
    if (binding == null) {
      return;
    }
    usages.computeIfAbsent(binding, k -> new ArrayList<>()).add(identifier);
  }

  /**
   * For example DoubleCheckedLockingCheck uses identity comparison.
   */
  private final Map<IBinding, ESymbol> symbolsCache = new HashMap<>();

  ETypeSymbol typeSymbol(ITypeBinding binding) {
    return (ETypeSymbol) symbolsCache.computeIfAbsent(binding, k -> new ETypeSymbol(this, (ITypeBinding) k));
  }

  EMethodSymbol methodSymbol(IMethodBinding binding) {
    return (EMethodSymbol) symbolsCache.computeIfAbsent(binding, k -> new EMethodSymbol(this, (IMethodBinding) k));
  }

  EVariableSymbol variableSymbol(IVariableBinding binding) {
    return (EVariableSymbol) symbolsCache.computeIfAbsent(binding, k -> new EVariableSymbol(this, (IVariableBinding) k));
  }

  private final Map<ITypeBinding, EType> typesCache = new HashMap<>();

  Type type(ITypeBinding binding) {
    return typesCache.computeIfAbsent(binding, k -> new EType(this, k));
  }
}
