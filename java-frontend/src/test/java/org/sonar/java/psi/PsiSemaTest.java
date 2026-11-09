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
package org.sonar.java.psi;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.sonar.java.model.JParserConfig;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PsiSemaTest {
  @Test
  void basic_types_and_selected_runtime_lookup() {
    try (var fixture = open("")) {
      var sema = fixture.tree().sema;
      assertThat(sema.getClassType("java.lang.String")).isSameAs(sema.getClassType("java.lang.String"));
      assertThat(sema.getClassType("java.util.Map$Entry[][]").isArray()).isTrue();
      assertThat(sema.getClassType("missing.Type")).isSameAs(Type.UNKNOWN);
      assertThat(sema.getClassType("int").primitiveWrapperType().is("java.lang.Integer")).isTrue();
      assertThat(sema.getClassType("java.lang.Integer").primitiveType().is("int")).isTrue();
      assertThat(sema.getClassType("int").isSubtypeOf("long")).isFalse();
      assertThat(sema.getClassType("java.lang.String").isSubtypeOf("java.lang.Object")).isTrue();
    }
  }

  @Test
  void declarations_usages_and_generic_calls() {
    try (var fixture = open("class A { <T> T id(T t) { return t; } void use() { String s = id(\"x\"); } }")) {
      var cls = (ClassTree) fixture.tree().types().getFirst();
      var method = (MethodTree) cls.members().getFirst();
      assertThat(cls.symbol().declaration()).isSameAs(cls);
      assertThat(method.symbol().declaration()).isSameAs(method);
      assertThat(method.symbol().parameterTypes()).extracting(Type::name).containsExactly("T");
      assertThat(method.symbol().signature()).isEqualTo("A#id(Ljava/lang/Object;)Ljava/lang/Object;");
      assertThat(method.parameters().getFirst().symbol().usages()).hasSize(1);
      assertThat(method.parameters().getFirst().symbol().owner()).isSameAs(method.symbol());
    }
  }

  @Test
  void recursive_bounds_and_substituted_hierarchies() {
    try (var fixture = open("class A<T extends Comparable<T>> extends java.util.ArrayList<String> {}")) {
      var cls = (ClassTree) fixture.tree().types().getFirst();
      assertThat(cls.symbol().superClass().typeArguments()).extracting(Type::fullyQualifiedName).containsExactly("java.lang.String");
      assertThat(cls.symbol().superTypes()).anyMatch(t -> t.is("java.util.List"));
      assertThat(cls.symbol().type()).isSameAs(cls.symbol().type());
    }
  }

  @Test
  void constants_and_metadata() {
    try (var fixture = open("@Deprecated class A { static final char C = 'a'; @Deprecated String value; }")) {
      var cls = (ClassTree) fixture.tree().types().getFirst();
      var constant = (VariableTree) cls.members().getFirst();
      assertThat(((Symbol.VariableSymbol) constant.symbol()).constantValue()).contains(97);
      assertThat(cls.symbol().metadata().isAnnotatedWith("java.lang.Deprecated")).isTrue();
      assertThat(((VariableTree) cls.members().get(1)).symbol().metadata().isAnnotatedWith("java.lang.Deprecated")).isTrue();
    }
  }

  @Test
  void lazy_queries_are_owned_by_the_environment() {
    var fixture = open("class A {}");
    var cls = (ClassTree) fixture.tree().types().getFirst();
    var sema = fixture.tree().sema;
    assertThat(fixture.tree().getEnvironmentCleaner()).isSameAs(fixture.tree().getEnvironmentCleaner());
    assertThat(cls.symbol().name()).isEqualTo("A");
    fixture.close();
    fixture.close();
    assertThatThrownBy(() -> cls.symbol().name()).isInstanceOf(IllegalStateException.class).hasMessage("PSI environment is closed");
  }

  @Test
  void generic_invocations_retain_substitutions_and_share_declarations() {
    try (var fixture = open("class A { <T> T id(T p) { return p; } void use() { id(\"one\"); id(\"two\"); id(42); } }")) {
      var calls = calls(fixture.tree());
      assertThat(calls).hasSize(3);
      var first = calls.getFirst().methodSymbol();
      assertThat(first).isSameAs(calls.get(1).methodSymbol()).isNotEqualTo(calls.get(2).methodSymbol());
      assertThat(first.parameterTypes()).extracting(Type::fullyQualifiedName).containsExactly("java.lang.String");
      assertThat(first.declarationParameters()).extracting(s -> s.type().name()).containsExactly("T");
      assertThat(first.declaration()).isSameAs(calls.get(2).methodSymbol().declaration());
      assertThat(first.usages()).hasSize(3);
    }
  }

  @Test
  void multi_file_resolution_keeps_declarations_and_usages_file_local() {
    var sources = List.of(source("A.java", "class A { void use(B b) { b.id(\"one\"); } }"),
      source("B.java", "class B { <T> T id(T value) { return value; } }"));
    try (var fixture = PsiSemanticTestSupport.open(sources, List.of(), JParserConfig.MAXIMUM_SUPPORTED_JAVA_VERSION)) {
      var first = calls(fixture.trees().getFirst()).getFirst().methodSymbol();
      var second = ((MethodTree) ((ClassTree) fixture.trees().get(1).types().getFirst()).members().getFirst()).symbol();
      assertThat(first.isUnknown()).isFalse();
      assertThat(first.declaration()).isNull();
      assertThat(first.usages()).hasSize(1);
      assertThat(second.declaration()).isNotNull();
      assertThat(second.usages()).isEmpty();
      assertThat(first.declarationParameters()).extracting(Symbol::name).containsExactly("arg0");
      assertThat(first.declarationParameters().getFirst().declaration()).isNull();
      assertThat(fixture.trees().getFirst().getEnvironmentCleaner()).isSameAs(fixture.trees().get(1).getEnvironmentCleaner());
    }
  }

  @Test
  void unicode_normalization_preserves_resolution_and_physical_ranges() {
    try (var fixture = open("class A { String \\u0076 = \"x\"; String use() { return \\u0076; } }")) {
      var cls = (ClassTree) fixture.tree().types().getFirst();
      var field = (VariableTree) cls.members().getFirst();
      assertThat(field.symbol().name()).isEqualTo("v");
      assertThat(field.symbol().usages()).hasSize(1);
      assertThat(field.symbol().usages().getFirst().symbol()).isSameAs(field.symbol());
      assertThat(field.symbol().usages().getFirst().identifierToken().text()).isEqualTo("\\u0076");
      assertThat(field.symbol().usages().getFirst().identifierToken().range().end().columnOffset()
        - field.symbol().usages().getFirst().identifierToken().range().start().columnOffset()).isEqualTo(6);
    }
  }

  @Test
  void repaired_resource_declarations_keep_their_enclosing_resolution_context() {
    try (var fixture = open("class A { void use(java.io.Closeable[] input) { try (java.io.Closeable resource[] = input) { resource.toString(); } } }")) {
      var cls = (ClassTree) fixture.tree().types().getFirst();
      var method = (MethodTree) cls.members().getFirst();
      var tried = (org.sonar.plugins.java.api.tree.TryStatementTree) method.block().body().getFirst();
      var resource = (VariableTree) tried.resourceList().getFirst();
      assertThat(resource.type().symbolType().isArray()).isTrue();
      assertThat(resource.symbol().owner()).isSameAs(method.symbol());
      assertThat(method.parameters().getFirst().symbol().usages()).hasSize(1);
    }
  }

  @Test
  void suppressing_the_semantic_model_retains_existing_tree_attachments() {
    try (var fixture = open("class A { int value; }")) {
      var cls = (ClassTree) fixture.tree().types().getFirst();
      fixture.tree().sema = null;
      assertThat(cls.symbol().name()).isEqualTo("A");
      assertThat(((VariableTree) cls.members().getFirst()).type().symbolType().is("int")).isTrue();
    }
  }

  @Test
  void package_metadata_selects_binary_info_instead_of_source_overlay() throws Exception {
    var sources = List.of(source("package-info.java", "@Deprecated package org.sonar.java.resolve.targets;"),
      source("A.java", "package org.sonar.java.resolve.targets; class A {}"));
    var annotations = new java.io.File(javax.annotation.ParametersAreNonnullByDefault.class.getProtectionDomain().getCodeSource().getLocation().toURI());
    try (var fixture = PsiSemanticTestSupport.open(sources, List.of(new java.io.File("target/test-classes"), annotations), JParserConfig.MAXIMUM_SUPPORTED_JAVA_VERSION)) {
      var cls = (ClassTree) fixture.trees().get(1).types().getFirst();
      assertThat(cls.symbol().owner().metadata().annotations()).hasSize(1);
      assertThat(cls.symbol().owner().metadata().annotations()).extracting(a -> a.symbol().type().fullyQualifiedName())
        .containsExactly("javax.annotation.ParametersAreNonnullByDefault");
      assertThat(cls.symbol().owner().metadata().isAnnotatedWith("java.lang.Deprecated")).isFalse();
    }
  }

  @Test
  void explicit_runtime_archive_excludes_the_host_runtime(@org.junit.jupiter.api.io.TempDir Path directory) throws Exception {
    var runtime = directory.resolve("rt.jar");
    try (var output = new java.util.jar.JarOutputStream(java.nio.file.Files.newOutputStream(runtime))) {
      output.finish();
    }
    try (var fixture = PsiSemanticTestSupport.open("A.java", "interface A {}", List.of(runtime.toFile()), JParserConfig.MAXIMUM_SUPPORTED_JAVA_VERSION)) {
      var sema = fixture.tree().sema;
      assertThat(sema.getClassType("java.lang.String")).isSameAs(Type.UNKNOWN);
      assertThat(sema.getClassType("int").primitiveWrapperType()).isNull();
      assertThat(((ClassTree) fixture.tree().types().getFirst()).symbol().superClass()).isSameAs(Type.UNKNOWN);
    }
  }

  @Test
  void canonical_factories_are_isolated_between_sessions() {
    try (var left = open("class A<T> {}" ); var right = open("class A<T> {}")) {
      var leftType = ((ClassTree) left.tree().types().getFirst()).symbol().type();
      var rightType = ((ClassTree) right.tree().types().getFirst()).symbol().type();
      assertThat(leftType).isNotSameAs(rightType).isEqualTo(rightType).hasSameHashCodeAs(rightType);
      assertThat(leftType.symbol()).isNotSameAs(rightType.symbol()).isEqualTo(rightType.symbol());
      left.close();
      assertThat(rightType.name()).isEqualTo("A");
      assertThat(right.tree().sema.getClassType("java.lang.String").name()).isEqualTo("String");
    }
  }

  @Test
  void capture_identity_includes_parameter_and_context() {
    try (var fixture = open("class Pair<A, B> {}")) {
      var sema = (PsiSema) fixture.tree().sema;
      sema.read(() -> {
        var cls = sema.file.getClasses()[0];
        var wildcard = com.intellij.psi.PsiWildcardType.createUnbounded(com.intellij.psi.PsiManager.getInstance(sema.project));
        var first = com.intellij.psi.PsiCapturedWildcardType.create(wildcard, cls, cls.getTypeParameters()[0]);
        var second = com.intellij.psi.PsiCapturedWildcardType.create(wildcard, cls, cls.getTypeParameters()[1]);
        var otherContext = com.intellij.psi.PsiCapturedWildcardType.create(wildcard, cls.getNameIdentifier(), cls.getTypeParameters()[0]);
        assertThat(sema.type(first)).isSameAs(sema.type(first)).isNotEqualTo(sema.type(second)).isNotEqualTo(sema.type(otherContext));
        return null;
      });
    }
  }

  private static List<MethodInvocationTree> calls(Tree root) {
    List<MethodInvocationTree> result = new ArrayList<>();
    root.accept(new BaseTreeVisitor() {
      @Override
      public void visitMethodInvocation(MethodInvocationTree tree) {
        result.add(tree);
        super.visitMethodInvocation(tree);
      }
    });
    return result;
  }

  private static JavaPsiEnvironment.SourceSnapshot source(String name, String contents) {
    return new JavaPsiEnvironment.SourceSnapshot(Path.of(name), contents, StandardCharsets.UTF_8);
  }

  private static PsiSemanticTestSupport open(String source) {
    return PsiSemanticTestSupport.open("File.java", source, List.of(), JParserConfig.MAXIMUM_SUPPORTED_JAVA_VERSION);
  }
}
