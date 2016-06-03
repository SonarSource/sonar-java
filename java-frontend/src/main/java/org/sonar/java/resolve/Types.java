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
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import org.sonar.java.resolve.WildCardType.BoundType;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Types {

  private final Symbols symbols;
  private final ParametrizedTypeCache parametrizedTypeCache;

  public Types(ParametrizedTypeCache parametrizedTypeCache, Symbols symbols) {
    this.symbols = symbols;
    this.parametrizedTypeCache = parametrizedTypeCache;
  }

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
          result = isSubtype(((ArrayJavaType) t).elementType(), ((ArrayJavaType) s).elementType());
          break;
        case JavaType.CLASS:
        case JavaType.PARAMETERIZED:
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
  public Type leastUpperBound(Set<Type> types) {
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
    List<Set<Type>> erasedSupertypes = erased(supertypes);

    List<Type> erasedCandidates = intersection(erasedSupertypes);
    List<Type> minimalErasedCandidates = minimalCandidates(erasedCandidates);
    if (minimalErasedCandidates.isEmpty()) {
      return Symbols.unknownType;
    }

    Multimap<Type, Type> relevantParameterizations = relevantParameterizations(minimalErasedCandidates, supertypes);

    return best(minimalErasedCandidates, relevantParameterizations.asMap());
  }

  private static List<Set<Type>> supertypes(Iterable<Type> types) {
    List<Set<Type>> results = new ArrayList<>();
    for (Type type : types) {
      Set<Type> supertypes = new LinkedHashSet<>();
      Type ownType = type;
      supertypes.add(ownType);
      for (Type supertype : ((JavaType) type).symbol.superTypes()) {
        supertypes.add(supertype);
      }
      results.add(supertypes);
    }
    return results;
  }

  private static List<Set<Type>> erased(Iterable<Set<Type>> typeSets) {
    List<Set<Type>> results = new ArrayList<>();
    for (Set<Type> typeSet : typeSets) {
      Set<Type> erasedTypes = new LinkedHashSet<>();
      for (Type type : typeSet) {
        erasedTypes.add(type.erasure());
      }
      results.add(erasedTypes);
    }
    return results;
  }

  private static List<Type> intersection(List<Set<Type>> supertypes) {
    List<Type> results = new ArrayList<>(supertypes.get(0));
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
    List<Type> results = new ArrayList<>();
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

  /**
   * For any element G of MEC that is a generic type, let the "relevant" parameterizations of G, Relevant(G), be:
   * Relevant(G) = { V | 1 ≤ i ≤ k: V in ST(Ui) and V = G<...> }
   *
   * @param minimalErasedCandidates MEC
   * @param supertypes
   * @return the set of known parameterizations for each generic type G of MEC 
   */
  private static Multimap<Type, Type> relevantParameterizations(List<Type> minimalErasedCandidates, List<Set<Type>> supertypes) {
    Multimap<Type, Type> result = HashMultimap.create();
    for (Set<Type> supertypesSet : supertypes) {
      for (Type supertype : supertypesSet) {
        Type erasedSupertype = supertype.erasure();
        if (minimalErasedCandidates.contains(erasedSupertype)) {
          result.put(erasedSupertype, supertype);
        }
      }
    }
    return result;
  }

  private Type best(List<Type> minimalErasedCandidates, Map<Type, Collection<Type>> relevantParameterizations) {
    Type best = best(minimalErasedCandidates);
    Collection<Type> parameterizations = relevantParameterizations.get(best);
    if (parameterizations != null && !parameterizations.contains(best)) {
      best = leastContainingParameterization(Lists.newArrayList(parameterizations));
    }
    return best;
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
    // FIXME SONARJAVA-1632 should return union of types
    return minimalCandidates.get(0);
  }

  /**
   * Let the "candidate" parameterization of G, Candidate(G), be the most specific parameterization of the generic type G that contains all
   * the relevant parameterizations of G: Candidate(G) = lcp(Relevant(G)), where lcp() is the least containing parameterization. 
   * @param types
   * @return
   */
  private Type leastContainingParameterization(List<Type> types) {
    if (types.size() == 1) {
      return types.get(0);
    }
    Type reduction = leastContainingTypeArgument((JavaType) types.get(0), (JavaType) types.get(1));

    List<Type> reducedList = Lists.newArrayList(reduction);
    reducedList.addAll(types.subList(2, types.size()));

    return leastContainingParameterization(reducedList);
  }

  private Type leastContainingTypeArgument(JavaType type1, JavaType type2) {
    Preconditions.checkArgument(type1.isTagged(JavaType.PARAMETERIZED) && type2.isTagged(JavaType.PARAMETERIZED));

    TypeSubstitution typeSubstitution1 = ((ParametrizedTypeJavaType) type1).typeSubstitution;
    TypeSubstitution typeSubstitution2 = ((ParametrizedTypeJavaType) type2).typeSubstitution;

    TypeSubstitution newTypeSubstitution = new TypeSubstitution();
    for (TypeVariableJavaType typeVar : typeSubstitution1.typeVariables()) {
      JavaType subs1 = typeSubstitution1.substitutedType(typeVar);
      JavaType subs2 = typeSubstitution2.substitutedType(typeVar);

      boolean isWildcard1 = subs1.isTagged(JavaType.WILDCARD);
      boolean isWildcard2 = subs2.isTagged(JavaType.WILDCARD);

      JavaType newSubs;
      if (subs1 == subs2) {
        newSubs = subs1;
      } else if (isWildcard1 && isWildcard2) {
        newSubs = lctaBothWildcards((WildCardType) subs1, (WildCardType) subs2);
      } else if (isWildcard1 ^ isWildcard2) {
        JavaType rawType = isWildcard1 ? subs2 : subs1;
        WildCardType wildcardType = (WildCardType) (isWildcard1 ? subs1 : subs2);
        newSubs = lctaOneWildcard(rawType, wildcardType);
      } else {
        // subs1 and subs2 are not wildcards
        JavaType newBound = (JavaType) leastUpperBound(Sets.newHashSet(subs1, subs2));
        newSubs = parametrizedTypeCache.getWildcardType(newBound, BoundType.EXTENDS);
      }
      newTypeSubstitution.add(typeVar, newSubs);
    }

    return parametrizedTypeCache.getParametrizedTypeType(type1.symbol, newTypeSubstitution);
  }

  private JavaType lctaOneWildcard(JavaType rawType, WildCardType wildcardType) {
    if (wildcardType.boundType == BoundType.SUPER) {
      JavaType glb = (JavaType) greatestLowerBound(Sets.newHashSet(rawType, wildcardType.bound));
      return parametrizedTypeCache.getWildcardType(glb, BoundType.SUPER);
    }
    JavaType lub = (JavaType) leastUpperBound(Sets.newHashSet(rawType, wildcardType.bound));
    return parametrizedTypeCache.getWildcardType(lub, BoundType.EXTENDS);
  }

  private JavaType lctaBothWildcards(WildCardType type1, WildCardType type2) {
    if (type1.boundType == BoundType.SUPER && type2.boundType == BoundType.SUPER) {
      JavaType glb = (JavaType) greatestLowerBound(Sets.newHashSet(type1.bound, type2.bound));
      return parametrizedTypeCache.getWildcardType(glb, BoundType.SUPER);
    }
    if (type1.boundType == BoundType.EXTENDS && type2.boundType == BoundType.EXTENDS) {
      JavaType lub = (JavaType) leastUpperBound(Sets.newHashSet(type1.bound, type2.bound));
      return parametrizedTypeCache.getWildcardType(lub, BoundType.EXTENDS);
    }
    if (type1.bound == type2.bound) {
      return type1.bound;
    }
    return symbols.unboundedWildcard;
  }

  /**
   * From JLS 8 5.1.10 - greatest lower bound :  glb(V1,...,Vm) is defined as V1 & ... & Vm.
   * @param types
   * @return
   */
  private static Type greatestLowerBound(Set<Type> types) {
    // TODO SONARJAVA-1632 implement, should return intersection of all types, not only first one
    return types.iterator().next();
  }

}
