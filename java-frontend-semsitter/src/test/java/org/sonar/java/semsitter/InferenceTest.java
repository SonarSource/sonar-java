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

class InferenceTest {

  private static final String BOX = "lib.Box";
  private static final String T = "{\"id\": \"T\", \"kind\": \"typevar\"}";
  private static final String X = "{\"id\": \"X\", \"kind\": \"typevar\"}";
  private static final String BOX_OF_T = "{\"id\": \"lib.Box\", \"args\": [" + T + "]}";
  private static final String INNER_FACT = "{\"id\": \"lib.Box$Inner\"}";
  private static final String MISSING = "()Llib/Missing;";
  private static final String COLLECTIONS = "java.util.Collections";
  private static final String OPTIONAL = "java.util.Optional";
  private static final String LIST = "java.util.List";
  private static final String SUPPLIER = "java.util.function.Supplier";

  private final JTypeFixtures fixtures = new JTypeFixtures(
    library(BOX, "\"typeParameters\": 1, \"typeVars\": [\"T\"], \"typeVarBounds\": [[{\"id\": \"java.lang.Object\", \"args\": []}]]",
      member("self", "()Llib/Box;", "\"retSelf\": true, \"retType\": " + BOX_OF_T),
      member("get", "()Ljava/lang/Object;", "\"retParam\": 0, \"retType\": " + T),
      member("inner", "()Llib/Box$Inner;", "\"retType\": " + INNER_FACT),
      member("make", "()Llib/Box$Inner;", "\"static\": true, \"retType\": " + INNER_FACT),
      member("nested", "()Llib/Box$Nested;", "\"retType\": {\"id\": \"lib.Box$Nested\"}"),
      member("name", "()Ljava/lang/String;", "\"retType\": {\"id\": \"java.lang.String\"}"),
      member("late", MISSING, "\"retType\": {\"id\": \"java.lang.String\"}"),
      member("absent", MISSING, "\"retType\": {\"id\": \"lib.Missing\"}"),
      member("lost", MISSING, "\"typeVars\": [\"E\"], \"retType\": {\"id\": \"E\", \"kind\": \"typevar\"}"),
      member("held", "()Llib/Missing;", "\"retType\": " + T),
      member("count", "()I", "\"retType\": {\"id\": \"int\"}"),
      member("deep", "()Llib/Box.Deep;", "\"retType\": {\"id\": \"lib.Box.Deep\"}"),
      member("silent", "()V", "\"throwsTypes\": []"),
      member("rethrow", "()V", "\"throwsTypes\": [" + T + "]"),
      member("pair", "(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/util/List;", "\"static\": true, \"varargs\": true, \"typeVars\": [\"E\"],"
        + "\"paramVars\": [\"m0\", \"m0[]\"], \"paramTypes\": [{\"id\": \"E\", \"kind\": \"typevar\"}, {\"kind\": \"array\", \"elem\": {\"id\": \"E\", \"kind\": \"typevar\"}}],"
        + "\"retType\": {\"id\": \"java.util.List\", \"args\": [{\"id\": \"E\", \"kind\": \"typevar\"}]}"),
      member("listed", "()Ljava/util/List;", "\"retType\": {\"id\": \"java.util.List\", \"args\": [{\"id\": \"java.lang.String\"}]}"),
      member("fail", "(Ljava/util/function/Supplier;)V", "\"typeVars\": [\"X\"], \"paramVars\": [\"\"], \"throwsTypes\": [{\"id\": \"java.io.IOException\"}, " + X + "],"
        + "\"paramTypes\": [{\"id\": \"java.util.function.Supplier\", \"args\": [{\"kind\": \"wildcard\", \"extends\": " + X + "}]}]"),
      member("id", "(Ljava/lang/Object;)Ljava/lang/Object;", "\"typeVars\": [\"T\"], \"paramVars\": [\"m0\"], \"paramTypes\": [" + T + "], \"retType\": " + T),
      member("untyped", "(Ljava/lang/Object;)Ljava/lang/Object;", "\"static\": true, \"typeVars\": null, \"paramVars\": [\"m0\"], \"paramTypes\": [" + T + "], \"retType\": " + T)),
    library("lib.Box$Inner", ""),
    library("lib.Box$Nested", "\"modifiers\": [\"public\", \"static\"]"),
    library("lib.Box.Deep", ""));
  private final Inference inference = fixtures.sema.inference();

  @Test
  void clone_of_an_array_is_the_array_type() {
    JType strings = JType.array(fixtures.sema, fixtures.string());
    JMethodSymbol clone = fixtures.method("java.lang.Object", "clone");

    assertThat(inference.invocationReturn(clone, strings, List.of())).isSameAs(strings);
    assertThat(inference.invocationReturn(clone, fixtures.string(), List.of())).isEqualTo(fixtures.object());
  }

  @Test
  void inner_class_returned_by_a_member_keeps_the_receiver_arguments() {
    JType boxed = box(fixtures.string());

    Type inner = inference.invocationReturn(boxMethod("inner"), boxed, List.of());

    assertThat(inner.fullyQualifiedName()).isEqualTo("lib.Box$Inner");
    assertThat(((JType) inner).outer()).isEqualTo(boxed);
    assertThat(((JType) inference.invocationReturn(boxMethod("inner"), fixtures.type(BOX), List.of())).outer()).isNull();
    assertThat(((JType) inference.invocationReturn(boxMethod("make"), boxed, List.of())).outer()).isNull();
    assertThat(((JType) inference.invocationReturn(boxMethod("inner"), null, List.of())).outer()).isNull();
  }

  @Test
  void a_static_member_type_has_no_enclosing_arguments() {
    assertThat(((JType) inference.invocationReturn(boxMethod("nested"), box(fixtures.string()), List.of())).outer()).isNull();
  }

  @Test
  void other_members_of_a_parameterized_receiver_keep_their_return() {
    JType boxed = box(fixtures.string());

    assertThat(((JType) inference.invocationReturn(boxMethod("deep"), boxed, List.of())).outer()).isEqualTo(boxed);
    assertThat(inference.invocationReturn(boxMethod("count"), boxed, List.of())).isEqualTo(fixtures.primitive("int"));
    assertThat(inference.invocationReturn(boxMethod("listed"), boxed, List.of())).isEqualTo(fixtures.type(LIST, fixtures.string()));
    assertThat(inference.invocationReturn(boxMethod("absent"), boxed, List.of()).isUnknown()).isTrue();
    assertThat(inference.invocationReturn(fixtures.method("java.lang.Object", "toString"), JType.array(fixtures.sema, fixtures.string()), List.of()))
      .isEqualTo(fixtures.string());
  }

  @Test
  void member_of_a_capture_that_is_no_class_is_the_declared_one() {
    JType capturedArray = fixtures.capture(JType.array(fixtures.sema, fixtures.string()));

    assertThat(inference.invocationReturn(boxMethod("get"), capturedArray, List.of())).isEqualTo(fixtures.object());
    assertThat(inference.invocationReturn(boxMethod("make"), fixtures.capture(box(fixtures.string())), List.of()).fullyQualifiedName()).isEqualTo("lib.Box$Inner");
  }

  @Test
  void type_argument_still_open_is_not_the_return() {
    JType open = JType.typeVar(fixtures.sema, "E", "lib.Other`1<E>");

    assertThat(inference.invocationReturn(fixtures.method(OPTIONAL, "get"), fixtures.type(OPTIONAL, open), List.of())).isEqualTo(open);
    assertThat(inference.invocationReturn(fixtures.method(OPTIONAL, "get"), fixtures.type(OPTIONAL), List.of())).isEqualTo(fixtures.object());
    assertThat(inference.invocationReturn(fixtures.method(OPTIONAL, "get"), null, List.of())).isEqualTo(fixtures.object());
  }

  @Test
  void self_typed_method_without_receiver_returns_its_declaration() {
    assertThat(inference.invocationReturn(boxMethod("self"), null, List.of()).erasure()).isEqualTo(fixtures.type(BOX));
    assertThat(inference.invocationReturn(boxMethod("self"), Type.UNKNOWN, List.of()).erasure()).isEqualTo(fixtures.type(BOX));
    assertThat(inference.invocationReturn(fixtures.method("java.lang.Object", "getClass"), null, List.of())).isEqualTo(fixtures.type("java.lang.Class"));
  }

  @Test
  void variable_arity_type_variable_is_inferred_from_the_leading_argument() {
    JMethodSymbol pair = boxMethod("pair", "java.lang.Object", "java.lang.Object[]");

    assertThat(inference.invocationReturn(pair, null, List.of(fixtures.string()))).isEqualTo(fixtures.type(LIST, fixtures.string()));
  }

  @Test
  void self_typed_method_returns_its_receiver() {
    JType boxed = box(fixtures.string());
    JType captured = fixtures.capture(boxed);

    assertThat(inference.invocationReturn(boxMethod("self"), boxed, List.of())).isSameAs(boxed);
    assertThat(inference.invocationReturn(boxMethod("self"), captured, List.of())).isSameAs(captured);
  }

  @Test
  void self_typed_method_of_a_receiver_with_captured_arguments_returns_a_capture() {
    Type result = inference.invocationReturn(boxMethod("self"), box(fixtures.capture(fixtures.string())), List.of());

    assertThat(((JType) result).isCapture()).isTrue();
    assertThat(result.erasure()).isEqualTo(fixtures.type(BOX));
  }

  @Test
  void self_typed_method_whose_self_argument_is_captured_returns_a_capture_of_the_receiver() {
    JMethodSymbol sequential = fixtures.method("java.util.stream.BaseStream", "sequential");
    JType stream = fixtures.type("java.util.stream.Stream", fixtures.string());
    JType receiver = fixtures.type("java.util.stream.BaseStream", fixtures.string(), fixtures.capture(stream));

    Type result = inference.invocationReturn(sequential, receiver, List.of());

    assertThat(((JType) result).isCapture()).isTrue();
    assertThat(result.erasure()).isEqualTo(fixtures.type("java.util.stream.BaseStream"));
  }

  @Test
  void member_of_a_capture_is_the_member_of_its_bound() {
    Type result = inference.invocationReturn(boxMethod("get"), fixtures.capture(box(fixtures.string())), List.of());

    assertThat(result).isEqualTo(fixtures.string());
  }

  @Test
  void declared_return_stands_for_a_return_type_the_descriptor_cannot_name() {
    assertThat(inference.invocationReturn(boxMethod("late"), box(fixtures.string()), List.of())).isEqualTo(fixtures.string());
    assertThat(inference.invocationReturn(boxMethod("late"), null, List.of())).isEqualTo(fixtures.string());
    assertThat(inference.invocationReturn(boxMethod("absent"), null, List.of()).isUnknown()).isTrue();
    assertThat(inference.invocationReturn(boxMethod("lost"), null, List.of()).isUnknown()).isTrue();
  }

  @Test
  void library_method_returns_the_parameterization_its_signature_spells() {
    assertThat(inference.invocationReturn(boxMethod("listed"), fixtures.type(BOX), List.of())).isEqualTo(fixtures.type(LIST, fixtures.string()));
  }

  @Test
  void static_method_type_variable_is_inferred_from_explicit_type_arguments() {
    JMethodSymbol emptyList = fixtures.method(COLLECTIONS, "emptyList");

    Type result = fixtures.sema.substitution().withExplicitTypeArguments(List.of(fixtures.string()), () -> inference.invocationReturn(emptyList, null, List.of()));

    assertThat(result).isEqualTo(fixtures.type(LIST, fixtures.string()));
  }

  @Test
  void static_method_type_variable_is_inferred_from_arguments() {
    JMethodSymbol singletonList = fixtures.method(COLLECTIONS, "singletonList", "java.lang.Object");

    assertThat(inference.invocationReturn(singletonList, null, List.of(fixtures.primitive("int")))).isEqualTo(fixtures.type(LIST, fixtures.integer()));
    assertThat(inference.invocationReturn(singletonList, null, List.of(Type.UNKNOWN))).isEqualTo(fixtures.type(LIST));
  }

  @Test
  void a_method_type_variable_shadows_the_class_one_of_the_same_name() {
    JType integer = fixtures.type("java.lang.Integer");

    assertThat(inference.invocationReturn(boxMethod("id", "java.lang.Object"), box(fixtures.string()), List.of(integer))).isEqualTo(integer);
  }

  @Test
  void an_unbound_method_type_variable_does_not_take_the_class_argument_of_the_same_name() {
    assertThat(inference.invocationReturn(boxMethod("id", "java.lang.Object"), box(fixtures.string()), List.of(Type.UNKNOWN))).isEqualTo(fixtures.object());
  }

  @Test
  void method_without_type_variables_infers_nothing_from_its_arguments() {
    assertThat(inference.invocationReturn(boxMethod("untyped", "java.lang.Object"), null, List.of(fixtures.string()))).isEqualTo(fixtures.object());
  }

  @Test
  void raw_return_erases_the_declared_return() {
    assertThat(inference.rawReturn(fixtures.method(OPTIONAL, "get"))).isEqualTo(fixtures.object());
    assertThat(inference.rawReturn(fixtures.method(OPTIONAL, "map", "java.util.function.Function"))).isEqualTo(fixtures.type(OPTIONAL));
    assertThat(inference.rawReturn(boxMethod("absent"))).isNull();
    assertThat(inference.rawReturn(fixtures.method(OPTIONAL, "of", "java.lang.Object"))).isNull();
    assertThat(inference.rawReturn(fixtures.method("java.lang.String", "length"))).isNull();
    assertThat(inference.rawReturn(fixtures.method("java.lang.Object", "clone"))).isNull();
    assertThat(inference.rawReturn(boxMethod("held"))).isEqualTo(fixtures.object());
  }

  @Test
  void thrown_type_variable_is_inferred_from_the_argument() {
    JMethodSymbol orElseThrow = fixtures.method(OPTIONAL, "orElseThrow", SUPPLIER);
    JType supplier = fixtures.type(SUPPLIER, fixtures.type("java.lang.IllegalStateException"));

    assertThat(inference.invocationThrown(orElseThrow, fixtures.type(OPTIONAL, fixtures.string()), List.of(supplier)))
      .containsExactly(fixtures.type("java.lang.IllegalStateException"));
    assertThat(inference.invocationThrown(orElseThrow, null, List.of(supplier))).containsExactly(fixtures.type("java.lang.IllegalStateException"));
    assertThat(inference.invocationThrown(orElseThrow, null, List.of(Type.UNKNOWN))).isNull();
  }

  @Test
  void thrown_types_that_are_no_type_variable_keep_their_declared_type() {
    JType supplier = fixtures.type(SUPPLIER, fixtures.type("java.lang.IllegalStateException"));

    assertThat(inference.invocationThrown(boxMethod("fail", SUPPLIER), null, List.of(supplier)))
      .containsExactly(fixtures.type("java.io.IOException"), fixtures.type("java.lang.IllegalStateException"));
    assertThat(inference.invocationThrown(boxMethod("get"), null, List.of())).isNull();
    assertThat(inference.invocationThrown(fixtures.method("java.lang.Object", "clone"), null, List.of())).isNull();
    assertThat(inference.invocationThrown(boxMethod("silent"), null, List.of())).isNull();
    assertThat(inference.invocationThrown(unknown(), null, List.of())).isNull();
    assertThat(inference.invocationThrown(boxMethod("rethrow"), box(fixtures.type("lib.Missing")), List.of())).isNull();
    assertThat(inference.invocationThrown(boxMethod("rethrow"), box(fixtures.type("java.io.IOException")), List.of()))
      .containsExactly(fixtures.type("java.io.IOException"));
  }

  @Test
  void target_fixes_the_type_variables_of_the_result() {
    JMethodSymbol emptyList = fixtures.method(COLLECTIONS, "emptyList");
    JMethodSymbol emptyMap = fixtures.method(COLLECTIONS, "emptyMap");

    assertThat(inference.targetedReturn(emptyList, fixtures.type(LIST, fixtures.string()))).isEqualTo(fixtures.type(LIST, fixtures.string()));
    assertThat(inference.targetedReturn(emptyList, fixtures.string())).isNull();
    assertThat(inference.targetedReturn(emptyMap, fixtures.type("java.util.Map", fixtures.string(), fixtures.extending(fixtures.object())))).isNull();
    assertThat(inference.targetedReturn(boxMethod("get"), fixtures.string())).isNull();
    assertThat(inference.targetedReturn(fixtures.method("java.lang.Object", "clone"), fixtures.string())).isNull();
    assertThat(inference.targetedReturn(boxMethod("fail", SUPPLIER), fixtures.string())).isNull();
    assertThat(inference.targetedReturn(boxMethod("untyped", "java.lang.Object"), fixtures.string())).isNull();
  }

  @Test
  void method_without_type_variable_list_declares_none() {
    assertThat(inference.declaresTypeVariables(boxMethod("untyped", "java.lang.Object"), true)).isFalse();
  }

  private JMethodSymbol unknown() {
    JMethodSymbol unknown = new JMethodSymbol(fixtures.sema, null, "get");
    unknown.unknown(true);
    return unknown;
  }

  private JType box(JType argument) {
    return fixtures.type(BOX, argument);
  }

  private JMethodSymbol boxMethod(String name, String... parameters) {
    return fixtures.method(BOX, name, parameters);
  }
}
