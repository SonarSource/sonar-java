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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.sonar.java.model.JUtils;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.NewArrayTree;
import org.sonar.plugins.java.api.tree.VariableTree;

import static org.assertj.core.api.Assertions.assertThat;

class JavacSemanticRegressionTest {

  @TempDir
  Path directory;

  @Test
  void resolves_type_hierarchy_and_members() throws IOException {
    String source = "interface Parent { void run(); } interface Other {} "
      + "class Outer { class Child extends java.util.ArrayList<String> implements Parent, Other { public void run() {} } }";
    try (JavacParser.Result parsed = JavacParser.parse("Outer.java", source,
      JavacParser.Options.defaults().withAnalysis(true))) {
      JavacSymbols symbols = symbols(parsed);
      Symbol.TypeSymbol child = (Symbol.TypeSymbol) symbols.symbol(parsed.task().getElements().getTypeElement("Outer.Child"));
      assertThat(child.owner().name()).isEqualTo("Outer");
      assertThat(child.enclosingClass().name()).isEqualTo("Outer");
      assertThat(child.outermostClass().name()).isEqualTo("Outer");
      assertThat(child.superClass().fullyQualifiedName()).isEqualTo("java.util.ArrayList");
      assertThat(child.interfaces()).extracting(Type::fullyQualifiedName).containsExactly("Parent", "Other");
      assertThat(child.superTypes()).extracting(Type::fullyQualifiedName)
        .contains("Parent", "Other", "java.util.ArrayList", "java.util.List");
      assertThat(child.lookupSymbols("run")).hasSize(1);
      assertThat(child.lookupSymbols("size")).isEmpty();
      assertThat(JUtils.directSuperTypes(child.type())).extracting(Type::fullyQualifiedName)
        .containsExactlyInAnyOrder("java.util.ArrayList", "Parent", "Other");
    }
  }

  @Test
  void resolves_binary_names_of_nested_supertypes() throws IOException {
    try (JavacParser.Result parsed = JavacParser.parse("Sample.java", "class Sample {}",
      JavacParser.Options.defaults().withAnalysis(true))) {
      TypeElement virtual = parsed.task().getElements().getTypeElement("java.lang.Thread.Builder.OfVirtual");
      Type type = symbols(parsed).type(virtual.asType());
      assertThat(type.is("java.lang.Thread$Builder$OfVirtual")).isTrue();
      assertThat(type.isSubtypeOf("java.lang.Thread$Builder")).isTrue();
    }
  }

  @Test
  void preserves_type_arguments_across_supertypes() throws IOException {
    String source = "class Sample { java.util.List<String> values; enum Choice { VALUE } }";
    try (JavacParser.Result parsed = JavacParser.parse("Sample.java", source,
      JavacParser.Options.defaults().withAnalysis(true))) {
      JavacSymbols symbols = symbols(parsed);
      TypeElement sample = parsed.task().getElements().getTypeElement("Sample");
      Type values = variable(symbols, sample, "values").type();
      assertThat(values.symbol().superTypes()).anySatisfy(superType -> {
        assertThat(superType.is("java.util.Collection")).isTrue();
        assertThat(superType.typeArguments()).extracting(Type::fullyQualifiedName).containsExactly("java.lang.String");
      });
      TypeElement choice = parsed.task().getElements().getTypeElement("Sample.Choice");
      assertThat(variable(symbols, choice, "VALUE").isEnum()).isTrue();
    }
  }

  @Test
  void resolves_deprecated_constructor_and_array_argument() throws IOException {
    String source = "class Sample { Object value = new String(new byte[3], 7); }";
    try (JavacSourceAnalysis analysis = JavacSourceAnalysis.analyze("Sample.java", source, JavacParser.Options.defaults())) {
      ClassTree sample = (ClassTree) analysis.tree().types().get(0);
      NewClassTree creation = (NewClassTree) ((VariableTree) sample.members().get(0)).initializer();
      assertThat(creation.methodSymbol().isDeprecated()).isTrue();
      assertThat(creation.arguments().get(0).symbolType().is("byte[]")).isTrue();
      assertThat(creation.arguments().get(1).symbolType().is("int")).isTrue();
    }
  }

  @Test
  void exposes_record_component_once_among_members() throws IOException {
    try (JavacParser.Result parsed = JavacParser.parse("Sample.java", "record Sample(int value) {}",
      JavacParser.Options.defaults().withAnalysis(true))) {
      Symbol.TypeSymbol sample = (Symbol.TypeSymbol) symbols(parsed).symbol(parsed.task().getElements().getTypeElement("Sample"));
      assertThat(sample.memberSymbols().stream()
        .filter(member -> member.isVariableSymbol() && member.name().equals("value"))).hasSize(1);
      assertThat(sample.memberSymbols().stream()
        .filter(member -> member.isMethodSymbol() && member.name().equals("value"))).hasSize(1);
    }
  }

  @Test
  void preserves_method_return_type_arguments() throws IOException {
    String source = "class Sample { java.util.List<String> values() { return null; } java.util.List raw() { return null; } }";
    try (JavacParser.Result parsed = JavacParser.parse("Sample.java", source,
      JavacParser.Options.defaults().withAnalysis(true))) {
      JavacSymbols symbols = symbols(parsed);
      TypeElement sample = parsed.task().getElements().getTypeElement("Sample");
      assertThat(method(symbols, sample, "values").returnType().type().typeArguments())
        .extracting(Type::fullyQualifiedName).containsExactly("java.lang.String");
      assertThat(method(symbols, sample, "raw").returnType().type().isParameterized()).isFalse();
    }
  }

  @Test
  void retains_annotation_metadata_when_repeatable_value_is_missing() throws IOException {
    String source = "class Sample { @java.lang.annotation.Repeatable @interface Item {} }";
    try (JavacParser.Result parsed = JavacParser.parse("Sample.java", source,
      JavacParser.Options.defaults().withAnalysis(true))) {
      Symbol.TypeSymbol item = (Symbol.TypeSymbol) symbols(parsed).symbol(
        parsed.task().getElements().getTypeElement("Sample.Item"));
      assertThat(item.metadata().isAnnotatedWith("java.lang.annotation.Repeatable")).isTrue();
    }
  }

  @Test
  void resolves_repeatable_annotation_inside_unresolved_container() throws IOException {
    String source = "class Sample { @Items({@Item(\"a\")}) void run() {} "
      + "@java.lang.annotation.Repeatable @interface Item { String value(); } }";
    try (JavacSourceAnalysis analysis = JavacSourceAnalysis.analyze("Sample.java", source, JavacParser.Options.defaults())) {
      ClassTree sample = (ClassTree) analysis.tree().types().get(0);
      MethodTree method = (MethodTree) sample.members().get(0);
      AnnotationTree container = method.modifiers().annotations().get(0);
      NewArrayTree group = (NewArrayTree) container.arguments().get(0);
      AnnotationTree item = (AnnotationTree) group.initializers().get(0);
      assertThat(item.symbolType().symbol().metadata().isAnnotatedWith("java.lang.annotation.Repeatable")).isTrue();
    }
  }

  @Test
  void resolves_inferred_lambda_parameter_type() throws IOException {
    String source = "class Sample { java.util.function.Consumer<java.util.List<String>> consumer = items -> {}; }";
    try (JavacSourceAnalysis analysis = JavacSourceAnalysis.analyze("Sample.java", source, JavacParser.Options.defaults())) {
      ClassTree sample = (ClassTree) analysis.tree().types().get(0);
      LambdaExpressionTree lambda = (LambdaExpressionTree) ((VariableTree) sample.members().get(0)).initializer();
      Type inferred = lambda.parameters().get(0).type().symbolType();
      assertThat(inferred.is("java.util.List")).isTrue();
      assertThat(inferred.typeArguments()).extracting(Type::fullyQualifiedName).containsExactly("java.lang.String");
    }
  }

  @Test
  void resolves_lambda_descriptor_return_type_in_context() throws IOException {
    String source = "class Sample { java.util.function.Supplier<Runnable> supplier = () -> new Thread(); }";
    try (JavacSourceAnalysis analysis = JavacSourceAnalysis.analyze("Sample.java", source, JavacParser.Options.defaults())) {
      ClassTree sample = (ClassTree) analysis.tree().types().get(0);
      LambdaExpressionTree lambda = (LambdaExpressionTree) ((VariableTree) sample.members().get(0)).initializer();
      assertThat(lambda.symbol().returnType().type().is("java.lang.Runnable")).isTrue();
    }
  }

  @Test
  void distinguishes_known_overload_from_dropped_unknown_declaration() throws IOException {
    String source = "abstract class Parent { abstract void run(Unknown value, int count); } "
      + "class Child extends Parent { void run(Unknown value, int count) {} void run(Exception value, int count) {} }";
    try (JavacSourceAnalysis analysis = JavacSourceAnalysis.analyze("Child.java", source, JavacParser.Options.defaults())) {
      ClassTree child = (ClassTree) analysis.tree().types().get(1);
      MethodTree known = (MethodTree) child.members().get(1);
      assertThat(known.isOverriding()).isFalse();
    }
  }

  @Test
  void finds_outermost_class_for_local_type() throws IOException {
    String source = "class Sample { void run() { class Local implements java.io.Serializable {} } }";
    try (JavacSourceAnalysis analysis = JavacSourceAnalysis.analyze("Sample.java", source, JavacParser.Options.defaults())) {
      ClassTree sample = (ClassTree) analysis.tree().types().get(0);
      MethodTree method = (MethodTree) sample.members().get(0);
      ClassTree local = (ClassTree) method.block().body().get(0);
      assertThat(local.symbol().outermostClass()).isEqualTo(sample.symbol());
      assertThat(local.symbol().owner()).isEqualTo(method.symbol());
    }
  }

  @Test
  void resolves_anonymous_class_symbol_and_interface() throws IOException {
    String source = "class Sample { java.io.Serializable value = new java.io.Serializable() {}; }";
    try (JavacSourceAnalysis analysis = JavacSourceAnalysis.analyze("Sample.java", source, JavacParser.Options.defaults())) {
      ClassTree sample = (ClassTree) analysis.tree().types().get(0);
      NewClassTree creation = (NewClassTree) ((VariableTree) sample.members().get(0)).initializer();
      Symbol.TypeSymbol anonymous = creation.classBody().symbol();
      assertThat(anonymous.isUnknown()).isFalse();
      assertThat(anonymous.interfaces()).anySatisfy(type -> assertThat(type.is("java.io.Serializable")).isTrue());
      assertThat(anonymous.outermostClass()).isEqualTo(sample.symbol());
      assertThat(anonymous.owner()).isEqualTo(sample.symbol());
    }
  }

  @Test
  void resolves_method_symbol_on_non_compiling_wildcard_map_call() throws IOException {
    String source = "class Sample { void run(java.util.Map<?, ?> map) { map.put(\"a\", 1); } }";
    try (JavacSourceAnalysis analysis = JavacSourceAnalysis.analyze("Sample.java", source, JavacParser.Options.defaults())) {
      ClassTree sample = (ClassTree) analysis.tree().types().get(0);
      MethodTree method = (MethodTree) sample.members().get(0);
      MethodInvocationTree invocation = (MethodInvocationTree) ((ExpressionStatementTree) method.block().body().get(0)).expression();
      assertThat(invocation.methodSymbol().name()).isEqualTo("put");
      assertThat(invocation.methodSymbol().owner().type().is("java.util.Map")).isTrue();
      MemberSelectExpressionTree select = (MemberSelectExpressionTree) invocation.methodSelect();
      assertThat(select.identifier().symbol()).isEqualTo(invocation.methodSymbol());
    }
  }

  @Test
  void resolves_method_owner_at_parameterized_call_site() throws IOException {
    String source = "class Sample { void run() { java.util.stream.Stream.of(\"A\").collect(java.util.stream.Collectors.toList()); } }";
    try (JavacSourceAnalysis analysis = JavacSourceAnalysis.analyze("Sample.java", source, JavacParser.Options.defaults())) {
      ClassTree sample = (ClassTree) analysis.tree().types().get(0);
      MethodTree method = (MethodTree) sample.members().get(0);
      MethodInvocationTree collect = (MethodInvocationTree) ((ExpressionStatementTree) method.block().body().get(0)).expression();
      Symbol.MethodSymbol symbol = collect.methodSymbol();
      assertThat(symbol.owner().type().is("java.util.stream.Stream")).isTrue();
      assertThat(symbol.owner().type().typeArguments()).singleElement().satisfies(type -> assertThat(type.is("java.lang.String")).isTrue());
      assertThat(symbol.parameterTypes()).singleElement().satisfies(type -> assertThat(type.is("java.util.stream.Collector")).isTrue());
      MemberSelectExpressionTree select = (MemberSelectExpressionTree) collect.methodSelect();
      assertThat(select.identifier().symbol()).isEqualTo(symbol);
    }
  }

  @Test
  void resolves_inferred_generic_varargs_parameter_at_call_site() throws IOException {
    String source = "class Sample { void run() { java.util.Arrays.asList(new byte[0]); } }";
    try (JavacSourceAnalysis analysis = JavacSourceAnalysis.analyze("Sample.java", source, JavacParser.Options.defaults())) {
      ClassTree sample = (ClassTree) analysis.tree().types().get(0);
      MethodTree method = (MethodTree) sample.members().get(0);
      MethodInvocationTree invocation = (MethodInvocationTree) ((ExpressionStatementTree) method.block().body().get(0)).expression();
      assertThat(invocation.methodSymbol().parameterTypes()).singleElement()
        .satisfies(type -> assertThat(type.is("byte[][]")).isTrue());
    }
  }

  @Test
  void resolves_constructor_parameter_at_parameterized_call_site() throws IOException {
    String source = "class Sample<T> { Sample(T value) {} void run() { new Sample<java.util.List<String>>(new java.util.ArrayList<String>()); } }";
    try (JavacSourceAnalysis analysis = JavacSourceAnalysis.analyze("Sample.java", source, JavacParser.Options.defaults())) {
      ClassTree sample = (ClassTree) analysis.tree().types().get(0);
      MethodTree method = (MethodTree) sample.members().get(1);
      NewClassTree creation = (NewClassTree) ((ExpressionStatementTree) method.block().body().get(0)).expression();
      assertThat(creation.methodSymbol().parameterTypes()).singleElement()
        .satisfies(type -> assertThat(type.isParameterized()).isTrue());
      assertThat(creation.methodSymbol().parameterTypes().get(0).typeArguments()).singleElement()
        .satisfies(type -> assertThat(type.is("java.lang.String")).isTrue());
    }
  }

  @Test
  void keeps_inherited_method_declaring_owner() throws IOException {
    String source = "class Sample extends java.math.BigDecimal { Sample() { super(1); } void run() { setScale(2); } }";
    try (JavacSourceAnalysis analysis = JavacSourceAnalysis.analyze("Sample.java", source, JavacParser.Options.defaults())) {
      ClassTree sample = (ClassTree) analysis.tree().types().get(0);
      MethodTree run = (MethodTree) sample.members().get(1);
      MethodInvocationTree invocation = (MethodInvocationTree) ((ExpressionStatementTree) run.block().body().get(0)).expression();
      assertThat(invocation.methodSymbol().owner().type().is("java.math.BigDecimal")).isTrue();
    }
  }

  @Test
  void marks_method_with_unresolved_contextual_return_type_unknown() throws IOException {
    String source = "class Sample { void run(java.util.Iterator<Unknown> delegate) { delegate.next(); } }";
    try (JavacSourceAnalysis analysis = JavacSourceAnalysis.analyze("Sample.java", source, JavacParser.Options.defaults())) {
      ClassTree sample = (ClassTree) analysis.tree().types().get(0);
      MethodTree run = (MethodTree) sample.members().get(0);
      MethodInvocationTree invocation = (MethodInvocationTree) ((ExpressionStatementTree) run.block().body().get(0)).expression();
      assertThat(invocation.methodSymbol().isUnknown()).isTrue();
    }
  }

  @Test
  void preserves_unresolved_class_literal_annotation_value_name() throws IOException {
    String source = "@interface RunWith { Class<?> value(); } @RunWith(Cucumber.class) class Sample {}";
    try (JavacSourceAnalysis analysis = JavacSourceAnalysis.analyze("Sample.java", source, JavacParser.Options.defaults())) {
      ClassTree sample = (ClassTree) analysis.tree().types().get(1);
      List<SymbolMetadata.AnnotationValue> values = sample.symbol().metadata().valuesForAnnotation("RunWith");
      assertThat(values).singleElement().satisfies(value -> {
        assertThat(value.value()).isInstanceOf(Symbol.TypeSymbol.class);
        assertThat(((Symbol.TypeSymbol) value.value()).name()).isEqualTo("Cucumber");
      });
    }
  }

  @Test
  void names_package_symbols_by_qualified_name() throws IOException {
    try (JavacParser.Result parsed = JavacParser.parse("Sample.java",
      "package com.example.pkg; class Sample {}", JavacParser.Options.defaults().withAnalysis(true))) {
      JavacSymbols symbols = symbols(parsed);
      Symbol.TypeSymbol sample = (Symbol.TypeSymbol) symbols.symbol(
        parsed.task().getElements().getTypeElement("com.example.pkg.Sample"));
      assertThat(sample.owner().isPackageSymbol()).isTrue();
      assertThat(sample.owner().name()).isEqualTo("com.example.pkg");
      assertThat(sample.owner().owner()).isSameAs(Symbol.ROOT_PACKAGE);
    }
  }

  @Test
  void exposes_symbol_kinds_modifiers_and_enclosing_class() throws IOException {
    String source = "public class Sample { public static final int CONSTANT = 1; private volatile int state; "
      + "protected synchronized void update() {} void packageMethod() {} "
      + "interface NestedInterface {} enum NestedEnum { VALUE } @interface NestedAnnotation {} }";
    try (JavacParser.Result parsed = JavacParser.parse("Sample.java", source,
      JavacParser.Options.defaults().withAnalysis(true))) {
      JavacSymbols symbols = symbols(parsed);
      TypeElement sampleElement = parsed.task().getElements().getTypeElement("Sample");
      Symbol.TypeSymbol sample = (Symbol.TypeSymbol) symbols.symbol(sampleElement);
      Symbol.VariableSymbol constant = variable(symbols, sampleElement, "CONSTANT");
      Symbol.VariableSymbol state = variable(symbols, sampleElement, "state");
      Symbol.MethodSymbol update = method(symbols, sampleElement, "update");
      Symbol.MethodSymbol packageMethod = method(symbols, sampleElement, "packageMethod");
      Symbol.TypeSymbol nestedInterface = (Symbol.TypeSymbol) symbols.symbol(parsed.task().getElements().getTypeElement("Sample.NestedInterface"));
      Symbol.TypeSymbol nestedEnum = (Symbol.TypeSymbol) symbols.symbol(parsed.task().getElements().getTypeElement("Sample.NestedEnum"));
      Symbol.TypeSymbol nestedAnnotation = (Symbol.TypeSymbol) symbols.symbol(parsed.task().getElements().getTypeElement("Sample.NestedAnnotation"));
      assertThat(sample.isTypeSymbol()).isTrue();
      assertThat(sample.isPublic()).isTrue();
      assertThat(sample.enclosingClass()).isSameAs(sample);
      assertThat(constant.isVariableSymbol()).isTrue();
      assertThat(constant.isStatic()).isTrue();
      assertThat(constant.isFinal()).isTrue();
      assertThat(constant.enclosingClass()).isSameAs(sample);
      assertThat(state.isPrivate()).isTrue();
      assertThat(state.isVolatile()).isTrue();
      assertThat(update.isMethodSymbol()).isTrue();
      assertThat(update.isProtected()).isTrue();
      assertThat(update.isSynchronizedMethod()).isTrue();
      assertThat(packageMethod.isPackageVisibility()).isTrue();
      assertThat(nestedInterface.isInterface()).isTrue();
      assertThat(nestedInterface.isAbstract()).isTrue();
      assertThat(nestedEnum.isEnum()).isTrue();
      assertThat(nestedAnnotation.isAnnotation()).isTrue();
    }
  }

  @Test
  void preserves_annotation_presence_when_value_cannot_be_resolved() throws IOException {
    String source = "@interface Profile { String[] value(); } @Profile(Constants.DEV) class Sample {}";
    try (JavacParser.Result parsed = JavacParser.parse("Sample.java", source,
      JavacParser.Options.defaults().withAnalysis(true))) {
      Symbol.TypeSymbol sample = (Symbol.TypeSymbol) symbols(parsed).symbol(parsed.task().getElements().getTypeElement("Sample"));
      SymbolMetadata metadata = sample.metadata();
      assertThat(metadata.isAnnotatedWith("Profile")).isTrue();
      assertThat(metadata.valuesForAnnotation("Profile")).hasSize(1);
      assertThat((Object[]) metadata.valuesForAnnotation("Profile").get(0).value()).containsExactly((Object) null);
    }
  }

  @Test
  void resolves_method_signatures_overrides_and_flags() throws IOException {
    String source = "interface Base { String name(); } class Sample implements Base { "
      + "public synchronized String name() { return \"x\"; } "
      + "private static native void nativeCall(String... args); "
      + "int count() { return 1; } void other() {} }";
    try (JavacParser.Result parsed = JavacParser.parse("Sample.java", source,
      JavacParser.Options.defaults().withAnalysis(true))) {
      JavacSymbols symbols = symbols(parsed);
      TypeElement sample = parsed.task().getElements().getTypeElement("Sample");
      Symbol.MethodSymbol name = method(symbols, sample, "name");
      Symbol.MethodSymbol nativeCall = method(symbols, sample, "nativeCall");
      Symbol.MethodSymbol count = method(symbols, sample, "count");
      assertThat(name.signature()).isEqualTo("Sample#name()Ljava/lang/String;");
      assertThat(name.isSynchronizedMethod()).isTrue();
      assertThat(name.overriddenSymbols()).hasSize(1);
      assertThat(name.overriddenSymbols().get(0).owner().name()).isEqualTo("Base");
      assertThat(name.returnType().type().is("java.lang.String")).isTrue();
      assertThat(nativeCall.isNativeMethod()).isTrue();
      assertThat(nativeCall.isVarArgsMethod()).isTrue();
      assertThat(nativeCall.isOverridable()).isFalse();
      assertThat(nativeCall.declarationParameters()).hasSize(1);
      assertThat(nativeCall.declarationParameters().get(0).isParameter()).isTrue();
      assertThat(count.returnType().type().is("int")).isTrue();
      assertThat(count.signature()).isEqualTo("Sample#count()I");
    }
  }

  @Test
  void resolves_variable_kinds_constants_and_ownership() throws IOException {
    String source = "class Sample { static final int CONSTANT = 42; static final char LETTER = 'A'; "
      + "final String instanceConstant = \"value\"; int field; "
      + "void method(String parameter) { final int local = 1; } }";
    try (JavacParser.Result parsed = JavacParser.parse("Sample.java", source,
      JavacParser.Options.defaults().withAnalysis(true))) {
      JavacSymbols symbols = symbols(parsed);
      TypeElement sample = parsed.task().getElements().getTypeElement("Sample");
      Symbol.VariableSymbol constant = variable(symbols, sample, "CONSTANT");
      Symbol.VariableSymbol letter = variable(symbols, sample, "LETTER");
      Symbol.VariableSymbol instanceConstant = variable(symbols, sample, "instanceConstant");
      Symbol.VariableSymbol field = variable(symbols, sample, "field");
      Symbol.MethodSymbol method = method(symbols, sample, "method");
      Symbol.VariableSymbol parameter = (Symbol.VariableSymbol) method.declarationParameters().get(0);
      assertThat(constant.constantValue()).contains(42);
      assertThat(letter.constantValue()).contains(65);
      assertThat(instanceConstant.constantValue()).isEmpty();
      assertThat(constant.isStatic()).isTrue();
      assertThat(field.constantValue()).isEmpty();
      assertThat(field.isLocalVariable()).isFalse();
      assertThat(parameter.isParameter()).isTrue();
      assertThat(parameter.owner()).isSameAs(method);
    }
  }

  @Test
  void resolves_nullability_annotations_and_inheritance() throws IOException {
    Path annotations = directory.resolve("Nullable.java");
    Files.writeString(annotations, "package org.jspecify.annotations; "
      + "@java.lang.annotation.Target({java.lang.annotation.ElementType.FIELD,java.lang.annotation.ElementType.TYPE_USE}) "
      + "public @interface Nullable {}");
    Path sample = directory.resolve("Sample.java");
    Files.writeString(sample, "import org.jspecify.annotations.Nullable; "
      + "class Sample { @Nullable String field; java.util.List<@Nullable String> values; }");
    try (JavacParser.Result parsed = JavacParser.parse(List.of(annotations, sample),
      JavacParser.Options.defaults().withAnalysis(true))) {
      JavacSymbols symbols = symbols(parsed);
      TypeElement type = parsed.task().getElements().getTypeElement("Sample");
      Symbol.VariableSymbol field = variable(symbols, type, "field");
      Symbol.VariableSymbol values = variable(symbols, type, "values");
      assertThat(field.metadata().isAnnotatedWith("org.jspecify.annotations.Nullable")).isTrue();
      assertThat(field.metadata().nullabilityData().type())
        .isEqualTo(SymbolMetadata.NullabilityType.STRONG_NULLABLE);
      assertThat(values.metadata().parametersMetadata()).hasSize(1);
      assertThat(values.metadata().parametersMetadata()[0].isAnnotatedWith("org.jspecify.annotations.Nullable")).isTrue();
    }
  }

  @Test
  void resolves_primitive_array_and_generic_types() throws IOException {
    try (JavacParser.Result parsed = JavacParser.parse("Sample.java", "class Sample {}",
      JavacParser.Options.defaults().withAnalysis(true))) {
      JavacSema sema = new JavacSema(parsed);
      assertThat(sema.getClassType("int").isPrimitive()).isTrue();
      assertThat(sema.getClassType("int").primitiveWrapperType().is("java.lang.Integer")).isTrue();
      assertThat(((Type.ArrayType) sema.getClassType("java.lang.String[]")).elementType().is("java.lang.String")).isTrue();
      assertThat(sema.getClassType("java.lang.String[]").isSubtypeOf("java.lang.Object")).isTrue();
      assertThat(sema.getClassType("missing.Type")).isSameAs(Type.UNKNOWN);
    }
  }

  @ParameterizedTest
  @CsvSource({
    "boolean, java.lang.Boolean",
    "byte, java.lang.Byte",
    "short, java.lang.Short",
    "char, java.lang.Character",
    "int, java.lang.Integer",
    "long, java.lang.Long",
    "float, java.lang.Float",
    "double, java.lang.Double"
  })
  void maps_primitive_and_wrapper_types(String primitive, String wrapper) throws IOException {
    try (JavacParser.Result parsed = JavacParser.parse("Sample.java", "class Sample {}",
      JavacParser.Options.defaults().withAnalysis(true))) {
      JavacSema sema = new JavacSema(parsed);
      Type primitiveType = sema.getClassType(primitive);
      Type wrapperType = sema.getClassType(wrapper);
      assertThat(primitiveType.isPrimitive()).isTrue();
      assertThat(primitiveType.primitiveWrapperType()).isEqualTo(wrapperType);
      assertThat(wrapperType.isPrimitiveWrapper()).isTrue();
      assertThat(wrapperType.primitiveType()).isEqualTo(primitiveType);
      assertThat(primitiveType.symbol().type().is(primitive)).isTrue();
    }
  }

  @ParameterizedTest
  @CsvSource({
    "javax.annotation.CheckForNull, STRONG_NULLABLE",
    "jakarta.annotation.CheckForNull, STRONG_NULLABLE",
    "org.jspecify.annotations.Nullable, STRONG_NULLABLE",
    "org.springframework.lang.Nullable, STRONG_NULLABLE",
    "reactor.util.annotation.Nullable, STRONG_NULLABLE",
    "androidx.annotation.Nullable, WEAK_NULLABLE",
    "javax.annotation.Nullable, WEAK_NULLABLE",
    "jakarta.annotation.Nullable, WEAK_NULLABLE",
    "org.jetbrains.annotations.Nullable, WEAK_NULLABLE",
    "org.checkerframework.checker.nullness.qual.Nullable, WEAK_NULLABLE",
    "org.jspecify.annotations.NonNull, NON_NULL",
    "org.jetbrains.annotations.NotNull, NON_NULL",
    "androidx.annotation.NonNull, NON_NULL",
    "lombok.NonNull, NON_NULL",
    "org.checkerframework.checker.nullness.qual.NonNull, NON_NULL"
  })
  void recognizes_common_nullability_annotations(String annotationName, SymbolMetadata.NullabilityType expected) throws IOException {
    int separator = annotationName.lastIndexOf('.');
    String packageName = annotationName.substring(0, separator);
    String simpleName = annotationName.substring(separator + 1);
    Path annotationDirectory = directory.resolve(packageName.replace('.', '/'));
    Files.createDirectories(annotationDirectory);
    Path annotation = annotationDirectory.resolve(simpleName + ".java");
    Files.writeString(annotation, "package " + packageName + "; public @interface " + simpleName + " {}");
    Path sample = directory.resolve("Sample.java");
    Files.writeString(sample, "class Sample { @" + annotationName + " String field; }");
    try (JavacParser.Result parsed = JavacParser.parse(List.of(annotation, sample),
      JavacParser.Options.defaults().withAnalysis(true))) {
      JavacSymbols symbols = symbols(parsed);
      TypeElement type = parsed.task().getElements().getTypeElement("Sample");
      Symbol.VariableSymbol field = variable(symbols, type, "field");
      assertThat(field.metadata().nullabilityData().type()).isEqualTo(expected);
    }
  }

  @ParameterizedTest
  @MethodSource("overrideCases")
  void resolves_overrides_across_class_and_interface_hierarchies(String source, String expectedOwners) throws IOException {
    try (JavacParser.Result parsed = JavacParser.parse("Child.java", source,
      JavacParser.Options.defaults().withAnalysis(true))) {
      JavacSymbols symbols = symbols(parsed);
      TypeElement child = parsed.task().getElements().getTypeElement("Child");
      Symbol.MethodSymbol run = method(symbols, child, "run");
      assertThat(run.overriddenSymbols()).extracting(overridden -> overridden.owner().name())
        .containsExactly(expectedOwners.split("\\|"));
    }
  }

  private static Stream<Arguments> overrideCases() {
    return Stream.of(
      Arguments.of("class Base { void run() {} } class Child extends Base { @Override void run() {} }", "Base"),
      Arguments.of("interface Parent { void run(); } class Child implements Parent { public void run() {} }", "Parent"),
      Arguments.of("interface Parent { void run(); } interface Middle extends Parent {} "
        + "class Child implements Middle { public void run() {} }", "Parent"),
      Arguments.of("interface Parent { void run(); } class Base implements Parent { public void run() {} } "
        + "class Middle extends Base { public void run() {} } class Child extends Middle { public void run() {} }",
        "Middle|Base|Parent")
    );
  }

  private static JavacSymbols symbols(JavacParser.Result parsed) {
    return new JavacSymbols(parsed.task().getTypes(), parsed.task().getElements());
  }

  private static Symbol.MethodSymbol method(JavacSymbols symbols, TypeElement owner, String name) {
    Element element = owner.getEnclosedElements().stream()
      .filter(candidate -> candidate.getSimpleName().contentEquals(name))
      .findFirst().orElseThrow();
    return (Symbol.MethodSymbol) symbols.symbol(element);
  }

  private static Symbol.VariableSymbol variable(JavacSymbols symbols, TypeElement owner, String name) {
    Element element = owner.getEnclosedElements().stream()
      .filter(candidate -> candidate.getSimpleName().contentEquals(name))
      .findFirst().orElseThrow();
    return (Symbol.VariableSymbol) symbols.symbol(element);
  }
}
