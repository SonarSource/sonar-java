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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;

import static org.assertj.core.api.Assertions.assertThat;

class MemberLookupTest {

  private static final String TYPES = """
    {
      "lib.Calls": {"kind": "class", "super": "lib.Parent", "interfaces": [], "members": [
        {"k": "m", "name": "take", "desc": "(Ljava/lang/String;)V"},
        {"k": "m", "name": "two", "desc": "(I)V"},
        {"k": "m", "name": "two", "desc": "(J)V"},
        {"k": "m", "name": "spread", "desc": "([Ljava/lang/String;)V", "varargs": true},
        {"k": "m", "name": "gen", "desc": "(Ljava/util/List;)V"},
        {"k": "f", "name": "own", "desc": "I"},
        {"k": "m", "name": "nodesc"},
        {"k": "f", "name": "take", "desc": "I"}
      ]},
      "lib.Parent": {"kind": "class", "super": "lib.Ghost", "interfaces": ["lib.Statics"], "members": [
        {"k": "f", "name": "inherited", "desc": "I"},
        {"k": "m", "name": "fromParent", "desc": "()V"}
      ]},
      "lib.Statics": {"kind": "interface", "members": [{"k": "m", "name": "util", "desc": "()V", "static": true}]},
      "lib.Fn": {"kind": "interface", "interfaces": ["lib.Ghost"], "members": [
        {"k": "m", "name": "apply", "desc": "(Ljava/lang/Object;)Ljava/lang/Object;", "abstract": true},
        {"k": "m", "name": "equals", "desc": "(Ljava/lang/Object;)Z", "abstract": true}
      ]},
      "lib.Two": {"kind": "interface", "interfaces": ["lib.Fn"], "members": [{"k": "m", "name": "other", "desc": "()V", "abstract": true}]},
      "lib.Marker": {"kind": "interface"},
      "lib.Proj": {"origin": "project", "kind": "class", "members": [
        {"id": "lib.Proj#count", "facts": {"kind": "field", "type": ["int"]}},
        {"id": "lib.Proj#ghost"},
        {"id": "lib.Proj#run(int)", "facts": {"kind": "method", "params": ["int"]}}
      ]},
      "java.lang.String": {"kind": "class", "super": "java.lang.Object"},
      "java.lang.Object": {"kind": "class"}
    }
    """;

  private JSema sema;
  private MemberLookup lookup;

  @BeforeEach
  void setUp() {
    sema = new JSema();
    sema.loadPackages(name -> null);
    sema.loadTypes(names -> new JsonObject());
    sema.ingestTypes(JsonParser.parseString(TYPES).getAsJsonObject());
    lookup = sema.lookup();
  }

  @Test
  void enum_constants_are_created_once_with_their_enum_as_type() {
    Symbol.VariableSymbol constant = lookup.enumConstant("lib.E", "A");
    Symbol.VariableSymbol ownerless = lookup.enumConstant(" ", "B");

    assertThat(lookup.enumConstant("lib.E", null)).isNull();
    assertThat(lookup.enumConstant("lib.E", "A")).isSameAs(constant);
    assertThat(constant.type().fullyQualifiedName()).isEqualTo("lib.E");
    assertThat(constant.isStatic()).isTrue();
    assertThat(ownerless.owner()).isSameAs(Symbol.UNKNOWN_SYMBOL);
    assertThat(lookup.enumConstant(null, "B")).isSameAs(ownerless);
  }

  @Test
  void receivers_are_named_after_the_type_id_or_name() {
    Type string = sema.getClassType("java.lang.String");

    assertThat(lookup.receiverSymbol(new JTypeSymbol(sema, null, "Local", false), "this", string)).hasToString("Local#this");
    assertThat(lookup.receiverSymbol(Symbol.TypeSymbol.UNKNOWN_TYPE, "super", string)).hasToString("!unknown!#super");
    assertThat(lookup.receiverSymbol(sema.typeSymbol("lib.Calls"), "this", string)).isSameAs(lookup.receiverSymbol(sema.typeSymbol("lib.Calls"), "this", string));
  }

  @Test
  void declared_method_is_the_applicable_one_or_the_only_one_of_that_arity() {
    Type string = sema.getClassType("java.lang.String");
    Type integer = sema.primitiveType("int");

    assertThat(lookup.declaredMethod("lib.Calls", "take", List.of(string)).name()).isEqualTo("take");
    assertThat(lookup.declaredMethod("lib.Calls", "take", List.of(integer)).name()).isEqualTo("take");
    assertThat(lookup.declaredMethod("lib.Calls", "two", List.of(sema.primitiveType("boolean")))).isNull();
    assertThat(lookup.declaredMethod("lib.Calls", "spread", List.of(integer, integer)).name()).isEqualTo("spread");
    assertThat(lookup.declaredMethod("lib.Calls", "fromParent", List.of()).name()).isEqualTo("fromParent");
    assertThat(lookup.declaredMethod("lib.Calls", "util", List.of())).isNull();
    assertThat(lookup.declaredMethod("lib.Statics", "util", List.of())).isNotNull();
    assertThat(lookup.declaredMethod("lib.Missing", "take", List.of())).isNull();
    assertThat(lookup.declaredMethod("lib.Calls", "take", List.of(string), Set.of(0))).isNull();
  }

  @Test
  void only_functional_interfaces_take_a_function() {
    assertThat(takesNoFunction(sema.primitiveType("int"))).isTrue();
    assertThat(takesNoFunction(sema.getClassType("java.lang.String[]"))).isTrue();
    assertThat(takesNoFunction(sema.getClassType("java.lang.String"))).isTrue();
    assertThat(takesNoFunction(sema.getClassType("lib.Marker"))).isTrue();
    assertThat(takesNoFunction(sema.getClassType("lib.Fn"))).isFalse();
    assertThat(takesNoFunction(sema.getClassType("lib.Nowhere"))).isFalse();
    assertThat(lookup.takesNoFunction(new JMethodSymbol(sema, "lib.X#m()", "m"), Set.of(0))).isFalse();
  }

  @Test
  void functional_method_is_the_single_abstract_method_but_object_ones() {
    assertThat(lookup.functionalMethod(sema.getClassType("lib.Fn")).name()).isEqualTo("apply");
    assertThat(lookup.functionalMethod(sema.getClassType("lib.Two"))).isNull();
    assertThat(lookup.functionalMethod(sema.getClassType("lib.Marker"))).isNull();
    assertThat(lookup.functionalMethod(sema.getClassType("lib.Calls"))).isNull();
    assertThat(lookup.functionalMethod(sema.primitiveType("int"))).isNull();
  }

  @Test
  void arity_matches_variable_arity_methods_with_fewer_arguments() {
    JMethodSymbol varargs = new JMethodSymbol(sema, "lib.X#v(int[])", "v");
    varargs.parameterTypes(List.of(sema.getClassType("int[]")));
    varargs.varArgs(true);

    assertThat(MemberLookup.arityMatches(varargs, 0)).isTrue();
    assertThat(MemberLookup.arityMatches(varargs, 3)).isTrue();
    assertThat(MemberLookup.arityMatches(new JMethodSymbol(sema, "lib.X#n()", "n"), 1)).isFalse();
  }

  @Test
  void declared_fields_are_found_in_the_type_or_its_supertypes() {
    assertThat(lookup.declaredField("lib.Calls", "own").type().fullyQualifiedName()).isEqualTo("int");
    assertThat(lookup.declaredField("lib.Calls", "inherited").owner().name()).isEqualTo("Parent");
    assertThat(lookup.declaredField("lib.Calls", "nothing")).isNull();
    assertThat(lookup.declaredField("lib.Proj", "count").type().fullyQualifiedName()).isEqualTo("int");
    assertThat(lookup.declaredField("lib.Proj", "ghost")).isNull();
    assertThat(lookup.declaredField("lib.Proj", "run")).isNull();
    assertThat(lookup.declaredField("lib.Missing", "own")).isNull();
  }

  @Test
  void members_match_by_kind_descriptor_and_erased_parameter_types() {
    assertThat(find("lib.Calls", "lib.Calls#take", MemberLookup.Match.ARITY).get("k").getAsString()).isEqualTo("f");
    assertThat(find("lib.Calls", "lib.Calls#nodesc()", MemberLookup.Match.ARITY)).isNull();
    assertThat(find("lib.Calls", "lib.Calls#spread(java.lang.String,java.lang.String)", MemberLookup.Match.TYPED)).isNull();
    assertThat(find("lib.Calls", "lib.Calls#spread(java.lang.String,java.lang.String)", MemberLookup.Match.VARARGS)).isNotNull();
    assertThat(find("lib.Calls", "lib.Calls#take()", MemberLookup.Match.VARARGS)).isNull();
    assertThat(find("lib.Calls", "lib.Calls#gen(java.util.List<java.util.Map<K,V>>)", MemberLookup.Match.TYPED)).isNotNull();
    assertThat(find("lib.Calls", "lib.Calls#take(int)", MemberLookup.Match.TYPED)).isNull();
    assertThat(find("lib.Calls", "lib.Calls#fromParent()", MemberLookup.Match.TYPED)).isNotNull();
    assertThat(find("lib.Calls", "lib.Calls#<init>()", MemberLookup.Match.TYPED)).isNull();
    assertThat(find("lib.Proj", "lib.Proj#run(1)", MemberLookup.Match.ARITY)).isNotNull();
    assertThat(find("lib.Proj", "lib.Proj#run(long)", MemberLookup.Match.TYPED)).isNull();
    assertThat(find("lib.Proj", "lib.Proj#run()", MemberLookup.Match.ARITY)).isNull();
  }

  private JsonObject find(String owner, String id, MemberLookup.Match match) {
    return lookup.findMember(owner, SemaFacts.parseMember(id), new HashSet<>(), match);
  }

  private boolean takesNoFunction(Type parameter) {
    JMethodSymbol method = new JMethodSymbol(sema, "lib.X#m(" + parameter.fullyQualifiedName() + ")", "m");
    method.parameterTypes(List.of(parameter));
    return lookup.takesNoFunction(method, Set.of(0));
  }

  @Test
  void inherited_library_members_are_owned_by_the_type_declaring_them() {
    assertThat(sema.resolve("lib.Calls#fromParent()").owner().name()).isEqualTo("Parent");
    assertThat(sema.resolve("lib.Calls#two(int)").owner().name()).isEqualTo("Calls");
    assertThat(lookup.declaringType("lib.Calls", new JsonObject(), new HashSet<>())).isNull();
  }

  @Test
  void a_closed_type_loader_leaves_types_undeclared_and_unknown() {
    JSema closed = new JSema();
    closed.loadPackages(name -> null);
    closed.loadTypes(names -> null);

    assertThat(closed.declaresType("lib.Missing")).isFalse();
    assertThat(closed.typeSymbol("lib.Missing").isTypeSymbol()).isFalse();
  }

}
