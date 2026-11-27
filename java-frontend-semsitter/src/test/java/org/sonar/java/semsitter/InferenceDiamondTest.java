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
import org.junit.jupiter.api.Test;
import org.sonar.plugins.java.api.semantic.Type;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.java.semsitter.JTypeFixtures.library;
import static org.sonar.java.semsitter.JTypeFixtures.member;

class InferenceDiamondTest {

  private static final String STREAM = "java.util.stream.Stream";
  private static final String COLLECTOR = "java.util.stream.Collector";
  private static final String LIST = "java.util.List";
  private static final String MAP = "java.util.Map";
  private static final String COLLECTION = "java.util.Collection";
  private static final String ARRAY_LIST = "java.util.ArrayList";
  private static final String CHAR_SEQUENCE = "java.lang.CharSequence";
  private static final String KEYED = "lib.Keyed";
  private static final String K = "{\"id\": \"K\", \"kind\": \"typevar\"}";
  private static final String V = "{\"id\": \"V\", \"kind\": \"typevar\"}";

  private final JTypeFixtures fixtures = new JTypeFixtures(
    library(KEYED, "\"typeParameters\": 2, \"typeVars\": [\"K\", \"V\"], \"interfaces\": [\"java.lang.Iterable\", \"java.lang.Comparable\"],"
      + "\"interfaceTypes\": [{\"id\": \"java.lang.Iterable\", \"args\": [" + V + "]}, {\"id\": \"java.lang.Comparable\", \"args\": [{\"id\": \"java.lang.String\"}]}]",
      member("<init>", "(Ljava/lang/Object;Ljava/lang/Object;)V", "\"paramVars\": [\"c0\", \"c1\"], \"paramTypes\": [" + K + ", " + V + "]"),
      member("<init>", "(Ljava/lang/Object;)V", "\"paramVars\": [\"c0\"], \"paramTypes\": [" + K + "]")));
  private final Inference inference = fixtures.sema.inference();
  private final JMethodSymbol collect = fixtures.method(STREAM, "collect", COLLECTOR);

  @Test
  void collected_result_takes_the_supertype_its_target_asks_for() {
    Type result = retarget(fixtures.string(), fixtures.type(LIST, fixtures.string()), fixtures.type(COLLECTION, fixtures.type(CHAR_SEQUENCE)));

    assertThat(result).isEqualTo(fixtures.type(LIST, fixtures.type(CHAR_SEQUENCE)));
  }

  @Test
  void collected_result_stands_when_the_target_does_not_widen_its_element() {
    JType strings = fixtures.type(LIST, fixtures.string());

    assertThat(retarget(fixtures.string(), strings, fixtures.type(COLLECTION, fixtures.string()))).isNull();
    assertThat(retarget(fixtures.string(), strings, fixtures.type(COLLECTION, fixtures.integer()))).isNull();
    assertThat(retarget(fixtures.string(), strings, fixtures.type(COLLECTION, fixtures.extending(fixtures.type(CHAR_SEQUENCE))))).isNull();
    assertThat(retarget(fixtures.string(), strings, fixtures.type(COLLECTION, fixtures.type(LIST, fixtures.string())))).isNull();
    assertThat(retarget(fixtures.string(), strings, fixtures.type(COLLECTION, JType.array(fixtures.sema, fixtures.string())))).isNull();
    assertThat(retarget(fixtures.string(), strings, fixtures.type(MAP, fixtures.string(), fixtures.string()))).isNull();
    assertThat(retarget(fixtures.string(), strings, fixtures.type(COLLECTION))).isNull();
    assertThat(retarget(fixtures.string(), strings, fixtures.type("java.lang.Iterable", fixtures.type(CHAR_SEQUENCE)))).isEqualTo(fixtures.type(LIST, fixtures.type(CHAR_SEQUENCE)));
  }

  @Test
  void collected_result_stands_when_its_element_is_not_a_plain_class() {
    JType strings = fixtures.type(LIST, fixtures.string());
    JType target = fixtures.type(COLLECTION, fixtures.type(CHAR_SEQUENCE));

    assertThat(retarget(fixtures.extending(fixtures.string()), strings, target)).isNull();
    assertThat(retarget(JType.unknown(fixtures.sema), strings, target)).isNull();
    assertThat(retarget(fixtures.type(LIST, fixtures.string()), strings, target)).isNull();
    assertThat(retarget(JType.array(fixtures.sema, fixtures.string()), strings, target)).isNull();
    assertThat(retarget(fixtures.string(), fixtures.type(MAP, fixtures.string(), fixtures.string()), fixtures.type(MAP, fixtures.type(CHAR_SEQUENCE), fixtures.string())))
      .isNull();
    assertThat(retarget(fixtures.string(), fixtures.type(LIST, fixtures.integer()), target)).isNull();
  }

  @Test
  void collected_result_stands_for_other_calls() {
    JType strings = fixtures.type(LIST, fixtures.string());
    JType target = fixtures.type(COLLECTION, fixtures.type(CHAR_SEQUENCE));
    JType stream = fixtures.type(STREAM, fixtures.string());

    assertThat(inference.retargetedElement(fixtures.method(STREAM, "count"), stream, strings, target)).isNull();
    assertThat(inference.retargetedElement(fixtures.method("java.lang.Object", "clone"), stream, strings, target)).isNull();
    assertThat(inference.retargetedElement(fixtures.method("java.util.ArrayList", "<init>", COLLECTION), stream, strings, target)).isNull();
    assertThat(inference.retargetedElement(collect, fixtures.type(STREAM), strings, target)).isNull();
    assertThat(inference.retargetedElement(collect, stream, fixtures.string(), target)).isNull();
    assertThat(inference.retargetedElement(collect, stream, strings, fixtures.string())).isNull();
    assertThat(inference.retargetedElement(collect, Type.UNKNOWN, strings, target)).isNull();
    assertThat(inference.retargetedElement(collect, stream, Type.UNKNOWN, target)).isNull();
    assertThat(inference.retargetedElement(collect, stream, strings, Type.UNKNOWN)).isNull();
    JMethodSymbol unknown = new JMethodSymbol(fixtures.sema, null, "collect");
    unknown.unknown(true);
    assertThat(inference.retargetedElement(unknown, stream, strings, target)).isNull();
  }

  @Test
  void collected_result_widens_only_the_argument_carrying_the_element() {
    JType counts = fixtures.type(MAP, fixtures.string(), fixtures.integer());

    assertThat(retarget(fixtures.string(), counts, fixtures.type(MAP, fixtures.string(), fixtures.integer()))).isNull();
    assertThat(retarget(fixtures.string(), counts, fixtures.type(MAP, fixtures.type(CHAR_SEQUENCE), fixtures.integer())))
      .isEqualTo(fixtures.type(MAP, fixtures.type(CHAR_SEQUENCE), fixtures.integer()));
  }

  @Test
  void diamond_takes_the_target_arguments() {
    assertThat(inference.inferDiamond(fixtures.type(ARRAY_LIST), fixtures.type(LIST, fixtures.string()))).isEqualTo(fixtures.type(ARRAY_LIST, fixtures.string()));
    assertThat(inference.inferDiamond(fixtures.type(ARRAY_LIST), fixtures.type(ARRAY_LIST, fixtures.string()))).isEqualTo(fixtures.type(ARRAY_LIST, fixtures.string()));
  }

  @Test
  void diamond_stands_when_the_target_leaves_a_type_parameter_open() {
    JType keyed = fixtures.type(KEYED);

    assertThat(inference.inferDiamond(keyed, fixtures.type("java.lang.Iterable", fixtures.string()))).isSameAs(keyed);
    assertThat(inference.inferDiamond(keyed, fixtures.type("java.lang.Comparable", fixtures.string()))).isSameAs(keyed);
    assertThat(inference.inferDiamond(keyed, fixtures.type(LIST, fixtures.string()))).isSameAs(keyed);
    assertThat(inference.inferDiamond(fixtures.string(), fixtures.type("java.lang.Comparable", fixtures.string()))).isSameAs(fixtures.string());
    assertThat(inference.inferDiamond(keyed, fixtures.type("java.lang.Iterable"))).isSameAs(keyed);
    assertThat(inference.inferDiamond(keyed, Type.UNKNOWN)).isSameAs(keyed);
    JType unknown = JType.unknown(fixtures.sema);
    assertThat(inference.inferDiamond(unknown, fixtures.type(LIST, fixtures.string()))).isSameAs(unknown);
    assertThat(inference.inferDiamond(Type.UNKNOWN, fixtures.type(LIST, fixtures.string()))).isSameAs(Type.UNKNOWN);
  }

  @Test
  void diamond_takes_the_constructor_arguments() {
    JMethodSymbol copy = fixtures.method(ARRAY_LIST, "<init>", COLLECTION);
    JMethodSymbol pair = fixtures.method(KEYED, "<init>", "java.lang.Object", "java.lang.Object");

    assertThat(inference.inferDiamondFromArguments(copy, fixtures.type(ARRAY_LIST), List.of(fixtures.type(LIST, fixtures.string()))))
      .isEqualTo(fixtures.type(ARRAY_LIST, fixtures.string()));
    assertThat(inference.inferDiamondFromArguments(pair, fixtures.type(KEYED), List.of(fixtures.string(), fixtures.primitive("int"))))
      .isEqualTo(fixtures.type(KEYED, fixtures.string(), fixtures.integer()));
  }

  @Test
  void diamond_stands_when_the_constructor_arguments_leave_a_type_parameter_open() {
    JType keyed = fixtures.type(KEYED);
    JType arrayList = fixtures.type(ARRAY_LIST);
    JMethodSymbol single = fixtures.method(KEYED, "<init>", "java.lang.Object");
    JMethodSymbol copy = fixtures.method(ARRAY_LIST, "<init>", COLLECTION);

    assertThat(inference.inferDiamondFromArguments(single, keyed, List.of(fixtures.string()))).isSameAs(keyed);
    assertThat(inference.inferDiamondFromArguments(copy, arrayList, List.of())).isSameAs(arrayList);
    assertThat(inference.inferDiamondFromArguments(copy, arrayList, List.of(Type.UNKNOWN))).isSameAs(arrayList);
    assertThat(inference.inferDiamondFromArguments(fixtures.method(ARRAY_LIST, "<init>"), arrayList, List.of())).isSameAs(arrayList);
    assertThat(inference.inferDiamondFromArguments(fixtures.method("java.lang.String", "<init>"), fixtures.string(), List.of())).isSameAs(fixtures.string());
    JType parameterized = fixtures.type(ARRAY_LIST, fixtures.string());
    assertThat(inference.inferDiamondFromArguments(copy, parameterized, List.of(fixtures.type(LIST, fixtures.integer())))).isSameAs(parameterized);
    JType unknown = JType.unknown(fixtures.sema);
    assertThat(inference.inferDiamondFromArguments(copy, unknown, List.of())).isSameAs(unknown);
    assertThat(inference.inferDiamondFromArguments(copy, Type.UNKNOWN, List.of())).isSameAs(Type.UNKNOWN);
  }

  @Test
  void ground_type_replaces_wildcards_by_their_bounds() {
    JType function = fixtures.type("java.util.function.Function", JType.superWildcard(fixtures.sema, fixtures.string()), fixtures.extending(fixtures.integer()));
    JType strings = fixtures.type(LIST, fixtures.string());

    assertThat(inference.groundType(function)).isEqualTo(fixtures.type("java.util.function.Function", fixtures.string(), fixtures.integer()));
    assertThat(inference.groundType(strings)).isSameAs(strings);
    assertThat(inference.groundType(fixtures.string())).isSameAs(fixtures.string());
    assertThat(inference.groundType(Type.UNKNOWN)).isSameAs(Type.UNKNOWN);
  }

  @Test
  void capture_conversion_captures_wildcard_arguments() {
    JType numbers = fixtures.type(MAP, fixtures.string(), fixtures.extending(fixtures.type("java.lang.Number")));
    JType strings = fixtures.type(LIST, fixtures.string());

    Type captured = inference.captureWildcards(numbers);

    assertThat(captured.typeArguments().get(0)).isSameAs(fixtures.string());
    assertThat(((JType) captured.typeArguments().get(1)).isCapture()).isTrue();
    assertThat(captured.typeArguments().get(1).erasure()).isEqualTo(fixtures.type("java.lang.Number"));
    assertThat(inference.captureWildcards(strings)).isSameAs(strings);
    assertThat(inference.captureWildcards(fixtures.string())).isSameAs(fixtures.string());
    assertThat(inference.captureWildcards(Type.UNKNOWN)).isSameAs(Type.UNKNOWN);
  }

  @Test
  void declared_type_variables_are_the_method_own() {
    assertThat(inference.declaresTypeVariables(fixtures.method("java.util.Collections", "emptyList"), false)).isTrue();
    assertThat(inference.declaresTypeVariables(fixtures.method("java.util.Optional", "get"), true)).isFalse();
    assertThat(inference.declaresTypeVariables(fixtures.method(ARRAY_LIST, "<init>", COLLECTION), true)).isTrue();
    assertThat(inference.declaresTypeVariables(fixtures.method("java.lang.Object", "clone"), false)).isFalse();
  }

  private Type retarget(JType element, JType returned, JType target) {
    return inference.retargetedElement(collect, fixtures.type(STREAM, element), returned, target);
  }
}
