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
package org.sonar.java.matcher;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonarsource.analyzer.commons.collections.SetUtils;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodReferenceTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;

public class MethodMatchersBuilder implements MethodMatchers.TypeBuilder, MethodMatchers.NameBuilder, MethodMatchers.ParametersBuilder, MethodMatchers {

  @Nullable
  private final Predicate<Type> typePredicate;

  @Nullable
  private final Predicate<String> namePredicate;

  @Nullable
  private final Predicate<List<Type>> parametersPredicate;

  public MethodMatchersBuilder() {
    this.typePredicate = null;
    this.namePredicate = null;
    this.parametersPredicate = null;
  }

  private MethodMatchersBuilder(@Nullable Predicate<Type> typePredicate, @Nullable Predicate<String> namePredicate, @Nullable Predicate<List<Type>> parametersPredicate) {
    this.typePredicate = typePredicate;
    this.namePredicate = namePredicate;
    this.parametersPredicate = parametersPredicate;
  }

  private static <T> Predicate<T> substituteAny(Predicate<T> predicate, String... elements) {
    if (SetUtils.immutableSetOf(elements).contains(ANY)) {
      if (elements.length > 1) {
        throw new IllegalStateException("Incompatible MethodMatchers.ANY with other predicates.");
      }
      return e -> true;
    }
    return predicate;
  }

  private static <T> Predicate<T> substituteAnyAndCreateEfficientPredicate(
    String[] elements,
    Function<String, Predicate<T>> singleElementPredicate,
    Function<List<String>, Predicate<T>> multiElementsPredicate) {
    if (elements.length == 0) {
      throw new IllegalStateException("Method arguments can not be empty, otherwise the predicate would be always false.");
    }
    if (elements.length == 1) {
      String singleElement = elements[0];
      return substituteAny(singleElementPredicate.apply(singleElement), elements);
    } else {
      List<String> multiElements = Arrays.asList(elements);
      return substituteAny(multiElementsPredicate.apply(multiElements), elements);
    }
  }

  @Override
  public NameBuilder ofSubTypes(String... fullyQualifiedTypeNames) {
    return ofType(substituteAnyAndCreateEfficientPredicate(
      fullyQualifiedTypeNames,
      name -> (type -> type.isSubtypeOf(name)),
      names -> (type -> names.stream().anyMatch(type::isSubtypeOf))));
  }

  @Override
  public NameBuilder ofAnyType() {
    return ofTypes(ANY);
  }

  @Override
  public NameBuilder ofTypes(String... fullyQualifiedTypeNames) {
    return ofType(substituteAnyAndCreateEfficientPredicate(
      fullyQualifiedTypeNames,
      name -> (type -> type.is(name)),
      names -> (type -> names.stream().anyMatch(type::is))));
  }

  @Override
  public NameBuilder ofType(Predicate<Type> typePredicate) {
    return new MethodMatchersBuilder(or(this.typePredicate, typePredicate), namePredicate, parametersPredicate);
  }

  @Override
  public ParametersBuilder names(String... names) {
    return name(substituteAnyAndCreateEfficientPredicate(
      names,
      name -> name::equals,
      nameList -> nameList::contains));
  }

  @Override
  public ParametersBuilder anyName() {
    return names(ANY);
  }

  @Override
  public ParametersBuilder constructor() {
    return names(MethodMatchers.CONSTRUCTOR);
  }

  @Override
  public ParametersBuilder name(Predicate<String> namePredicate) {
    return new MethodMatchersBuilder(typePredicate, or(this.namePredicate, namePredicate), parametersPredicate);
  }

  @Override
  public ParametersBuilder addParametersMatcher(String... parametersType) {
    return addParametersMatcher(Arrays.stream(parametersType)
      .<Predicate<Type>>map(parameterType -> substituteAny(type -> type.is(parameterType), parameterType))
      .collect(Collectors.toList()));
  }

  private ParametersBuilder addParametersMatcher(List<Predicate<Type>> parametersType) {
    return addParametersMatcher((List<Type> actualTypes) -> exactMatchesParameters(parametersType, actualTypes));
  }

  @Override
  public ParametersBuilder addWithoutParametersMatcher() {
    return addParametersMatcher(Collections.emptyList());
  }

  @Override
  public ParametersBuilder withAnyParameters() {
    if (parametersPredicate != null) {
      throw new IllegalStateException("Incompatible 'any parameters' constraint added to existing parameters constraint.");
    }
    return addParametersMatcher((List<Type> actualParameters) -> true);
  }

  @Override
  public ParametersBuilder addParametersMatcher(Predicate<List<Type>> parametersPredicate) {
    return new MethodMatchersBuilder(typePredicate, namePredicate, or(this.parametersPredicate, parametersPredicate));
  }

  private static boolean exactMatchesParameters(List<Predicate<Type>> expectedTypes, List<Type> actualTypes) {
    return actualTypes.size() == expectedTypes.size() && matchesParameters(expectedTypes, actualTypes);
  }

  private static boolean matchesParameters(List<Predicate<Type>> expectedTypes, List<Type> actualTypes) {
    for (int i = 0; i < expectedTypes.size(); i++) {
      if (!expectedTypes.get(i).test(actualTypes.get(i))) {
        return false;
      }
    }
    return true;
  }

  @Override
  public boolean matches(NewClassTree newClassTree) {
    return matches(newClassTree.methodSymbol(), null);
  }

  @Override
  public boolean matches(MethodInvocationTree mit) {
    IdentifierTree id = getIdentifier(mit);
    return matches(id.symbol(), getCallSiteType(mit));
  }

  @Override
  public boolean matches(MethodTree methodTree) {
    Symbol.MethodSymbol symbol = methodTree.symbol();
    Symbol.TypeSymbol enclosingClass = symbol.enclosingClass();
    return enclosingClass != null && matches(symbol, enclosingClass.type());
  }

  @Override
  public boolean matches(MethodReferenceTree methodReferenceTree) {
    return matches(methodReferenceTree.method().symbol(), getCallSiteType(methodReferenceTree));
  }

  @Override
  public boolean matches(Symbol symbol) {
    return matches(symbol, null);
  }

  @Override
  public MethodMatchers build() {
    if (typePredicate == null || namePredicate == null || parametersPredicate == null) {
      throw new IllegalStateException("MethodMatchers need to be fully initialized.");
    }
    return this;
  }

  private boolean matches(Symbol symbol, @Nullable Type callSiteType) {
    return symbol.isMethodSymbol() && isSearchedMethod((Symbol.MethodSymbol) symbol, callSiteType);
  }

  @CheckForNull
  private static Type getCallSiteType(MethodReferenceTree referenceTree) {
    Tree expression = referenceTree.expression();
    if (expression instanceof ExpressionTree) {
      return ((ExpressionTree) expression).symbolType();
    }
    return null;
  }

  @CheckForNull
  private static Type getCallSiteType(MethodInvocationTree mit) {
    ExpressionTree methodSelect = mit.methodSelect();
    // methodSelect can only be Tree.Kind.IDENTIFIER or Tree.Kind.MEMBER_SELECT
    if (methodSelect.is(Tree.Kind.IDENTIFIER)) {
      Symbol.TypeSymbol enclosingClassSymbol = ((IdentifierTree) methodSelect).symbol().enclosingClass();
      return enclosingClassSymbol != null ? enclosingClassSymbol.type() : null;
    } else {
      return ((MemberSelectExpressionTree) methodSelect).expression().symbolType();
    }
  }

  private boolean isSearchedMethod(Symbol.MethodSymbol symbol, @Nullable Type callSiteType) {
    Type type = callSiteType;
    if (type == null) {
      Symbol owner = symbol.owner();
      if (owner != null) {
        type = owner.type();
      }
    }
    return type != null &&
      namePredicate.test(symbol.name()) &&
      parametersPredicate.test(symbol.parameterTypes()) &&
      typePredicate.test(type);
  }

  public static IdentifierTree getIdentifier(MethodInvocationTree mit) {
    // methodSelect can only be Tree.Kind.IDENTIFIER or Tree.Kind.MEMBER_SELECT
    if (mit.methodSelect().is(Tree.Kind.IDENTIFIER)) {
      return (IdentifierTree) mit.methodSelect();
    }
    return ((MemberSelectExpressionTree) mit.methodSelect()).identifier();
  }

  private static <T> Predicate<T> or(@Nullable Predicate<T> accumulator, Predicate<T> next) {
    return accumulator != null ? accumulator.or(next) : next;
  }

}
