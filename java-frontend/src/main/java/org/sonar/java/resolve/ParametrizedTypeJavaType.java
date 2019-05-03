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

import com.google.common.base.Preconditions;
import org.sonar.plugins.java.api.semantic.Type;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class ParametrizedTypeJavaType extends ClassJavaType {

  private final TypeSubstitutionSolver typeSubstitutionSolver;
  final TypeSubstitution typeSubstitution;
  final JavaType rawType;

  public ParametrizedTypeJavaType(JavaSymbol.TypeJavaSymbol symbol, TypeSubstitution typeSubstitution, TypeSubstitutionSolver typeSubstitutionSolver) {
    super(PARAMETERIZED, symbol);
    this.rawType = symbol.getType().erasure();
    this.typeSubstitution = typeSubstitution;
    this.typeSubstitutionSolver = typeSubstitutionSolver;
  }

  @Override
  public JavaType erasure() {
    return rawType.erasure();
  }

  @Nullable
  public JavaType substitution(TypeVariableJavaType typeVariableType) {
    JavaType result = null;
    if (typeSubstitution != null) {
      result = typeSubstitution.substitutedType(typeVariableType);
    }
    return result;
  }

  public List<TypeVariableJavaType> typeParameters() {
    if (typeSubstitution != null) {
      return typeSubstitution.typeVariables();
    }
    return new ArrayList<>();
  }

  @Override
  public boolean isSubtypeOf(Type superType) {
    if (((JavaType) superType).isTagged(TYPEVAR)) {
      return false;
    }
    if (erasure() == superType.erasure()) {
      return !((JavaType) superType).isParameterized() || checkSubstitutedTypesCompatibility((ParametrizedTypeJavaType) superType);
    }
    if (verifySuperTypes(superType)) {
      return true;
    }
    return ((JavaType) superType).isTagged(WILDCARD) && ((WildCardType) superType).isSubtypeOfBound(this);
  }

  private boolean verifySuperTypes(Type superType) {
    JavaType superclass = getSuperType();
    return (superclass != null && superclass.isSubtypeOf(superType))
      || symbol.getInterfaces().stream().map(si -> typeSubstitutionSolver.applySubstitution(si, this.typeSubstitution)).anyMatch(si -> si.isSubtypeOf(superType));
  }

  private boolean checkSubstitutedTypesCompatibility(ParametrizedTypeJavaType superType) {
    List<JavaType> myTypes = typeSubstitution.substitutedTypes();
    List<JavaType> itsTypes = superType.typeSubstitution.substitutedTypes();
    Preconditions.checkState(myTypes.size() == itsTypes.size());
    if (itsTypes.size() != myTypes.size()) {
      return false;
    }
    for (int i = 0; i < myTypes.size(); i++) {
      JavaType myType = myTypes.get(i);
      JavaType itsType = itsTypes.get(i);
      if (itsType.isTagged(WILDCARD)) {
        if (!myType.isSubtypeOf(itsType)) {
          return false;
        }
      } else if (!myType.equals(itsType)) {
        return false;
      }
    }
    return true;
  }

  @Override
  protected ClassJavaType substitutedType(ClassJavaType type) {
    return (ClassJavaType) typeSubstitutionSolver.applySubstitution(type, typeSubstitution);
  }
}
