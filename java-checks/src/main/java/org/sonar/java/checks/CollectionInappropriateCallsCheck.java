/*
 * SonarQube Java
 * Copyright (C) 2012-2020 SonarSource SA
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
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.matcher.TypeCriteria;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.model.JUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;

@Rule(key = "S2175")
public class CollectionInappropriateCallsCheck extends IssuableSubscriptionVisitor {

  private static final List<TypeChecker> TYPE_CHECKERS = new TypeCheckerListBuilder()
    .on("java.util.Collection")
      .method("remove").argument(1).outOf(1).shouldMatchParametrizedType(1).add()
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
    if (hasSemantic()) {
      MethodInvocationTree mit = (MethodInvocationTree) tree;
      TYPE_CHECKERS.stream()
        .filter(typeChecker -> typeChecker.methodMatcher.matches(mit))
        .forEach(typeChecker -> checkMethodInvocation(mit, typeChecker));
    }
  }

  private void checkMethodInvocation(MethodInvocationTree tree, TypeChecker typeChecker) {
    ExpressionTree argument = tree.arguments().get(typeChecker.argumentIndex);
    if (argument.symbolType().isUnknown()) {
      // could happen with type inference.
      return;
    }

    Type actualMethodType = getMethodOwnerType(tree);
    Type checkedMethodType = findSuperTypeMatching(actualMethodType, typeChecker.methodOwnerType);
    if (checkedMethodType == null || !JUtils.isParametrized(checkedMethodType)) {
      return;
    }

    List<Type> parameters = JUtils.typeArguments(checkedMethodType);
    if (parameters.size() <= typeChecker.parametrizedTypeIndex) {
      return;
    }

    boolean isCallToParametrizedOrUnknownMethod = isCallToParametrizedOrUnknownMethod(argument);
    if (!isCallToParametrizedOrUnknownMethod && tree.methodSelect().is(Tree.Kind.MEMBER_SELECT)) {
      isCallToParametrizedOrUnknownMethod = isCallToParametrizedOrUnknownMethod(((MemberSelectExpressionTree) tree.methodSelect()).expression());
    }

    Type parameterType = parameters.get(typeChecker.parametrizedTypeIndex);
    if (!parameterType.isUnknown()
      && !isCallToParametrizedOrUnknownMethod
      && !isArgumentCompatible(argument.symbolType(), parameterType)) {
      reportIssue(ExpressionUtils.methodName(tree), message(actualMethodType, checkedMethodType, parameterType, argument.symbolType()));
    }
  }

  private static String message(Type actualMethodType, Type checkedMethodType, Type parameterType, Type argumentType) {
    String actualType = typeNameWithParameters(actualMethodType);
    boolean actualTypeHasTheParameterType = JUtils.typeArguments(actualMethodType).stream().anyMatch(typeArg -> typeArg.equals(parameterType));
    boolean checkedTypeHasSeveralParameters = JUtils.typeArguments(checkedMethodType).size() > 1;
    String typeDescription = checkedTypeHasSeveralParameters ? (" in a \"" + parameterType + "\" type") : "";
    if (actualTypeHasTheParameterType) {
      return MessageFormat.format("A \"{0}\" cannot contain a \"{1}\"{2}.", actualType, argumentType.name(), typeDescription);
    } else {
      String checkedType = typeNameWithParameters(checkedMethodType);
      return MessageFormat.format("\"{0}\" is a \"{1}\" which cannot contain a \"{2}\"{3}.",
        actualType, checkedType, argumentType.name(), typeDescription);
    }
  }

  private static String typeNameWithParameters(Type type) {
    if (JUtils.isParametrized(type)) {
      return type.name() + JUtils.typeArguments(type).stream()
        .map(Type::name)
        .collect(Collectors.joining(", ", "<", ">"));
    }
    return type.name();
  }

  private static boolean isCallToParametrizedOrUnknownMethod(ExpressionTree expressionTree) {
    if (expressionTree.is(Tree.Kind.METHOD_INVOCATION)) {
      Symbol.MethodSymbol symbol = (Symbol.MethodSymbol) ((MethodInvocationTree) expressionTree).symbol();
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

  @Nullable
  private static Type findSuperTypeMatching(Type type, String genericTypeName) {
    if (type.is(genericTypeName)) {
      return type;
    }
    return JUtils.superTypes(type.symbol())
      .stream()
      .filter(superType -> superType.is(genericTypeName))
      .findFirst()
      .orElse(null);
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
      && JUtils.isPrimitiveWrapper(collectionParameterType)
      && isSubtypeOf(JUtils.primitiveWrapperType(argumentType), collectionParameterType);
  }

  private static class TypeChecker {
    private final String methodOwnerType;
    private final MethodMatcher methodMatcher;
    private final int argumentIndex;
    private final int parametrizedTypeIndex;

    private TypeChecker(String methodOwnerType, MethodMatcher methodMatcher, int argumentIndex, int parametrizedTypeIndex) {
      this.methodOwnerType = methodOwnerType;
      this.methodMatcher = methodMatcher;
      this.argumentIndex = argumentIndex;
      this.parametrizedTypeIndex = parametrizedTypeIndex;
    }
  }

  private static class TypeCheckerListBuilder {

    private final List<TypeChecker> typeCheckers = new ArrayList<>();

    private String methodOwnerType;
    private String methodName;
    private int argumentPosition;
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
      return this;
    }

    private TypeCheckerListBuilder add() {
      int argumentIndex = argumentPosition - 1;
      int parametrizedTypeIndex = parametrizedTypePosition - 1;
      MethodMatcher methodMatcher = MethodMatcher.create().typeDefinition(TypeCriteria.subtypeOf(methodOwnerType)).name(methodName);
      for (int i = 0; i < argumentCount; i++) {
        methodMatcher.addParameter(i == argumentIndex ? TypeCriteria.is("java.lang.Object") : TypeCriteria.anyType());
      }
      typeCheckers.add(new TypeChecker(methodOwnerType, methodMatcher, argumentIndex, parametrizedTypeIndex));
      return this;
    }

    private List<TypeChecker> build() {
      return typeCheckers;
    }

  }

}
