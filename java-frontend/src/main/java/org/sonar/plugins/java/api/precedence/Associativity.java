/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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

package org.sonar.plugins.java.api.precedence;

import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.Tree;

public final class Associativity {

  private Associativity(){
  }

  public static boolean isKnownAssociativeOperator(Type lhsType, Tree.Kind operatorKind, Type rhsType){
    return typeAllowsAssociativity(lhsType) && typeAllowsAssociativity(rhsType) && switch (operatorKind){
      case PLUS, MULTIPLY, CONDITIONAL_AND, CONDITIONAL_OR -> true;
      default -> false;
    };
  }

  private static boolean typeAllowsAssociativity(Type tpe){
    Type primitiveType;
    if (tpe.isPrimitive()){
      primitiveType = tpe;
    } else if (tpe.isPrimitiveWrapper()) {
      primitiveType = tpe.primitiveType();
    } else {
      return true;
    }
    return !primitiveType.isPrimitive(Type.Primitives.FLOAT) && !primitiveType.isPrimitive(Type.Primitives.DOUBLE);
  }

}
