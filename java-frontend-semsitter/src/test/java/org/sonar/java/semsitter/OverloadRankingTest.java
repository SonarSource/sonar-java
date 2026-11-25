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
          %s, %s, %s, %s, %s, %s, %s
        ]
      }
    }
    """.formatted(member("(Ljava/lang/Long;)V", false, true), member("([Ljava/lang/Object;)V", true, true), member("([Ljava/lang/String;)V", true, true),
    member("([Ljava/lang/Integer;)V", true, true), member("(I[Ljava/lang/String;)V", true, false),
    member("(Ljava/lang/String;[Ljava/lang/Object;)V", true, true), member("take", "(Llib/Missing;)V", false, true));

  private final JTypeFixtures fixtures = new JTypeFixtures(VARARGS_TYPE);

  @Test
  void instance_ranking_keeps_the_given_choice() {
    OverloadRanking ranking = fixtures.sema.overloads();
    JMethodSymbol objects = varargs(OBJECT + "[]");

    assertThat(ranking.preferOverload(objects, valueOf("int"), List.of(fixtures.string()), Set.of())).isSameAs(objects);
    assertThat(ranking.mostSpecificVarArgs(objects, List.of(fixtures.string()))).isSameAs(objects);
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
