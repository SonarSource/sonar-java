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
package org.sonar.java.resolve;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import javax.annotation.CheckForNull;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TypeSubstitution {
  private LinkedHashMap<TypeVariableJavaType, JavaType> substitutions = Maps.newLinkedHashMap();

  private static final TypeSubstitution UNCHECKED = new TypeSubstitution() {
    @Override
    public boolean isUnchecked() {
      return true;
    }
  };

  public TypeSubstitution() {
    // default behavior
  }

  public TypeSubstitution(TypeSubstitution typeSubstitution) {
    // copy the substitution
    this.substitutions = Maps.newLinkedHashMap(typeSubstitution.substitutions);
  }

  public boolean isUnchecked() {
    return false;
  }
  public static TypeSubstitution uncheckedTypeSubstitution() {
    return UNCHECKED;
  }

  @CheckForNull
  public JavaType substitutedType(JavaType javaType) {
    return substitutions.get(javaType);
  }

  public List<TypeVariableJavaType> typeVariables() {
    return Lists.newArrayList(substitutions.keySet());
  }

  public List<Map.Entry<TypeVariableJavaType, JavaType>> substitutionEntries() {
    return Lists.newArrayList(substitutions.entrySet());
  }

  public List<JavaType> substitutedTypes() {
    return Lists.newArrayList(substitutions.values());
  }

  public TypeSubstitution add(TypeVariableJavaType typeVariableType, JavaType javaType) {
    substitutions.put(typeVariableType, javaType.isPrimitive() ? javaType.primitiveWrapperType() : javaType);
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

  public boolean isIdentity() {
    return substitutionEntries().stream().noneMatch(s -> s.getKey() != s.getValue());
  }

  /**
   * Produce new substitution based on two substitutions using the same keys.
   * if this.substitution is: A -> S, B -> I and source.substitution is : A -> Y, B -> X,
   * produces Y -> S, X -> I
   * @param source the substitution which values will be used as keys.
   * @return combination of the two substitutions.
   */
  public TypeSubstitution combine(TypeSubstitution source) {
    TypeSubstitution result = new TypeSubstitution();
    for (Map.Entry<TypeVariableJavaType, JavaType> substitution : substitutionEntries()) {
      TypeVariableJavaType typeVar = substitution.getKey();
      JavaType targetType = filterWildcard(substitution.getValue());
      JavaType substitutedType = source.substitutedType(typeVar);
      if(substitutedType == null || targetType == substitutedType) {
        result.add(typeVar, targetType);
        continue;
      }
      substitutedType = filterWildcard(substitutedType);
      if(targetType.isArray() && substitutedType.isArray()) {
        targetType = elementType(targetType);
        substitutedType = elementType(substitutedType);
      }
      if (targetType.isTagged(JavaType.TYPEVAR)) {
        result.add((TypeVariableJavaType) targetType, substitutedType);
      } else if(targetType.isParameterized() && substitutedType.isParameterized()) {
        TypeSubstitution combined = ((ParametrizedTypeJavaType) targetType).typeSubstitution.combine(((ParametrizedTypeJavaType) substitutedType).typeSubstitution);
        result.substitutions.putAll(combined.substitutions);
      } else {
        result.add(typeVar, targetType);
      }
    }
    return result;
  }

  private static JavaType filterWildcard(JavaType javaType) {
    if (javaType.isTagged(JavaType.WILDCARD)) {
      return ((WildCardType) javaType).bound;
    }
    return javaType;
  }

  private static JavaType elementType(JavaType javaType) {
    if(javaType.isArray()) {
      return elementType(((ArrayJavaType) javaType).elementType);
    }
    return javaType;
  }
}
