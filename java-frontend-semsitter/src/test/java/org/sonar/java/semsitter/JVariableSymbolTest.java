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

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.VariableTree;

import static org.assertj.core.api.Assertions.assertThat;

class JVariableSymbolTest {

  private static final String SOURCE = """
    class C {
      static final long L = 1_000L;
      static final float F = 1.5f;
      static final double D = 2.5;
      static final int I = 1 + 2;
      static final long WIDE = 5;
      static final double DOUBLE = 1;
      static final float FLOAT = 2;
      static final Integer BOXED = 3;
      static final String S = "s";
      static final int NONE;
      final int instance = 4;
      static int mutable = 6;
    }
    """;

  @TempDir
  Path baseDir;

  @Test
  void constants_of_declared_fields_are_typed_as_their_declaration() throws Exception {
    JSema sema = new JSema();
    List<VariableTree> fields = fields();

    assertThat(constant(sema, fields, "L", "static", "final")).contains(1000L);
    assertThat(constant(sema, fields, "F", "static", "final")).contains(1.5f);
    assertThat(constant(sema, fields, "D", "static", "final")).contains(2.5);
    assertThat(constant(sema, fields, "I", "static", "final")).contains(3);
    assertThat(constant(sema, fields, "WIDE", "static", "final")).contains(5L);
    assertThat(constant(sema, fields, "DOUBLE", "static", "final")).contains(1.0);
    assertThat(constant(sema, fields, "FLOAT", "static", "final")).contains(2.0f);
    assertThat(constant(sema, fields, "S", "static", "final")).contains("s");
  }

  @Test
  void boxed_uninitialized_instance_and_mutable_fields_are_not_constants() throws Exception {
    JSema sema = new JSema();
    List<VariableTree> fields = fields();

    assertThat(constant(sema, fields, "BOXED", "static", "final")).isEmpty();
    assertThat(constant(sema, fields, "NONE", "static", "final")).isEmpty();
    assertThat(constant(sema, fields, "instance", "final")).isEmpty();
    assertThat(constant(sema, fields, "mutable", "static")).isEmpty();
    assertThat(new JVariableSymbol(sema, null, "x", "field").constantValue()).isEmpty();
  }

  @Test
  void a_given_constant_value_is_kept() {
    JVariableSymbol field = new JVariableSymbol(new JSema(), "C#X", "X", "field");
    field.constantValue(7);

    assertThat(field.constantValue()).contains(7);
  }

  @Test
  void variables_owned_by_a_method_are_local() {
    JSema sema = new JSema();
    JVariableSymbol variable = new JVariableSymbol(sema, "C#m()#v", "v", "field");
    variable.owner(new JMethodSymbol(sema, "C#m()", "m"));
    JVariableSymbol parameter = new JVariableSymbol(sema, "C#m()#p", "p", "param");

    assertThat(variable.isLocalVariable()).isTrue();
    assertThat(variable.isParameter()).isFalse();
    assertThat(parameter.isParameter()).isTrue();
    assertThat(new JVariableSymbol(sema, "C#f", "f", "field").isLocalVariable()).isFalse();
  }

  @Test
  void parameter_placeholder_of_an_undeclared_method_carries_nothing_but_its_type() {
    JSema sema = new JSema();
    JTypeSymbol owner = sema.typeSymbol("p.C");
    JMethodSymbol method = new JMethodSymbol(sema, null, "m");
    method.owner(owner);
    Symbol.VariableSymbol placeholder = (Symbol.VariableSymbol) sema.factTypes().parameterPlaceholder(method, 0, sema.primitiveType("int"));

    assertThat(placeholder.name()).isEqualTo("arg0");
    assertThat(placeholder.owner()).isSameAs(method);
    assertThat(placeholder.type().fullyQualifiedName()).isEqualTo("int");
    assertThat(placeholder.isUnknown()).isFalse();
    assertThat(placeholder.enclosingClass()).isEqualTo(owner);
    assertThat(placeholder.usages()).isEmpty();
    assertThat(placeholder.declaration()).isNull();
    assertThat(placeholder.isEffectivelyFinal()).isFalse();
    assertThat(placeholder.constantValue()).isEmpty();
    assertThat(placeholder.isLocalVariable()).isFalse();
    assertThat(placeholder.isParameter()).isTrue();
    assertThat(placeholder.isVariableSymbol()).isTrue();
    assertThat(placeholder.isFinal()).isFalse();

    SymbolMetadata metadata = placeholder.metadata();
    assertThat(metadata.isAnnotatedWith("java.lang.Deprecated")).isFalse();
    assertThat(metadata.valuesForAnnotation("java.lang.Deprecated")).isNull();
    assertThat(metadata.annotations()).isEmpty();
    assertThat(metadata.symbolAnnotations()).isEmpty();
    assertThat(metadata.nullabilityData().isNonNull(SymbolMetadata.NullabilityLevel.PACKAGE, false, false)).isFalse();
    assertThat(metadata.nullabilityData(SymbolMetadata.NullabilityTarget.PARAMETER).isNullable(SymbolMetadata.NullabilityLevel.PACKAGE, false, false)).isFalse();
    assertThat(metadata.nullabilityData()).isEqualTo(method.metadata().nullabilityData(SymbolMetadata.NullabilityTarget.PARAMETER));
    assertThat(metadata.findAnnotationTree(null)).isNull();
    assertThat(metadata.parametersMetadata()).isEmpty();
  }

  @Test
  void parameter_placeholder_of_an_unknown_method_has_unknown_nullability() {
    JSema sema = new JSema();
    JMethodSymbol method = new JMethodSymbol(sema, null, "m");
    method.unknown(true);
    Symbol.VariableSymbol placeholder = (Symbol.VariableSymbol) sema.factTypes().parameterPlaceholder(method, 0, sema.primitiveType("int"));

    assertThat(placeholder.metadata().nullabilityData()).isEqualTo(JSymbolMetadata.unknownNullabilityAt(SymbolMetadata.NullabilityLevel.VARIABLE));
  }

  private Optional<Object> constant(JSema sema, List<VariableTree> fields, String name, String... modifiers) {
    JVariableSymbol field = new JVariableSymbol(sema, "C#" + name, name, "field");
    field.addModifiers(List.of(modifiers));
    sema.declare("C#" + name, fields.stream().filter(tree -> tree.simpleName().name().equals(name)).findFirst().orElseThrow());
    return field.constantValue();
  }

  private List<VariableTree> fields() throws Exception {
    CompilationUnitTree unit = (CompilationUnitTree) Replays.parseWithEcj(baseDir, "C", SOURCE).get();
    return ((ClassTree) unit.types().get(0)).members().stream().map(VariableTree.class::cast).toList();
  }
}
