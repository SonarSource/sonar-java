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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonarsource.semsitter.wire.Document;
import org.sonarsource.semsitter.wire.TypeRef;

import static org.assertj.core.api.Assertions.assertThat;

class FactTypesTest {

  private static final String TYPES = """
    {
      "p.Bounded": {"kind": "class", "typeVarBounds": [[{"kind": "class", "id": "java.lang.Number"}], [{"kind": "class", "id": "java.lang.Object"}],
        [], ["text"], "flat", [{"kind": "unknown"}]]},
      "p.Plain": {"kind": "class"},
      "java.lang.Number": {"kind": "class"}
    }
    """;

  private final JSema sema = new JSema();
  private final FactTypes types = sema.factTypes();

  @Test
  void facts_without_a_class_id_are_unknown() {
    assertThat(types.typeFromFact(json("{\"kind\": \"unknown\", \"id\": \"p.A\"}")).isUnknown()).isTrue();
    assertThat(types.typeFromFact(json("{\"kind\": \"class\"}")).isUnknown()).isTrue();
    assertThat(types.typeFromFact(json("{\"kind\": \"array\"}")).isUnknown()).isTrue();
    assertThat(types.typeFromFact(json("{\"kind\": \"primitive\", \"id\": \"int\"}")).fullyQualifiedName()).isEqualTo("int");
  }

  @Test
  void type_arguments_that_are_no_facts_are_skipped() {
    JType type = types.typeFromFact(json("{\"kind\": \"class\", \"id\": \"java.util.Map\", \"args\": [\"K\", {\"kind\": \"class\", \"id\": \"java.lang.String\"}]}"));

    assertThat(type.typeArguments()).extracting(Type::fullyQualifiedName).containsExactly("java.lang.String");
  }

  @Test
  void type_variables_are_substituted_or_named_after_their_id() {
    Type string = sema.getClassType("java.lang.String");

    assertThat(types.typeFromFact(json("{\"kind\": \"typevar\", \"id\": \"p.A<T>\"}"), Map.of("p.A<T>", string))).isSameAs(string);
    assertThat(types.typeFromFact(json("{\"kind\": \"typevar\", \"id\": \"p.A<T>\"}")).name()).isEqualTo("T");
    assertThat(types.typeFromFact(json("{\"kind\": \"typevar\", \"id\": \"T\"}")).name()).isEqualTo("T");
    assertThat(types.typeFromFact(json("{\"kind\": \"typevar\", \"id\": \"p.A<T>x\"}")).name()).isEqualTo("A<T>x");
    assertThat(types.typeFromFact(json("{\"kind\": \"typevar\"}"), new HashMap<>()).name()).isEmpty();
  }

  @Test
  void wildcards_are_bounded_or_captured() {
    JType lower = types.typeFromFact(json("{\"kind\": \"wildcard\", \"super\": {\"kind\": \"class\", \"id\": \"java.lang.Integer\"}}"));
    JType captured = types.typeFromFact(json("{\"kind\": \"wildcard\", \"extends\": {\"kind\": \"unknown\"}}"), Map.of(), true);

    assertThat(lower.isWildcard()).isTrue();
    assertThat(captured.isCapture()).isTrue();
    assertThat(captured.element.fullyQualifiedName()).isEqualTo("java.lang.Object");
  }

  @Test
  void captured_unbounded_wildcards_take_the_declared_class_bound() {
    sema.ingestTypes(json(TYPES));
    String wildcard = "{\"kind\": \"wildcard\"}";
    JType bounded = types.typeFromFact(json("{\"kind\": \"class\", \"id\": \"p.Bounded\", \"args\": [" + String.join(",", List.of(wildcard, wildcard, wildcard, wildcard,
      wildcard, wildcard, wildcard)) + "]}"), Map.of(), true);
    JType plain = types.typeFromFact(json("{\"kind\": \"class\", \"id\": \"p.Plain\", \"args\": [" + wildcard + "]}"), Map.of(), true);
    JType undeclared = types.typeFromFact(json("{\"kind\": \"class\", \"id\": \"p.Undeclared\", \"args\": [" + wildcard + "]}"), Map.of(), true);
    JType typed = types.typeFromFact(json("{\"kind\": \"class\", \"id\": \"p.Bounded\", \"args\": [{\"kind\": \"class\", \"id\": \"java.lang.Long\"}]}"), Map.of(), true);

    assertThat(bounded.typeArguments()).extracting(argument -> ((JType) argument).element.fullyQualifiedName())
      .containsExactly("java.lang.Number", "java.lang.Object", "java.lang.Object", "java.lang.Object", "java.lang.Object", "java.lang.Object", "java.lang.Object");
    assertThat(((JType) plain.typeArguments().get(0)).element.fullyQualifiedName()).isEqualTo("java.lang.Object");
    assertThat(((JType) undeclared.typeArguments().get(0)).element.fullyQualifiedName()).isEqualTo("java.lang.Object");
    assertThat(typed.typeArguments()).extracting(Type::fullyQualifiedName).containsExactly("java.lang.Long");
  }

  @Test
  void document_type_refs_build_arrays_wildcards_variables_and_parameterized_types() {
    TypeRef list = TypeRef.newBuilder().setKind("class").setId("java.util.List").addArguments(TypeRef.newBuilder().setKind("wildcard")).build();

    assertThat(types.typeFromRef(TypeRef.newBuilder().setKind("array").setElement(list).build()).isArray()).isTrue();
    assertThat(types.typeFromRef(TypeRef.newBuilder().setKind("array").build()).isUnknown()).isTrue();
    assertThat(types.typeFromRef(TypeRef.newBuilder().setKind("class").build()).isUnknown()).isTrue();
    assertThat(types.typeFromRef(TypeRef.newBuilder().setKind("typevar").setId("p.A<T>").build()).name()).isEqualTo("T");
    assertThat(types.typeFromRef(list).typeArguments()).hasSize(1);
  }

  @Test
  void supertype_facts_keep_their_type_arguments() {
    Type raw = types.supertypeFromFact(json("{\"id\": \"java.util.List\", \"args\": []}"));
    Type parameterized = types.supertypeFromFact(json("{\"id\": \"java.util.List\", \"args\": [1, {\"kind\": \"class\", \"id\": \"java.lang.String\"}]}"));
    Type unknown = types.supertypeFromFact(json("{\"id\": \"\", \"args\": [{\"kind\": \"class\", \"id\": \"java.lang.String\"}]}"));

    assertThat(raw.isParameterized()).isFalse();
    assertThat(parameterized.typeArguments()).extracting(Type::fullyQualifiedName).containsExactly("java.lang.String");
    assertThat(unknown).isSameAs(Type.UNKNOWN);
    assertThat(FactTypes.objects(json("{\"a\": [1, {}], \"b\": {}}"), "a")).hasSize(1);
    assertThat(FactTypes.objects(json("{\"b\": {}}"), "b")).isEmpty();
    assertThat(FactTypes.objects(json("{}"), "a")).isEmpty();
  }

  @Test
  void descriptors_give_primitive_class_and_array_types() {
    List<Type> parameters = types.parameterTypesOf("(BCDFIJSZ[[Ljava/lang/String;)V");

    assertThat(parameters).extracting(Type::fullyQualifiedName)
      .containsExactly("byte", "char", "double", "float", "int", "long", "short", "boolean", "java.lang.String[][]");
    assertThat(types.readDescriptor("V", new int[] {0}).fullyQualifiedName()).isEqualTo("void");
    assertThat(types.parameterTypesOf("(I")).extracting(Type::fullyQualifiedName).containsExactly("int");
  }

  @Test
  void type_parameter_items_name_the_variables_of_classes_and_callables_by_position() {
    sema.ingestDocument(Document.newBuilder()
      .addSymbols(symbol("p.A", "type", "A"))
      .addSymbols(symbol("p.A<T>", "type", "T"))
      .addSymbols(symbol("p.A$B", "type", "B"))
      .addSymbols(symbol("p.A$B<U>", "type", "U"))
      .addSymbols(symbol("p.A$B<V<W>>", "type", "V"))
      .addSymbols(symbol("p.A$B<>", "type", ""))
      .addSymbols(symbol("p.A$B<X>#f", "field", "f"))
      .addSymbols(symbol("p.A$B<Y>", "field", "Y"))
      .addSymbols(symbol("p.G<E>", "type", "E"))
      .addSymbols(symbol("p.A#m()", "method", "m"))
      .addSymbols(symbol("p.A#m()<M>", "type", "M"))
      .build());

    assertThat(types.typeFromName("p.A::`0").name()).isEqualTo("T");
    assertThat(types.typeFromName("p.A$B::`1").name()).isEqualTo("U");
    assertThat(types.typeFromName("p.G`1::`0").name()).isEqualTo("E");
    assertThat(types.typeFromName("p.A#m()::``0").name()).isEqualTo("M");
    assertThat(types.typeFromName("p.A$B::`2").isTypeVar()).isFalse();
    assertThat(types.typeFromName("::`0").isTypeVar()).isFalse();
    assertThat(types.typeFromName("p.A::`").isTypeVar()).isFalse();
    assertThat(types.typeFromName("p.A::`x").isTypeVar()).isFalse();
    assertThat(types.typeFromName("int[]").fullyQualifiedName()).isEqualTo("int[]");
  }

  @Test
  void parameter_placeholders_are_shared_per_method_and_position_unless_annotated() {
    JMethodSymbol method = new JMethodSymbol(sema, "p.A#m(int)", "m");
    JMethodSymbol annotated = new JMethodSymbol(sema, "p.A#a(int)", "a");
    sema.declaredMembers.libraryAnnotations.put(SemaFacts.parameterKey("p.A#a(int)", 0), List.of());
    JMethodSymbol anonymous = new JMethodSymbol(sema, null, "n");
    Type type = sema.primitiveType("int");

    assertThat(types.parameterPlaceholder(method, 0, type)).isSameAs(types.parameterPlaceholder(method, 0, type));
    assertThat(types.parameterPlaceholder(annotated, 0, type)).isInstanceOf(JVariableSymbol.class).isSameAs(types.parameterPlaceholder(annotated, 0, type));
    assertThat(((Symbol.VariableSymbol) types.parameterPlaceholder(annotated, 0, type)).isParameter()).isTrue();
    assertThat(types.parameterPlaceholder(anonymous, 0, type)).isNotSameAs(types.parameterPlaceholder(anonymous, 0, type));
  }

  private static org.sonarsource.semsitter.wire.Symbol.Builder symbol(String id, String kind, String name) {
    return org.sonarsource.semsitter.wire.Symbol.newBuilder().setId(id).setKind(kind).setName(name);
  }

  private static JsonObject json(String text) {
    return JsonParser.parseString(text).getAsJsonObject();
  }
}
