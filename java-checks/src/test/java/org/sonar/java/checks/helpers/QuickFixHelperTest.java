/*
 * SonarQube Java
 * Copyright (C) 2012-2021 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.java.checks.helpers;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.sonar.java.checks.helpers.QuickFixHelper.ImportSupplier;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.java.model.statement.BlockTreeImpl;
import org.sonar.java.reporting.JavaTextEdit;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class QuickFixHelperTest {

  @Test
  void nextToken() {
    CompilationUnitTree cut = JParserTestUtils.parse("class A { void foo() {} }");
    ClassTree a = (ClassTree) cut.types().get(0);
    MethodTree foo = (MethodTree) a.members().get(0);

    assertThat(QuickFixHelper.nextToken(foo.simpleName())).isEqualTo(foo.openParenToken());

    // through non-existing nodes (modifiers of method)
    assertThat(QuickFixHelper.nextToken(a.openBraceToken()))
      .isEqualTo(QuickFixHelper.nextToken(foo.modifiers()))
      .isEqualTo(foo.returnType().firstToken());

    // need to go through parent
    assertThat(QuickFixHelper.nextToken(foo.block().lastToken())).isEqualTo(a.closeBraceToken());

    // end of file
    assertThat(QuickFixHelper.nextToken(a.closeBraceToken()))
      .isEqualTo(QuickFixHelper.nextToken(cut))
      .isEqualTo(cut.lastToken());
    assertThat(((InternalSyntaxToken) cut.lastToken()).isEOF()).isTrue();
  }

  @Test
  void previousToken() {
    CompilationUnitTree cut = JParserTestUtils.parse("class A { void foo() {} }");
    ClassTree a = (ClassTree) cut.types().get(0);
    MethodTree foo = (MethodTree) a.members().get(0);

    assertThat(QuickFixHelper.previousToken(foo.simpleName())).isEqualTo(foo.returnType().lastToken());

    // through non-existing nodes (modifiers of method)
    assertThat(QuickFixHelper.previousToken(a.openBraceToken())).isEqualTo(a.simpleName().lastToken());

    // need to go through parent
    assertThat(QuickFixHelper.previousToken(foo.returnType())).isEqualTo(a.openBraceToken());

    // start of file
    assertThat(QuickFixHelper.previousToken(a.declarationKeyword())).isEqualTo(a.declarationKeyword());
  }

  @Nested
  class NextVariable {
    @Test
    void throws_an_illegal_argument_exception_when_parent_type_is_not_supported() {
      Tree parent = mock(Tree.class);
      VariableTree variable = mock(VariableTree.class);
      when(variable.parent()).thenReturn(parent);

      assertThatThrownBy(() -> QuickFixHelper.nextVariable(variable))
        .isInstanceOfAny(IllegalArgumentException.class)
        .hasMessageContaining("The variable's parent kind is not handled by this method!");
    }

    @Test
    void returns_empty_on_single_variable_declaration_in_block() {
      CompilationUnitTree cut = JParserTestUtils.parse("class A { void f() { int target = 42; } }");
      ClassTree theClass = (ClassTree) cut.types().get(0);
      MethodTree method = (MethodTree) theClass.members().get(0);
      VariableTree target = (VariableTree) ((BlockTreeImpl) method.block()).getChildren().get(1);
      assertThat(QuickFixHelper.nextVariable(target)).isEmpty();
    }

    @Test
    void returns_empty_on_separate_variable_declarations_in_block() {
      CompilationUnitTree cut = JParserTestUtils.parse("class A { void f() { int target = 42; int notRelevant; } }");
      ClassTree theClass = (ClassTree) cut.types().get(0);
      MethodTree method = (MethodTree) theClass.members().get(0);
      VariableTree target = (VariableTree) ((BlockTreeImpl) method.block()).getChildren().get(2);
      assertThat(QuickFixHelper.nextVariable(target)).isEmpty();
    }

    @Test
    void returns_next_on_2_variable_declaration_in_block() {
      CompilationUnitTree cut = JParserTestUtils.parse("class A { void f() { int target = 42, next; } }");
      ClassTree theClass = (ClassTree) cut.types().get(0);
      MethodTree method = (MethodTree) theClass.members().get(0);
      VariableTree target = (VariableTree) ((BlockTreeImpl) method.block()).getChildren().get(1);
      VariableTree next = (VariableTree) ((BlockTreeImpl) method.block()).getChildren().get(2);
      assertThat(QuickFixHelper.nextVariable(target)).contains(next);
    }

    @Test
    void returns_next_on_3_variable_declaration_in_block() {
      CompilationUnitTree cut = JParserTestUtils.parse("class A { void f() { int first, target = 42, next; } }");
      ClassTree theClass = (ClassTree) cut.types().get(0);
      MethodTree method = (MethodTree) theClass.members().get(0);
      VariableTree target = (VariableTree) ((BlockTreeImpl) method.block()).getChildren().get(2);
      VariableTree next = (VariableTree) ((BlockTreeImpl) method.block()).getChildren().get(3);
      assertThat(QuickFixHelper.nextVariable(target)).contains(next);
    }

    @Test
    void returns_next_on_single_variable_declaration() {
      CompilationUnitTree cut = JParserTestUtils.parse("class A { int target = 42; }");
      ClassTree theClass = (ClassTree) cut.types().get(0);
      VariableTree target = (VariableTree) theClass.members().get(0);
      assertThat(QuickFixHelper.nextVariable(target)).isEmpty();
    }

    @Test
    void returns_empty_on_separate_variable_declarations() {
      CompilationUnitTree cut = JParserTestUtils.parse("class A { int target = 42; int notRelevant; }");
      ClassTree theClass = (ClassTree) cut.types().get(0);
      VariableTree target = (VariableTree) theClass.members().get(1);
      assertThat(QuickFixHelper.nextVariable(target)).isEmpty();
    }

    @Test
    void returns_next_on_2_variable_declaration() {
      CompilationUnitTree cut = JParserTestUtils.parse("class A { int target = 42, next; }");
      ClassTree theClass = (ClassTree) cut.types().get(0);
      VariableTree target = (VariableTree) theClass.members().get(0);
      VariableTree next = (VariableTree) theClass.members().get(1);
      assertThat(QuickFixHelper.nextVariable(target)).contains(next);
    }

    @Test
    void returns_next_on_3_variable_declaration() {
      CompilationUnitTree cut = JParserTestUtils.parse("class A { int first, target, next; }");
      ClassTree theClass = (ClassTree) cut.types().get(0);
      VariableTree target = (VariableTree) theClass.members().get(1);
      VariableTree next = (VariableTree) theClass.members().get(2);
      assertThat(QuickFixHelper.nextVariable(target)).contains(next);
    }
  }

  @Nested
  class PreviousVariable {
    @Test
    void throws_an_illegal_argument_exception_when_parent_type_is_not_supported() {
      Tree parent = mock(Tree.class);
      VariableTree variable = mock(VariableTree.class);
      when(variable.parent()).thenReturn(parent);

      assertThatThrownBy(() -> QuickFixHelper.previousVariable(variable))
        .isInstanceOfAny(IllegalArgumentException.class)
        .hasMessageContaining("The variable's parent kind is not handled by this method!");
    }

    @Test
    void returns_empty_on_single_variable_declaration_in_block() {
      CompilationUnitTree cut = JParserTestUtils.parse("class A { void f() { int target = 42; } }");
      ClassTree theClass = (ClassTree) cut.types().get(0);
      MethodTree method = (MethodTree) theClass.members().get(0);
      VariableTree target = (VariableTree) ((BlockTreeImpl) method.block()).getChildren().get(1);
      assertThat(QuickFixHelper.previousVariable(target)).isEmpty();
    }

    @Test
    void returns_empty_on_separate_variable_declarations_in_block() {
      CompilationUnitTree cut = JParserTestUtils.parse("class A { void f() { int notRelevant; int target = 42; } }");
      ClassTree theClass = (ClassTree) cut.types().get(0);
      MethodTree method = (MethodTree) theClass.members().get(0);
      VariableTree target = (VariableTree) ((BlockTreeImpl) method.block()).getChildren().get(2);
      assertThat(QuickFixHelper.previousVariable(target)).isEmpty();
    }

    @Test
    void returns_previous_on_2_variable_declaration_in_block() {
      CompilationUnitTree cut = JParserTestUtils.parse("class A { void f() { int first, target = 42; } }");
      ClassTree theClass = (ClassTree) cut.types().get(0);
      MethodTree method = (MethodTree) theClass.members().get(0);
      VariableTree previous = (VariableTree) ((BlockTreeImpl) method.block()).getChildren().get(1);
      VariableTree target = (VariableTree) ((BlockTreeImpl) method.block()).getChildren().get(2);
      assertThat(QuickFixHelper.previousVariable(target)).contains(previous);
    }

    @Test
    void returns_previous_on_3_variable_declaration_in_block() {
      CompilationUnitTree cut = JParserTestUtils.parse("class A { void f() { int first, previous, target; } }");
      ClassTree theClass = (ClassTree) cut.types().get(0);
      MethodTree method = (MethodTree) theClass.members().get(0);
      VariableTree previous = (VariableTree) ((BlockTreeImpl) method.block()).getChildren().get(2);
      VariableTree target = (VariableTree) ((BlockTreeImpl) method.block()).getChildren().get(3);
      assertThat(QuickFixHelper.previousVariable(target)).contains(previous);
    }

    @Test
    void returns_empty_on_single_variable_declaration() {
      CompilationUnitTree cut = JParserTestUtils.parse("class A { int target = 42; }");
      ClassTree theClass = (ClassTree) cut.types().get(0);
      VariableTree target = (VariableTree) theClass.members().get(0);
      assertThat(QuickFixHelper.previousVariable(target)).isEmpty();
    }

    @Test
    void returns_empty_on_separate_variable_declarations() {
      CompilationUnitTree cut = JParserTestUtils.parse("class A { int notRelevant; int target = 42; }");
      ClassTree theClass = (ClassTree) cut.types().get(0);
      VariableTree target = (VariableTree) theClass.members().get(1);
      assertThat(QuickFixHelper.previousVariable(target)).isEmpty();
    }

    @Test
    void returns_previous_on_2_variable_declaration() {
      CompilationUnitTree cut = JParserTestUtils.parse("class A { int previous, target = 42; }");
      ClassTree theClass = (ClassTree) cut.types().get(0);
      VariableTree target = (VariableTree) theClass.members().get(1);
      VariableTree previous = (VariableTree) theClass.members().get(0);
      assertThat(QuickFixHelper.previousVariable(target)).contains(previous);
    }

    @Test
    void returns_previous_on_3_variable_declaration() {
      CompilationUnitTree cut = JParserTestUtils.parse("class A { int first, previous, target; }");
      ClassTree theClass = (ClassTree) cut.types().get(0);
      VariableTree target = (VariableTree) theClass.members().get(2);
      VariableTree previous = (VariableTree) theClass.members().get(1);
      assertThat(QuickFixHelper.previousVariable(target)).contains(previous);
    }
  }

  @Nested
  class Imports {

    /**
     * Can only happen in a package-info file
     */
    @Test
    void no_imports() {
      String source = "package org.foo;";

      JavaFileScannerContext context = mockContext(source);
      ImportSupplier supplier = QuickFixHelper.newImportSupplier(context);

      assertThat(supplier.requiresImportOf("org.foo.A")).isFalse();
      assertThat(supplier.requiresImportOf("org.bar.A")).isTrue();
    }

    @Test
    void imported_via_star_import() {
      String source = "package org.foo;\n"
        + "import java.util.*;\n"
        + "import org.bar.B;\n"
        + "import static java.util.function.Function.identity;\n"
        + "class A { }";

      JavaFileScannerContext context = mockContext(source);
      ImportSupplier supplier = QuickFixHelper.newImportSupplier(context);

      assertThat(supplier.requiresImportOf("org.foo.B")).isFalse();
      assertThat(supplier.requiresImportOf("java.util.List")).isFalse();
      assertThat(supplier.requiresImportOf("java.util.Collections")).isFalse();

      // requires import
      assertThat(supplier.requiresImportOf("org.bar.A")).isTrue();
      assertThat(supplier.requiresImportOf("java.util.function.Function")).isTrue();
    }

    @Test
    void imported_via_explicit_import() {
      String source = "package org.foo;\n"
        + "import java.util.List;\n"
        + "import org.bar.B;\n"
        + "import static java.util.function.Function.identity;\n"
        + "class A { }";

      JavaFileScannerContext context = mockContext(source);
      ImportSupplier supplier = QuickFixHelper.newImportSupplier(context);

      assertThat(supplier.requiresImportOf("org.foo.B")).isFalse();
      assertThat(supplier.requiresImportOf("java.util.List")).isFalse();

      // requires import
      assertThat(supplier.requiresImportOf("org.bar.A")).isTrue();
      assertThat(supplier.requiresImportOf("java.util.Collections")).isTrue();
    }

    @Test
    void default_package() {
      String source = "import java.util.List;\n"
        + "import org.bar.B;\n"
        + "class A { }";

      JavaFileScannerContext context = mockContext(source);
      ImportSupplier supplier = QuickFixHelper.newImportSupplier(context);

      assertThat(supplier.requiresImportOf("java.util.List")).isFalse();
      assertThat(supplier.requiresImportOf("org.bar.B")).isFalse();

      // requires import
      assertThat(supplier.requiresImportOf("org.foo.B")).isTrue();
      assertThat(supplier.requiresImportOf("org.bar.A")).isTrue();
      assertThat(supplier.requiresImportOf("java.util.Collections")).isTrue();
    }

    @Test
    void import_insertion_are_computed_only_once() {
      String source = "package org.foo;\n"
        + "class A { }";

      JavaFileScannerContext context = mockContext(source);
      ImportSupplier supplier = QuickFixHelper.newImportSupplier(context);

      assertThat(supplier.newImportEdit("java.util.List"))
        .isSameAs(supplier.newImportEdit("java.util.List"))
        .isPresent();

      assertThat(supplier.newImportEdit("org.foo.B"))
        .isSameAs(supplier.newImportEdit("org.foo.B"))
        .isNotPresent();
    }

    @Test
    void type_already_imported_does_not_require_edits() {
      String source = "package org.foo;\n"
        + "import java.util.List;\n"
        + "import java.util.function.*;\n"
        + "import static java.util.function.Function.identity;\n"
        + "class A { }";

      JavaFileScannerContext context = mockContext(source);
      ImportSupplier supplier = QuickFixHelper.newImportSupplier(context);

      assertThat(supplier.newImportEdit("java.util.List")).isNotPresent();
      assertThat(supplier.newImportEdit("java.util.function.Function")).isNotPresent();
      assertThat(supplier.newImportEdit("org.foo.B")).isNotPresent();
    }

    @Test
    void import_inserted_middle_other_imports() {
      String source = "package org.foo;\n"
        + "import java.util.List;\n"
        + "import org.bar.B;\n"
        + "import static java.util.function.Function.identity;\n"
        + "class A { }";

      JavaFileScannerContext context = mockContext(source);
      ImportSupplier supplier = QuickFixHelper.newImportSupplier(context);

      Optional<JavaTextEdit> returnedValueSet = supplier.newImportEdit("java.util.Set");
      assertThat(returnedValueSet).isPresent();
      JavaTextEdit javaUtilSetEdit = returnedValueSet.get();
      assertThat(javaUtilSetEdit.getReplacement()).isEqualTo("\nimport java.util.Set;");
      assertThat(javaUtilSetEdit.getTextSpan().startLine).isEqualTo(2);
      assertThat(javaUtilSetEdit.getTextSpan().startCharacter).isEqualTo(22);

      Optional<JavaTextEdit> returnedValueAnimal = supplier.newImportEdit("org.bar.Animal");
      assertThat(returnedValueAnimal).isPresent();
      JavaTextEdit orgBarAnimal = returnedValueAnimal.get();
      assertThat(orgBarAnimal.getReplacement()).isEqualTo("\nimport org.bar.Animal;");
      assertThat(orgBarAnimal.getTextSpan().startLine).isEqualTo(2);
      assertThat(orgBarAnimal.getTextSpan().startCharacter).isEqualTo(22);
    }

    @Test
    void import_inserted_on_top_of_other_imports() {
      String source = "package org.foo;\n"
        + "import java.util.List;\n"
        + "import org.bar.B;\n"
        + "class A { }";

      JavaFileScannerContext context = mockContext(source);
      ImportSupplier supplier = QuickFixHelper.newImportSupplier(context);

      Optional<JavaTextEdit> returnedValue = supplier.newImportEdit("a.b.C");
      assertThat(returnedValue).isPresent();
      JavaTextEdit abcEdit = returnedValue.get();
      assertThat(abcEdit.getReplacement()).isEqualTo("import a.b.C;\n");
      assertThat(abcEdit.getTextSpan().startLine).isEqualTo(2);
      assertThat(abcEdit.getTextSpan().startCharacter).isZero();
    }

    @Test
    void import_inserted_after_other_imports() {
      String source = "package org.foo;\n"
        + "import java.util.List;\n"
        + "import org.bar.B;\n"
        + "class A { }";

      JavaFileScannerContext context = mockContext(source);
      ImportSupplier supplier = QuickFixHelper.newImportSupplier(context);

      Optional<JavaTextEdit> returnedValue = supplier.newImportEdit("z.z.Z");
      assertThat(returnedValue).isPresent();
      JavaTextEdit zzzEdit = returnedValue.get();
      assertThat(zzzEdit.getReplacement()).isEqualTo("\nimport z.z.Z;");
      assertThat(zzzEdit.getTextSpan().startLine).isEqualTo(3);
      assertThat(zzzEdit.getTextSpan().startCharacter).isEqualTo(17);
    }

    @Test
    void import_inserted_after_package_when_no_imports() {
      String source = "package org.foo;\n"
        + "class A { }";

      JavaFileScannerContext context = mockContext(source);
      ImportSupplier supplier = QuickFixHelper.newImportSupplier(context);

      Optional<JavaTextEdit> returnedValue = supplier.newImportEdit("org.bar.B");
      assertThat(returnedValue).isPresent();
      JavaTextEdit orgBarBEdit = returnedValue.get();
      assertThat(orgBarBEdit.getReplacement()).isEqualTo("\n\nimport org.bar.B;");
      assertThat(orgBarBEdit.getTextSpan().startLine).isEqualTo(1);
      assertThat(orgBarBEdit.getTextSpan().startCharacter).isEqualTo(16);
    }

    @Test
    void import_inserted_before_first_type_when_no_imports() {
      String source = "class A { }";

      JavaFileScannerContext context = mockContext(source);
      ImportSupplier supplier = QuickFixHelper.newImportSupplier(context);

      Optional<JavaTextEdit> returnedValue = supplier.newImportEdit("org.bar.B");
      assertThat(returnedValue).isPresent();
      JavaTextEdit orgBarBEdit = returnedValue.get();
      assertThat(orgBarBEdit.getReplacement()).isEqualTo("import org.bar.B;\n\n");
      assertThat(orgBarBEdit.getTextSpan().startLine).isEqualTo(1);
      assertThat(orgBarBEdit.getTextSpan().startCharacter).isZero();
    }

    @Test
    void import_not_inserted_on_empty_file() {
      String source = "";

      JavaFileScannerContext context = mockContext(source);
      ImportSupplier supplier = QuickFixHelper.newImportSupplier(context);

      assertThat(supplier.newImportEdit("org.bar.B")).isEmpty();
    }

    private JavaFileScannerContext mockContext(String source) {
      JavaFileScannerContext context = Mockito.mock(JavaFileScannerContext.class);
      Mockito.when(context.getTree())
        .thenReturn(JParserTestUtils.parse(source));
      return context;
    }
  }
}
