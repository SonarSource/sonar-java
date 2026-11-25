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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata.AnnotationValue;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.MethodTree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/** Reads annotation values from trees the ECJ frontend gives, as those of the SemSitter frontend have the same shape. */
class TreeAnnotationTest {

  private static final String SOURCE = """
    import java.lang.annotation.Retention;
    import java.lang.annotation.RetentionPolicy;

    @interface Values {
      String[] names() default {};
      int number() default 0;
      long big() default 0;
      char letter() default 'a';
      Class<?> type() default Object.class;
      Deprecated nested() default @Deprecated;
      String text() default "";
      int sum() default 0;
      boolean flag() default false;
      float ratio() default 0;
      double scale() default 0;
      char escaped() default 'a';
      char unicode() default 'a';
      char octal() default 'a';
    }

    class Holder {
      static final int CONSTANT = 7;
      static String field = "f";

      @Values(names = "one", number = 42, big = 3L, letter = 'z', type = String.class, nested = @Deprecated(since = "9"), text = \"""
        block\""", sum = 1 + 2, flag = true)
      void literals() {}

      @Values(ratio = 1.5f, scale = 2_0.5, escaped = '\\n', unicode = '\\u0041', octal = '\\101')
      void numbers() {}

      @Values(names = {"x", "y"}, number = CONSTANT, sum = Holder.CONSTANT)
      void constants() {}

      @SuppressWarnings("unchecked")
      void single() {}

      @Retention(RetentionPolicy.RUNTIME)
      void enumConstant() {}

      @SuppressWarnings(Holder.field)
      void notConstant() {}

      @SuppressWarnings(MISSING)
      void unknownValue() {}

      @Missing(1)
      void unknownAnnotation() {}
    }
    """;

  @TempDir
  Path baseDir;

  private final JSema sema = new JSema();
  private final Map<String, AnnotationTree> annotations = new HashMap<>();

  @BeforeEach
  void parse() throws Exception {
    Replays.parseWithEcj(baseDir, "TreeAnnotationSample", SOURCE).get().accept(new BaseTreeVisitor() {
      @Override
      public void visitMethod(MethodTree tree) {
        if (!tree.modifiers().annotations().isEmpty()) {
          annotations.put(tree.simpleName().name(), tree.modifiers().annotations().get(0));
        }
        super.visitMethod(tree);
      }
    });
  }

  @Test
  void literal_values_keep_their_java_type() {
    Map<String, Object> values = values("literals");

    assertThat((Object[]) values.get("names")).containsExactly("one");
    assertThat(values)
      .containsEntry("number", 42)
      .containsEntry("big", 3L)
      .containsEntry("letter", 'z')
      .containsEntry("sum", 3)
      .containsEntry("flag", true);
    assertThat(values.get("text")).asString().contains("block");
    assertThat(values.get("type")).isInstanceOfSatisfying(Symbol.TypeSymbol.class, type -> assertThat(type.type().fullyQualifiedName()).isEqualTo("java.lang.String"));
    assertThat(values.get("nested")).isInstanceOfSatisfying(TreeAnnotation.class, nested -> {
      assertThat(nested.symbol().type().fullyQualifiedName()).isEqualTo("java.lang.Deprecated");
      assertThat(nested.values()).extracting(AnnotationValue::name, AnnotationValue::value).containsExactly(tuple("since", "9"));
    });
  }

  @Test
  void floating_and_escaped_char_literals_keep_their_java_type() {
    assertThat(values("numbers"))
      .containsEntry("ratio", 1.5f)
      .containsEntry("scale", 20.5)
      .containsEntry("escaped", '\n')
      .containsEntry("unicode", 'A')
      .containsEntry("octal", 'A');
  }

  @Test
  void constant_names_give_their_value() {
    Map<String, Object> values = values("constants");

    assertThat((Object[]) values.get("names")).containsExactly("x", "y");
    assertThat(values).containsEntry("number", 7).containsEntry("sum", 7);
  }

  @Test
  void a_single_value_of_an_array_member_is_an_array() {
    assertThat((Object[]) values("single").get("value")).containsExactly("unchecked");
  }

  @Test
  void enum_constants_are_their_symbol() {
    assertThat(values("enumConstant").get("value")).isInstanceOfSatisfying(Symbol.VariableSymbol.class, constant -> assertThat(constant.name()).isEqualTo("RUNTIME"));
  }

  @Test
  void a_variable_that_is_not_a_constant_has_no_value() {
    assertThat((Object[]) values("notConstant").get("value")).containsOnlyNulls();
  }

  @Test
  void an_unknown_name_has_no_value_as_with_ecj() {
    assertThat((Object[]) values("unknownValue").get("value")).containsOnlyNulls();
  }

  @Test
  void an_unknown_annotation_has_no_values() {
    TreeAnnotation annotation = new TreeAnnotation(sema, annotations.get("unknownAnnotation"));

    assertThat(annotation.symbol().isUnknown()).isTrue();
    assertThat(annotation.values()).isEmpty();
  }

  private Map<String, Object> values(String method) {
    List<AnnotationValue> values = new TreeAnnotation(sema, annotations.get(method)).values();
    Map<String, Object> byName = new HashMap<>();
    values.forEach(value -> byName.put(value.name(), value.value()));
    return byName;
  }
}
