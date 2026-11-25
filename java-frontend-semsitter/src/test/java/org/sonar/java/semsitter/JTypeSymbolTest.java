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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;

import static org.assertj.core.api.Assertions.assertThat;

class JTypeSymbolTest {

  private static final String TYPES = """
    {
      "p.Base": {"kind": "class", "super": "java.lang.Object", "interfaces": []},
      "p.Iface": {"kind": "interface", "interfaces": []},
      "p.Anon": {"kind": "class", "super": "p.Iface", "interfaces": []},
      "p.AnonBoth": {"kind": "class", "super": "p.Iface", "interfaces": ["p.Iface"]},
      "p.Ann": {"kind": "annotation"},
      "p.CycA": {"kind": "class", "super": "p.CycB"},
      "p.CycB": {"kind": "class", "super": "p.CycA"},
      "p.Loop": {"kind": "class", "super": "p.CycA"},
      "p.Rec": {"kind": "class", "super": "p.Nowhere"},
      "p.En": {"kind": "enum"},
      "p.EnumSuper": {"kind": "enum", "super": "java.lang.Enum"},
      "p.Facts": {"kind": "class", "super": "java.lang.Object", "interfaces": ["java.io.Serializable", "java.lang.Cloneable"],
        "interfaceTypes": [{"kind": "class"}, {"kind": "class", "id": {}}]},
      "p.Members": {"kind": "class", "super": "java.lang.Object", "members": [
        {"id": "p.Members#<init>()"},
        {"id": "p.Members#a-b"},
        {"id": "p.Members#1x"},
        {"id": "p.Members#"},
        {"id": "p.Members#size"},
        {"name": "size", "k": "m", "desc": "()I"},
        {"name": "other", "k": "f", "desc": "I"},
        {"k": "m", "desc": "()V"},
        {"id": "p.Members<T>", "facts": {"kind": "typeparameter"}}
      ]}
    }
    """;

  @TempDir
  Path baseDir;

  private JSema sema;

  @BeforeEach
  void setUp() {
    sema = new JSema();
    sema.loadPackages(name -> null);
    sema.loadTypes(names -> new JsonObject());
    sema.ingestTypes(JsonParser.parseString(TYPES).getAsJsonObject());
  }

  @Test
  void an_anonymous_class_recorded_with_an_interface_as_superclass_extends_object() {
    Symbol.TypeSymbol anonymous = sema.typeSymbol("p.Anon");
    Symbol.TypeSymbol both = sema.typeSymbol("p.AnonBoth");

    assertThat(anonymous.superClass().fullyQualifiedName()).isEqualTo("java.lang.Object");
    assertThat(anonymous.interfaces()).extracting(Type::fullyQualifiedName).containsExactly("p.Iface");
    assertThat(both.interfaces()).extracting(Type::fullyQualifiedName).containsExactly("p.Iface");
  }

  @Test
  void enums_extend_enum_of_themselves() {
    assertThat(describe(sema.typeSymbol("p.En").superClass())).isEqualTo("java.lang.Enum<p.En>");
    assertThat(describe(sema.typeSymbol("p.EnumSuper").superClass())).isEqualTo("java.lang.Enum<p.EnumSuper>");
  }

  private static String describe(Type type) {
    return type.fullyQualifiedName() + type.typeArguments().stream().map(Type::fullyQualifiedName).toList().toString().replace('[', '<').replace(']', '>');
  }

  @Test
  void a_cyclic_hierarchy_falls_back_to_object() {
    assertThat(sema.typeSymbol("p.CycA").superClass().fullyQualifiedName()).isEqualTo("java.lang.Object");
    assertThat(sema.typeSymbol("p.Loop").superClass().fullyQualifiedName()).isEqualTo("p.CycA");
  }

  @Test
  void annotations_implement_annotation_and_facts_without_a_class_id_are_ignored() {
    assertThat(sema.typeSymbol("p.Ann").interfaces()).extracting(Type::fullyQualifiedName).containsExactly("java.lang.annotation.Annotation");
    assertThat(sema.typeSymbol("p.Ann").superClass().fullyQualifiedName()).isEqualTo("java.lang.Object");
    assertThat(sema.typeSymbol("p.Facts").interfaces()).extracting(Type::fullyQualifiedName).containsExactly("java.io.Serializable", "java.lang.Cloneable");
  }

  @Test
  void a_diamond_on_the_named_supertype_parameterizes_it() {
    JTypeSymbol anonymous = sema.typeSymbol("p.Base");
    JTypeSymbol child = new JTypeSymbol(sema, "p.Child", "Child", false);
    child.superName("p.Base");
    JType parameterized = JType.named(sema, "p.Base", List.of((JType) sema.getClassType("java.lang.String")));
    child.declaredSupertype(parameterized);
    JTypeSymbol implementer = new JTypeSymbol(sema, "p.Impl", "Impl", false);
    implementer.interfaceNames(List.of("p.Iface"));
    JType iface = JType.named(sema, "p.Iface", List.of((JType) sema.getClassType("java.lang.String")));
    implementer.declaredSupertype(iface);

    assertThat(anonymous.superClass().fullyQualifiedName()).isEqualTo("java.lang.Object");
    assertThat(child.superClass()).isSameAs(parameterized);
    assertThat(implementer.interfaces()).containsExactly(iface);
  }

  @Test
  void undeclared_supertypes_are_kept_when_named_and_dropped_when_unknown() {
    JTypeSymbol type = new JTypeSymbol(sema, "p.Partial", "Partial", false);
    type.type(sema.rawNamed("p.Partial"));
    type.declaredSupertypes(null, List.of(JType.unknown(sema), JType.unknown(sema)));

    assertThat(type.interfaces()).extracting(Type::fullyQualifiedName).containsExactly("!Unknown!");
    assertThat(type.superTypes()).extracting(Type::fullyQualifiedName).containsExactly("java.lang.Object");
    assertThat(sema.typeSymbol("p.Rec").superTypes()).extracting(Type::fullyQualifiedName).containsExactly("p.Nowhere");
    assertThat(sema.typeSymbol("p.Missing").superTypes()).isEmpty();
  }

  @Test
  void members_named_skip_type_parameters_and_facts_naming_another_member() {
    JTypeSymbol members = sema.typeSymbol("p.Members");

    assertThat(members.lookupSymbols("size")).extracting(Symbol::name).containsOnly("size");
    assertThat(members.lookupSymbols("size")).hasSize(2);
    assertThat(members.lookupSymbols("other")).extracting(Symbol::isVariableSymbol).containsExactly(true);
    assertThat(members.lookupSymbols("T")).isEmpty();
    assertThat(members.memberSymbols()).hasSize(8);
  }

  @Test
  void a_parameterized_view_shares_the_members_of_its_generic_type() {
    JTypeSymbol members = sema.typeSymbol("p.Members");
    JTypeSymbol view = members.parameterizedAs(JType.named(sema, "p.Members", List.of((JType) sema.getClassType("java.lang.String"))));

    assertThat(view.memberSymbols()).isSameAs(members.memberSymbols());
    assertThat(view.lookupSymbols("other")).isEqualTo(members.lookupSymbols("other"));
    assertThat(view.interfaces()).isEmpty();
  }

  @Test
  void an_enum_of_this_file_has_a_default_constructor_values_and_value_of() throws Exception {
    CompilationUnitTree unit = (CompilationUnitTree) Replays.parseWithEcj(baseDir, "En", "enum En { A }").get();
    sema.declare("p.En", unit.types().get(0));

    List<Symbol> members = List.copyOf(sema.typeSymbol("p.En").memberSymbols());

    assertThat(members).extracting(Symbol::name).containsExactly("<init>", "values", "valueOf");
    assertThat(((Symbol.MethodSymbol) members.get(1)).returnType().type().fullyQualifiedName()).isEqualTo("p.En[]");
    assertThat(((Symbol.MethodSymbol) members.get(2)).parameterTypes()).extracting(Type::fullyQualifiedName).containsExactly("java.lang.String");
  }

  @Test
  void a_type_without_owner_is_its_own_outermost_class() {
    JTypeSymbol orphan = new JTypeSymbol(sema, "x", "x", false);

    assertThat(orphan.outermostClass()).isSameAs(orphan);
    assertThat(sema.typeSymbol("p.Base").outermostClass()).isSameAs(sema.typeSymbol("p.Base"));
  }
}
