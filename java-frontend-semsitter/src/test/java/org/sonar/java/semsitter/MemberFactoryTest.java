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
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.TypeParameterTree;
import org.sonarsource.semsitter.wire.Document;
import org.sonarsource.semsitter.wire.Fact;
import org.sonarsource.semsitter.wire.FactEntry;
import org.sonarsource.semsitter.wire.Prop;

import static org.assertj.core.api.Assertions.assertThat;

class MemberFactoryTest {

  @TempDir
  Path baseDir;

  private final JSema sema = new JSema();
  private final MemberFactory factory = sema.memberFactory();

  @Test
  void type_variable_bounds_are_the_declared_ones_or_object() throws Exception {
    CompilationUnitTree unit = (CompilationUnitTree) Replays.parseWithEcj(baseDir, "G", "class G<T extends Number & Comparable<T>, U extends Missing, V> {}").get();
    List<TypeParameterTree> parameters = ((ClassTree) unit.types().get(0)).typeParameters();
    bounded("p.G<T>", parameters.get(0));
    bounded("p.G<U>", parameters.get(1));
    bounded("p.G<V>", parameters.get(2));

    assertThat(factory.typeVariableBound("p.G<T>").fullyQualifiedName()).isEqualTo("java.lang.Number");
    assertThat(factory.typeVariableBounds("p.G<T>")).extracting(Type::fullyQualifiedName).containsExactly("java.lang.Number", "java.lang.Comparable");
    assertThat(factory.typeVariableBound("p.G<U>").fullyQualifiedName()).isEqualTo("java.lang.Object");
    assertThat(factory.typeVariableBound("p.G<V>").fullyQualifiedName()).isEqualTo("java.lang.Object");
    assertThat(factory.typeVariableBounds("p.G<V>")).extracting(Type::fullyQualifiedName).containsExactly("java.lang.Object");
    assertThat(factory.typeVariableBound(null).fullyQualifiedName()).isEqualTo("java.lang.Object");
    assertThat(factory.typeVariableBounds(null)).extracting(Type::fullyQualifiedName).containsExactly("java.lang.Object");
    assertThat(factory.typeVariableBound("p.G#f").fullyQualifiedName()).isEqualTo("java.lang.Object");
  }

  @Test
  void type_parameters_are_named_after_their_id() {
    assertThat(sema.resolve("p.A<T>").name()).isEqualTo("T");
    assertThat(factory.typeParameter("p.A<T").name()).isEqualTo("p.A<T");
  }

  @Test
  void a_member_being_created_resolves_to_the_unknown_symbol() {
    sema.memberFactory().creating.add("p.A#m()");

    assertThat(sema.resolve("p.A#m()")).isSameAs(sema.unknownSymbol);
    assertThat(sema.resolve("array#clone()").isMethodSymbol()).isTrue();
  }

  @Test
  void local_and_anonymous_classes_are_owned_by_their_method() {
    JSema withFacts = sema(Document.newBuilder()
      .addFacts(fact("p.C#f()$Local", Fact.newBuilder().setKind("class").addProps(Prop.newBuilder().setName("modifiers").addValues("final"))))
      .build());

    Symbol.TypeSymbol local = (Symbol.TypeSymbol) withFacts.resolve("p.C#f()$Local");
    Symbol.TypeSymbol anonymous = (Symbol.TypeSymbol) withFacts.resolve("p.C#f()$Local$1");

    assertThat(local.name()).isEqualTo("Local");
    assertThat(local.isFinal()).isTrue();
    assertThat(local.owner().name()).isEqualTo("f");
    assertThat(anonymous.name()).isEmpty();
    assertThat(anonymous.owner()).isSameAs(local);
  }

  @Test
  void enum_constants_without_a_type_fact_are_of_their_enum() {
    JSema withFacts = sema(Document.newBuilder()
      .addFacts(fact("p.E", Fact.newBuilder().setKind("enum").setParent("p::<module>")))
      .addFacts(fact("p.E#A", Fact.newBuilder().setKind("enum_member")))
      .addFacts(fact("p.E#B", Fact.newBuilder().setKind("enum")))
      .addSymbols(org.sonarsource.semsitter.wire.Symbol.newBuilder().setId("p.E").setKind("type").setName("E"))
      .build());

    Symbol constant = withFacts.resolve("p.E#A");
    Symbol other = withFacts.resolve("p.E#B");

    assertThat(constant.type().fullyQualifiedName()).isEqualTo("p.E");
    assertThat(constant.isEnum()).isTrue();
    assertThat(constant.isStatic()).isTrue();
    assertThat(constant.isPublic()).isTrue();
    assertThat(other.isFinal()).isTrue();
    assertThat(withFacts.resolve("p.E#none").isUnknown()).isTrue();
  }

  @Test
  void project_members_take_served_declared_types_aliases_and_variable_arity() {
    sema.loadTypes(names -> new JsonObject());
    sema.ingestTypes(json("""
      {"q.B": {"origin": "project", "kind": "class", "members": [
        {"id": "q.B#list", "facts": {"kind": "field", "type": ["java.util.List"]},
          "declaredType": {"kind": "class", "id": "java.util.List", "args": [{"kind": "class", "id": "java.lang.String"}]}},
        {"id": "q.B#old", "useIds": ["q.B#alias"], "facts": {"kind": "field", "type": ["int"]}, "annotations": [{"name": "java.lang.Deprecated"}]},
        {"id": "q.B#v(int[])", "useIds": ["q.B#w(int[])"], "varargs": false, "facts": {"kind": "method", "params": ["int[]"]}},
        {"id": "q.B#p(int,int)", "useIds": ["q.B#r(2)", "q.B#s(long)"], "facts": {"kind": "method", "params": ["int", "int"],
          "props": {"param_types": ["int", "int..."]}}}
      ]}}
      """));

    assertThat(sema.resolve("q.B#list").type().typeArguments()).extracting(Type::fullyQualifiedName).containsExactly("java.lang.String");
    assertThat(sema.resolve("q.B#alias").isDeprecated()).isTrue();
    assertThat(sema.resolve("q.B#alias").type().fullyQualifiedName()).isEqualTo("int");
    assertThat(((Symbol.MethodSymbol) sema.resolve("q.B#w(int[])")).isVarArgsMethod()).isFalse();
    Symbol.MethodSymbol arity = (Symbol.MethodSymbol) sema.resolve("q.B#r(2)");
    assertThat(arity.isUnknown()).isTrue();
    assertThat(arity.parameterTypes()).hasSize(2);
    Symbol.MethodSymbol mismatched = (Symbol.MethodSymbol) sema.resolve("q.B#s(long)");
    assertThat(mismatched.parameterTypes()).extracting(Type::fullyQualifiedName).containsExactly("long");
    assertThat(mismatched.isVarArgsMethod()).isTrue();
  }

  @Test
  void type_facts_give_kinds_supertypes_and_implicit_modifiers() {
    JSema withFacts = sema(Document.newBuilder()
      .addFacts(fact("p.O", Fact.newBuilder().setKind("class").setParent("p::<module>")))
      .addFacts(fact("p.O$I", Fact.newBuilder().setKind("interface").setParent("p.O").addImplements("p.J")))
      .addFacts(fact("p.O$M", Fact.newBuilder().setKind("method").setParent("p.O")))
      .addFacts(fact("p.O$M$R", Fact.newBuilder().setKind("record").setParent("p.O$M")))
      .addFacts(fact("p.O$N", Fact.newBuilder().setKind("enum").setParent("p.Nowhere")))
      .addFacts(fact("p.X", Fact.newBuilder().setKind("class").addExtends("java.lang.Enum`1")))
      .addSymbols(type("p.O")).addSymbols(type("p.O$I")).addSymbols(type("p.O$M$R")).addSymbols(type("p.O$N")).addSymbols(type("p.X"))
      .build());

    Symbol.TypeSymbol nestedInterface = (Symbol.TypeSymbol) withFacts.resolve("p.O$I");
    assertThat(nestedInterface.isStatic()).isTrue();
    assertThat(nestedInterface.interfaces()).extracting(Type::fullyQualifiedName).containsExactly("p.J");
    assertThat(((JTypeSymbol) withFacts.resolve("p.O$M$R")).hasModifier("static")).isFalse();
    assertThat(((JTypeSymbol) withFacts.resolve("p.O$M$R")).hasModifier("final")).isTrue();
    assertThat(((JTypeSymbol) withFacts.resolve("p.O$N")).hasModifier("static")).isFalse();
    assertThat(withFacts.resolve("p.X").isEnum()).isTrue();
  }

  @Test
  void type_information_reads_interface_supertypes_and_type_parameter_counts() {
    JTypeSymbol anonymous = new JTypeSymbol(sema, null, "Anonymous", false);
    factory.applyTypeInfo(anonymous, json("{\"kind\": \"interface\", \"super\": [\"java.lang.Runnable\"], \"typeParameters\": null}"));
    JTypeSymbol generic = sema.typeSymbol("p.Gen");
    factory.applyTypeInfo(generic, json("{\"kind\": \"class\", \"super\": \"java.lang.Enum\", \"typeParameters\": 2}"));

    assertThat(anonymous.isInterface()).isTrue();
    assertThat(anonymous.interfaces()).extracting(Type::fullyQualifiedName).containsExactly("java.lang.Runnable");
    assertThat(anonymous.typeParameterCount()).isZero();
    assertThat(generic.isEnum()).isTrue();
    assertThat(generic.typeParameterCount()).isEqualTo(2);
  }

  @Test
  void recovered_types_are_unknown_unless_declared() {
    sema.loadTypes(names -> new JsonObject());
    sema.ingestTypes(json("{\"p.Known\": {\"kind\": \"class\"}}"));

    assertThat(factory.recoveredType("p.Known").isUnknown()).isFalse();
    assertThat(factory.recoveredType("p.Unknown").isUnknown()).isTrue();
    assertThat(new JSema().memberFactory().recoveredType("p.Fresh").isUnknown()).isTrue();
  }

  private void bounded(String id, TypeParameterTree tree) {
    ((JTypeSymbol) factory.typeParameter(id)).boundTrees(tree.bounds());
  }

  private static JSema sema(Document document) {
    JSema sema = new JSema();
    sema.ingestDocument(document);
    return sema;
  }

  private static org.sonarsource.semsitter.wire.Symbol.Builder type(String id) {
    return org.sonarsource.semsitter.wire.Symbol.newBuilder().setId(id).setKind("type").setName(SemaFacts.simpleName(id));
  }

  private static FactEntry fact(String key, Fact.Builder fact) {
    return FactEntry.newBuilder().setKey(key).setValue(fact).build();
  }

  private static JsonObject json(String text) {
    return JsonParser.parseString(text).getAsJsonObject();
  }
}
