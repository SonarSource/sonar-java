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

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.matcher.TypeCriteria;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.model.JUtils;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;

@Rule(key = "S2175")
public class CollectionInappropriateCallsCheck extends AbstractMethodDetection {

  @Override
  protected List<MethodMatcher> getMethodInvocationMatchers() {
    return Arrays.asList(
      collectionMethodInvocation("remove"),
      collectionMethodInvocation("contains")
    );
  }

  private static MethodMatcher collectionMethodInvocation(String methodName) {
    return MethodMatcher.create()
      .typeDefinition(TypeCriteria.subtypeOf("java.util.Collection"))
      .name(methodName)
      .addParameter("java.lang.Object");
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree tree) {
    ExpressionTree firstArgument = tree.arguments().get(0);
    Type argumentType = firstArgument.symbolType();
    if (argumentType.isUnknown()) {
      // could happen with type inference.
      return;
    }
    Type collectionType = getMethodOwner(tree);
    // can be null when using raw types
    Type collectionParameterType = getTypeArgument(collectionType);

    boolean isCallToParametrizedOrUnknownMethod = isCallToParametrizedOrUnknownMethod(firstArgument);
    if (!isCallToParametrizedOrUnknownMethod && tree.methodSelect().is(Tree.Kind.MEMBER_SELECT)) {
      isCallToParametrizedOrUnknownMethod = isCallToParametrizedOrUnknownMethod(((MemberSelectExpressionTree) tree.methodSelect()).expression());
    }

    if (collectionParameterType != null
      && !collectionParameterType.isUnknown()
      && !isCallToParametrizedOrUnknownMethod
      && !isArgumentCompatible(argumentType, collectionParameterType)) {
      reportIssue(ExpressionUtils.methodName(tree), MessageFormat.format("A \"{0}<{1}>\" cannot contain a \"{2}\"", collectionType, collectionParameterType, argumentType));
    }
  }

  private static boolean isCallToParametrizedOrUnknownMethod(ExpressionTree expressionTree) {
    if (expressionTree.is(Tree.Kind.METHOD_INVOCATION)) {
      Symbol.MethodSymbol symbol = (Symbol.MethodSymbol) ((MethodInvocationTree) expressionTree).symbol();
      return symbol.isUnknown() || JUtils.isParametrizedMethod(symbol);
    }
    return false;
  }

  private static Type getMethodOwner(MethodInvocationTree mit) {
    if (mit.methodSelect().is(Kind.MEMBER_SELECT)) {
      return ((MemberSelectExpressionTree) mit.methodSelect()).expression().symbolType();
    }
    return mit.symbol().owner().type();
  }

  @Nullable
  private static Type getTypeArgument(Type collectionType) {
    if (collectionType.is("java.util.Collection") && JUtils.isParametrized(collectionType)) {
      return JUtils.typeArguments(collectionType).get(0);
    }
    return JUtils.directSuperTypes(collectionType)
      .stream()
      .map(CollectionInappropriateCallsCheck::getTypeArgument)
      .filter(Objects::nonNull)
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
}
