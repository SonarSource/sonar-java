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

import static org.assertj.core.api.Assertions.assertThat;

class LibraryMembersTest {

  private final JSema sema = new JSema();
  private final LibraryMembers members = sema.libraryMembers();

  @Test
  void thrown_types_are_only_known_for_answered_project_members() {
    served("""
      {"q.B": {"origin": "project", "kind": "class", "members": [
        {"id": null}, {"name": "noId"},
        {"id": "q.B#t()", "throws": ["java.io.IOException"], "varargs": "yes", "facts": {"kind": "method"}},
        {"id": "q.B#g", "declaredType": {"kind": "typevar", "id": "q.B<T>"}, "facts": {"kind": "field", "type": ["T"]}}
      ]},
      "q.L": {"origin": "library", "kind": "class", "members": [{"id": "q.L#t()", "throws": ["java.lang.Exception"]}]},
      "q.N": {"origin": "project", "kind": "class"}}
      """);

    assertThat(members.projectThrown(null)).isNull();
    assertThat(members.projectThrown("q.B#none()")).isNull();
    assertThat(members.projectThrown("q.L#t()")).isNull();
    assertThat(members.projectThrown("q.B#t()")).extracting(Type::fullyQualifiedName).containsExactly("java.io.IOException");
    assertThat(sema.declaredMembers.projectVarArgs).doesNotContainKey("q.B#t()");
    assertThat(sema.declaredMembers.projectFieldTypes).containsKey("q.B#g");
  }

  @Test
  void project_members_get_their_served_constant_only_when_undeclared() {
    served("""
      {"q.C": {"origin": "project", "kind": "class", "members": [
        {"id": "q.C#K", "constant": "42", "facts": {"kind": "field", "type": ["int"]}},
        {"id": "q.C#m()", "constant": "1", "facts": {"kind": "method"}},
        {"id": "q.C#N", "facts": {"kind": "field", "type": ["int"]}}
      ]}}
      """);
    JTypeSymbol owner = sema.typeSymbol("q.C");

    assertThat(((JVariableSymbol) members.memberFromTypeInfo(owner, json("{\"id\": \"q.C#K\", \"constant\": \"42\"}"))).constantValue()).contains(42);
    assertThat(members.memberFromTypeInfo(owner, json("{\"id\": \"q.C#m()\", \"constant\": \"1\"}")).isMethodSymbol()).isTrue();
    assertThat(((JVariableSymbol) members.memberFromTypeInfo(owner, json("{\"id\": \"q.C#N\"}"))).constantValue()).isEmpty();
    assertThat(members.servedConstant((JVariableSymbol) sema.resolve("q.C#K"))).isEqualTo(42);
    assertThat(members.servedConstant((JVariableSymbol) sema.resolve("q.C#N"))).isNull();
    assertThat(members.servedConstant(new JVariableSymbol(sema, null, "x", "field"))).isNull();
    assertThat(members.servedConstant(new JVariableSymbol(sema, "local", "x", "local"))).isNull();
    assertThat(members.servedConstant(new JVariableSymbol(sema, "q.Undeclared#x", "x", "field"))).isNull();
  }

  @Test
  void served_constant_needs_a_member_list() {
    served("{\"q.E\": {\"origin\": \"project\", \"kind\": \"class\"}}");

    assertThat(members.servedConstant(new JVariableSymbol(sema, "q.E#x", "x", "field"))).isNull();
  }

  @Test
  void classpath_members_read_descriptors_flags_and_return_types() {
    sema.loadTypes(names -> new JsonObject());
    sema.ingestTypes(json("{\"lib.I\": {\"kind\": \"interface\"}}"));
    JTypeSymbol iface = sema.typeSymbol("lib.I");
    JTypeSymbol anonymous = new JTypeSymbol(sema, null, "Anon", false);

    JVariableSymbol untyped = (JVariableSymbol) members.memberFromTypeInfo(anonymous, json("{\"k\": \"f\"}"));
    assertThat(untyped.id).isEqualTo("Anon#null");
    assertThat(untyped.name()).isEmpty();
    assertThat(members.memberFromTypeInfo(anonymous, json("{\"k\": \"f\"}"))).isSameAs(untyped);

    JMethodSymbol nameless = (JMethodSymbol) members.memberFromTypeInfo(iface, json("{\"desc\": \"V\", \"vis\": \"private\", \"retParam\": null, \"default\": {}}"));
    assertThat(nameless.name()).isEmpty();
    assertThat(nameless.hasModifier("default")).isFalse();
    assertThat(nameless.parameterTypes()).isEmpty();

    JMethodSymbol defaulted = (JMethodSymbol) members.memberFromTypeInfo(iface, json("""
      {"name": "d", "desc": "()Ljava/util/List;", "varargs": [],
       "retType": {"kind": "class", "id": "java.util.List", "args": [{"kind": "wildcard"}]}}
      """));
    assertThat(defaulted.hasModifier("default")).isTrue();
    assertThat(defaulted.isVarArgsMethod()).isFalse();
    assertThat(defaulted.returnType().type().isParameterized()).isFalse();

    JMethodSymbol unknownReturn = (JMethodSymbol) members.memberFromTypeInfo(iface, json("""
      {"name": "u", "desc": "()Ljava/lang/Object;", "abstract": true,
       "retType": {"kind": "class", "id": "lib.Missing", "args": [{"kind": "class", "id": "java.lang.String"}]}}
      """));
    assertThat(unknownReturn.returnType().type().fullyQualifiedName()).isEqualTo("java.lang.Object");

    JMethodSymbol staticReturn = (JMethodSymbol) members.memberFromTypeInfo(iface, json("""
      {"name": "s", "desc": "()Ljava/util/List;", "static": true,
       "retType": {"kind": "class", "id": "java.util.List", "args": [{"kind": "class", "id": "java.lang.String"}]}}
      """));
    assertThat(staticReturn.hasModifier("default")).isFalse();
    assertThat(staticReturn.returnType().type().isParameterized()).isFalse();

    JMethodSymbol plainReturn = (JMethodSymbol) members.memberFromTypeInfo(iface, json("""
      {"name": "p", "desc": "()Ljava/util/List;", "abstract": true, "retType": {"kind": "class", "id": "java.util.List"}}
      """));
    assertThat(plainReturn.returnType().type().isParameterized()).isFalse();
  }

  @Test
  void dump_symbols_already_created_are_kept() {
    sema.symbols.put("p.A#f", new JVariableSymbol(sema, "p.A#f", "kept", "field"));
    sema.ingestDocument(Document.newBuilder()
      .addSymbols(symbol("p.A#f", "field", "f", "p.A"))
      .addSymbols(symbol("p.A#l", "label", "l", "p.A"))
      .build());

    assertThat(sema.symbols.get("p.A#f").name()).isEqualTo("kept");
    assertThat(sema.symbols).doesNotContainKey("p.A#l");
  }

  @Test
  void dump_members_without_owner_fall_back_to_their_fact_parent() {
    sema.ingestDocument(Document.newBuilder()
      .addFacts(fact("p.A#m()", Fact.newBuilder().setKind("method").setParent("p.A")))
      .addFacts(fact("p.A#f", Fact.newBuilder().setKind("field").setParent("p.Outer").setTier("IN_PROJECT").addType("long")))
      .addFacts(fact("p.A#g", Fact.newBuilder().setKind("field").addType("java.lang.String")))
      .addFacts(fact("p.A#h", Fact.newBuilder().setKind("field").addType("int").addType("long")))
      .addSymbols(symbol("p.A", "type", "A", ""))
      .addSymbols(org.sonarsource.semsitter.wire.Symbol.newBuilder().setId("p.A#m()").setKind("method").setName("m"))
      .addSymbols(symbol("p.A#f", "field", "f", "p.Missing"))
      .addSymbols(symbol("p.A#g", "field", "g", "p.A"))
      .addSymbols(symbol("p.A#h", "field", "h", "p.A"))
      .addSymbols(symbol("m", "method", "m", "p.A"))
      .build());

    assertThat(sema.resolve("p.A#m()").owner().name()).isEqualTo("A");
    assertThat(sema.resolve("p.A#f").owner().name()).isEqualTo("Outer");
    assertThat(sema.resolve("p.A#f").type().fullyQualifiedName()).isEqualTo("long");
    assertThat(sema.resolve("p.A#g").type().isUnknown()).isTrue();
    assertThat(sema.resolve("p.A#h").type().isUnknown()).isTrue();
  }

  @Test
  void implicit_enum_methods_need_an_owner_and_the_right_signature() {
    FactValue implicit = FactValue.newBuilder().setFlag(true).build();
    sema.ingestDocument(Document.newBuilder()
      .addSymbols(symbol("p.E", "type", "E", ""))
      .addSymbols(org.sonarsource.semsitter.wire.Symbol.newBuilder().setId("p.E#values()").setKind("method").setName("values").putFacts("implicit", implicit))
      .addSymbols(symbol("p.E#values(int)", "method", "values", "p.E").putFacts("implicit", implicit))
      .addSymbols(symbol("p.E#valueOf(int)", "method", "valueOf", "p.E").putFacts("implicit", implicit))
      .addSymbols(symbol("p.E#other()", "method", "other", "p.E").putFacts("implicit", implicit))
      .addSymbols(symbol("p.E#valueOf(java.lang.String)", "method", "valueOf", "p.E").putFacts("implicit", implicit))
      .build());

    assertThat(((Symbol.MethodSymbol) sema.resolve("p.E#values()")).isStatic()).isFalse();
    assertThat(((Symbol.MethodSymbol) sema.resolve("p.E#values(int)")).isStatic()).isFalse();
    assertThat(((Symbol.MethodSymbol) sema.resolve("p.E#valueOf(int)")).isStatic()).isFalse();
    assertThat(((Symbol.MethodSymbol) sema.resolve("p.E#other()")).isStatic()).isFalse();
    Symbol.MethodSymbol valueOf = (Symbol.MethodSymbol) sema.resolve("p.E#valueOf(java.lang.String)");
    assertThat(valueOf.isStatic()).isTrue();
    assertThat(valueOf.returnType().type().fullyQualifiedName()).isEqualTo("p.E");
  }

  @Test
  void library_members_named_by_the_dump_take_served_flags() {
    sema.loadTypes(names -> new JsonObject());
    sema.ingestTypes(json("""
      {"lib.L": {"kind": "class", "members": [
        {"k": "m", "name": "m", "desc": "(I)V", "static": true, "throws": ["java.io.IOException"], "varargs": true},
        {"k": "m", "name": "n", "desc": "(I)V", "varargs": "maybe"},
        {"k": "f", "name": "f", "desc": "I", "static": true, "throws": ["java.io.IOException"]},
        {"id": "lib.L#p(int)", "facts": {"kind": "method"}}
      ]}}
      """));
    sema.ingestDocument(Document.newBuilder()
      .addSymbols(symbol("lib.L#m(int)", "method", "m", "lib.L"))
      .addSymbols(symbol("lib.L#n(int)", "method", "n", "lib.L"))
      .addSymbols(symbol("lib.L#f", "field", "f", "lib.L"))
      .addSymbols(symbol("lib.L#p(int)", "method", "p", "lib.L"))
      .addSymbols(symbol("lib.L#q(1)", "method", "q", "lib.L"))
      .addSymbols(symbol("array#clone()", "method", "clone", "array"))
      .build());

    Symbol.MethodSymbol m = (Symbol.MethodSymbol) sema.resolve("lib.L#m(int)");
    assertThat(m.isStatic()).isTrue();
    assertThat(m.isVarArgsMethod()).isTrue();
    assertThat(m.thrownTypes()).extracting(Type::fullyQualifiedName).containsExactly("java.io.IOException");
    assertThat(((Symbol.MethodSymbol) sema.resolve("lib.L#n(int)")).thrownTypes()).isEmpty();
    assertThat(sema.resolve("lib.L#f").isStatic()).isTrue();
    assertThat(sema.resolve("lib.L#p(int)").isStatic()).isFalse();
    assertThat(sema.resolve("lib.L#q(1)").isStatic()).isFalse();
  }

  private void served(String answer) {
    sema.loadTypes(names -> new JsonObject());
    sema.ingestTypes(json(answer));
  }

  private static org.sonarsource.semsitter.wire.Symbol.Builder symbol(String id, String kind, String name, String owner) {
    return org.sonarsource.semsitter.wire.Symbol.newBuilder().setId(id).setKind(kind).setName(name).setOwner(owner);
  }

  private static FactEntry fact(String key, Fact.Builder fact) {
    return FactEntry.newBuilder().setKey(key).setValue(fact).build();
  }

  private static JsonObject json(String text) {
    return JsonParser.parseString(text).getAsJsonObject();
  }
}
