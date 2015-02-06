/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.java.checks;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.checks.methods.MethodInvocationMatcher;
import org.sonar.java.checks.methods.TypeCriteria;
import org.sonar.java.model.AbstractTypedTree;
import org.sonar.java.model.expression.MethodInvocationTreeImpl;
import org.sonar.java.resolve.Type;
import org.sonar.java.resolve.Type.ParametrizedTypeType;
import org.sonar.java.resolve.Type.TypeVariableType;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import javax.annotation.Nullable;

import java.text.MessageFormat;
import java.util.List;
import java.util.Map;

@Rule(
  key = "S2175",
  name = "Inappropriate \"Collection\" calls should not be made",
  tags = {"bug"},
  priority = Priority.CRITICAL)
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.DATA_RELIABILITY)
@SqaleConstantRemediation("15min")
public class CollectionInappropriateCallsCheck extends AbstractMethodDetection {

  private final Map<String, String> primitiveToPrimitiveWrapper = buildPrimitiveMapper();

  @Override
  protected List<MethodInvocationMatcher> getMethodInvocationMatchers() {
    return ImmutableList.of(
      collectionMethodInvocation("remove"),
      collectionMethodInvocation("contains")
      );
  }

  private MethodInvocationMatcher collectionMethodInvocation(String methodName) {
    return MethodInvocationMatcher.create()
      .typeDefinition(TypeCriteria.subtypeOf("java.util.Collection"))
      .name(methodName)
      .addParameter("java.lang.Object");
  }

  @Override
  protected void onMethodFound(MethodInvocationTree tree) {
    Type argumentType = getType(tree.arguments().get(0));
    Type collectionType = getMethodOwner(tree);
    Type collectionParameterType = getTypeParameter(collectionType);

    if (isKnown(collectionParameterType) && !isArgumentCompatible(argumentType, collectionParameterType)) {
      addIssue(tree, MessageFormat.format("A \"{0}<{1}>\" cannot contain a \"{2}\"", collectionType, collectionParameterType, argumentType));
    }
  }

  private Type getType(ExpressionTree tree) {
    return ((AbstractTypedTree) tree).getSymbolType();
  }

  private Type getMethodOwner(MethodInvocationTree mit) {
    if (mit.methodSelect().is(Kind.MEMBER_SELECT)) {
      return getType(((MemberSelectExpressionTree) mit.methodSelect()).expression());
    }
    return ((MethodInvocationTreeImpl) mit).getSymbol().owner().getType();
  }

  @Nullable
  private Type getTypeParameter(Type collectionType) {
    if (collectionType instanceof ParametrizedTypeType) {
      return getFirstTypeParameter((ParametrizedTypeType) collectionType);
    }
    return null;
  }

  @Nullable
  private Type getFirstTypeParameter(ParametrizedTypeType parametrizedTypeType) {
    for (TypeVariableType variableType : parametrizedTypeType.typeParameters()) {
      return parametrizedTypeType.substitution(variableType);
    }
    return null;
  }

  private boolean isArgumentCompatible(Type argumentType, Type collectionParameterType) {
    return (isSubtypeOf(argumentType, collectionParameterType) || isBoxingPossible(argumentType, collectionParameterType));
  }

  private boolean isSubtypeOf(Type argumentType, Type collectionParameterType) {
    return argumentType.isSubtypeOf(collectionParameterType.getSymbol().getFullyQualifiedName());
  }

  private boolean isKnown(Type type) {
    return type != null && !type.isTagged(Type.UNKNOWN);
  }

  private boolean isBoxingPossible(Type argumentType, Type collectionParameterType) {
    return argumentType.isPrimitive() && collectionParameterType.isPrimitiveWrapper() && boxingMatch(argumentType, collectionParameterType);
  }

  private boolean boxingMatch(Type primitiveType, Type primitiveWrapperType) {
    String primitiveWrapperFullyQualifiedName = primitiveToPrimitiveWrapper.get(primitiveType.getSymbol().getFullyQualifiedName());
    return primitiveWrapperFullyQualifiedName.equals(primitiveWrapperType.getSymbol().getFullyQualifiedName());
  }

  private Map<String, String> buildPrimitiveMapper() {
    Map<String, String> map = Maps.newHashMap();
    map.put("int", "java.lang.Integer");
    map.put("boolean", "java.lang.Boolean");
    map.put("byte", "java.lang.Byte");
    map.put("double", "java.lang.Double");
    map.put("char", "java.lang.Character");
    map.put("short", "java.lang.Short");
    map.put("float", "java.lang.Float");
    map.put("long", "java.lang.Long");
    return map;
  }
}
