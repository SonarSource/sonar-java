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
package org.sonar.java.resolve;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import javax.annotation.CheckForNull;

import java.util.List;
import java.util.Map;

public class TypeSubstitution {
  private Map<JavaType.TypeVariableJavaType, JavaType> substitutions = Maps.newLinkedHashMap();

  @CheckForNull
  public JavaType substitutedType(JavaType javaType) {
    return substitutions.get(javaType);
  }

  public List<JavaType.TypeVariableJavaType> typeVariables() {
    return Lists.newArrayList(substitutions.keySet());
  }

  public List<Map.Entry<JavaType.TypeVariableJavaType, JavaType>> substitutionEntries() {
    return Lists.newArrayList(substitutions.entrySet());
  }

  public List<JavaType> substitutedTypes() {
    return Lists.newArrayList(substitutions.values());
  }

  public TypeSubstitution add(JavaType.TypeVariableJavaType typeVariableType, JavaType javaType) {
    substitutions.put(typeVariableType, javaType);
    return this;
  }

  public int size() {
    return substitutions.size();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    } else if (obj == null || getClass() != obj.getClass()) {
      return false;
    } else {
      TypeSubstitution newSubstitution = (TypeSubstitution) obj;

      // take order of entries into account
      return substitutions.equals(newSubstitution.substitutions)
        && this.substitutionEntries().equals(newSubstitution.substitutionEntries());
    }
  }

  @Override
  public int hashCode() {
    return this.substitutionEntries().hashCode();
  }
}
