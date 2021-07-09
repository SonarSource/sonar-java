package org.sonar.java.checks.helpers;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.MethodTree;

import static org.assertj.core.api.Assertions.assertThat;

class RecordUtilsTest {
  @Nested
  class isACanonicalConstructor {

    @Test
    void returns_true_on_canonical_constructor() {
      String code =
        "record MyRecord(int a, String b, Object c) {\n" +
          "  MyRecord(int a, String b, Object c) { this.a = a; this.b = b; this.c = c; }\n" +
          "}";
      MethodTree constructor = parseMethodOutOfRecord(code);
      assertThat(RecordUtils.isACanonicalConstructor(constructor)).isTrue();
    }

    @Test
    void returns_false_on_constructor_from_class() {
      String code =
        "class MyRecord {\n" +
          "  MyRecord(int a, String b, Object c) { this.a = a; this.b = b; this.c = c; }\n" +
          "  private int a; private String b; private Object c;\n" +
          "}";
      MethodTree constructor = parseMethodOutOfRecord(code);
      assertThat(RecordUtils.isACanonicalConstructor(constructor)).isFalse();
    }

    @Test
    void returns_false_on_non_constructor_method() {
      String code =
        "record MyRecord(int a, String b, Object c) {\n" +
          "void MyRecord(int a, String b, Object c) {}\n" +
          "}";
      MethodTree constructor = parseMethodOutOfRecord(code);
      assertThat(RecordUtils.isACanonicalConstructor(constructor)).isFalse();
    }

    @Test
    void returns_false_on_non_canonical_constructor() {
      String code =
        "record MyRecord(int a, String b, Object c) {\n" +
          "MyRecord(int a) { this(a, null, null); }\n" +
          "}";
      MethodTree constructor = parseMethodOutOfRecord(code);
      assertThat(RecordUtils.isACanonicalConstructor(constructor)).isFalse();
    }

    @Test
    void returns_false_on_constructor_with_misordered_parameters() {
      String code =
        "record MyRecord(int a, String b, Object c) {\n" +
          "MyRecord(String b, int a, Object c) {  this.a = a; this.b = b; this.c = c; }\n" +
          "}";
      MethodTree constructor = parseMethodOutOfRecord(code);
      assertThat(RecordUtils.isACanonicalConstructor(constructor)).isFalse();
    }

    @Test
    void returns_false_on_constructor_with_mistyped_parameters() {
      String code =
        "record MyRecord(int a, Object b, Object c) {\n" +
          "MyRecord(int a, String b, String c) {  this.a = a; this.b = b; this.c = c; }\n" +
          "}";
      MethodTree constructor = parseMethodOutOfRecord(code);
      assertThat(RecordUtils.isACanonicalConstructor(constructor)).isFalse();
    }

    @Test
    void returns_false_on_constructor_with_throw_clause() {
      String code =
        "record MyRecord(int a, String b, Object c) {\n" +
          "MyRecord(int a, String b, Object c) throws Exception {  this.a = a; this.b = b; this.c = c; }\n" +
          "}";
      MethodTree constructor = parseMethodOutOfRecord(code);
      assertThat(RecordUtils.isACanonicalConstructor(constructor)).isFalse();
    }
  }

  @Nested
  class isACompactConstructor {
    @Test
    void returns_true_on_compact_constructor() {
      String code =
        "record MyRecord(int a, String b, Object c) {\n" +
          "MyRecord { }\n" +
          "}";
      MethodTree constructor = parseMethodOutOfRecord(code);
      assertThat(RecordUtils.isACompactConstructor(constructor)).isTrue();
    }

    @Test
    void returns_false_on_class_constructor() {
      String code =
        "class MyRecord {\n" +
          "  MyRecord(int a, String b, Object c) { this.a = a; this.b = b; this.c = c; }\n" +
          "  private int a; private String b; private Object c;\n" +
          "}";
      MethodTree constructor = parseMethodOutOfRecord(code);
      assertThat(RecordUtils.isACompactConstructor(constructor)).isFalse();
    }

    @Test
    void returns_false_on_canonical_constructor() {
      String code =
        "record MyRecord(int a, String b, Object c) {\n" +
          "  MyRecord(int a, String b, Object c) { this.a = a; this.b = b; this.c = c; }\n" +
          "}";
      MethodTree constructor = parseMethodOutOfRecord(code);
      assertThat(RecordUtils.isACompactConstructor(constructor)).isFalse();
    }

    @Test
    void returns_false_on_parameterless_constructor() {
      String code =
        "record MyRecord(int a, String b, Object c) {\n" +
          "  MyRecord() { }\n" +
          "}";
      MethodTree constructor = parseMethodOutOfRecord(code);
      assertThat(RecordUtils.isACompactConstructor(constructor)).isFalse();
    }
  }

  private static MethodTree parseMethodOutOfRecord(String code) {
    ClassTree record = parseRecord(code);
    return (MethodTree) record.members().get(0);
  }

  private static ClassTree parseRecord(String code) {
    CompilationUnitTree compilationUnitTree = JParserTestUtils.parse(code);
    return (ClassTree) compilationUnitTree.types().get(0);
  }
}
