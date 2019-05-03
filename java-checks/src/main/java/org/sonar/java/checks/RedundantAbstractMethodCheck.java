/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
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

import com.google.common.collect.ImmutableMultiset;
import org.sonar.check.Rule;
import org.sonar.java.resolve.JavaSymbol;
import org.sonar.java.resolve.MethodJavaType;
import org.sonar.java.resolve.ParametrizedTypeJavaType;
import org.sonar.java.resolve.SymbolMetadataResolve;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata.AnnotationInstance;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.Collections;
import java.util.List;

@Rule(key = "S3038")
public class RedundantAbstractMethodCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    if (!hasSemantic()) {
      return;
    }
    Symbol.MethodSymbol method = ((MethodTree) tree).symbol();
    if (method.isAbstract() && method.owner().isAbstract()) {
      checkMethod((JavaSymbol.MethodJavaSymbol) method);
    }
  }

  private void checkMethod(JavaSymbol.MethodJavaSymbol method) {
    JavaSymbol.MethodJavaSymbol overridee = method.overriddenSymbol();
    if (overridee != null && overridee.owner().isInterface() && !differentContract(method, overridee)) {
      reportIssue(method.declaration(), "\"" + method.name() + "\" is defined in the \"" + overridee.owner().name() + "\" interface and can be removed from this class.");
    }
  }

  private static boolean differentContract(JavaSymbol.MethodJavaSymbol method, JavaSymbol.MethodJavaSymbol overridee) {
    return removingParametrizedAspect(method, overridee)
      || differentThrows(method, overridee)
      || differentReturnType(method, overridee)
      || differentParameters(method, overridee)
      || differentAnnotations(method.metadata(), overridee.metadata());
  }

  private static boolean removingParametrizedAspect(JavaSymbol.MethodJavaSymbol method, JavaSymbol.MethodJavaSymbol overridee) {
    return !method.isParametrized() && overridee.isParametrized();
  }

  private static boolean differentThrows(JavaSymbol.MethodJavaSymbol method, JavaSymbol.MethodJavaSymbol overridee) {
    return !ImmutableMultiset.copyOf(method.thrownTypes()).equals(ImmutableMultiset.copyOf(overridee.thrownTypes()));
  }

  private static boolean differentReturnType(JavaSymbol.MethodJavaSymbol method, JavaSymbol.MethodJavaSymbol overridee) {
    Type methodResultType = resultType(method);
    Type overrideeResultType = resultType(overridee);
    return specializationOfReturnType(methodResultType.erasure(), overrideeResultType.erasure()) || useRawTypeOfParametrizedType(methodResultType, overrideeResultType);
  }

  private static Type resultType(JavaSymbol.MethodJavaSymbol method) {
    return ((MethodJavaType) method.type()).resultType();
  }

  private static boolean specializationOfReturnType(Type methodResultType, Type overrideeResultType) {
    return !methodResultType.isVoid()
      && (methodResultType.isSubtypeOf(overrideeResultType) && !overrideeResultType.isSubtypeOf(methodResultType));
  }

  private static boolean differentParameters(JavaSymbol.MethodJavaSymbol method, JavaSymbol.MethodJavaSymbol overridee) {
    return useRawTypeOfParametrizedType(method.parameterTypes(), overridee.parameterTypes())
      || differentAnnotations(method.getParameters().scopeSymbols(), overridee.getParameters().scopeSymbols());
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
    return overrideeParam instanceof ParametrizedTypeJavaType && methodParam.equals(overrideeParam.erasure());
  }

  private static boolean differentAnnotations(List<JavaSymbol> methodParamSymbols, List<JavaSymbol> overrideeParamSymbols) {
    for (int i = 0; i < methodParamSymbols.size(); i++) {
      if (differentAnnotations(methodParamSymbols.get(i).metadata(), overrideeParamSymbols.get(i).metadata())) {
        return true;
      }
    }
    return false;
  }

  private static boolean differentAnnotations(SymbolMetadataResolve methodMetadata, SymbolMetadataResolve overrideeMetadata) {
    for (AnnotationInstance annotation : methodMetadata.annotations()) {
      Type type = annotation.symbol().type();
      if (!type.is("java.lang.Override") && !overrideeMetadata.isAnnotatedWith(type.fullyQualifiedName())) {
        return true;
      }
    }
    return false;
  }
}
