/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.plugins.java.api.semantic;

import org.sonar.java.annotations.Beta;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import org.sonar.java.matcher.NoneMethodMatchers;
import org.sonar.java.matcher.MethodMatchersBuilder;
import org.sonar.java.matcher.MethodMatchersList;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodReferenceTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewClassTree;

/**
 * Immutable helper interface to help to identify method with given a Type, Name and Parameters.
 * <p>
 * The starting point to define a MethodMatchers is {@link #create()}.
 * <p>
 * It is required to provide the following:
 * <ul>
 *  <li> a type definition
 *    <ul>
 *      <li> {@link TypeBuilder#ofSubTypes(String...)} </li>
 *      <li> {@link TypeBuilder#ofTypes(String...)} </li>
 *      <li> {@link TypeBuilder#ofType(Predicate<Type>)} </li>
 *      <li> {@link TypeBuilder#ofAnyType()} {@code // same as ofType(type -> true)} </li>
 *   </ul>
 *  </li>
 *  <li> a method name
 *   <ul>
 *    <li> {@link NameBuilder#names(String...)} </li>
 *    <li> {@link NameBuilder#constructor()} </li>
 *    <li> {@link NameBuilder#name(Predicate<String>)} </li>
 *    <li> {@link NameBuilder#anyName()} {@code // same as name(name -> true)} </li>
 *   </ul>
 *  </li>
 *  <li>a list of parameters, 1 or more call to:
 * <p>
 *   (It is possible to define several parameters matcher, to match several method signatures)
 *   <ul>
 *     <li> {@link ParametersBuilder#addWithoutParametersMatcher()} </li>
 *     <li> {@link ParametersBuilder#addParametersMatcher(String...)} </li>
 *     <li> {@link ParametersBuilder#addParametersMatcher(Predicate<List<Type>>)} </li>
 *     <li> {@link ParametersBuilder#withAnyParameters()} {@code // same as addParametersMatcher((List<Type> parameters) -> true)} </li>
 *   </ul>
 *  </li>
 * </ul>
 * The matcher will return true only when the three predicates are respected.
 * <p>
 * Examples:
 * <p>
 * <ul>
 *  <li>match method "a" and "b" from any type, and without parameters:
 *      {@code MethodMatchers.create().ofAnyType().names("a", "b").addWithoutParametersMatcher().build();}
 *  </li>
 *  <li>match method "a" and "b" from (subtype) of A, and "b" and "c" from B, with any parameters:
 *    <pre>
 *      MethodMatchers.or(
 *      MethodMatchers.create().ofSubTypes("A").names("a", "b").withAnyParameters().build(),
 *      MethodMatchers.create().ofSubTypes("B").names("b", "c").withAnyParameters().build());
 *    </pre>
 *  </li>
 *  <li>
 *    match method "f" with any type and with:
 *    {@code MethodMatchers.create().ofAnyType().names("f")}
 *    <ul>
 *      <li>one parameter of type either {@code int} or {@code long}
 *          {@code .addParametersMatcher("int").addParametersMatcher("long");}
 *      </li>
 *      <li>
 *        one parameter of type {@code int} or one parameter of type {@code long} with any other number of parameters
 *        {@code .addParametersMatcher("int").addParametersMatcher(params -> params.size() >= 1 &amp;&amp; params.get(0).is("long"));}
 *      </li>
 *    </ul>
 *    {@code .build();}
 *  </li>
 *  <li>match any method with any type, with parameter {@code int, any, int}:
 *      {@code MethodMatchers.create().ofAnyType().anyName().addParametersMatcher("int", ANY, "int").build();}
 *  </li>
 *  <li>match any type AND method name {@code a} OR {@code b} AND parameter {@code int} OR {@code long}:
 *      {@code MethodMatchers.create().ofAnyType().names("a", "b").addParametersMatcher("int").addParametersMatcher("long").build();}
 *  </li>
 * </ul>
 */
@Beta
public interface MethodMatchers {

  boolean matches(NewClassTree newClassTree);

  boolean matches(MethodInvocationTree mit);

  boolean matches(MethodTree methodTree);

  boolean matches(MethodReferenceTree methodReferenceTree);

  boolean matches(Symbol symbol);

  static MethodMatchers.TypeBuilder create() {
    return new MethodMatchersBuilder();
  }

  // Methods related to combination

  /**
   * Combine multiple method matcher. The matcher will match any of the given matcher.
   */
  static MethodMatchers or(MethodMatchers... matchers) {
    return or(Arrays.asList(matchers));
  }

  static MethodMatchers or(List<? extends MethodMatchers> matchers) {
    return new MethodMatchersList(matchers);
  }

  static MethodMatchers none() {
    return NoneMethodMatchers.getInstance();
  }

  String ANY = "*";
  String CONSTRUCTOR = "<init>";

  interface TypeBuilder {

    /**
     * Match any of the type and sub-type of the fully qualified names.
     */
    MethodMatchers.NameBuilder ofSubTypes(String... fullyQualifiedTypeNames);

    /**
     * Match any type.
     */
    MethodMatchers.NameBuilder ofAnyType();

    /**
     * Match any of the fully qualified name types, but not the subtype.
     */
    MethodMatchers.NameBuilder ofTypes(String... fullyQualifiedTypeNames);

    /**
     * Match a type matching a predicate.
     */
    MethodMatchers.NameBuilder ofType(Predicate<Type> typePredicate);
  }

  interface NameBuilder {
    /**
     * Match a method with any name is the list.
     */
    MethodMatchers.ParametersBuilder names(String... names);

    /**
     * Match a method with any name.
     * Equivalent to .name(n -> true).
     */
    MethodMatchers.ParametersBuilder anyName();

    /**
     * Match a constructor.
     * Equivalent to .name(n -> "<init>".equals(n))
     */
    MethodMatchers.ParametersBuilder constructor();

    /**
     * Match the name matching the predicate.
     */
    MethodMatchers.ParametersBuilder name(Predicate<String> namePredicate);
  }

  interface ParametersBuilder {
    /**
     * Match a method signature with any number of parameters of any types.
     * Others method adding parameters matchers can not be called with this method.
     */
    MethodMatchers.ParametersBuilder withAnyParameters();

    /**
     * Match a method signature without parameters.
     * Others method adding parameters matcher can be called to match multiples signatures.
     */
    MethodMatchers.ParametersBuilder addWithoutParametersMatcher();

    /**
     * Match a method signature with exactly the types provided.
     * Others method adding parameters matcher can be called to match multiples signatures.
     */
    MethodMatchers.ParametersBuilder addParametersMatcher(String... parametersType);

    /**
     * Match a method signature respecting the predicate.
     * Others method adding parameters matcher can be called to match multiples signatures.
     */
    MethodMatchers.ParametersBuilder addParametersMatcher(Predicate<List<Type>> parametersType);

    /**
     * Build a MethodMatchers. Throw an Exception if the MethodMatchers is not correctly setup (no parameters list defined).
     */
    MethodMatchers build();
  }

}
