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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.sonar.check.Rule;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.model.JUtils;
import org.sonar.java.model.Symbols;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;

import static org.sonar.plugins.java.api.semantic.MethodMatchers.ANY;

@Rule(key = "S2175")
public class CollectionInappropriateCallsCheck extends IssuableSubscriptionVisitor {

  private static final String JAVA_UTIL_COLLECTION = "java.util.Collection";

  private static final List<TypeChecker> TYPE_CHECKERS = new TypeCheckerListBuilder()
    .on(JAVA_UTIL_COLLECTION)
      .method("remove").argument(1).outOf(1).shouldMatchParametrizedType(1).add()
      .method("removeAll").argument(1).outOf(1).shouldMatchCollectionOfParametrizedType(1).add()
      .method("contains").argument(1).outOf(1).shouldMatchParametrizedType(1).add()
    .on("java.util.List")
      .method("indexOf").argument(1).outOf(1).shouldMatchParametrizedType(1).add()
      .method("lastIndexOf").argument(1).outOf(1).shouldMatchParametrizedType(1).add()
    .on("java.util.Map")
      .method("containsKey").argument(1).outOf(1).shouldMatchParametrizedType(1).add()
      .method("containsValue").argument(1).outOf(1).shouldMatchParametrizedType(2).add()
      .method("get").argument(1).outOf(1).shouldMatchParametrizedType(1).add()
      .method("getOrDefault").argument(1).outOf(2).shouldMatchParametrizedType(1).add()
      .method("remove")
        .argument(1).outOf(1).shouldMatchParametrizedType(1).add()
        .argument(1).outOf(2).shouldMatchParametrizedType(1).add()
        .argument(2).outOf(2).shouldMatchParametrizedType(2).add()
    .build();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Kind.METHOD_INVOCATION);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodInvocationTree mit = (MethodInvocationTree) tree;
    TYPE_CHECKERS.stream()
      .filter(typeChecker -> typeChecker.methodMatcher.matches(mit))
      .forEach(typeChecker -> checkMethodInvocation(mit, typeChecker));
  }

  private void checkMethodInvocation(MethodInvocationTree tree, TypeChecker typeChecker) {
    ExpressionTree argument = tree.arguments().get(typeChecker.argumentIndex);
    Type argumentTypeToCheck = argument.symbolType();
    if (typeChecker.argumentIsACollection) {
      argumentTypeToCheck = getTypeArgumentAt(findSuperTypeMatching(argumentTypeToCheck, JAVA_UTIL_COLLECTION), 0);
    }
    if (argumentTypeToCheck.isUnknown()) {
      // could happen with type inference.
      return;
    }

    Type actualMethodType = getMethodOwnerType(tree);
    Type checkedMethodType = findSuperTypeMatching(actualMethodType, typeChecker.methodOwnerType);
    Type parameterType = getTypeArgumentAt(checkedMethodType, typeChecker.parametrizedTypeIndex);

    boolean isCallToParametrizedOrUnknownMethod = isCallToParametrizedOrUnknownMethod(argument);
    if (!isCallToParametrizedOrUnknownMethod && tree.methodSelect().is(Tree.Kind.MEMBER_SELECT)) {
      isCallToParametrizedOrUnknownMethod = isCallToParametrizedOrUnknownMethod(((MemberSelectExpressionTree) tree.methodSelect()).expression());
    }
    if (!checkedMethodType.isUnknown()
      && !parameterType.isUnknown()
      && !isCallToParametrizedOrUnknownMethod
      && !isArgumentCompatible(argumentTypeToCheck, parameterType)) {
      reportIssue(ExpressionUtils.methodName(tree), message(actualMethodType, checkedMethodType, parameterType, argumentTypeToCheck));
    }
  }

  private static String message(Type actualMethodType, Type checkedMethodType, Type parameterType, Type argumentType) {
    String actualType = typeNameWithParameters(actualMethodType);
    boolean actualTypeHasTheParameterType = actualMethodType.typeArguments().stream().anyMatch(typeArg -> typeArg.equals(parameterType));
    boolean checkedTypeHasSeveralParameters = checkedMethodType.typeArguments().size() > 1;
    String typeDescription = checkedTypeHasSeveralParameters ? (" in a \"" + parameterType + "\" type") : "";
    if (actualTypeHasTheParameterType) {
      return MessageFormat.format("A \"{0}\" cannot contain a \"{1}\"{2}.", actualType, argumentType.name(), typeDescription);
    }
    String checkedType = typeNameWithParameters(checkedMethodType);
    return MessageFormat.format("\"{0}\" is a \"{1}\" which cannot contain a \"{2}\"{3}.",
      actualType, checkedType, argumentType.name(), typeDescription);
  }

  private static String typeNameWithParameters(Type type) {
    if (type.isParameterized()) {
      return type.name() + type.typeArguments().stream()
        .map(Type::name)
        .collect(Collectors.joining(", ", "<", ">"));
    }
    return type.name();
  }

  private static boolean isCallToParametrizedOrUnknownMethod(ExpressionTree expressionTree) {
    if (expressionTree.is(Tree.Kind.METHOD_INVOCATION)) {
      Symbol.MethodSymbol symbol = ((MethodInvocationTree) expressionTree).symbol();
      return symbol.isUnknown() || JUtils.isParametrizedMethod(symbol);
    }
    return false;
  }

  private static Type getMethodOwnerType(MethodInvocationTree mit) {
    if (mit.methodSelect().is(Kind.MEMBER_SELECT)) {
      return ((MemberSelectExpressionTree) mit.methodSelect()).expression().symbolType();
    }
    return mit.symbol().owner().type();
  }

  private static Type getTypeArgumentAt(Type type, int index) {
    if (type.isParameterized()) {
      List<Type> parameters = type.typeArguments();
      if (index < parameters.size()) {
        return parameters.get(index);
      }
    }
    return Symbols.unknownType;
  }

  private static Type findSuperTypeMatching(Type type, String genericTypeName) {
    if (type.is(genericTypeName)) {
      return type;
    }
    return JUtils.superTypes(type.symbol())
      .stream()
      .filter(superType -> superType.is(genericTypeName))
      .findFirst()
      .orElse(Symbols.unknownType);
  }

  private static boolean isArgumentCompatible(Type argumentType, Type collectionParameterType) {
    return isSubtypeOf(argumentType, collectionParameterType)
      || isSubtypeOf(collectionParameterType, argumentType)
      || autoboxing(argumentType, collectionParameterType);
  }

  private static boolean isSubtypeOf(Type type, Type superType) {
    return type.isSubtypeOf(superType.erasure());
  }

  private static boolean autoboxing(Type argumentType, Type collectionParameterType) {
    return argumentType.isPrimitive()
      && isSubtypeOf(JUtils.primitiveWrapperType(argumentType), collectionParameterType);
  }

  private static class TypeChecker {
    private final String methodOwnerType;
    private final MethodMatchers methodMatcher;
    private final int argumentIndex;
    private boolean argumentIsACollection;
    private final int parametrizedTypeIndex;

    private TypeChecker(String methodOwnerType, MethodMatchers methodMatcher, int argumentIndex, boolean argumentIsACollection, int parametrizedTypeIndex) {
      this.methodOwnerType = methodOwnerType;
      this.methodMatcher = methodMatcher;
      this.argumentIndex = argumentIndex;
      this.argumentIsACollection = argumentIsACollection;
      this.parametrizedTypeIndex = parametrizedTypeIndex;
    }
  }

  private static class TypeCheckerListBuilder {

    private final List<TypeChecker> typeCheckers = new ArrayList<>();

    private String methodOwnerType;
    private String methodName;
    private int argumentPosition;
    private boolean argumentIsACollection;
    private int argumentCount;
    private int parametrizedTypePosition;

    private TypeCheckerListBuilder on(String methodOwnerType) {
      this.methodOwnerType = methodOwnerType;
      return this;
    }

    private TypeCheckerListBuilder method(String methodName) {
      this.methodName = methodName;
      return this;
    }

    private TypeCheckerListBuilder argument(int argumentPosition) {
      this.argumentPosition = argumentPosition;
      return this;
    }

    private TypeCheckerListBuilder outOf(int argumentCount) {
      this.argumentCount = argumentCount;
      return this;
    }

    private TypeCheckerListBuilder shouldMatchParametrizedType(int parametrizedTypePosition) {
      this.parametrizedTypePosition = parametrizedTypePosition;
      this.argumentIsACollection = false;
      return this;
    }

    private TypeCheckerListBuilder shouldMatchCollectionOfParametrizedType(int parametrizedTypePosition) {
      this.parametrizedTypePosition = parametrizedTypePosition;
      this.argumentIsACollection = true;
      return this;
    }

    private TypeCheckerListBuilder add() {
      int argumentIndex = argumentPosition - 1;
      int parametrizedTypeIndex = parametrizedTypePosition - 1;

      List<String> methodMatcherParameters = new ArrayList<>();
      for (int i = 0; i < argumentCount; i++) {
        String parameterType = ANY;
        if (i == argumentIndex) {
          parameterType = argumentIsACollection ? JAVA_UTIL_COLLECTION : "java.lang.Object";
        }
        methodMatcherParameters.add(parameterType);
      }

      MethodMatchers methodMatcher = MethodMatchers.create()
        .ofSubTypes(methodOwnerType)
        .names(methodName)
        .addParametersMatcher(methodMatcherParameters.toArray(new String[0]))
        .build();

      typeCheckers.add(new TypeChecker(methodOwnerType, methodMatcher, argumentIndex, argumentIsACollection, parametrizedTypeIndex));
      return this;
    }

    private List<TypeChecker> build() {
      return typeCheckers;
    }

  }

}
