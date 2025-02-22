/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.checks;

import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.ValueBasedUtils;
import org.sonar.java.checks.serialization.SerializableContract;
import org.sonar.java.model.ModifiersUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.VariableTree;

import java.util.Arrays;
import java.util.List;

@Rule(key = "S3437")
public class ValueBasedObjectsShouldNotBeSerializedCheck extends IssuableSubscriptionVisitor {

  private static final String MESSAGE = "Make this value-based field transient so it is not included in the serialization of this class.";

  @Override
  public List<Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.CLASS, Tree.Kind.ENUM, Tree.Kind.ANNOTATION_TYPE);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree classTree = (ClassTree) tree;

    if (classTree.is(Tree.Kind.ANNOTATION_TYPE)) {
      classTree.members().stream()
        .filter(member -> member.is(Kind.METHOD))
        .map(MethodTree.class::cast)
        .filter(ValueBasedObjectsShouldNotBeSerializedCheck::isReturnTypeValueBased)
        .forEach(meth -> reportIssue(meth.simpleName(), MESSAGE));
    } else if (isSerializable(classTree) && !SerializableContract.hasSpecialHandlingSerializationMethods(classTree)) {
      classTree.members().stream()
        .filter(ValueBasedObjectsShouldNotBeSerializedCheck::isVariable)
        .map(VariableTree.class::cast)
        .filter(v -> !isStatic(v))
        .filter(v -> !isTransient(v))
        .filter(ValueBasedObjectsShouldNotBeSerializedCheck::isVarSerializableAndValueBased)
        .forEach(v -> reportIssue(v.simpleName(), MESSAGE));
    }
  }

  private static boolean isVarSerializableAndValueBased(VariableTree variable) {
    return variable.type() != null && isSerializableAndValueBased(variable.type().symbolType());
  }

  private static boolean isReturnTypeValueBased(MethodTree method) {
    return ValueBasedUtils.isValueBased(method.returnType().symbolType());
  }

  private static boolean isSerializableAndValueBased(Type type) {
    // we check first the ParametrizedTypeJavaType, in order to filter out the non-serializable
    // generic value-based class Optional<T>
    if (type.isParameterized()) {
      return isSubtypeOfCollectionApi(type) &&
        type.typeArguments().stream().anyMatch(ValueBasedObjectsShouldNotBeSerializedCheck::isSerializableAndValueBased);
    }
    return ValueBasedUtils.isValueBased(type);
  }

  private static boolean isVariable(Tree member) {
    return member.is(Tree.Kind.VARIABLE, Tree.Kind.ENUM_CONSTANT);
  }

  private static boolean isSerializable(ClassTree classTree) {
    return classTree.symbol().type().isSubtypeOf("java.io.Serializable");
  }

  private static boolean isStatic(VariableTree variable) {
    return ModifiersUtils.hasModifier(variable.modifiers(), Modifier.STATIC);
  }

  private static boolean isTransient(VariableTree variable) {
    return ModifiersUtils.hasModifier(variable.modifiers(), Modifier.TRANSIENT);
  }

  private static boolean isSubtypeOfCollectionApi(Type type) {
    return type.isSubtypeOf("java.util.Collection") || type.isSubtypeOf("java.util.Map");
  }

}
