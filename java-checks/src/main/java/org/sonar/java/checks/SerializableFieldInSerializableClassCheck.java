/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
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
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.model.ModifiersUtils;
import org.sonar.java.resolve.JavaType;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.ParameterizedTypeTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonar.plugins.java.api.tree.WildcardTree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import javax.annotation.Nullable;

import java.util.List;

@Rule(
  key = "S1948",
  name = "Fields in a \"Serializable\" class should either be transient or serializable",
  tags = {"bug", "cwe", "serialization"},
  priority = Priority.CRITICAL)
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.DATA_RELIABILITY)
@SqaleConstantRemediation("30min")
public class SerializableFieldInSerializableClassCheck extends SubscriptionBaseVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.CLASS, Tree.Kind.ENUM);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree classTree = (ClassTree) tree;
    if (isSerializable(classTree) && !hasSpecialHandlingSerializationMethods(classTree)) {
      for (Tree member : classTree.members()) {
        if (member.is(Tree.Kind.VARIABLE)) {
          VariableTree variableTree = (VariableTree) member;
          if (!isStatic(variableTree) && !isTransientOrSerializable(variableTree) && !isCollectionOfSerializable(variableTree.type())) {
            addIssue(member, "Make \"" + variableTree.simpleName().name() + "\" transient or serializable.");
          }
        }
      }
    }
  }

  private static boolean isCollectionOfSerializable(TypeTree typeTree) {
    Type type = typeTree.symbolType();
    if (type.isSubtypeOf("java.util.Collection") && typeTree.is(Tree.Kind.PARAMETERIZED_TYPE)) {
      return isSerializable(((ParameterizedTypeTree) typeTree).typeArguments().get(0));
    }
    return false;
  }

  private static boolean isStatic(VariableTree member) {
    return ModifiersUtils.hasModifier(member.modifiers(), Modifier.STATIC);
  }

  private static boolean hasSpecialHandlingSerializationMethods(ClassTree classTree) {
    boolean hasWriteObject = false;
    boolean hasReadObject = false;
    for (Tree member : classTree.members()) {
      if (member.is(Tree.Kind.METHOD)) {
        MethodTree methodTree = (MethodTree) member;
        // FIXME detect methods based on type of arg and throws, not arity.
        if (ModifiersUtils.hasModifier(methodTree.modifiers(), Modifier.PRIVATE) && methodTree.parameters().size() == 1) {
          hasWriteObject |= "writeObject".equals(methodTree.simpleName().name()) && methodTree.throwsClauses().size() == 1;
          hasReadObject |= "readObject".equals(methodTree.simpleName().name()) && methodTree.throwsClauses().size() == 2;
        }
      }
    }
    return hasReadObject && hasWriteObject;
  }

  private static boolean isTransientOrSerializable(VariableTree member) {
    return ModifiersUtils.hasModifier(member.modifiers(), Modifier.TRANSIENT) || isSerializable(member.type());
  }

  private static boolean isSerializable(Tree tree) {
    if (tree.is(Tree.Kind.ENUM, Tree.Kind.PRIMITIVE_TYPE)) {
      return true;
    } else if (tree.is(Tree.Kind.CLASS)) {
      Symbol.TypeSymbol symbol = ((ClassTree) tree).symbol();
      return implementsSerializable(symbol.type());
    } else if (tree.is(Tree.Kind.EXTENDS_WILDCARD, Tree.Kind.UNBOUNDED_WILDCARD)) {
      TypeTree bound = ((WildcardTree) tree).bound();
      return bound != null && implementsSerializable(bound.symbolType());
    }
    return implementsSerializable(((TypeTree) tree).symbolType());
  }

  private static boolean implementsSerializable(@Nullable Type type) {
    if (type == null || type.isUnknown()) {
      return false;
    }
    if (type.isPrimitive()) {
      return true;
    }
    if (type.isArray()) {
      return implementsSerializable(((Type.ArrayType) type).elementType());
    }
    if (type.isClass() || ((JavaType) type).isTagged(JavaType.TYPEVAR)) {
      return type.erasure().isSubtypeOf("java.io.Serializable");
    }
    return false;
  }

}
