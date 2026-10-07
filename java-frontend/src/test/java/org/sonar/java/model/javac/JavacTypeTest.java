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

import javax.lang.model.type.TypeKind;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;

import static org.assertj.core.api.Assertions.assertThat;

class JavacTypeTest {

  @ParameterizedTest
  @CsvSource({
    "boolean, false, false, true, false, false",
    "byte, false, false, true, true, false",
    "char, false, false, true, true, false",
    "int, false, false, true, true, false",
    "long, false, false, true, true, false",
    "void, false, false, false, false, true",
    "java.lang.String, false, true, false, false, false",
    "java.util.List, false, true, false, false, false",
    "java.lang.String[], true, false, false, false, false",
    "int[], true, false, false, false, false"
  })
  void exposes_type_kind_flags(String name, boolean array, boolean declared, boolean primitive,
    boolean numerical, boolean voidType) throws Exception {
    try (JavacParser.Result parsed = JavacParser.parse("Kinds.java", "class Kinds {}",
      JavacParser.Options.defaults().withAnalysis(true))) {
      Type type = new JavacSema(parsed).getClassType(name);
      assertThat(type.isArray()).isEqualTo(array);
      assertThat(type.isClass()).isEqualTo(declared);
      assertThat(type.isPrimitive()).isEqualTo(primitive);
      assertThat(type.isNumerical()).isEqualTo(numerical);
      assertThat(type.isVoid()).isEqualTo(voidType);
    }
  }

  @ParameterizedTest
  @CsvSource({
    "int, int",
    "int[][], int[][]",
    "java.util.Map, Map",
    "java.util.Map[][], Map[][]",
    "java.util.Map$Entry, Entry",
    "java.util.Map$Entry[][], Entry[][]"
  })
  void exposes_binary_and_simple_names(String qualifiedName, String simpleName) throws Exception {
    try (JavacParser.Result parsed = JavacParser.parse("Names.java", "class Names {}",
      JavacParser.Options.defaults().withAnalysis(true))) {
      Type type = new JavacSema(parsed).getClassType(qualifiedName);
      assertThat(type.is(qualifiedName)).isTrue();
      assertThat(type.name()).isEqualTo(simpleName);
      assertThat(type.toString()).isEqualTo(simpleName);
    }
  }

  @Test
  void exposes_javac_type_names_hierarchy_arrays_and_generics() throws Exception {
    try (JavacParser.Result parsed = JavacParser.parse("Types.java", "class Types {}",
      JavacParser.Options.defaults().withAnalysis(true))) {
      Types types = parsed.task().getTypes();
      Elements elements = parsed.task().getElements();
      JavacType string = new JavacType(elements.getTypeElement("java.lang.String").asType(), types, elements);
      JavacType integer = new JavacType(elements.getTypeElement("java.lang.Integer").asType(), types, elements);
      JavacType primitive = new JavacType(types.getPrimitiveType(TypeKind.INT), types, elements);
      JavacType array = new JavacType(types.getArrayType(string.mirror()), types, elements);
      JavacType list = new JavacType(types.getDeclaredType(elements.getTypeElement("java.util.List"), string.mirror()), types, elements);
      JavacType rawList = new JavacType(types.getDeclaredType(elements.getTypeElement("java.util.List")), types, elements);

      assertThat(string.fullyQualifiedName()).isEqualTo("java.lang.String");
      assertThat(string.isSubtypeOf("java.lang.CharSequence")).isTrue();
      assertThat(integer.isSubtypeOf("java.lang.Number")).isTrue();
      assertThat(primitive.primitiveWrapperType().is("java.lang.Integer")).isTrue();
      assertThat(integer.primitiveType().is("int")).isTrue();
      assertThat(array.isArray()).isTrue();
      assertThat(array.fullyQualifiedName()).isEqualTo("java.lang.String[]");
      assertThat(array.elementType().is("java.lang.String")).isTrue();
      assertThat(array.isSubtypeOf("java.lang.Object")).isTrue();
      assertThat(list.isParameterized()).isTrue();
      assertThat(list.typeArguments()).hasSize(1).first().extracting(Type::fullyQualifiedName).isEqualTo("java.lang.String");
      assertThat(list.erasure().is("java.util.List")).isTrue();
      assertThat(rawList.isRawType()).isTrue();
      assertThat(string.symbol()).isSameAs(Symbol.TypeSymbol.UNKNOWN_TYPE);
    }
  }
}
