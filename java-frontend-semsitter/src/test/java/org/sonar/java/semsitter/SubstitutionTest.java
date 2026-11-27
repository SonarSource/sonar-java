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
import static org.sonar.java.semsitter.JTypeFixtures.json;
import static org.sonar.java.semsitter.JTypeFixtures.library;
import static org.sonar.java.semsitter.JTypeFixtures.member;

class SubstitutionTest {

  private static final String BOX = "lib.Box";
  private static final String INNER = "lib.Box$Inner";
  private static final String LIST = "java.util.List";
  private static final String ARRAY_LIST = "java.util.ArrayList";
  private static final String OBJECT = "java.lang.Object";
  private static final String T = "{\"id\": \"T\", \"kind\": \"typevar\"}";
  private static final String BOX_VARIABLES = "\"typeParameters\": 1, \"typeVars\": [\"T\"]";

  private final JTypeFixtures fixtures = new JTypeFixtures(
    library(BOX, BOX_VARIABLES,
      member("put", "(Ljava/lang/Object;)V", "\"paramVars\": [\"c0\"], \"paramTypes\": [" + T + "]"),
      member("both", "(Ljava/lang/Object;Ljava/lang/Object;)V", "\"paramVars\": [\"\", \"\"], \"paramTypes\": [" + T + "]"),
      member("named", "(Ljava/util/List;)V", "\"paramVars\": [\"\"], \"paramTypes\": [{\"id\": \"java.util.List\", \"args\": [{\"id\": \"java.lang.String\"}]}]"),
      member("loose", "(Ljava/lang/Object;)V", "")),
    library(INNER, "", member("put", "(Ljava/lang/Object;)V", "\"paramVars\": [\"\"], \"paramTypes\": [" + T + "]")),
    "{\"lib.Bare\": {\"origin\": \"classpath\", \"kind\": \"class\", \"super\": \"java.lang.Object\", \"typeParameters\": 1, \"typeVars\": [\"T\"]}}");
  private final Substitution substitution = fixtures.sema.substitution();
  private final JType variable = JType.typeVar(fixtures.sema, "T", "T");

  @Test
  void member_type_reads_the_declaring_arguments() {
    assertThat(substitution.asMemberOf(variable, box(fixtures.string()))).isEqualTo(fixtures.string());
    assertThat(substitution.asMemberOf(variable, fixtures.type(BOX))).isSameAs(variable);
    assertThat(substitution.asMemberOf(variable, Type.UNKNOWN)).isSameAs(variable);
    assertThat(substitution.asMemberOf(Type.UNKNOWN, box(fixtures.string()))).isSameAs(Type.UNKNOWN);
  }

  @Test
  void field_read_through_a_receiver_takes_its_arguments() {
    JVariableSymbol field = field(variable);

    assertThat(substitution.fieldTypeOn(field, box(fixtures.string()))).isEqualTo(fixtures.string());
    assertThat(substitution.fieldTypeOn(field, fixtures.type(BOX))).isSameAs(variable);
    assertThat(substitution.fieldTypeOn(field, null)).isSameAs(variable);
    assertThat(substitution.fieldTypeOn(field(Type.UNKNOWN), box(fixtures.string()))).isSameAs(Type.UNKNOWN);
  }

  @Test
  void field_of_a_class_without_type_variables_keeps_its_type() {
    JVariableSymbol field = new JVariableSymbol(fixtures.sema, "lib.Box$Inner#held", "held", "field");
    field.owner(fixtures.type(INNER).symbol());
    field.type(variable);

    assertThat(substitution.fieldTypeOn(field, fixtures.type(INNER))).isSameAs(variable);
  }

  @Test
  void method_without_known_owner_is_declared_by_the_receiver() {
    JMethodSymbol orphan = new JMethodSymbol(fixtures.sema, null, "put");

    assertThat(substitution.declaringType(orphan, fixtures.string())).isSameAs(fixtures.string());
    assertThat(substitution.declaringType(method(BOX, "put", OBJECT), fixtures.string())).isSameAs(fixtures.string());
  }

  @Test
  void invocation_view_keeps_the_thrown_types_and_the_receiver_parameterization() {
    JMethodSymbol put = method(BOX, "put", OBJECT);
    List<Type> parameters = put.parameterTypes();
    Type returned = put.returnType().type();

    JMethodSymbol thrown = substitution.invocationView(put, null, parameters, returned, List.of(fixtures.type("java.io.IOException")));
    JMethodSymbol owned = substitution.invocationView(put, box(fixtures.string()), parameters, returned);

    assertThat(thrown).isNotSameAs(put);
    assertThat(thrown.thrownTypes()).containsExactly(fixtures.type("java.io.IOException"));
    assertThat(owned.owner().type()).isEqualTo(box(fixtures.string()));
    assertThat(substitution.invocationView(put, null, parameters, returned)).isSameAs(put);
    assertThat(substitution.invocationView(put, fixtures.type(BOX), parameters, returned)).isSameAs(put);
  }

  @Test
  void parameters_take_the_receiver_arguments() {
    assertThat(substitution.invocationParameters(method(BOX, "put", OBJECT), box(fixtures.string()), List.of(fixtures.integer()))).containsExactly(fixtures.string());
    assertThat(substitution.invocationParameters(method(BOX, "put", OBJECT), fixtures.type(BOX), List.of())).containsExactly(fixtures.object());
    assertThat(substitution.invocationParameters(method(BOX, "named", LIST), null, List.of())).containsExactly(fixtures.type(LIST, fixtures.string()));
    assertThat(substitution.invocationParameters(method(BOX, "both", OBJECT, OBJECT), box(fixtures.string()), List.of()))
      .containsExactly(fixtures.object(), fixtures.object());
    assertThat(substitution.invocationParameters(method(BOX, "loose", OBJECT), box(fixtures.string()), List.of())).containsExactly(fixtures.object());
  }

  @Test
  void member_of_an_inner_class_reads_the_enclosing_arguments() {
    JType inner = fixtures.type(INNER).withOuter(box(fixtures.string()));

    assertThat(substitution.invocationParameters(method(INNER, "put", OBJECT), inner, List.of())).containsExactly(fixtures.string());
    assertThat(substitution.classSubstitution(method(INNER, "put", OBJECT), inner)).containsEntry("T", fixtures.string());
    assertThat(substitution.classSubstitution(method(INNER, "put", OBJECT), fixtures.type(INNER))).isEmpty();
    assertThat(substitution.classSubstitution(method(BOX, "put", OBJECT), fixtures.type(INNER).withOuter(fixtures.type("lib.Bare", fixtures.string()))))
      .isEmpty();
  }

  @Test
  void arguments_are_preserved_when_the_supertype_gets_them() {
    JType strings = fixtures.type(ARRAY_LIST, fixtures.string());

    assertThat(substitution.preservesArguments(strings, fixtures.type(LIST, fixtures.string()))).isTrue();
    assertThat(substitution.preservesArguments(strings, fixtures.type(LIST, fixtures.extending(fixtures.object())))).isTrue();
    assertThat(substitution.preservesArguments(strings, fixtures.type(LIST))).isTrue();
    assertThat(substitution.preservesArguments(strings, fixtures.type(LIST, fixtures.integer()))).isFalse();
    assertThat(substitution.preservesArguments(fixtures.string(), fixtures.type(LIST, fixtures.string()))).isFalse();
    assertThat(substitution.preservesArguments(fixtures.type(ARRAY_LIST), fixtures.type(LIST, fixtures.string()))).isFalse();
    assertThat(substitution.preservesArguments(fixtures.type(LIST, fixtures.string(), fixtures.string()), fixtures.type(LIST, fixtures.string()))).isFalse();
  }

  @Test
  void supertype_is_instantiated_with_the_arguments_of_its_subtype() {
    JType declared = fixtures.type(LIST, JType.typeVar(fixtures.sema, "E", "E"));

    assertThat(substitution.instantiateSupertype(fixtures.type(ARRAY_LIST, fixtures.string()), declared)).isEqualTo(fixtures.type(LIST, fixtures.string()));
    assertThat(substitution.instantiateSupertype(fixtures.type(ARRAY_LIST), declared)).isSameAs(declared);
    assertThat(substitution.instantiateSupertype(fixtures.type(ARRAY_LIST, fixtures.string()), Type.UNKNOWN)).isSameAs(Type.UNKNOWN);
    assertThat(substitution.instantiateSupertype(fixtures.type("lib.Undeclared", fixtures.string()), declared)).isEqualTo(declared);
  }

  @Test
  void explicit_type_argument_fixes_a_method_variable() {
    List<Type> explicit = List.of(fixtures.string(), Type.UNKNOWN);

    assertThat(withExplicit(explicit, List.of("m0"), "m0", List.of())).isEqualTo(fixtures.string());
    assertThat(withExplicit(explicit, List.of("m1"), "m1", List.of(fixtures.integer()))).isEqualTo(fixtures.integer());
    assertThat(withExplicit(explicit, List.of("m2"), "m2", List.of(fixtures.integer()))).isEqualTo(fixtures.integer());
    assertThat(withExplicit(explicit, List.of("mx"), "mx", List.of(fixtures.integer()))).isEqualTo(fixtures.integer());
    assertThat(withExplicit(explicit, List.of("c0"), "c0", List.of(fixtures.integer()))).isEqualTo(fixtures.integer());
  }

  @Test
  void method_variable_is_inferred_from_its_first_known_argument() {
    JType strings = JType.array(fixtures.sema, fixtures.string());
    JType ints = JType.array(fixtures.sema, fixtures.primitive("int"));

    assertThat(substitution.methodArgument(List.of("m0", "m0"), "m0", List.of(Type.UNKNOWN, JType.nullType(fixtures.sema)))).isNull();
    assertThat(substitution.methodArgument(List.of("", "m0"), "m0", List.of(fixtures.string(), fixtures.primitive("long")))).isEqualTo(fixtures.type("java.lang.Long"));
    assertThat(substitution.methodArgument(List.of("m0[]"), "m0", List.of(strings))).isEqualTo(fixtures.string());
    assertThat(substitution.methodArgument(List.of("m0[]"), "m0", List.of(ints))).isEqualTo(ints);
    assertThat(substitution.methodArgument(List.of("m0[]", "m0"), "m0", List.of(fixtures.string(), fixtures.integer()))).isEqualTo(fixtures.integer());
    assertThat(substitution.methodArgument(List.of("m0", "m0[]"), "m0", List.of(Type.UNKNOWN, fixtures.string(), fixtures.integer())))
      .isEqualTo(fixtures.string());
  }

  @Test
  void class_variable_argument_is_read_from_the_declaring_type() {
    JMethodSymbol put = method(BOX, "put", OBJECT);

    assertThat(substitution.classVariableArgument(put, 0, box(fixtures.string()))).isEqualTo(fixtures.string());
    assertThat(substitution.classVariableArgument(put, 1, box(fixtures.string()))).isNull();
    assertThat(substitution.classVariableArgument(put, 0, fixtures.type(BOX))).isNull();
    assertThat(substitution.classVariableArgument(method(BOX, "named", LIST), 0, box(fixtures.string()))).isNull();
    assertThat(substitution.classVariableArgument(put, 0, fixtures.type("lib.Undeclared", fixtures.string()))).isNull();
    assertThat(substitution.classVariableArgument(put, 0, fixtures.type(LIST, fixtures.string()))).isNull();
  }

  @Test
  void returned_class_variable_is_found_among_the_declaring_variables() {
    JMethodSymbol get = fixtures.method("java.util.Optional", "get");

    assertThat(substitution.returnedClassVariable(get, fixtures.type("java.util.Optional"))).isZero();
    assertThat(substitution.returnedClassVariable(get, fixtures.type("lib.Undeclared"))).isNull();
    assertThat(substitution.returnedClassVariable(get, fixtures.string())).isNull();
    assertThat(substitution.returnedClassVariable(method(BOX, "put", OBJECT), fixtures.type(BOX))).isNull();
  }

  @Test
  void annotation_entry_names_the_type_argument_it_is_on() {
    assertThat(Substitution.isArgumentOf(json("{\"path\": [\"<1>\"]}"), 1)).isTrue();
    assertThat(Substitution.isArgumentOf(json("{\"path\": [\"<1>\"]}"), 0)).isFalse();
    assertThat(Substitution.isArgumentOf(json("{\"path\": [\"<1>\", \"<0>\"]}"), 1)).isFalse();
    assertThat(Substitution.isArgumentOf(json("{\"path\": [{\"kind\": \"array\"}]}"), 1)).isFalse();
    assertThat(Substitution.isArgumentOf(json("{}"), 1)).isFalse();
  }

  @Test
  void generic_member_needs_a_declaration_with_its_signature() {
    JMethodSymbol bare = new JMethodSymbol(fixtures.sema, "lib.Bare#get()", "get");
    bare.owner(fixtures.type("lib.Bare").symbol());
    bare.parameterTypes(List.of());

    assertThat(substitution.lookupGenericMember(bare)).isNull();
    assertThat(substitution.lookupGenericMember(method(BOX, "put", OBJECT))).isNotNull();
    assertThat(substitution.declaredReturn(method(BOX, "put", OBJECT))).isNull();
    assertThat(substitution.parameterVariables(bare)).isEmpty();
  }

  @Test
  void parameters_named_by_a_type_variable_match_any_erasure() {
    JMethodSymbol put = method(BOX, "put", OBJECT);
    JMethodSymbol unknown = put.withSignature(List.of(Type.UNKNOWN), put.returnType().type());
    JMethodSymbol variableParameter = put.withSignature(List.of(variable), put.returnType().type());
    JMethodSymbol local = put.withSignature(List.of(fixtures.type("lib.Box::Local")), put.returnType().type());
    JMethodSymbol other = put.withSignature(List.of(fixtures.string()), put.returnType().type());

    assertThat(substitution.lookupGenericMember(unknown)).isNotNull();
    assertThat(substitution.lookupGenericMember(variableParameter)).isNotNull();
    assertThat(substitution.lookupGenericMember(local)).isNotNull();
    assertThat(substitution.lookupGenericMember(other)).isNull();
  }

  private Type withExplicit(List<Type> explicit, List<String> variables, String base, List<Type> arguments) {
    return substitution.withExplicitTypeArguments(explicit, () -> substitution.methodArgument(variables, base, arguments));
  }

  private JVariableSymbol field(Type type) {
    JVariableSymbol field = new JVariableSymbol(fixtures.sema, "lib.Box#value", "value", "field");
    field.owner(fixtures.type(BOX).symbol());
    field.type(type);
    return field;
  }

  private JType box(JType argument) {
    return fixtures.type(BOX, argument);
  }

  private JMethodSymbol method(String owner, String name, String... parameters) {
    return fixtures.method(owner, name, parameters);
  }
}
