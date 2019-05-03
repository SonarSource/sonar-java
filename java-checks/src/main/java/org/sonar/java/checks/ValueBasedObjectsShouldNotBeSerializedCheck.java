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

import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.ValueBasedUtils;
import org.sonar.java.checks.serialization.SerializableContract;
import org.sonar.java.model.ModifiersUtils;
import org.sonar.java.resolve.JavaType;
import org.sonar.java.resolve.ParametrizedTypeJavaType;
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
        .map(member -> (MethodTree) member)
        .filter(ValueBasedObjectsShouldNotBeSerializedCheck::isReturnTypeValueBased)
        .forEach(meth -> reportIssue(meth.simpleName(), MESSAGE));
    } else if (isSerializable(classTree) && !SerializableContract.hasSpecialHandlingSerializationMethods(classTree)) {
      classTree.members().stream()
        .filter(ValueBasedObjectsShouldNotBeSerializedCheck::isVariable)
        .map(member -> (VariableTree) member)
        .filter(var -> !isStatic(var))
        .filter(var -> !isTransient(var))
        .filter(ValueBasedObjectsShouldNotBeSerializedCheck::isVarSerializableAndValueBased)
        .forEach(var -> reportIssue(var.simpleName(), MESSAGE));
    }
  }

  private static boolean isVarSerializableAndValueBased(VariableTree var) {
    return var.type() != null && isSerializableAndValueBased(var.type().symbolType());
  }

  private static boolean isReturnTypeValueBased(MethodTree method) {
    return ValueBasedUtils.isValueBased(method.returnType().symbolType());
  }

  private static boolean isSerializableAndValueBased(Type type) {
    // we check first the ParametrizedTypeJavaType, in order to filter out the non-serializable
    // generic value-based class Optional<T>
    JavaType javaType = (JavaType) type;
    if (javaType.isParameterized()) {
      ParametrizedTypeJavaType parameterizedType = (ParametrizedTypeJavaType) javaType;
      return isSubtypeOfCollectionApi(parameterizedType) &&
        parameterizedType.typeParameters().stream()
          .anyMatch(t -> isSerializableAndValueBased(parameterizedType.substitution(t)));
    }
    return ValueBasedUtils.isValueBased(javaType);
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
