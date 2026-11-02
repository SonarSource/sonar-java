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
package org.sonar.java.model;

import org.junit.jupiter.api.Test;
import org.sonar.java.model.declaration.VariableTreeImpl;
import org.sonar.java.model.expression.IdentifierTreeImpl;
import org.sonar.java.model.expression.LiteralTreeImpl;
import org.sonar.java.model.expression.MemberSelectExpressionTreeImpl;
import org.sonar.java.model.expression.MethodInvocationTreeImpl;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * This test warns you if you are changing classes which have a big impact
 * on memory consumption. If the change is intentional, just update the failing tests.
 *
 * <p>We used to test the size in bytes of instance using jol-core,
 * but it was removed due to licensing.
 */
class ClassesLayoutTest {

  @Test
  void shared_model_and_contracts_have_no_compiler_imports() throws Exception {
    Path model = Path.of("src/main/java/org/sonar/java/model");
    Set<String> sharedFiles = Set.of("AbstractTypedTree.java", "JavaTree.java", "KeywordThis.java", "KeywordSuper.java",
      "TypeParameterTreeImpl.java", "JUtils.java", "JSymbolMetadata.java", "JSymbolMetadataNullabilityHelper.java",
      "JProblem.java", "JWarning.java", "SemanticValues.java");
    try (Stream<Path> files = Files.walk(model)) {
      for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
        if (sharedFiles.contains(file.getFileName().toString()) || List.of("declaration", "expression", "pattern").contains(file.getParent().getFileName().toString())) {
          assertThat(Files.readAllLines(file).stream().filter(line -> line.startsWith("import "))).as(file.toString())
            .noneMatch(line -> line.contains("org.eclipse.jdt"));
        }
      }
    }
  }

  @Test
  void token() {
    assertThat(countFields(InternalSyntaxToken.class)).isEqualTo(7);
  }

  @Test
  void identifier() {
    assertThat(countFields(IdentifierTreeImpl.class)).isEqualTo(9);
  }

  @Test
  void literal() {
    assertThat(countFields(LiteralTreeImpl.class)).isEqualTo(6);
  }

  @Test
  void variable_declaration() {
    assertThat(countFields(VariableTreeImpl.class)).isEqualTo(10);
  }

  @Test
  void member_select() {
    assertThat(countFields(MemberSelectExpressionTreeImpl.class)).isEqualTo(8);
  }

  @Test
  void method_invocation() {
    assertThat(countFields(MethodInvocationTreeImpl.class)).isEqualTo(8);
  }

  @Test
  void type() {
    assertThat(countFields(JType.class)).isEqualTo(7);
  }

  @Test
  void symbol_type() {
    assertThat(countFields(JTypeSymbol.class)).isEqualTo(11);
  }

  @Test
  void symbol_method() {
    assertThat(countFields(JMethodSymbol.class)).isEqualTo(11);
  }

  @Test
  void symbol_variable() {
    assertThat(countFields(JVariableSymbol.class)).isEqualTo(7);
  }

  @Test
  void annotation() {
    assertThat(countFields(EcjSymbolMetadata.JAnnotationInstance.class)).isEqualTo(3);
  }

  /** Count number of fields in instances, including inherited fields. */
  private static int countFields(Class<?> clazz) {
    int count = 0;
    while (clazz != Object.class) {
      for (Field f : clazz.getDeclaredFields()) {
        if (!Modifier.isStatic(f.getModifiers())) {
          count++;
        }
      }
      clazz = clazz.getSuperclass();
    }
    return count;
  }

  static class C {
    private int x;
    public long y;
    HashSet<Object> hashSet;

    C(int x) {
      this.x = x;
    }
  }

  static class D extends C {
    char z;
    static String staticField;

    D(int x) {
      super(x);
    }
  }

  @Test
  void validateCountFields() {
    assertThat(countFields(D.class)).isEqualTo(4);
  }
}
