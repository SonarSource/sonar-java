/*
 * SonarQube Java
 * Copyright (C) 2012-2023 SonarSource SA
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

import java.util.Optional;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

public class VariableTreeUtils {

  private VariableTreeUtils() {
  }

  public static boolean isClassField(VariableTree variableTree) {
    return Optional.ofNullable(variableTree.parent())
      .filter(parent -> parent.is(Tree.Kind.CLASS))
      .isPresent();
  }

  public static boolean isSetterParameter(VariableTree variableTree) {
    return Optional.ofNullable(variableTree.parent())
      .filter(parent -> parent.is(Tree.Kind.METHOD))
      .map(MethodTree.class::cast)
      .filter(MethodTreeUtils::isSetterMethod)
      .isPresent();
  }

  public static boolean isConstructorParameter(VariableTree variableTree) {
    return Optional.ofNullable(variableTree.parent())
      .filter(parent -> parent.is(Tree.Kind.CONSTRUCTOR))
      .isPresent();
  }
}
