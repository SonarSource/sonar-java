/*
 * SonarQube Java
 * Copyright (C) 2012-2018 SonarSource SA
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

import org.sonar.java.model.LiteralUtils;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.Tree;

public class ConstantsHelper {

  private ConstantsHelper() {
  }

  public static boolean isStringLiteralWithValue(Tree tree, String expectedValue) {
    if (tree == null || expectedValue == null || !tree.is(Tree.Kind.STRING_LITERAL)) {
      return false;
    }
    String mechanismName = LiteralUtils.trimQuotes(((LiteralTree) tree).value());
    return expectedValue.equals(mechanismName);
  }
}
