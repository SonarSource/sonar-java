/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.psi;

import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnonymousClass;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiSubstitutor;
import com.intellij.psi.PsiTypeParameter;
import com.intellij.psi.impl.light.LightDefaultConstructor;
import com.intellij.psi.util.PsiTreeUtil;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.sonar.java.model.Symbols;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.VariableTree;

final class PsiTypeSymbol extends PsiSymbol implements Symbol.TypeSymbol {
  private final PsiType type;
  final SpecialField thisSymbol = new SpecialField(false);
  final SpecialField superSymbol = new SpecialField(true);

  PsiTypeSymbol(PsiSema sema, PsiType type, String key) {
    super(sema, type.resolvedClass(), key);
    this.type = type;
  }

  @Override
  public Type type() {
    return type;
  }

  @Override
  public String name() {
    return type.name();
  }

  @Override
  public boolean isUnknown() {
    return type.isUnknown();
  }

  @Override
  public Symbol owner() {
    return sema.read(() -> {
      if (isUnknown()) {
        return Symbol.UNKNOWN_SYMBOL;
      }
      if (element instanceof PsiClass cls && cls.getContainingClass() != null && !isStatic()) {
        var substitution = ((PsiClassType) type.psi).resolveGenerics().getSubstitutor();
        return sema.type(JavaPsiFacade.getElementFactory(sema.project).createType(cls.getContainingClass(), substitution)).symbol();
      }
      return element == null ? Symbol.ROOT_PACKAGE : super.owner();
    });
  }

  @Override
  public TypeSymbol enclosingClass() {
    return sema.read(() -> {
      var enclosing = element == null ? null : PsiTreeUtil.getParentOfType(element, PsiClass.class);
      return enclosing == null ? this : (TypeSymbol) sema.symbol(enclosing);
    });
  }

  @Override
  public ClassTree declaration() {
    return (ClassTree) super.declaration();
  }

  @Override
  public boolean isAnnotation() {
    return sema.read(() -> element instanceof PsiClass cls && cls.isAnnotationType());
  }

  @Override
  public Type superClass() {
    return sema.read(() -> {
      if (type.isArray() || isInterface()) {
        return sema.getClassType("java.lang.Object");
      }
      if (!(element instanceof PsiClass cls)) {
        return null;
      }
      if (cls instanceof PsiAnonymousClass anonymous) {
        var base = sema.type(anonymous.getBaseClassType());
        return base.symbol().isInterface() ? sema.getClassType("java.lang.Object") : base;
      }
      if (cls instanceof PsiTypeParameter) {
        var supers = type.directSuperTypes();
        return supers.stream().filter(t -> !t.symbol().isInterface()).findFirst().orElse(sema.getClassType("java.lang.Object"));
      }
      var parents = cls.getExtendsListTypes();
      if (parents.length != 0) {
        var substitution = ((PsiClassType) type.psi).resolveGenerics().getSubstitutor();
        return sema.type(substitution.substitute(parents[0]));
      }
      var parent = cls.getSuperClass();
      return parent == null ? null : sema.type(cls.getSuperTypes()[0]);
    });
  }

  @Override
  public List<Type> interfaces() {
    return sema.read(() -> {
      if (element instanceof PsiClass cls && !(cls instanceof PsiTypeParameter)) {
        if (cls instanceof PsiAnonymousClass anonymous) {
          var base = sema.type(anonymous.getBaseClassType());
          return base.isUnknown() || base.symbol().isInterface() ? List.of(base) : List.of();
        }
        var parents = cls.isInterface() ? cls.getExtendsListTypes() : cls.getImplementsListTypes();
        var substitution = ((PsiClassType) type.psi).resolveGenerics().getSubstitutor();
        return java.util.Arrays.stream(parents).map(parent -> sema.type(substitution.substitute(parent))).toList();
      }
      return type.directSuperTypes().stream().filter(t -> t.isUnknown() || t.symbol().isInterface()).toList();
    });
  }

  @Override
  public Collection<Symbol> memberSymbols() {
    return sema.read(() -> {
      List<Symbol> result = new ArrayList<>();
      if (element instanceof PsiClass cls) {
        for (var child : cls.getInnerClasses()) {
          result.add(sema.symbol(child));
        }
        var substitution = type.psi instanceof PsiClassType classType ? classType.resolveGenerics().getSubstitutor() : PsiSubstitutor.EMPTY;
        for (var field : cls.getFields()) {
          result.add(sema.variable(field, substitution));
        }
        for (var method : cls.getMethods()) {
          result.add(sema.method(method, substitution));
        }
        var constructor = LightDefaultConstructor.create(cls);
        if (constructor != null) {
          result.add(sema.method(constructor, substitution));
        }
      }
      return result;
    });
  }

  @Override
  public Collection<Symbol> lookupSymbols(String name) {
    return memberSymbols().stream().filter(symbol -> symbol.name().equals(name)).collect(Collectors.toSet());
  }

  @Override
  public Set<Type> superTypes() {
    return sema.read(() -> {
      Set<Type> result = new HashSet<>();
      collect(type, result);
      return result;
    });
  }

  private static void collect(Type type, Set<Type> result) {
    for (var parent : org.sonar.java.model.JUtils.directSuperTypes(type)) {
      if (result.add(parent)) {
        collect(parent, result);
      }
    }
  }

  @Override
  public TypeSymbol outermostClass() {
    Symbol current = this;
    TypeSymbol result = this;
    while (current != null && !current.isPackageSymbol() && !current.isUnknown()) {
      if (current instanceof TypeSymbol cls) {
        result = cls;
      }
      current = current.owner();
    }
    return result;
  }

  final class SpecialField extends Symbols.DefaultSymbol implements VariableSymbol {
    private final boolean isSuper;

    SpecialField(boolean isSuper) {
      this.isSuper = isSuper;
    }

    @Override
    public String name() {
      return isSuper ? "super" : "this";
    }

    @Override
    public Symbol owner() {
      return PsiTypeSymbol.this;
    }

    @Override
    public Type type() {
      return isSuper && !PsiTypeSymbol.this.isInterface() ? Optional.ofNullable(superClass()).orElse(Type.UNKNOWN) : type;
    }

    @Override
    public boolean isVariableSymbol() {
      return true;
    }

    @Override
    public boolean isFinal() {
      return true;
    }

    @Override
    public boolean isUnknown() {
      return false;
    }

    @Override
    public TypeSymbol enclosingClass() {
      return PsiTypeSymbol.this;
    }

    @Override
    public List<IdentifierTree> usages() {
      return List.of();
    }

    @Override
    public VariableTree declaration() {
      return null;
    }

    @Override
    public boolean isEffectivelyFinal() {
      return false;
    }

    @Override
    public Optional<Object> constantValue() {
      return Optional.empty();
    }
  }
}
