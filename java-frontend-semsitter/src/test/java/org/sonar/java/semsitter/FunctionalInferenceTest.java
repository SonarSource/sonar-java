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

  private static final String FUNCTION = "java.util.function.Function";
  private static final String SUPPLIER = "java.util.function.Supplier";
  private static final String OPTIONAL = "java.util.Optional";
  private static final String LIST = "java.util.List";
  private static final String COLLECTIONS = "java.util.Collections";
  private static final String T = "{\"id\": \"T\", \"kind\": \"typevar\"}";
  private static final String LIST_OF_T = "{\"id\": \"java.util.List\", \"args\": [" + T + "]}";

  private final JTypeFixtures fixtures = new JTypeFixtures();
  private final FunctionalInference inference = fixtures.sema.functionalInference();

  @Test
  void function_producing_a_value_knows_only_its_result() {
    Type function = inference.functionProducing(fixtures.type(FUNCTION, fixtures.string(), fixtures.object()), fixtures.primitive("int"));

    assertThat(function.erasure()).isEqualTo(fixtures.type(FUNCTION));
    assertThat(function.typeArguments().get(0).isUnknown()).isTrue();
    assertThat(function.typeArguments().get(1)).isEqualTo(fixtures.integer());
    assertThat(inference.functionProducing(fixtures.type(SUPPLIER, fixtures.object()), fixtures.string()).typeArguments()).containsExactly(fixtures.string());
  }

  @Test
  void function_producing_needs_a_parameterized_functional_interface_and_a_known_value() {
    JType function = fixtures.type(FUNCTION, fixtures.string(), fixtures.object());
    JType variable = JType.typeVar(fixtures.sema, "E", "lib.Other`1<E>");

    assertThat(inference.functionProducing(fixtures.type(FUNCTION), fixtures.string())).isNull();
    assertThat(inference.functionProducing(Type.UNKNOWN, fixtures.string())).isNull();
    assertThat(inference.functionProducing(function, Type.UNKNOWN)).isNull();
    assertThat(inference.functionProducing(function, variable)).isNull();
    assertThat(inference.functionProducing(function, fixtures.type(LIST, variable))).isNull();
    assertThat(inference.functionProducing(fixtures.type(LIST, fixtures.string()), fixtures.string())).isNull();
    assertThat(inference.functionProducing(fixtures.type("java.util.function.Consumer", fixtures.string()), fixtures.string())).isNull();
    assertThat(inference.functionProducing(fixtures.type(FUNCTION, fixtures.string()), fixtures.string())).isNull();
  }

  @Test
  void methods_returning_a_type_variable_are_recognized() {
    assertThat(inference.returnsClassVariable(fixtures.method(OPTIONAL, "get"))).isTrue();
    assertThat(inference.returnsClassVariable(fixtures.method("java.lang.String", "length"))).isFalse();
    assertThat(inference.returnsClassVariable(fixtures.method("java.lang.Object", "clone"))).isFalse();
    JMethodSymbol source = new JMethodSymbol(fixtures.sema, null, "get");
    source.returnTypeSymbol(fixtures.sema.memberFactory().typeParameter("lib.Box`1<T>").type().symbol());
    assertThat(inference.returnsClassVariable(source)).isTrue();
  }

  @Test
  void unbound_reference_takes_its_result_from_the_receiver_the_function_applies_to() {
    JMethodSymbol get = fixtures.method(OPTIONAL, "get");

    assertThat(inference.unboundReferenceResult(get, fixtures.type(FUNCTION, fixtures.type(OPTIONAL, fixtures.string()), fixtures.object())))
      .isEqualTo(fixtures.string());
    assertThat(inference.unboundReferenceResult(get, fixtures.type(FUNCTION, JType.superWildcard(fixtures.sema, fixtures.type(OPTIONAL, fixtures.string())),
      fixtures.object()))).isEqualTo(fixtures.string());
  }

  @Test
  void unbound_reference_result_is_unknown_without_a_known_receiver() {
    JMethodSymbol get = fixtures.method(OPTIONAL, "get");
    JType variable = JType.typeVar(fixtures.sema, "E", "lib.Other`1<E>");

    assertThat(inference.unboundReferenceResult(get, fixtures.type(FUNCTION, fixtures.type(OPTIONAL), fixtures.object()))).isNull();
    assertThat(inference.unboundReferenceResult(get, fixtures.type(FUNCTION, fixtures.string(), fixtures.object()))).isNull();
    assertThat(inference.unboundReferenceResult(get, fixtures.type(FUNCTION, JType.unknown(fixtures.sema), fixtures.object()))).isNull();
    assertThat(inference.unboundReferenceResult(get, fixtures.type(FUNCTION, fixtures.type(OPTIONAL, variable), fixtures.object()))).isNull();
    assertThat(inference.unboundReferenceResult(get, fixtures.type(SUPPLIER, fixtures.string()))).isNull();
    assertThat(inference.unboundReferenceResult(get, fixtures.string())).isNull();
  }

  @Test
  void unbound_reference_result_is_unknown_for_static_or_generic_methods() {
    JType function = fixtures.type(FUNCTION, fixtures.type("java.util.stream.Stream", fixtures.string()), fixtures.object());

    assertThat(inference.unboundReferenceResult(fixtures.method("java.util.stream.Stream", "collect", "java.util.stream.Collector"), function)).isNull();
    assertThat(inference.unboundReferenceResult(fixtures.method(COLLECTIONS, "singletonList", "java.lang.Object"), function)).isNull();
  }

  @Test
  void target_fixes_the_parameters_of_a_generic_method() {
    JMethodSymbol singletonList = fixtures.method(COLLECTIONS, "singletonList", "java.lang.Object");
    JMethodSymbol entry = fixtures.method("java.util.Map", "entry", "java.lang.Object", "java.lang.Object");

    assertThat(inference.targetedParameters(singletonList, fixtures.type(LIST, fixtures.string()))).containsExactly(fixtures.string());
    assertThat(inference.targetedParameters(singletonList, fixtures.type("java.util.Collection", fixtures.string()))).containsExactly(fixtures.string());
    assertThat(inference.targetedParameters(entry, fixtures.type("java.util.Map$Entry", fixtures.string(), fixtures.extending(fixtures.object()))))
      .containsExactly(fixtures.string(), Type.UNKNOWN);
  }

  @Test
  void target_fixes_nothing_for_other_methods() {
    JMethodSymbol singletonList = fixtures.method(COLLECTIONS, "singletonList", "java.lang.Object");

    assertThat(inference.targetedParameters(singletonList, fixtures.string())).isNull();
    assertThat(inference.targetedParameters(singletonList, fixtures.type("java.util.Map", fixtures.string(), fixtures.string()))).isNull();
    assertThat(inference.targetedParameters(fixtures.method(COLLECTIONS, "emptyList"), fixtures.type(LIST, fixtures.string()))).isNull();
    assertThat(inference.targetedParameters(fixtures.method(OPTIONAL, "get"), fixtures.string())).isNull();
    assertThat(inference.targetedParameters(fixtures.method("java.lang.Object", "clone"), fixtures.string())).isNull();
    assertThat(inference.targetedParameters(fixtures.method("java.util.ArrayList", "<init>", "java.util.Collection"), fixtures.string())).isNull();
    assertThat(inference.targetedParameters(singletonList, Type.UNKNOWN)).isNull();
    assertThat(inference.targetedParameters(fixtures.method("java.util.Objects", "requireNonNull", "java.lang.Object"), fixtures.type(LIST, fixtures.type("lib.Missing"))))
      .isNull();
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
