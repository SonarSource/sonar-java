/*
 * SonarQube Java
 * Copyright (C) 2012-2022 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.java.checks;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.model.JUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.semantic.SymbolMetadata.AnnotationInstance;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S3038")
public class RedundantAbstractMethodCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    Symbol.MethodSymbol method = ((MethodTree) tree).symbol();
    if (method.isAbstract() && method.owner().isAbstract()) {
      checkMethod(method);
    }
  }

  private void checkMethod(Symbol.MethodSymbol method) {
    List<Symbol.MethodSymbol> overridees = method.overriddenSymbols();
    if (overridees.isEmpty()) {
      return;
    }
    Symbol.MethodSymbol overridee = overridees.get(0);
    if (!overridees.isEmpty() && overridee.owner().isInterface() && !differentContract(method, overridee)) {
      reportIssue(method.declaration(), "\"" + method.name() + "\" is defined in the \"" + overridee.owner().name() + "\" interface and can be removed from this class.");
    }
  }

  private static boolean differentContract(Symbol.MethodSymbol method, Symbol.MethodSymbol overridee) {
    return removingParametrizedAspect(method, overridee)
      || differentThrows(method, overridee)
      || differentReturnType(method, overridee)
      || differentParameters(method, overridee)
      || differentAnnotations(method.metadata(), overridee.metadata());
  }

  private static boolean removingParametrizedAspect(Symbol.MethodSymbol method, Symbol.MethodSymbol overridee) {
    return !JUtils.isParametrizedMethod(method) && JUtils.isParametrizedMethod(overridee);
  }

  private static boolean differentThrows(Symbol.MethodSymbol method, Symbol.MethodSymbol overridee) {
    return !(new HashSet<>(method.thrownTypes()).equals(new HashSet<>(overridee.thrownTypes())));
  }

  private static boolean differentReturnType(Symbol.MethodSymbol method, Symbol.MethodSymbol overridee) {
    Type methodResultType = resultType(method);
    Type overrideeResultType = resultType(overridee);
    return specializationOfReturnType(methodResultType.erasure(), overrideeResultType.erasure()) || useRawTypeOfParametrizedType(methodResultType, overrideeResultType);
  }

  private static Type resultType(Symbol.MethodSymbol method) {
    return method.returnType().type();
  }

  private static boolean specializationOfReturnType(Type methodResultType, Type overrideeResultType) {
    return !methodResultType.isVoid()
      && (methodResultType.isSubtypeOf(overrideeResultType) && !overrideeResultType.isSubtypeOf(methodResultType));
  }

  private static boolean differentParameters(Symbol.MethodSymbol method, Symbol.MethodSymbol overridee) {
    return useRawTypeOfParametrizedType(method.parameterTypes(), overridee.parameterTypes())
      || differentAnnotationsOnParameters(method, overridee);
  }

  private static boolean useRawTypeOfParametrizedType(List<Type> methodParamTypes, List<Type> overrideeParamType) {
    for (int i = 0; i < methodParamTypes.size(); i++) {
      if (useRawTypeOfParametrizedType(methodParamTypes.get(i), overrideeParamType.get(i))) {
        return true;
      }
    }
    return false;
  }

  private static boolean useRawTypeOfParametrizedType(Type methodParam, Type overrideeParam) {
    return !methodParam.isParameterized()
      && overrideeParam.isParameterized()
      && methodParam.erasure().equals(overrideeParam.erasure());
  }

  private static boolean differentAnnotationsOnParameters(Symbol.MethodSymbol method, Symbol.MethodSymbol overridee) {
    for (int i = 0; i < method.parameterTypes().size(); i++) {
      if (differentAnnotations(
        JUtils.parameterAnnotations(method, i),
        JUtils.parameterAnnotations(overridee, i))) {
        return true;
      }
    }
    return false;
  }

  private static boolean differentAnnotations(SymbolMetadata methodMetadata, SymbolMetadata overrideeMetadata) {
    for (AnnotationInstance annotation : methodMetadata.annotations()) {
      Type type = annotation.symbol().type();
      if (!type.is("java.lang.Override") && !overrideeMetadata.isAnnotatedWith(type.fullyQualifiedName())) {
        return true;
      }
    }
    return false;
  }
}
