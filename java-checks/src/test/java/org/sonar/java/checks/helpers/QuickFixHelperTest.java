/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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

import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.mockito.Mockito;
import org.sonar.java.Preconditions;
import org.sonar.java.checks.helpers.QuickFixHelper.ImportSupplier;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.java.reporting.JavaTextEdit;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.InferedTypeTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class QuickFixHelperTest {

  static class VariableWithoutNext implements ArgumentsProvider {
    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) {
      return Stream.of(
        arguments("record A(int target, int ignore) { }"),
        arguments("record A(int ignore, int target) { }"),
        arguments("class A { void f() { int target = 42; } }"),
        arguments("class A { void f() { int ignore, target = 42; } }"),
        arguments("class A { void f() { int target = 42; System.out.println(\"Hello, World!\"); } }"),
        arguments("class A { void f() { int target = 42; int ignore; } }"),
        arguments("class A { int target = 42; int ignore; }"),
        arguments("class A { int target = 42; }"),
        arguments("record A(int ignore1) { final static int ignore2 = 42, target = 0; }"),
        arguments("record A(int ignore1) { final static int ignore2 = 42, target = 0; int ignore3; }"),
        arguments("enum MyEnum { A, B; int ignore, target; }"),
        arguments("enum MyEnum { A, B; int ignore1, target; int ignore2; }"),
        arguments("interface I { int ignore, target; }"),
        arguments("interface I { int ignore1, target; int ignore2; }"),
        arguments("class A { void f() { for (int target; ;); } }"),
        arguments("class A { void f() { for (int ignore, target; ;); } }"),
        arguments("class A { void f(Object ignore) { if (ignore instanceof String target) {} } }"),
        arguments("class A { void f(String... ignoreList) { for(String target : ignoreList) { } } }"),
        arguments("class A { void f() { Function<String, Boolean> ignore = (String target) -> true; } }"),
        arguments("class A { void f() { try {} catch(Exception target) {} } }"),
        arguments("class A { A(int target, int ignore) {} }"),
        arguments("class A { void f(int target, int ignore) {} }"),
        arguments("public @interface A { String target = \"\"; int ignore = 2; }"),
        arguments("class A { static { int target = 42; }}"),
        arguments("class A { { int target = 42; } }")
      );
    }
  }

  static class VariableWithoutPrevious implements ArgumentsProvider {
    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) {
      return Stream.of(
        arguments("record A(int target, int ignore) { }"),
        arguments("record A(int ignore, int target) { }"),
        arguments("class A { void f() { int target = 42; } }"),
        arguments("class A { void f() { int target = 42, ignore; } }"),
        arguments("class A { void f() { System.out.println(\"Hello, World!\"); int target = 42; } }"),
        arguments("class A { void f() { int ignore; int target = 42; } }"),
        arguments("class A { int ignore; int target = 42; }"),
        arguments("class A { int target = 42; }"),
        arguments("record A(int ignore1) { final static int target = 0, ignore2 = 42; }"),
        arguments("record A(int ignore1) { int ignore2; final static int target = 0, ignore3 = 42; }"),
        arguments("enum MyEnum { A, B; int target, ignore; }"),
        arguments("enum MyEnum { A, B; int ignore1; int target, ignore2; }"),
        arguments("interface I { int target, ignore; }"),
        arguments("interface I { int ignore1; int target, ignore2; }"),
        arguments("class A { void f() { for (int target; ;); } }"),
        arguments("class A { void f() { for (int target, ignore; ;); } }"),
        arguments("class A { void f(Object ignore) { if (ignore instanceof String target) {} } }"),
        arguments("class A { void f(String... ignoreList) { for(String target : ignoreList) { } } }"),
        arguments("class A { void f() { Function<String, Boolean> ignore = (String target) -> true; } }"),
        arguments("class A { void f() { try {} catch(Exception target) {} } }"),
        arguments("class A { A(int ignore, int target) {} }"),
        arguments("class A { void f(int ignore, int target) {} }"),
        arguments("public @interface A { String ignore = \"\"; int target = 2; }"),
        arguments("class A { static { int target = 42; }}"),
        arguments("class A { { int target = 42; } }")
      );
    }
  }

  static class VariableWithPrevious implements ArgumentsProvider {
    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) {
      return Stream.of(
        arguments("class A { int previous, target = 42; }"),
        arguments("class A { int ignore, previous, target = 42; }"),
        arguments("class A { static { int previous = 12, target; } }"),
        arguments("class A { { int previous, target; } }"),
        arguments("public @interface A { String previous = \"\", target = \"1\"; }"),
        arguments("class A { void f() { int previous, target = 42; } }"),
        arguments("class A { void f() { int ignore, previous, target; } }"),
        arguments("record A(int ignore1, int ignore2) { final static int previous = 42, target = 0; }"),
        arguments("enum MyEnum { A, B; int previous, target; }"),
        arguments("interface I { int previous, target; }"),
        arguments("class A { void f() { for (int previous, target; ;) { } } }"),
        arguments("class A { void f(int ignore) { switch(ignore) { case 1: int previous, target; } } }")
      );
    }
  }

  static class VariableWithNext implements ArgumentsProvider {
    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) {
      return Stream.of(
        arguments("class A { int target, next = 42; }"),
        arguments("class A { int ignore, target, next = 42; }"),
        arguments("class A { int target, next, ignore = 42; }"),
        arguments("class A { static { int target, next; } }"),
        arguments("class A { { int target, next; } }"),
        arguments("public @interface A { String target = \"1\", next = \"\"; }"),
        arguments("class A { void f() { int target = 42, next; } }"),
        arguments("class A { void f() { int target, next, ignore; } }"),
        arguments("record A(int ignore1, int ignore2) { final static int target = 0, next = 42; }"),
        arguments("enum MyEnum { A, B; int target, next; }"),
        arguments("interface I { int target, next; }"),
        arguments("class A { void f() { for (int target, next; ;) { } } }"),
        arguments("class A { void f(int ignore) { switch(ignore) { case 1: int target, next; } } }")
      );
    }
  }

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

  @Test
  void content_for_empty_token() {
    String content = QuickFixHelper.contentForTree(new InferedTypeTree(), mock(JavaFileScannerContext.class));
    assertThat(content).isEmpty();
  }

  @Nested
  class NextVariable {

    @ParameterizedTest
    @ArgumentsSource(VariableWithNext.class)
    void returns_next(String source) {
      CompilationUnitTree cut = JParserTestUtils.parse(source);
      VariableExtractor extractor = new VariableExtractor();
      cut.accept(extractor);
      assertThat(QuickFixHelper.nextVariable(extractor.target)).contains(extractor.next);
    }

    @ParameterizedTest
    @ArgumentsSource(VariableWithoutNext.class)
    void returns_empty(String source) {
      CompilationUnitTree cut = JParserTestUtils.parse(source);
      VariableExtractor extractor = new VariableExtractor();
      cut.accept(extractor);
      assertThat(QuickFixHelper.nextVariable(extractor.target)).isEmpty();
    }

    @Test
    void throws_an_illegal_argument_exception_when_parent_type_is_not_supported() {
      Tree parent = mock(LiteralTree.class);
      VariableTree variable = mock(VariableTree.class);
      when(variable.parent()).thenReturn(parent);
      when(parent.kind()).thenReturn(Tree.Kind.STRING_LITERAL);

      assertThatThrownBy(() -> QuickFixHelper.nextVariable(variable))
        .isInstanceOfAny(IllegalArgumentException.class)
        .hasMessageContaining("The variable's parent kind STRING_LITERAL is not handled by this method!");
    }

  }

  @Nested
  class PreviousVariable {

    @ParameterizedTest
    @ArgumentsSource(VariableWithPrevious.class)
    void returns_previous(String source) {
      CompilationUnitTree cut = JParserTestUtils.parse(source);
      VariableExtractor extractor = new VariableExtractor();
      cut.accept(extractor);
      assertThat(QuickFixHelper.previousVariable(extractor.target)).contains(extractor.previous);
    }

    @ParameterizedTest
    @ArgumentsSource(VariableWithoutPrevious.class)
    void returns_empty(String source) {
      CompilationUnitTree cut = JParserTestUtils.parse(source);
      VariableExtractor extractor = new VariableExtractor();
      cut.accept(extractor);
      assertThat(QuickFixHelper.previousVariable(extractor.target)).isEmpty();
    }

    @Test
    void throws_an_illegal_argument_exception_when_parent_type_is_not_supported() {
      Tree parent = mock(LiteralTree.class);
      VariableTree variable = mock(VariableTree.class);
      when(variable.parent()).thenReturn(parent);
      when(parent.kind()).thenReturn(Tree.Kind.STRING_LITERAL);

      assertThatThrownBy(() -> QuickFixHelper.previousVariable(variable))
        .isInstanceOfAny(IllegalArgumentException.class)
        .hasMessageContaining("The variable's parent kind STRING_LITERAL is not handled by this method!");
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

      assertThat(supplier.requiresImportOf("B")).isFalse();
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


  static class VariableExtractor extends BaseTreeVisitor {
    VariableTree target;
    VariableTree next;
    VariableTree previous;
    @Override
    public void visitVariable(VariableTree tree) {
      super.visitVariable(tree);
      String name = tree.simpleName().name();
      if (name.equals("target")) {
        Preconditions.checkState(target == null);
        target = tree;
      } else if (name.equals("next")) {
        Preconditions.checkState(next == null);
        next = tree;
      } else if (name.equals("previous")) {
        Preconditions.checkState(previous == null);
        previous = tree;
      } else if (!name.startsWith("ignore")) {
        throw new IllegalStateException(name);
      }
    }
  }

}
