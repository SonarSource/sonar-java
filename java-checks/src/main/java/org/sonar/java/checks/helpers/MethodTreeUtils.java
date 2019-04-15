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
package org.sonar.java.checks.helpers;

import org.sonar.java.model.ModifiersUtils;
import org.sonar.plugins.java.api.tree.ArrayTypeTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.PrimitiveTypeTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.VariableTree;

public final class MethodTreeUtils {

  private MethodTreeUtils() {
  }

  public static boolean isMainMethod(MethodTree m) {
    return isPublic(m) && isStatic(m) && isNamed(m, "main") && returnsPrimitive(m, "void") && hasStringArrayParameter(m);
  }

  private static boolean hasStringArrayParameter(MethodTree m) {
    return m.parameters().size() == 1 && isParameterStringArray(m);
  }

  private static boolean isParameterStringArray(MethodTree m) {
    VariableTree variableTree = m.parameters().get(0);
    boolean result = false;
    if (variableTree.type().is(Tree.Kind.ARRAY_TYPE)) {
      ArrayTypeTree arrayTypeTree = (ArrayTypeTree) variableTree.type();
      result = arrayTypeTree.type().symbolType().isClass() && "String".equals(arrayTypeTree.type().symbolType().name());
    }
    return result;
  }

  public static boolean isEqualsMethod(MethodTree m) {
    boolean hasEqualsSignature = isNamed(m, "equals") && returnsPrimitive(m, "boolean") && hasObjectParameter(m);
    return isPublic(m) && !isStatic(m) && hasEqualsSignature;
  }

  private static boolean hasObjectParameter(MethodTree m) {
    return m.parameters().size() == 1 && m.parameters().get(0).type().symbolType().is("java.lang.Object");
  }

  public static boolean isHashCodeMethod(MethodTree m) {
    boolean hasHashCodeSignature = isNamed(m, "hashCode") && m.parameters().isEmpty() && returnsInt(m);
    return isPublic(m) && !isStatic(m) && hasHashCodeSignature;
  }

  private static boolean isNamed(MethodTree m, String name) {
    return name.equals(m.simpleName().name());
  }

  private static boolean isStatic(MethodTree m) {
    return ModifiersUtils.hasModifier(m.modifiers(), Modifier.STATIC);
  }

  private static boolean isPublic(MethodTree m) {
    return ModifiersUtils.hasModifier(m.modifiers(), Modifier.PUBLIC);
  }

  private static boolean returnsInt(MethodTree m) {
    return returnsPrimitive(m, "int");
  }

  private static boolean returnsPrimitive(MethodTree m, String primitive) {
    TypeTree returnType = m.returnType();
    if (returnType == null) {
      return false;
    }
    return returnType.is(Tree.Kind.PRIMITIVE_TYPE)
      && primitive.equals(((PrimitiveTypeTree) returnType).keyword().text());
  }

}
