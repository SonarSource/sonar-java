/*
 * SonarQube Java
 * Copyright (C) 2012-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.java.matcher;

import java.util.List;
import org.sonar.plugins.java.api.semantic.Type;

@FunctionalInterface
public interface ParametersCriteria {

  boolean matches(List<Type> actualTypes);

  static ParametersCriteria none() {
    return parameterTypes -> parameterTypes.isEmpty();
  }

  static ParametersCriteria any() {
    return parameterTypes -> true;
  }

  static ParametersCriteria of(List<TypeCriteria> expectedTypes) {
    return actualTypes -> matches(expectedTypes, actualTypes);
  }

  static boolean matches(List<TypeCriteria> expectedTypes, List<Type> actualTypes) {
    if (actualTypes.size() != expectedTypes.size()) {
      return false;
    }
    for (int i = 0; i < actualTypes.size(); i++) {
      if (!expectedTypes.get(i).matches(actualTypes.get(i))) {
        return false;
      }
    }
    return true;
  }

}
