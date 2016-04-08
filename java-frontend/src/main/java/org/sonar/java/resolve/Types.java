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
package org.sonar.java.resolve;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;

import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class Types {

  /**
   * JLS7 4.10. Subtyping
   */
  public boolean isSubtype(JavaType t, JavaType s) {
    boolean result;

    if (t == s) {
      result = true;
    } else {
      switch (t.tag) {
        case JavaType.BYTE:
        case JavaType.CHAR:
          result = t.tag == s.tag || (t.tag + /* skip char for byte and short for char */2 <= s.tag && s.tag <= JavaType.DOUBLE);
          break;
        case JavaType.SHORT:
        case JavaType.INT:
        case JavaType.LONG:
        case JavaType.FLOAT:
        case JavaType.DOUBLE:
          result = t.tag <= s.tag && s.tag <= JavaType.DOUBLE;
          break;
        case JavaType.BOOLEAN:
        case JavaType.VOID:
          result = t.tag == s.tag;
          break;
        case JavaType.ARRAY:
          if(t.tag != s.tag) {
            //t is array, if tags are different then the only way t is subtype of s is s to be object ie: superclass of arrayClass
            result = t.getSymbol().getSuperclass() == s;
            break;
          }
          result = isSubtype(((JavaType.ArrayJavaType) t).elementType(), ((JavaType.ArrayJavaType) s).elementType());
          break;
        case JavaType.CLASS:
        case JavaType.WILDCARD:
        case JavaType.TYPEVAR:
          result = t.isSubtypeOf(s);
          break;
        case JavaType.BOT:
          result = s.tag == JavaType.BOT || s.tag == JavaType.CLASS || s.tag == JavaType.ARRAY;
          break;
        default:
          // TODO error recovery, but should be rewritten to not happen at all
          result = false;
          break;
      }
    }

    return result;
  }

  /**
   * Compute the "Least Upper Bound" ("lub", jls8 §4.10.4) of a list of type. The "lub" is a shared supertype that is more specific than any
   * other shared supertype (that is, no other shared supertype is a subtype of the least upper bound)
   *
   * Parameterized types are currently ignored, as the method is used only to handle Union Types Trees, themselves being used only
   * in catch trees. Note that Exceptions (any subclass of Throwable) cannot be generic (jls8 §8.1.2, §11.1.1: "compile-time error if a generic
   * class is a direct or indirect subclass of Throwable")
   *
   * @param types
   * @return the least upper bound of the types
   */
  public static Type leastUpperBound(Set<Type> types) {
    Preconditions.checkArgument(!types.isEmpty());
    Iterator<Type> iterator = types.iterator();
    Type first = iterator.next();
    //lub(U) = U
    if(types.size() == 1) {
      return first;
    }
    // Handle particular case while generics are not properly supported if a type is subtype of another then lub is that type: solve some cases of conditional operator.
    // FIXME: should be removed when dealing with generics is properly supported see SONARJAVA-1631
    if(types.size() == 2) {
      Type type2 = iterator.next();
      if(first.isSubtypeOf(type2)) {
        return type2;
      } else if(type2.isSubtypeOf(first)) {
        return first;
      }
    }

    List<Set<Type>> supertypes = supertypes(types);

    List<Type> candidates = intersection(supertypes);
    List<Type> minimalCandidates = minimalCandidates(candidates);
    if (minimalCandidates.isEmpty()) {
      return Symbols.unknownType;
    }

    return best(minimalCandidates);
  }

  @VisibleForTesting
  static Type best(List<Type> minimalCandidates) {
    Collections.sort(minimalCandidates, new Comparator<Type>() {
      // Sort minimal candidates by name with classes before interfaces, to guarantee always the same type is returned when approximated.
      @Override
      public int compare(Type type, Type t1) {
        Symbol.TypeSymbol typeSymbol = type.symbol();
        Symbol.TypeSymbol t1Symbol = t1.symbol();
        if (typeSymbol.isInterface() && t1Symbol.isInterface()) {
          return type.name().compareTo(t1.name());
        } else if (typeSymbol.isInterface()) {
          return 1;
        } else if (t1Symbol.isInterface()) {
          return -1;
        }
        return type.name().compareTo(t1.name());
      }
    });
    return minimalCandidates.get(0);
  }

  private static List<Set<Type>> supertypes(Iterable<Type> types) {
    List<Set<Type>> results = new LinkedList<>();
    for (Type type : types) {
      Set<Type> supertypes = new LinkedHashSet<>();
      supertypes.add(type.erasure());
      for (Type supertype : ((JavaType) type).symbol.superTypes()) {
        // Rely on erasure as approximation for generic type
        supertypes.add(supertype.erasure());
      }
      results.add(supertypes);
    }
    return results;
}

  private static List<Type> intersection(List<Set<Type>> supertypes) {
    List<Type> results = new LinkedList<>(supertypes.get(0));
    for (int i = 1; i < supertypes.size(); i++) {
      results.retainAll(supertypes.get(i));
    }
    return results;
  }

  /**
   * Let MEC, the minimal erased candidate set for U1 ... Uk, be:
   * MEC = { V | V in EC, and for all W != V in EC, it is not the case that W <: V }
   * @param erasedCandidates
   * @return
   */
  private static List<Type> minimalCandidates(List<Type> erasedCandidates) {
    List<Type> results = new LinkedList<>();
    for (Type v : erasedCandidates) {
      boolean isValid = true;
      for (Type w : erasedCandidates) {
        if (!w.equals(v) && w.isSubtypeOf(v)) {
          isValid = false;
          break;
        }
      }
      if (isValid) {
        results.add(v);
      }
    }
    return results;
  }

}
