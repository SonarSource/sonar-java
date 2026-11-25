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
import org.junit.jupiter.api.Test;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonarsource.semsitter.wire.Document;
import org.sonarsource.semsitter.wire.Fact;
import org.sonarsource.semsitter.wire.FactEntry;
import org.sonarsource.semsitter.wire.FactValue;
import org.sonarsource.semsitter.wire.Prop;
import org.sonarsource.semsitter.wire.TypeRef;

import static org.assertj.core.api.Assertions.assertThat;

class JSemaTest {

  @Test
  void graph_facts_give_kind_supertypes_modifiers_and_annotations() {
    JSema sema = sema(Document.newBuilder()
      .addFacts(fact("p.A", Fact.newBuilder().setKind("class").setParent("p::<module>")
        .addProps(Prop.newBuilder().setName("modifiers").addValues("public,final"))
        .addProps(Prop.newBuilder().setName("modifiers").addValues("abstract"))
        .addAnnotations("java.lang.Deprecated").addExtends("java.lang.Object").addImplements("java.io.Serializable")))
      .addSymbols(symbol("p.A", "type", "A", ""))
      .build());

    Symbol.TypeSymbol type = (Symbol.TypeSymbol) sema.resolve("p.A");
    assertThat(type.isAbstract()).isTrue();
    assertThat(type.isFinal()).isFalse();
    assertThat(type.isDeprecated()).isTrue();
    assertThat(type.superClass().fullyQualifiedName()).isEqualTo("java.lang.Object");
    assertThat(type.interfaces()).extracting(Type::fullyQualifiedName).containsExactly("java.io.Serializable");
  }

  @Test
  void declared_types_of_symbols_are_read_from_type_refs() {
    JSema sema = sema(Document.newBuilder()
      .addSymbols(symbol("p.A", "type", "A", ""))
      .addSymbols(symbol("p.A<T>", "type", "T", "p.A"))
      .addSymbols(field("p.A#list", TypeRef.newBuilder().setKind("class").setId("java.util.List").addArguments(TypeRef.newBuilder().setKind("wildcard"))))
      .addSymbols(field("p.A#array", TypeRef.newBuilder().setKind("array").setElement(TypeRef.newBuilder().setKind("primitive").setId("int"))))
      .addSymbols(field("p.A#variable", TypeRef.newBuilder().setKind("typevar").setId("p.A<T>")))
      .addSymbols(field("p.A#unknown", TypeRef.newBuilder().setKind("unknown")))
      .build());

    assertThat(sema.referencedTypeNames()).contains("java.util.List", "p.A");
    assertThat(sema.resolve("p.A#list").type().fullyQualifiedName()).isEqualTo("java.util.List");
    assertThat(sema.resolve("p.A#array").type().fullyQualifiedName()).isEqualTo("int[]");
    assertThat(sema.resolve("p.A#variable").type().name()).isEqualTo("T");
    assertThat(sema.resolve("p.A#unknown").type().isUnknown()).isTrue();
  }

  @Test
  void only_an_implicit_enum_method_gets_its_return_type_from_the_owner() {
    JSema sema = sema(Document.newBuilder()
      .addSymbols(symbol("p.E", "type", "E", ""))
      .addSymbols(symbol("p.E#values()", "method", "values", "p.E").putFacts("implicit", FactValue.newBuilder().setFlag(true).build()))
      .addSymbols(symbol("p.E#valueOf(java.lang.String)", "method", "valueOf", "p.E").putFacts("implicit", FactValue.getDefaultInstance()))
      .build());

    Symbol.MethodSymbol values = (Symbol.MethodSymbol) sema.resolve("p.E#values()");
    assertThat(values.returnType().type().fullyQualifiedName()).isEqualTo("p.E[]");
    assertThat(values.isStatic()).isTrue();
    assertThat(((Symbol.MethodSymbol) sema.resolve("p.E#valueOf(java.lang.String)")).returnType().type().isUnknown()).isTrue();
  }

  @Test
  void member_facts_of_a_types_answer_read_like_document_facts() {
    JSema sema = sema(Document.getDefaultInstance());
    JsonObject answer = JsonParser.parseString("""
      {"q.B": {"origin":"project","kind":"class","members":[
        {"id":"q.B#k(int)","facts":{"kind":"method","returns":"int","params":["int"],"props":{"modifiers":"static","param_types":["int..."]}}},
        {"id":"q.B#c","facts":{"kind":"field","type":["long"],"props":{"modifiers":["private,final"]}}},
        {"id":"q.B#d","facts":{"kind":"field","type":"java.lang.String","annotations":["java.lang.Deprecated"]}}
      ]}}
      """).getAsJsonObject();
    sema.loadTypes(names -> answer);

    Symbol.MethodSymbol method = (Symbol.MethodSymbol) sema.resolve("q.B#k(int)");
    assertThat(method.isStatic()).isTrue();
    assertThat(method.isVarArgsMethod()).isTrue();
    assertThat(method.returnType().type().fullyQualifiedName()).isEqualTo("int");
    Symbol constant = sema.resolve("q.B#c");
    assertThat(constant.isPrivate()).isTrue();
    assertThat(constant.isFinal()).isTrue();
    assertThat(constant.type().fullyQualifiedName()).isEqualTo("long");
    Symbol deprecated = sema.resolve("q.B#d");
    assertThat(deprecated.isDeprecated()).isTrue();
    assertThat(deprecated.type().fullyQualifiedName()).isEqualTo("java.lang.String");
  }

  private static JSema sema(Document document) {
    JSema sema = new JSema();
    sema.ingestDocument(document);
    return sema;
  }

  private static FactEntry fact(String key, Fact.Builder fact) {
    return FactEntry.newBuilder().setKey(key).setValue(fact).build();
  }

  private static org.sonarsource.semsitter.wire.Symbol.Builder symbol(String id, String kind, String name, String owner) {
    return org.sonarsource.semsitter.wire.Symbol.newBuilder().setId(id).setKind(kind).setName(name).setOwner(owner);
  }

  private static org.sonarsource.semsitter.wire.Symbol.Builder field(String id, TypeRef.Builder type) {
    return symbol(id, "field", id.substring(id.indexOf('#') + 1), "p.A").putFacts("type", FactValue.newBuilder().setType(type).build());
  }
}
