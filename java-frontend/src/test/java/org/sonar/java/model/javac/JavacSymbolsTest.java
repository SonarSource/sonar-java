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
package org.sonar.java.model.javac;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import org.junit.jupiter.api.Test;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;

import static org.assertj.core.api.Assertions.assertThat;

class JavacSymbolsTest {

  @Test
  void wraps_resolved_types_methods_and_variables() throws Exception {
    String source = "interface Named { String name(); } "
      + "public class Sample implements Named { public static final int VALUE = 42; "
      + "public String name() { return \"sample\"; } }";
    try (JavacParser.Result parsed = JavacParser.parse("Sample.java", source,
      JavacParser.Options.defaults().withAnalysis(true))) {
      JavacSymbols symbols = new JavacSymbols(parsed.task().getTypes(), parsed.task().getElements());
      TypeElement typeElement = parsed.task().getElements().getTypeElement("Sample");
      Symbol.TypeSymbol sample = (Symbol.TypeSymbol) symbols.symbol(typeElement);
      Element fieldElement = typeElement.getEnclosedElements().stream()
        .filter(element -> element.getSimpleName().contentEquals("VALUE"))
        .findFirst().orElseThrow();
      Symbol.VariableSymbol field = (Symbol.VariableSymbol) symbols.symbol(fieldElement);
      Symbol.MethodSymbol method = (Symbol.MethodSymbol) sample.memberSymbols().stream()
        .filter(member -> member.name().equals("name"))
        .findFirst().orElseThrow();

      assertThat(sample.name()).isEqualTo("Sample");
      assertThat(sample.type().symbol()).isSameAs(sample);
      assertThat(sample.isPublic()).isTrue();
      assertThat(sample.interfaces()).anySatisfy(type -> assertThat(type.is("Named")).isTrue());
      assertThat(sample.superTypes()).anySatisfy(type -> assertThat(type.is("Named")).isTrue());
      assertThat(sample.lookupSymbols("name")).isNotEmpty();
      assertThat(symbols.symbol(typeElement)).isSameAs(sample);
      assertThat(field.owner()).isSameAs(sample);
      assertThat(field.isStatic()).isTrue();
      assertThat(field.isFinal()).isTrue();
      assertThat(field.constantValue()).contains(42);
      assertThat(method.owner()).isSameAs(sample);
      assertThat(method.returnType().type().is("java.lang.String")).isTrue();
      assertThat(method.overriddenSymbols()).hasSize(1);
      assertThat(method.signature()).isEqualTo("Sample#name()Ljava/lang/String;");
    }
  }

  @Test
  void resolves_annotation_values_and_type_argument_metadata() throws Exception {
    String source = "import java.lang.annotation.*; "
      + "@Target({ElementType.TYPE_USE, ElementType.FIELD}) @interface Mark { int value(); Class<?> type() default String.class; } "
      + "class Annotated { @Mark(7) java.util.List<@Mark(8) String> names; }";
    try (JavacParser.Result parsed = JavacParser.parse("Annotated.java", source,
      JavacParser.Options.defaults().withAnalysis(true))) {
      JavacSymbols symbols = new JavacSymbols(parsed.task().getTypes(), parsed.task().getElements());
      TypeElement annotated = parsed.task().getElements().getTypeElement("Annotated");
      Element fieldElement = annotated.getEnclosedElements().stream()
        .filter(element -> element.getSimpleName().contentEquals("names"))
        .findFirst().orElseThrow();
      Symbol field = symbols.symbol(fieldElement);
      SymbolMetadata metadata = field.metadata();

      assertThat(metadata.isAnnotatedWith("Mark")).isTrue();
      assertThat(metadata.symbolAnnotations()).hasSize(1);
      assertThat(metadata.valuesForAnnotation("Mark")).anySatisfy(value -> {
        assertThat(value.name()).isEqualTo("value");
        assertThat(value.value()).isEqualTo(7);
      });
      assertThat(metadata.parametersMetadata()).hasSize(1);
      assertThat(metadata.parametersMetadata()[0].valuesForAnnotation("Mark"))
        .anySatisfy(value -> assertThat(value.value()).isEqualTo(8));
      assertThat(field.type().symbol().name()).isEqualTo("List");
      assertThat(field.type().typeArguments()).first()
        .satisfies(type -> assertThat(type.symbol().name()).isEqualTo("String"));
    }
  }
}
