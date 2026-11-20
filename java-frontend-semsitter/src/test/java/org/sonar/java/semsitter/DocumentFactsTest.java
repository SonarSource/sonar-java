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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.sonarsource.semsitter.wire.Fact;
import org.sonarsource.semsitter.wire.FactValue;
import org.sonarsource.semsitter.wire.Place;
import org.sonarsource.semsitter.wire.Prop;
import org.sonarsource.semsitter.wire.StringList;
import org.sonarsource.semsitter.wire.Symbol;
import org.sonarsource.semsitter.wire.TypeRef;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentFactsTest {

  @Test
  void symbol_keeps_identity_place_and_facts() {
    Symbol symbol = Symbol.newBuilder()
      .setId("p.A#f")
      .setKind("field")
      .setName("f")
      .setOwner("p.A")
      .setPlace(Place.newBuilder().setStartByte(10).setEndByte(11).setLine(2).setColumn(4))
      .putFacts("static", FactValue.newBuilder().setFlag(true).build())
      .build();

    assertThat(DocumentFacts.symbol(symbol)).isEqualTo(json("""
      {"id":"p.A#f","kind":"field","name":"f","owner":"p.A","start":10,"end":11,"line":2,"col":4,"facts":{"static":true}}"""));
  }

  @Test
  void symbol_without_owner_place_or_facts_has_only_its_identity() {
    Symbol symbol = Symbol.newBuilder().setId("p").setKind("package").setName("p").build();

    assertThat(DocumentFacts.symbol(symbol)).isEqualTo(json("""
      {"id":"p","kind":"package","name":"p"}"""));
  }

  @Test
  void place_starting_at_zero_is_kept() {
    Symbol symbol = Symbol.newBuilder().setId("A").setKind("type").setName("A").setPlace(Place.newBuilder()).build();

    JsonObject object = DocumentFacts.symbol(symbol);

    assertThat(object.get("start").getAsInt()).isZero();
    assertThat(object.get("line").getAsInt()).isZero();
  }

  @Test
  void fact_values_are_read_by_their_case() {
    FactValue ids = FactValue.newBuilder().setIds(StringList.newBuilder().addValues("a").addValues("b")).build();
    FactValue text = FactValue.newBuilder().setText("public").build();
    FactValue flag = FactValue.newBuilder().setFlag(false).build();

    assertThat(DocumentFacts.value(ids)).isEqualTo(json("[\"a\",\"b\"]"));
    assertThat(DocumentFacts.value(text)).isEqualTo(json("\"public\""));
    assertThat(DocumentFacts.value(flag)).isEqualTo(json("false"));
    assertThat(DocumentFacts.value(FactValue.getDefaultInstance())).isNull();
  }

  @Test
  void facts_with_no_value_are_left_out() {
    JsonObject object = DocumentFacts.values(Map.of(
      "empty", FactValue.getDefaultInstance(),
      "kept", FactValue.newBuilder().setText("x").build()));

    assertThat(object).isEqualTo(json("{\"kept\":\"x\"}"));
  }

  @Test
  void type_names_its_parts_with_the_keys_of_the_types_answer() {
    TypeRef type = TypeRef.newBuilder()
      .setKind("class")
      .setId("java.util.List")
      .setOrigin("classpath")
      .addArguments(TypeRef.newBuilder().setKind("class").setId("java.lang.String"))
      .setElement(TypeRef.newBuilder().setKind("primitive").setId("int"))
      .addMembers(TypeRef.newBuilder().setKind("class").setId("java.io.Serializable"))
      .setBound("extends")
      .setMutable(true)
      .addNames("T")
      .setValue("1")
      .setSource("declared")
      .setVia("inference")
      .setReason("raw")
      .build();

    assertThat(DocumentFacts.type(type)).isEqualTo(json("""
      {"kind":"class","id":"java.util.List","origin":"classpath",
       "args":[{"kind":"class","id":"java.lang.String"}],
       "elem":{"kind":"primitive","id":"int"},
       "members":[{"kind":"class","id":"java.io.Serializable"}],
       "bound":"extends","mutable":true,"names":["T"],"value":"1","source":"declared","via":"inference","reason":"raw"}"""));
  }

  @Test
  void type_leaves_out_what_is_not_set() {
    TypeRef type = TypeRef.newBuilder().setKind("class").setId("p.A").build();

    assertThat(DocumentFacts.type(type)).isEqualTo(json("{\"kind\":\"class\",\"id\":\"p.A\"}"));
  }

  @Test
  void type_fact_value_is_read_as_a_type_object() {
    FactValue value = FactValue.newBuilder().setType(TypeRef.newBuilder().setKind("class").setId("p.A")).build();

    assertThat(DocumentFacts.value(value)).isEqualTo(json("{\"kind\":\"class\",\"id\":\"p.A\"}"));
  }

  @Test
  void graph_fact_reads_props_and_edges() {
    Fact fact = Fact.newBuilder()
      .setKind("class")
      .setTier("project")
      .setParent("p")
      .addProps(Prop.newBuilder().setName("modifiers").addValues("public").addValues("final"))
      .addExtends("p.B")
      .addImplements("p.I")
      .addPermits("p.C")
      .addReturns("int")
      .addType("p.T")
      .addParams("int")
      .addOverrides("p.B#m()")
      .addAnnotations("java.lang.Deprecated")
      .build();

    JsonObject object = DocumentFacts.fact(fact);

    assertThat(object).isEqualTo(json("""
      {"kind":"class","tier":"project","parent":"p","props":{"modifiers":["public","final"]},
       "extends":["p.B"],"implements":["p.I"],"permits":["p.C"],"returns":["int"],"type":["p.T"],
       "params":["int"],"overrides":["p.B#m()"],"annotations":["java.lang.Deprecated"]}"""));
  }

  @Test
  void graph_fact_without_edges_is_only_its_scalars() {
    Fact fact = Fact.newBuilder().setKind("method").build();

    JsonObject object = DocumentFacts.fact(fact);

    assertThat(object).isEqualTo(json("{\"kind\":\"method\"}"));
    assertThat(object.get("extends")).isNull();
  }

  @Test
  void empty_prop_is_an_empty_array() {
    Fact fact = Fact.newBuilder().addProps(Prop.newBuilder().setName("modifiers")).build();

    JsonArray modifiers = DocumentFacts.fact(fact).getAsJsonObject("props").getAsJsonArray("modifiers");

    assertThat(modifiers).isEmpty();
  }

  private static JsonElement json(String text) {
    return JsonParser.parseString(text);
  }
}
