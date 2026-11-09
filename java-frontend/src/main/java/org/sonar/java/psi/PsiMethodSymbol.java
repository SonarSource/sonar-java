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
import com.intellij.psi.PsiAnnotationMethod;
import com.intellij.psi.PsiArrayType;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiLambdaExpression;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiSubstitutor;
import com.intellij.psi.PsiTypes;
import com.intellij.psi.util.MethodSignatureUtil;
import com.intellij.psi.util.TypeConversionUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.MethodTree;

final class PsiMethodSymbol extends PsiSymbol implements Symbol.MethodSymbol {
  final PsiMethod method;
  final PsiSubstitutor substitutor;
  private final PsiLambdaExpression lambda;

  PsiMethodSymbol(PsiSema sema, PsiMethod method, PsiSubstitutor substitutor, PsiLambdaExpression lambda, String key) {
    super(sema, lambda == null ? method : lambda, key);
    this.method = method;
    this.substitutor = substitutor;
    this.lambda = lambda;
  }

  boolean isLambda() { return lambda != null; }

  @Override
  public String name() {
    return sema.read(() -> method.isConstructor() ? "<init>" : method.getName());
  }

  @Override
  public Symbol owner() {
    return sema.read(() -> sema.type(JavaPsiFacade.getElementFactory(sema.project).createType(method.getContainingClass(), substitutor)).symbol());
  }

  @Override
  public TypeSymbol enclosingClass() {
    return (TypeSymbol) owner();
  }

  @Override
  public List<Type> parameterTypes() {
    return sema.read(() -> Arrays.stream(method.getParameterList().getParameters()).map(p -> sema.type(substitute(p.getType()))).toList());
  }

  @Override
  public Symbol.TypeSymbol returnType() {
    return sema.read(() -> sema.type(method.isConstructor() ? PsiTypes.voidType() : substitute(method.getReturnType())).symbol());
  }

  private com.intellij.psi.PsiType substitute(com.intellij.psi.PsiType type) {
    var result = substitutor.substitute(type);
    return result != null && !method.hasModifierProperty(PsiModifier.STATIC) && !method.hasTypeParameters()
      && Arrays.stream(method.getContainingClass().getTypeParameters()).anyMatch(parameter -> substitutor.substitute(parameter) == null)
      ? TypeConversionUtil.erasure(result) : result;
  }

  @Override
  public List<Type> thrownTypes() {
    return sema.read(() -> Arrays.stream(method.getThrowsList().getReferencedTypes()).map(t -> sema.type(substitutor.substitute(t))).toList());
  }

  @Override
  public MethodTree declaration() {
    return lambda == null ? (MethodTree) super.declaration() : null;
  }

  @Override
  public boolean isParametrizedMethod() {
    return sema.read(() -> method.hasTypeParameters());
  }

  @Override
  public boolean isDefaultMethod() {
    return modifier(PsiModifier.DEFAULT);
  }

  @Override
  public boolean isSynchronizedMethod() {
    return modifier(PsiModifier.SYNCHRONIZED);
  }

  @Override
  public boolean isNativeMethod() {
    return modifier(PsiModifier.NATIVE);
  }

  @Override
  public boolean isVarArgsMethod() {
    return sema.read(method::isVarArgs);
  }

  @Override
  public boolean isOverridable() {
    return !isPrivate() && !isStatic() && !isFinal() && !owner().isFinal();
  }

  @Override
  public List<Symbol> declarationParameters() {
    return sema.read(() -> Arrays.stream(lambda == null ? method.getParameterList().getParameters() : lambda.getParameterList().getParameters())
      .map(sema::symbol).toList());
  }

  @Override
  public Object annotationElementDefaultValue() {
    return sema.read(() -> method instanceof PsiAnnotationMethod annotation ? PsiSymbolMetadata.value(sema, annotation.getDefaultValue()) : null);
  }

  @Override
  public List<MethodSymbol> overriddenSymbols() {
    return sema.read(() -> {
      Set<MethodSymbol> result = new LinkedHashSet<>();
      if (!isStatic() && !isPrivate()) {
        collectOverrides(method.getContainingClass(), substitutor, result, new HashSet<>());
      }
      return List.copyOf(result);
    });
  }

  private void collectOverrides(PsiClass current, PsiSubstitutor substitution, Set<MethodSymbol> result, Set<String> seen) {
    var parents = new ArrayList<>(Arrays.asList(current.getSuperTypes()));
    if (current.isInterface()) {
      var object = sema.resolveType("java.lang.Object");
      if (object instanceof PsiClassType objectType) {
        parents.addFirst(objectType);
      }
    }
    for (var type : parents) {
      if (!(substitution.substitute(type) instanceof PsiClassType parentType)) {
        continue;
      }
      var resolved = parentType.resolveGenerics();
      var cls = resolved.getElement();
      if (cls == null || !seen.add(sema.key(cls))) {
        continue;
      }
      for (var parent : method.isConstructor() ? cls.getConstructors() : cls.findMethodsByName(method.getName(), false)) {
        if (!parent.hasModifierProperty(PsiModifier.PRIVATE) && !parent.hasModifierProperty(PsiModifier.STATIC)
          && (method.isConstructor() ? sameConstructorParameters(parent, resolved.getSubstitutor())
            : MethodSignatureUtil.isSubsignature(parent.getSignature(resolved.getSubstitutor()), method.getSignature(substitutor)))) {
          result.add(sema.method(parent, resolved.getSubstitutor()));
          break;
        }
      }
      collectOverrides(cls, resolved.getSubstitutor(), result, seen);
    }
  }

  private boolean sameConstructorParameters(PsiMethod parent, PsiSubstitutor parentSubstitution) {
    return Arrays.equals(
      parent.getSignature(parentSubstitution).getParameterTypes(), method.getSignature(substitutor).getParameterTypes());
  }

  @Override
  public String signature() {
    return sema.read(() -> sema.binaryName(method.getContainingClass()) + "#" + name() + "("
      + Arrays.stream(method.getParameterList().getParameters()).map(p -> descriptor(p.getType())).collect(Collectors.joining())
      + ")" + descriptor(method.isConstructor() ? PsiTypes.voidType() : method.getReturnType()));
  }

  private String descriptor(com.intellij.psi.PsiType type) {
    var erased = TypeConversionUtil.erasure(type);
    if (erased instanceof PsiArrayType array) {
      return "[" + descriptor(array.getComponentType());
    }
    return switch (erased.getCanonicalText(false)) {
      case "void" -> "V";
      case "boolean" -> "Z";
      case "byte" -> "B";
      case "char" -> "C";
      case "short" -> "S";
      case "int" -> "I";
      case "long" -> "J";
      case "float" -> "F";
      case "double" -> "D";
      default -> "L" + sema.type(erased).fullyQualifiedName().replace('.', '/') + ";";
    };
  }
}
