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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.sonar.plugins.java.api.semantic.Type;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.java.semsitter.JTypeFixtures.json;

class FunctionalInferenceTest {

  private static final String LIST = "java.util.List";
  private static final String T = "{\"id\": \"T\", \"kind\": \"typevar\"}";
  private static final String LIST_OF_T = "{\"id\": \"java.util.List\", \"args\": [" + T + "]}";

  private final JTypeFixtures fixtures = new JTypeFixtures();
  private final FunctionalInference inference = fixtures.sema.functionalInference();

  @Test
  void functional_types_are_unknown_until_inference_from_the_target_lands() {
    JMethodSymbol valueOf = fixtures.method("java.lang.String", "valueOf", "java.lang.Object");
    JType string = fixtures.string();

    assertThat(inference.functionProducing(string, string)).isNull();
    assertThat(inference.returnsClassVariable(valueOf)).isFalse();
    assertThat(inference.unboundReferenceResult(valueOf, string)).isNull();
    assertThat(inference.targetedParameters(valueOf, string)).isNull();
  }

  @Test
  void target_binds_only_the_variables_its_supertype_arguments_name() {
    JType collection = fixtures.type("java.util.Collection", fixtures.string());

    assertThat(bindReturn(LIST_OF_T, fixtures.type("java.util.Collection", fixtures.extending(fixtures.string())), Set.of("T"))).isEmpty();
    assertThat(bindReturn(LIST_OF_T, collection, Set.of("U"))).isEmpty();
    assertThat(bindReturn("{\"id\": \"java.util.List\", \"args\": [{\"id\": \"java.lang.String\"}]}", collection, Set.of("T"))).isEmpty();
    assertThat(bindReturn(LIST_OF_T, collection, Set.of("T"))).containsEntry("T", fixtures.string());
  }

  @Test
  void binds_type_variables_through_wildcards_arrays_and_supertypes() {
    assertThat(bind("{\"kind\": \"wildcard\", \"super\": " + T + "}", fixtures.string())).containsEntry("T", fixtures.string());
    assertThat(bind("{\"kind\": \"wildcard\", \"extends\": " + T + "}", fixtures.string())).containsEntry("T", fixtures.string());
    assertThat(bind("{\"kind\": \"wildcard\"}", fixtures.string())).isEmpty();
    assertThat(bind("{\"kind\": \"array\", \"elem\": " + T + "}", JType.array(fixtures.sema, fixtures.primitive("int")))).containsEntry("T", fixtures.integer());
    assertThat(bind("{\"kind\": \"array\", \"elem\": " + T + "}", fixtures.string())).isEmpty();
    assertThat(bind(LIST_OF_T, fixtures.type("java.util.ArrayList", fixtures.string()))).containsEntry("T", fixtures.string());
    assertThat(bind(LIST_OF_T, fixtures.type("java.util.ArrayList"))).isEmpty();
    assertThat(bind(LIST_OF_T, fixtures.string())).isEmpty();
    assertThat(bind("{\"id\": \"java.util.List\", \"args\": [\"T\"]}", fixtures.type(LIST, fixtures.string()))).isEmpty();
    assertThat(bind("{\"id\": \"java.util.List\", \"args\": []}", fixtures.type(LIST, fixtures.string()))).isEmpty();
    assertThat(bind("{\"args\": [" + T + "]}", fixtures.type(LIST, fixtures.string()))).isEmpty();
    assertThat(bind("{\"id\": \"U\", \"kind\": \"typevar\"}", fixtures.string())).isEmpty();
  }

  @Test
  void binds_nothing_from_open_or_unknown_types() {
    JType variable = JType.typeVar(fixtures.sema, "E", "lib.Other`1<E>");

    assertThat(bind(T, Type.UNKNOWN)).isEmpty();
    assertThat(bind(T, JType.nullType(fixtures.sema))).isEmpty();
    assertThat(bind(T, variable)).isEmpty();
    assertThat(bind(T, fixtures.extending(fixtures.object()))).isEmpty();
    assertThat(bind(T, fixtures.object())).containsEntry("T", fixtures.object());
  }

  @Test
  void bound_of_a_wildcard_binds_as_a_capture() {
    Type bound = bind(T, fixtures.extending(fixtures.string())).get("T");

    assertThat(((JType) bound).isCapture()).isTrue();
    assertThat(bound.erasure()).isEqualTo(fixtures.string());
    assertThat(bind(T, JType.superWildcard(fixtures.sema, fixtures.string()))).containsEntry("T", fixtures.string());
  }

  private Map<String, Type> bindReturn(String declared, Type target, Set<String> variables) {
    Map<String, Type> bound = new HashMap<>();
    inference.bindReturnToTarget(json(declared), target, variables, bound);
    return bound;
  }

  private Map<String, Type> bind(String declared, Type actual) {
    Map<String, Type> bound = new HashMap<>();
    inference.bindFromType(json(declared), actual, Set.of("T"), bound);
    return bound;
  }
}
