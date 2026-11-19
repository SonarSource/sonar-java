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
package org.sonar.java.model;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.semantic.Type;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class JSemaTest {

  private JSema sema;

  @BeforeEach
  void setup() {
    sema = new JSema();
    sema.ingestTypes(new JsonObject());
  }

  @Test
  void testGetClassTypeUnknown() {
    assertThat(sema.getClassType("com.google.common.collect.ImmutableMap").isUnknown()).isTrue();
    assertThat(sema.getClassType("java.lang.Object").isUnknown()).isFalse();
  }

  @Test
  void resolveType() {
    assertAll(
      () -> assertThat(sema.getClassType("void").isUnknown()).isFalse(),
      () -> assertThat(sema.getClassType("java.util.Map").isUnknown()).isTrue(),
      () -> assertThat(sema.getClassType("java.lang.Object[]").isArray()).isTrue()
    );
  }

  @Test
  void typeIsStable() {
    Type first = sema.getClassType("int");
    assertThat(first).isNotNull().isSameAs(sema.getClassType("int"));
  }

  @Test
  void projectDeclarationsAreReadInTheTypedShape() {
    JsonObject types = JsonParser.parseString("""
      {"p.Marker": {"origin": "project", "kind": "annotation", "typeParameters": 0,
        "annotations": [{"name": "java.lang.annotation.Retention",
          "values": [{"name": "value", "kind": "enum", "type": "java.lang.annotation.RetentionPolicy", "const": "RUNTIME"}]}],
        "members": []},
       "p.Box": {"origin": "project", "kind": "class", "typeParameters": 1, "annotations": [], "members": [
        {"id": "p.Box#run()", "throws": ["java.io.IOException", "T"],
          "annotations": [{"name": "p.Tag", "values": [
            {"name": "limit", "kind": "int", "value": 3},
            {"name": "inner", "kind": "annotation", "value": {"name": "p.Tag", "values": []}},
            {"name": "items", "kind": "array", "value": [{"name": "value", "kind": "string", "value": "a"}]},
            {"name": "folded", "kind": "expression", "text": "A + B"}]}],
          "facts": {"kind": "method", "props": {"thrown_types": ["java.io.IOException,T"]}}},
        {"id": "p.Box#plain()", "throws": [], "annotations": []}]}}
      """).getAsJsonObject();
    sema.ingestTypes(types);

    // A type carries its own annotation values.
    List<JSema.DeclaredAnnotation> retention = sema.declaredAnnotations("p.Marker");
    assertThat(retention).hasSize(1);
    assertThat(retention.get(0).name()).isEqualTo("java.lang.annotation.Retention");
    assertThat(retention.get(0).valuesKnown()).isTrue();
    assertThat(((Symbol.VariableSymbol) retention.get(0).values().get(0).value()).name()).isEqualTo("RUNTIME");

    // A member carries its annotation values of every element kind.
    List<SymbolMetadata.AnnotationValue> values = sema.declaredAnnotations("p.Box#run()").get(0).values();
    assertThat(values).extracting(SymbolMetadata.AnnotationValue::name).containsExactly("limit", "inner", "items", "folded");
    assertThat(values.get(0).value()).isEqualTo(3);
    assertThat(values.get(1).value()).isEqualTo("p.Tag");
    assertThat((Object[]) values.get(2).value()).containsExactly("a");
    assertThat(values.get(3).value()).isNull();
    assertThat(sema.declaredAnnotations("p.Box#plain()")).isEmpty();

    // A member carries its declared exceptions, a served empty list included; an unserved member has none.
    assertThat(sema.projectThrown("p.Box#run()")).hasSize(2);
    assertThat(sema.projectThrown("p.Box#plain()")).isEmpty();
    assertThat(sema.projectThrown("p.Box#other()")).isNull();
  }

  @Test
  void libraryMemberKeepsTheTypeArgumentsOfItsDeclaredReturn() {
    sema.ingestTypes(JsonParser.parseString("""
      {"java.lang.String": {"origin": "library", "kind": "class", "typeParameters": 0, "annotations": [], "members": []},
       "java.util.List": {"origin": "library", "kind": "interface", "typeParameters": 1, "annotations": [], "members": []},
       "p.Lib": {"origin": "library", "kind": "class", "typeParameters": 0, "annotations": [], "members": [
        {"name": "names", "desc": "()Ljava/util/List;",
          "retType": {"id": "java.util.List", "args": [{"id": "java.lang.String"}]}},
        {"name": "wild", "desc": "()Ljava/util/List;",
          "retType": {"id": "java.util.List", "args": [{"kind": "wildcard"}]}}]}}
      """).getAsJsonObject());
    JTypeSymbol owner = (JTypeSymbol) sema.getClassType("p.Lib").symbol();
    JMethodSymbol names = (JMethodSymbol) owner.memberSymbols().stream().filter(member -> "names".equals(member.name())).findFirst().orElseThrow();
    JMethodSymbol wild = (JMethodSymbol) owner.memberSymbols().stream().filter(member -> "wild".equals(member.name())).findFirst().orElseThrow();

    // A fully spelled parameterized return is the member's own return type, so a call view is the member itself.
    assertThat(names.returnType().type().isParameterized()).isTrue();
    Type returned = JType.named(sema, "java.util.List", List.of(JType.named(sema, "java.lang.String", List.of())));
    assertThat(sema.invocationView(names, null, List.of(), returned)).isSameAs(names);

    // A wildcard argument keeps the erased return.
    assertThat(wild.returnType().type().isParameterized()).isFalse();
  }

  @Test
  void implicitEnumMethodsGetTheirReturnTypeFromTheEnum() {
    sema.ingestDocument(JsonParser.parseString("""
      {"symbols": [
        {"id": "p.Cmd", "kind": "type", "name": "Cmd"},
        {"id": "p.Cmd#valueOf(java.lang.String)", "kind": "method", "name": "valueOf", "owner": "p.Cmd", "facts": {"implicit": true}},
        {"id": "p.Cmd#values()", "kind": "method", "name": "values", "owner": "p.Cmd", "facts": {"implicit": true}},
        {"id": "p.Cmd#other()", "kind": "method", "name": "other", "owner": "p.Cmd", "facts": {"implicit": true}}]}
      """).getAsJsonObject());

    JMethodSymbol valueOf = (JMethodSymbol) sema.resolve("p.Cmd#valueOf(java.lang.String)");
    JMethodSymbol values = (JMethodSymbol) sema.resolve("p.Cmd#values()");
    JMethodSymbol other = (JMethodSymbol) sema.resolve("p.Cmd#other()");

    // valueOf returns the enum, values its array, both public and static.
    assertThat(valueOf.returnType().type().fullyQualifiedName()).isEqualTo("p.Cmd");
    assertThat(values.returnType().type().isArray()).isTrue();
    assertThat(((Type.ArrayType) values.returnType().type()).elementType().fullyQualifiedName()).isEqualTo("p.Cmd");
    assertThat(valueOf.isPublic()).isTrue();
    assertThat(valueOf.isStatic()).isTrue();
    assertThat(values.isStatic()).isTrue();

    // Any other implicit member keeps its unknown return type.
    assertThat(other.returnType().type().isUnknown()).isTrue();
  }

}
