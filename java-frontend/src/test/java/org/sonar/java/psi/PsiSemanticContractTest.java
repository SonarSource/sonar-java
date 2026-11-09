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

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.sonar.java.model.JParserConfig;
import org.sonar.java.model.JParserTestUtils;
import org.sonar.java.model.JavaTree;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.VariableTree;

import static org.assertj.core.api.Assertions.assertThat;

class PsiSemanticContractTest {
  private final java.util.List<PsiSemanticTestSupport> fixtures = new java.util.ArrayList<>();

  @org.junit.jupiter.api.AfterEach
  void close() {
    fixtures.forEach(PsiSemanticTestSupport::close);
  }

  @Test
  void implicit_constructor_has_a_semantic_member_without_a_source_member() {
    var tree = parse("class A { A value = new A(); }");
    var cls = (ClassTree) tree.types().getFirst();
    var creation = (NewClassTree) ((VariableTree) cls.members().getFirst()).initializer();
    var constructor = (Symbol.MethodSymbol) creation.identifier().symbolType().symbol().lookupSymbols("<init>").iterator().next();
    var called = (Symbol.MethodSymbol) ((IdentifierTree) creation.identifier()).symbol();
    assertThat(called.isUnknown()).isFalse();
    assertThat(called.name()).isEqualTo("<init>");
    assertThat(called.signature()).isEqualTo("A#<init>()V");
    assertThat(called.returnType().type().isVoid()).isTrue();
    assertThat(called.declaration()).isNull();
    assertThat(constructor).isEqualTo(called);
    assertThat(cls.members()).hasSize(1);
  }

  @Test
  void array_length_has_a_final_variable_symbol_and_unknown_owner_type() {
    var tree = parse("class A { int size(String[] values) { return values.length; } }");
    var select = new ArrayList<MemberSelectExpressionTree>();
    tree.accept(new BaseTreeVisitor() {
      @Override
      public void visitMemberSelectExpression(MemberSelectExpressionTree member) {
        if (member.identifier().name().equals("length")) {
          select.add(member);
        }
        super.visitMemberSelectExpression(member);
      }
    });
    var symbol = select.getFirst().identifier().symbol();
    assertThat(symbol.isVariableSymbol()).isTrue();
    assertThat(symbol.type().is("int")).isTrue();
    assertThat(symbol.isFinal()).isTrue();
    assertThat(symbol.owner().type()).isSameAs(Type.UNKNOWN);
    assertThat(symbol.declaration()).isNull();
  }

  @Test
  void record_accessors_and_compact_constructor_keep_declaration_signatures() {
    var tree = parse("record R(String value) { R { value = value.trim(); } static String use() { return new R(\"x\").value(); } }");
    var cls = (ClassTree) tree.types().getFirst();
    var methods = calls(tree);
    var accessor = methods.stream().map(MethodInvocationTree::methodSymbol).filter(s -> s.name().equals("value")).findFirst().orElseThrow();
    assertThat(accessor.signature()).isEqualTo("R#value()Ljava/lang/String;");
    assertThat(accessor.returnType().type().is("java.lang.String")).isTrue();
    assertThat(accessor.declaration()).isNull();
    var constructor = (MethodTree) cls.members().getFirst();
    assertThat(constructor.symbol().signature()).isEqualTo("R#<init>(Ljava/lang/String;)V");
    assertThat(constructor.symbol().parameterTypes()).extracting(Type::fullyQualifiedName).containsExactly("java.lang.String");
    assertThat(cls.symbol().lookupSymbols("value")).hasSize(2);
  }

  @Test
  void parameterized_owner_and_field_symbols_retain_the_call_context() {
    var tree = parse("class Box<T> { T value; T get() { return value; } static void use(Box<String> b) { String v = b.value; b.get(); } }");
    var cls = (ClassTree) tree.types().getFirst();
    var method = (MethodTree) cls.members().get(1);
    assertThat(method.symbol().owner()).isSameAs(cls.symbol());
    var call = calls(tree).getFirst().methodSymbol();
    assertThat(call.owner().type().typeArguments()).extracting(Type::fullyQualifiedName).containsExactly("java.lang.String");
    assertThat(call.returnType().type().is("java.lang.String")).isTrue();
    var symbols = new ArrayList<Symbol>();
    tree.accept(new BaseTreeVisitor() {
      @Override
      public void visitMemberSelectExpression(MemberSelectExpressionTree member) {
        if (member.identifier().name().equals("value")) {
          symbols.add(member.identifier().symbol());
        }
        super.visitMemberSelectExpression(member);
      }
    });
    assertThat(symbols.getFirst().type().is("java.lang.String")).isTrue();
    assertThat(symbols.getFirst().declaration()).isSameAs(cls.members().getFirst());
    assertThat(symbols.getFirst().enclosingClass()).isSameAs(symbols.getFirst().owner());
  }

  @Test
  void invalid_candidates_do_not_hide_independent_resolved_calls() {
    var tree = parse("class A { void pick(String s) {} void pick(Integer i) {} void use() { pick(null); missing(); \"x\".trim(); } }");
    var methods = calls(tree);
    assertThat(methods.get(0).methodSymbol().signature()).isEqualTo("A#pick(Ljava/lang/String;)V");
    assertThat(methods.get(1).methodSymbol().isUnknown()).isTrue();
    assertThat(methods.get(2).methodSymbol().signature()).isEqualTo("java.lang.String#trim()Ljava/lang/String;");
    assertThat(methods.get(2).symbolType().is("java.lang.String")).isTrue();
  }

  @Test
  void source_package_annotations_are_not_merged_into_package_metadata() {
    var tree = parse("package-info.java", "@Deprecated package sample;");
    assertThat(tree.packageDeclaration().packageName().symbolType().symbol().metadata().annotations()).isEmpty();
  }

  @Test
  void annotation_shorthand_preserves_explicit_arrays_and_scalar_defaults() {
    var tree = parse("""
      @interface Values { String[] value() default "default"; Class<?> type() default String.class; }
      @Values("explicit") class A {}
      """);
    var annotation = (ClassTree) tree.types().getFirst();
    var value = ((MethodTree) annotation.members().getFirst()).symbol().annotationElementDefaultValue();
    assertThat(value).isEqualTo("default");
    var literal = ((MethodTree) annotation.members().get(1)).symbol().annotationElementDefaultValue();
    assertThat(((Symbol.TypeSymbol) literal).type().is("java.lang.String")).isTrue();
    var cls = (ClassTree) tree.types().get(1);
    assertThat((Object[]) cls.symbol().metadata().annotations().getFirst().values().getFirst().value()).containsExactly("explicit");
  }

  @Test
  void inaccessible_and_inapplicable_calls_keep_only_supported_recovery() {
    var tree = parse("""
      class Secret { private String hidden() { return ""; } }
      class A { void accept(String s) {} void use(Secret s) { s.hidden(); accept(1); } }
      """);
    var calls = calls(tree);
    assertThat(calls.getFirst().methodSymbol().signature()).isEqualTo("Secret#hidden()Ljava/lang/String;");
    assertThat(calls.get(1).methodSymbol().signature()).isEqualTo("A#accept(Ljava/lang/String;)V");
  }

  @Test
  void nested_generic_owners_keep_enclosing_substitutions() {
    var tree = parse("""
      class Outer<T> {
        class Inner<U> { U get() { return null; } }
        void use(Outer<String>.Inner<Integer> value) { value.get(); }
      }
      """);
    var method = calls(tree).getFirst().methodSymbol();
    assertThat(method.owner().type().typeArguments()).extracting(Type::fullyQualifiedName).containsExactly("java.lang.Integer");
    assertThat(method.owner().owner().type().typeArguments()).extracting(Type::fullyQualifiedName).containsExactly("java.lang.String");
    assertThat(method.enclosingClass()).isSameAs(method.owner());
  }

  @Test
  void intersection_lambda_targets_preserve_the_functional_signature() {
    var tree = parse("""
      class A { Object value = (java.util.function.Function<String, Integer> & java.io.Serializable) text -> text.length(); }
      """);
    var lambdas = new ArrayList<org.sonar.plugins.java.api.tree.LambdaExpressionTree>();
    tree.accept(new BaseTreeVisitor() {
      @Override
      public void visitLambdaExpression(org.sonar.plugins.java.api.tree.LambdaExpressionTree lambda) {
        lambdas.add(lambda);
        super.visitLambdaExpression(lambda);
      }
    });
    assertThat(lambdas.getFirst().symbol().parameterTypes()).extracting(Type::fullyQualifiedName).containsExactly("java.lang.String");
    assertThat(lambdas.getFirst().symbol().returnType().type().is("java.lang.Integer")).isTrue();
  }

  private JavaTree.CompilationUnitTreeImpl parse(String source) {
    return parse("File.java", source);
  }

  private JavaTree.CompilationUnitTreeImpl parse(String name, String source) {
    var fixture = PsiSemanticTestSupport.open(name, source, JParserTestUtils.DEFAULT_CLASSPATH, JParserConfig.MAXIMUM_SUPPORTED_JAVA_VERSION);
    fixtures.add(fixture);
    return fixture.tree();
  }

  private static List<MethodInvocationTree> calls(CompilationUnitTree tree) {
    var result = new ArrayList<MethodInvocationTree>();
    tree.accept(new BaseTreeVisitor() {
      @Override
      public void visitMethodInvocation(MethodInvocationTree call) {
        result.add(call);
        super.visitMethodInvocation(call);
      }
    });
    return result;
  }
}
