/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.semsitter;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.sonar.plugins.java.api.semantic.Type;

import static org.assertj.core.api.Assertions.assertThat;

class OverloadRankingTest {

  private static final String STRING = "java.lang.String";
  private static final String OBJECT = "java.lang.Object";
  private static final String VALUE_OF = "valueOf";
  private static final String VARARGS = "lib.Varargs";

  private static final String VARARGS_TYPE = """
    {
      "lib.Varargs": {
        "origin": "classpath", "kind": "class", "modifiers": ["public"], "super": "java.lang.Object", "interfaces": [],
        "typeParameters": 0, "typeVars": [],
        "members": [
          %s, %s, %s, %s, %s, %s, %s, %s, %s
        ]
      }
    }
    """.formatted(member("(Ljava/lang/Long;)V", false, true), member("([Ljava/lang/Object;)V", true, true), member("([Ljava/lang/String;)V", true, true),
    member("([Ljava/lang/Integer;)V", true, true), member("(I[Ljava/lang/String;)V", true, false),
    member("(Ljava/lang/String;[Ljava/lang/Object;)V", true, true), member("take", "(Llib/Missing;)V", false, true),
    member("run", "(Ljava/lang/Runnable;)V", false, true), member("run", "(Ljava/lang/Thread;)V", false, true));

  private final JTypeFixtures fixtures = new JTypeFixtures(VARARGS_TYPE);
  private final OverloadRanking ranking = fixtures.sema.overloads();

  @Test
  void keeps_current_when_candidate_cannot_compete() {
    JMethodSymbol current = valueOf(OBJECT);
    List<Type> arguments = List.of(fixtures.primitive("int"));

    assertThat(ranking.preferOverload(current, null, arguments, Set.of())).isSameAs(current);
    assertThat(ranking.preferOverload(current, current, arguments, Set.of())).isSameAs(current);
    assertThat(ranking.preferOverload(current, valueOf("char[]", "int", "int"), arguments, Set.of())).isSameAs(current);
    assertThat(ranking.preferOverload(current, unknown(), arguments, Set.of())).isSameAs(current);
  }

  @Test
  void keeps_current_when_neither_overload_takes_the_function() {
    JMethodSymbol message = fixtures.method("java.util.Objects", "requireNonNull", OBJECT, STRING);
    JMethodSymbol fallback = fixtures.method("java.util.Objects", "toString", OBJECT, STRING);
    List<Type> arguments = List.of(fixtures.object(), fixtures.string());

    assertThat(ranking.preferOverload(message, fallback, arguments, Set.of(1))).isSameAs(message);
  }

  @Test
  void keeps_current_when_both_overloads_are_as_specific() {
    JMethodSymbol current = valueOf(OBJECT);

    assertThat(ranking.preferOverload(current, fixtures.method("java.util.Objects", "toString", OBJECT), List.of(fixtures.string()), Set.of()))
      .isSameAs(current);
  }

  @Test
  void prefers_candidate_taking_the_function_an_argument_is() {
    JMethodSymbol message = fixtures.method("java.util.Objects", "requireNonNull", OBJECT, STRING);
    JMethodSymbol supplier = fixtures.method("java.util.Objects", "requireNonNull", OBJECT, "java.util.function.Supplier");
    List<Type> arguments = List.of(fixtures.object(), Type.UNKNOWN);

    assertThat(ranking.preferOverload(message, supplier, arguments, Set.of(1))).isSameAs(supplier);
    assertThat(ranking.preferOverload(supplier, message, arguments, Set.of(1))).isSameAs(supplier);
  }

  @Test
  void keeps_current_when_the_candidate_cannot_take_the_function() {
    JMethodSymbol runnable = fixtures.method(VARARGS, "run", "java.lang.Runnable");
    JMethodSymbol thread = fixtures.method(VARARGS, "run", "java.lang.Thread");

    assertThat(ranking.preferOverload(runnable, thread, List.of(Type.UNKNOWN), Set.of(0))).isSameAs(runnable);
  }

  @Test
  void prefers_overload_applicable_in_an_earlier_phase() {
    JMethodSymbol object = valueOf(OBJECT);
    JMethodSymbol primitive = valueOf("int");

    assertThat(ranking.preferOverload(object, primitive, List.of(fixtures.primitive("int")), Set.of())).isSameAs(primitive);
    assertThat(ranking.preferOverload(primitive, object, List.of(fixtures.integer()), Set.of())).isSameAs(object);
    assertThat(ranking.preferOverload(object, primitive, List.of(fixtures.integer()), Set.of())).isSameAs(object);
    assertThat(ranking.preferOverload(valueOf("char[]"), primitive, List.of(fixtures.integer()), Set.of())).isSameAs(primitive);
  }

  @Test
  void prefers_more_specific_of_two_applicable_overloads() {
    JMethodSymbol object = valueOf(OBJECT);
    JMethodSymbol chars = valueOf("char[]");
    List<Type> arguments = List.of(JType.array(fixtures.sema, fixtures.primitive("char")));

    assertThat(ranking.preferOverload(object, chars, arguments, Set.of())).isSameAs(chars);
    assertThat(ranking.preferOverload(chars, object, arguments, Set.of())).isSameAs(chars);
  }

  @Test
  void keeps_current_when_no_overload_applies() {
    JMethodSymbol primitive = valueOf("int");

    assertThat(ranking.preferOverload(primitive, valueOf("char[]"), List.of(fixtures.string()), Set.of())).isSameAs(primitive);
  }

  @Test
  void picks_most_specific_variable_arity_overload() {
    JMethodSymbol objects = varargs("java.lang.Object[]");
    List<Type> strings = List.of(fixtures.string(), fixtures.string());

    assertThat(OverloadRanking.mostSpecificVarArgs(objects, strings)).isSameAs(varargs("java.lang.String[]"));
  }

  @Test
  void keeps_variable_arity_choice_when_no_overload_is_most_specific() {
    JMethodSymbol strings = varargs("java.lang.String[]");
    JType nullType = JType.nullType(fixtures.sema);

    assertThat(OverloadRanking.mostSpecificVarArgs(strings, List.of(nullType, nullType))).isSameAs(strings);
  }

  @Test
  void keeps_choice_that_needs_no_variable_arity() {
    JMethodSymbol objects = varargs("java.lang.Object[]");
    JMethodSymbol fixed = valueOf(OBJECT);

    assertThat(OverloadRanking.mostSpecificVarArgs(objects, List.of(JType.array(fixtures.sema, fixtures.string())))).isSameAs(objects);
    assertThat(OverloadRanking.mostSpecificVarArgs(objects, List.of(fixtures.string(), Type.UNKNOWN))).isSameAs(objects);
    assertThat(OverloadRanking.mostSpecificVarArgs(fixed, List.of(fixtures.string(), fixtures.string()))).isSameAs(fixed);
    JMethodSymbol unknown = unknown();
    assertThat(OverloadRanking.mostSpecificVarArgs(unknown, List.of(fixtures.string()))).isSameAs(unknown);
  }

  @Test
  void keeps_variable_arity_choice_applicable_by_boxing() {
    JMethodSymbol counted = varargs("int", "java.lang.String[]");

    assertThat(OverloadRanking.mostSpecificVarArgs(counted, List.of(fixtures.integer(), JType.array(fixtures.sema, fixtures.string())))).isSameAs(counted);
  }

  @Test
  void argument_or_parameter_unknown_is_applicable() {
    JMethodSymbol missing = fixtures.method(VARARGS, "take", "lib.Missing");

    assertThat(OverloadRanking.applicable(missing, List.of(fixtures.string()), 0)).isTrue();
    assertThat(OverloadRanking.applicable(valueOf("int"), List.of(Type.UNKNOWN), 0)).isTrue();
    assertThat(OverloadRanking.applicable(valueOf("int"), List.of(fixtures.string()), 1)).isFalse();
  }

  @Test
  void boxing_applies_from_the_second_phase() {
    JType primitive = fixtures.primitive("int");

    assertThat(OverloadRanking.applicable(valueOf(OBJECT), List.of(primitive), 0)).isFalse();
    assertThat(OverloadRanking.applicable(valueOf(OBJECT), List.of(primitive), 1)).isTrue();
    assertThat(OverloadRanking.applicable(valueOf("int"), List.of(fixtures.type("java.lang.Long")), 1)).isFalse();
  }

  @Test
  void variable_arity_needs_the_arguments_before_the_last_parameter() {
    assertThat(OverloadRanking.applicable(varargs("int", "java.lang.String[]"), List.of(), 2)).isFalse();
    assertThat(OverloadRanking.applicable(varargs("int", "java.lang.String[]"), List.of(fixtures.primitive("int")), 2)).isTrue();
  }

  @Test
  void more_specific_with_other_arity_is_more_specific() {
    JMethodSymbol format = fixtures.method(STRING, "format", STRING, "java.lang.Object[]");
    JMethodSymbol localized = fixtures.method(STRING, "format", "java.util.Locale", STRING, "java.lang.Object[]");

    assertThat(OverloadRanking.moreSpecific(format, localized)).isTrue();
    assertThat(OverloadRanking.moreSpecific(valueOf(OBJECT), valueOf("int"))).isFalse();
  }

  private JMethodSymbol valueOf(String... parameters) {
    return fixtures.method(STRING, VALUE_OF, parameters);
  }

  private JMethodSymbol unknown() {
    JMethodSymbol unknown = new JMethodSymbol(fixtures.sema, null, "of");
    unknown.unknown(true);
    return unknown;
  }

  private JMethodSymbol varargs(String... parameters) {
    return fixtures.method(VARARGS, "of", parameters);
  }

  private static String member(String descriptor, boolean varargs, boolean isStatic) {
    return member("of", descriptor, varargs, isStatic);
  }

  private static String member(String name, String descriptor, boolean varargs, boolean isStatic) {
    return """
      {"k": "m", "name": "%s", "desc": "%s", "vis": "public", "abstract": false, "final": false, "static": %s,
       "synchronized": false, "varargs": %s, "retSelf": false, "throws": []}
      """.formatted(name, descriptor, isStatic, varargs);
  }
}
