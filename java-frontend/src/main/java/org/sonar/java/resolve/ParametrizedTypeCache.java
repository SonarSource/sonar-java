/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
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

import com.google.common.collect.Maps;

import org.sonar.java.resolve.WildCardType.BoundType;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class ParametrizedTypeCache {

  private Map<JavaSymbol, Map<TypeSubstitution, ParametrizedTypeJavaType>> typeCache = Maps.newHashMap();
  private Map<JavaType, Map<WildCardType.BoundType, WildCardType>> wildcardCache = Maps.newHashMap();

  public JavaType getParametrizedTypeType(JavaSymbol.TypeJavaSymbol symbol, TypeSubstitution typeSubstitution) {
    if (symbol.getType().isUnknown()) {
      return symbol.getType();
    }
    if (typeCache.get(symbol) == null) {
      Map<TypeSubstitution, ParametrizedTypeJavaType> map = Maps.newHashMap();
      typeCache.put(symbol, map);
    }
    TypeSubstitution newSubstitution = typeSubstitution;
    if (newSubstitution.size() == 0) {
      newSubstitution = identitySubstitution(symbol.typeVariableTypes);
    }
    if (typeCache.get(symbol).get(newSubstitution) == null) {
      typeCache.get(symbol).put(newSubstitution, new ParametrizedTypeJavaType(symbol, newSubstitution));
    }
    return typeCache.get(symbol).get(newSubstitution);
  }

  private static TypeSubstitution identitySubstitution(List<TypeVariableJavaType> typeVariables) {
    TypeSubstitution result = new TypeSubstitution();
    for (TypeVariableJavaType typeVar : typeVariables) {
      result.add(typeVar, typeVar);
    }
    return result;
  }

  public JavaType getWildcardType(JavaType bound, BoundType boundType) {
    Map<WildCardType.BoundType, WildCardType> map = wildcardCache.get(bound);
    if (map == null) {
      map = new EnumMap<>(WildCardType.BoundType.class);
      wildcardCache.put(bound, map);
    }
    WildCardType wildcardType = map.get(boundType);
    if (wildcardType == null) {
      wildcardType = new WildCardType(bound, boundType);
      map.put(boundType, wildcardType);
    }
    return wildcardType;
  }

}
