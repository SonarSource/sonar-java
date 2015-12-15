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
package org.sonar.java.checks.methods;

import org.sonar.plugins.java.api.semantic.Type;

public abstract class TypeCriteria {
  public static TypeCriteria subtypeOf(String fullyQualifiedName) {
    return new SubtypeTypeCriteria(fullyQualifiedName);
  }

  public static TypeCriteria is(String fullyQualifiedName) {
    return new FullyQualifiedNameTypeCriteria(fullyQualifiedName);
  }

  public static TypeCriteria anyType() {
    return new AnyTypeCriteria();
  }

  public abstract boolean matches(Type type);

  private static class FullyQualifiedNameTypeCriteria extends TypeCriteria {
    private String fullyQualifiedName;

    public FullyQualifiedNameTypeCriteria(String fullyQualifiedName) {
      this.fullyQualifiedName = fullyQualifiedName;
    }

    @Override
    public boolean matches(Type type) {
      return type.is(fullyQualifiedName);
    }
  }

  private static class SubtypeTypeCriteria extends TypeCriteria {
    private String superTypeName;

    public SubtypeTypeCriteria(String superTypeName) {
      this.superTypeName = superTypeName;
    }

    @Override
    public boolean matches(Type type) {
      return type.isSubtypeOf(superTypeName);
    }
  }

  private static class AnyTypeCriteria extends TypeCriteria {

    @Override
    public boolean matches(Type type) {
      return true;
    }
  }
}
