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
package org.sonar.java.checks.helpers;

import javax.annotation.CheckForNull;
import org.sonar.plugins.java.api.tree.ExpressionTree;

/**
 * This is deprecated, rely on {@link ExpressionTree#asConstant()} methods instead
 *
 * @deprecated since SonarJava 6.1 - now available from {@link ExpressionTree} public API.
 */
@Deprecated
public class ConstantUtils {

  private ConstantUtils() {
  }

  @CheckForNull
  public static String resolveAsStringConstant(ExpressionTree tree) {
    return tree.asConstant(String.class).orElse(null);
  }

  @CheckForNull
  public static Integer resolveAsIntConstant(ExpressionTree tree) {
    return tree.asConstant(Integer.class).orElse(null);
  }

  @CheckForNull
  public static Long resolveAsLongConstant(ExpressionTree tree) {
    Object constant = tree.asConstant().orElse(null);
    if (constant instanceof Long) {
      return (Long) constant;
    }
    if (constant instanceof Integer) {
      return ((Integer) constant).longValue();
    }
    return null;
  }

  @CheckForNull
  public static Boolean resolveAsBooleanConstant(ExpressionTree tree) {
    return tree.asConstant(Boolean.class).orElse(null);
  }

  @CheckForNull
  public static Object resolveAsConstant(ExpressionTree tree) {
    return tree.asConstant().orElse(null);
  }
}
