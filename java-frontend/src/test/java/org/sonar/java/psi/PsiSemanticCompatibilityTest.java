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
import org.sonar.java.model.JavaTree;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodReferenceTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.VariableTree;

import static org.assertj.core.api.Assertions.assertThat;

class PsiSemanticCompatibilityTest {
  @Test
  void unknown_class_literals_preserve_reflection_calls_but_unknown_generic_receivers_do_not() {
    var tree = parse("""
      class A {
        void use(java.util.List<Missing> values, Class<Missing> type) throws ReflectiveOperationException {
          Missing.class.getField("name").getName();
          type.getField("name").getName();
          values.size();
        }
      }
      """);
    try {
      var calls = calls(tree);
      assertThat(calls.getFirst().methodSymbol().signature()).isEqualTo("java.lang.reflect.Field#getName()Ljava/lang/String;");
      assertThat(calls.get(1).methodSymbol().signature()).isEqualTo("java.lang.Class#getField(Ljava/lang/String;)Ljava/lang/reflect/Field;");
      assertThat(calls.subList(2, 5)).allSatisfy(call -> assertThat(call.methodSymbol().isUnknown()).isTrue());
    } finally {
      tree.getEnvironmentCleaner().run();
    }
  }

  @Test
  void declaration_errors_skip_flow_analysis_only_in_the_affected_scope() {
    var sources = List.of(
      "class A { @missing.A int field; void use() { int value=0; value=1; } }",
      "class A { @Missing int field; void use() { int value=0; value=1; } }",
      "@Missing class A { void use() { int value=0; value=1; } }",
      "class A { @Missing void use() { int value=0; value=1; } }",
      "class A { Missing field; void use() { int value=0; value=1; } }",
      "class A { void use() { missing(); int value=0; value=1; } }");
    for (var source : sources) {
      var tree = parse(source);
      try {
        var variable = variables(tree).stream().filter(v -> v.simpleName().name().equals("value")).findFirst().orElseThrow();
        assertThat(((Symbol.VariableSymbol) variable.symbol()).isEffectivelyFinal()).as(source).isTrue();
      } finally {
        tree.getEnvironmentCleaner().run();
      }
    }
    var tree = parse("class A { void other() { missing(); } void use() { int value=0; value=1; } }");
    try {
      assertThat(((Symbol.VariableSymbol) variables(tree).getFirst().symbol()).isEffectivelyFinal()).isFalse();
    } finally {
      tree.getEnvironmentCleaner().run();
    }
  }

  @Test
  void invalid_void_return_preserves_effectively_final_recovery() {
    var tree = parse("""
      class A {
        void use() {
          if (Math.random() > 0.5) { return "invalid"; }
          Object value = null;
          if (Math.random() > 0.5) { value = "changed"; }
        }
      }
      """);
    try {
      assertThat(((Symbol.VariableSymbol) variables(tree).getFirst().symbol()).isEffectivelyFinal()).isTrue();
    } finally {
      tree.getEnvironmentCleaner().run();
    }
  }

  @Test
  void unresolved_lambda_bodies_have_no_symbols_types_or_usages() {
    var tree = parse("""
      class A {
        void use() {
          int value = 1;
          missing(() -> { int local = 1; java.time.Instant.now(); System.out.println(value); });
          Runnable valid = () -> { java.time.Instant.now(); System.out.println(value); };
        }
      }
      """);
    try {
      var calls = calls(tree);
      assertThat(calls).extracting(call -> call.methodSymbol().isUnknown()).containsExactly(true, true, true, false, false);
      assertThat(calls.subList(0, 3)).allSatisfy(call -> assertThat(call.symbolType().isUnknown()).isTrue());
      var variables = variables(tree);
      assertThat(variables.getFirst().symbol().usages()).hasSize(1);
      assertThat(variables.get(1).symbol().isUnknown()).isTrue();
      assertThat(variables.get(1).type().symbolType().isUnknown()).isTrue();
    } finally {
      tree.getEnvironmentCleaner().run();
    }
  }

  @Test
  void incompatible_generic_target_preserves_the_method_but_loses_the_expression_type() {
    var tree = parse("""
      import java.util.stream.*;
      @interface Val {}
      class A { void use() { Val value = Stream.of("A", "B").collect(Collectors.toList()); } }
      """);
    try {
      var call = calls(tree).getFirst();
      assertThat(call.methodSymbol().name()).isEqualTo("collect");
      assertThat(call.methodSymbol().returnType().type().is("java.util.List")).isTrue();
      assertThat(call.symbolType().isUnknown()).isTrue();
    } finally {
      tree.getEnvironmentCleaner().run();
    }
  }

  @Test
  void raw_receiver_erases_array_return_components() {
    var tree = parse("class A { void use(java.lang.reflect.Constructor constructor) { constructor.getParameterTypes(); } }");
    try {
      var result = calls(tree).getFirst().methodSymbol().returnType().type();
      var component = ((Type.ArrayType) result).elementType();
      assertThat(component.isRawType()).isTrue();
      var parent = variables(tree).getFirst().symbol().type().symbol().superClass();
      var inherited = (Symbol.MethodSymbol) parent.symbol().lookupSymbols("getParameterTypes").iterator().next();
      assertThat(inherited.returnType().type()).isNotSameAs(result);
      assertThat(((Type.ArrayType) inherited.returnType().type()).elementType().isParameterized()).isTrue();
    } finally {
      tree.getEnvironmentCleaner().run();
    }
  }

  @Test
  void inapplicable_overloads_recover_in_declaration_order() {
    var tree = parse("class A { void pick(long n) {} void pick(String s) {} void use() { pick(new Object()); } }");
    try {
      assertThat(calls(tree).getFirst().methodSymbol().signature()).isEqualTo("A#pick(J)V");
    } finally {
      tree.getEnvironmentCleaner().run();
    }
  }

  @Test
  void unknown_constructor_argument_keeps_the_matching_arity() {
    var tree = parse("class A { Object value = new javax.crypto.spec.IvParameterSpec(MISSING); }");
    try {
      var creation = (NewClassTree) ((VariableTree) ((ClassTree) tree.types().getFirst()).members().getFirst()).initializer();
      assertThat(creation.methodSymbol().signature()).isEqualTo("javax.crypto.spec.IvParameterSpec#<init>([B)V");
    } finally {
      tree.getEnvironmentCleaner().run();
    }
  }

  @Test
  void incompatible_constructor_reference_arity_stays_unknown() {
    var tree = parse("class A { A(int value) {} java.util.function.Supplier<A> factory = A::new; }");
    try {
      var reference = (MethodReferenceTree) ((VariableTree) ((ClassTree) tree.types().getFirst()).members().get(1)).initializer();
      assertThat(reference.method().symbol().isUnknown()).isTrue();
    } finally {
      tree.getEnvironmentCleaner().run();
    }
  }

  @Test
  void constructor_references_preserve_inferred_type_arguments() {
    var tree = parse("""
      import java.util.*;
      import java.util.function.*;
      import java.util.stream.*;
      class A {
        Map<Integer, List<Object>> values = Stream.of(1, 2, 3)
          .collect(Collectors.toMap(Function.identity(), ArrayList::new));
      }
      """);
    try {
      var references = new ArrayList<MethodReferenceTree>();
      tree.accept(new BaseTreeVisitor() {
        @Override
        public void visitMethodReference(MethodReferenceTree reference) {
          references.add(reference);
          super.visitMethodReference(reference);
        }
      });
      assertThat(((Symbol.MethodSymbol) references.getFirst().method().symbol()).signature()).isEqualTo("java.util.ArrayList#<init>(I)V");
    } finally {
      tree.getEnvironmentCleaner().run();
    }
  }

  @Test
  void incompatible_method_reference_return_type_stays_unknown() {
    var tree = parse("""
      class A { java.util.function.Function<java.io.InputStream, String> value = java.io.InputStreamReader::new; }
      """);
    try {
      var reference = (MethodReferenceTree) ((VariableTree) ((ClassTree) tree.types().getFirst()).members().getFirst()).initializer();
      assertThat(reference.method().symbol().isUnknown()).isTrue();
    } finally {
      tree.getEnvironmentCleaner().run();
    }
  }

  private static JavaTree.CompilationUnitTreeImpl parse(String source) {
    var config = JParserConfig.Mode.FILE_BY_FILE.create(JParserConfig.MAXIMUM_SUPPORTED_JAVA_VERSION, List.of());
    return config.parse("A.java", source);
  }

  private static List<MethodInvocationTree> calls(JavaTree.CompilationUnitTreeImpl tree) {
    var calls = new ArrayList<MethodInvocationTree>();
    tree.accept(new BaseTreeVisitor() {
      @Override
      public void visitMethodInvocation(MethodInvocationTree call) {
        calls.add(call);
        super.visitMethodInvocation(call);
      }
    });
    return calls;
  }

  private static List<VariableTree> variables(JavaTree.CompilationUnitTreeImpl tree) {
    var variables = new ArrayList<VariableTree>();
    tree.accept(new BaseTreeVisitor() {
      @Override
      public void visitVariable(VariableTree variable) {
        variables.add(variable);
        super.visitVariable(variable);
      }
    });
    return variables;
  }

  @Test
  void compact_constructor_parameters_belong_to_the_explicit_constructor() {
    String source = "record R(int value) { R { value = 42; } }";
    var tree = PsiParserConfig.create(JParserConfig.Mode.FILE_BY_FILE, JParserConfig.MAXIMUM_SUPPORTED_JAVA_VERSION, List.of(), false).parse("R.java", source);
    try {
      var constructor = (MethodTree) ((ClassTree) tree.types().getFirst()).members().getFirst();
      var assignment = (AssignmentExpressionTree) ((ExpressionStatementTree) constructor.block().body().getFirst()).expression();
      var symbol = ((IdentifierTree) assignment.variable()).symbol();
      assertThat(symbol.isParameter()).isTrue();
      assertThat(symbol.owner()).isSameAs(constructor.symbol());
      assertThat(symbol.name()).isEqualTo("value");
    } finally {
      tree.getEnvironmentCleaner().run();
    }
  }
}
