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
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.java.AnalysisProgress;
import org.sonar.java.TestUtils;
import org.sonar.java.model.JParserConfig;
import org.sonar.java.model.JParserTestUtils;
import org.sonar.java.model.JavaTree;
import org.sonar.java.model.JavaTree.CompilationUnitTreeImpl;
import org.sonar.java.model.declaration.ClassTreeImpl;
import org.sonar.java.model.declaration.MethodTreeImpl;
import org.sonar.java.model.expression.MethodInvocationTreeImpl;
import org.sonar.java.model.expression.NewClassTreeImpl;
import org.sonar.java.model.statement.ExpressionStatementTreeImpl;
import org.sonar.java.model.statement.ReturnStatementTreeImpl;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Sema;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata.AnnotationInstance;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.VariableTree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.sonar.java.model.JParserTestUtils.sema;
import static org.sonar.java.model.JParserTestUtils.symbolOf;
import static org.sonar.java.model.JParserTestUtils.typeOf;
import static org.sonar.java.psi.JUtilsTest.firstClass;
import static org.sonar.java.psi.JUtilsTest.firstMethod;
import static org.sonar.java.psi.JUtilsTest.nthMethod;

class JMethodSymbolTest {

  @Test
  void annotation_default_values_use_semantic_api_types() {
    var tree = test("""
      @interface Nested { String value(); }
      enum Choice { FIRST }
      @interface Defaults {
        int number() default 42;
        String text() default "value";
        Class<?> type() default String.class;
        Choice choice() default Choice.FIRST;
        Nested nested() default @Nested("value");
        Class<?>[] types() default {String.class, int.class};
        Choice[] choices() default {Choice.FIRST};
        Nested[] annotations() default {@Nested("value")};
        int[] numbers() default {1, 2};
        String absent();
      }
      """);
    var defaults = ((ClassTreeImpl) tree.types().get(2)).symbol();

    assertThat(annotationElementDefaultValue(defaults, "number")).isEqualTo(42);
    assertThat(annotationElementDefaultValue(defaults, "text")).isEqualTo("value");
    assertThat(((Symbol.TypeSymbol) annotationElementDefaultValue(defaults, "type")).type().is("java.lang.String")).isTrue();
    assertThat(((Symbol.VariableSymbol) annotationElementDefaultValue(defaults, "choice")).name()).isEqualTo("FIRST");
    assertThat(((AnnotationInstance) annotationElementDefaultValue(defaults, "nested")).values().get(0).value()).isEqualTo("value");
    assertThat((Object[]) annotationElementDefaultValue(defaults, "types"))
      .allSatisfy(value -> assertThat(value).isInstanceOf(Symbol.TypeSymbol.class));
    assertThat((Object[]) annotationElementDefaultValue(defaults, "choices"))
      .allSatisfy(value -> assertThat(value).isInstanceOf(Symbol.VariableSymbol.class));
    assertThat((Object[]) annotationElementDefaultValue(defaults, "annotations"))
      .allSatisfy(value -> assertThat(value).isInstanceOf(AnnotationInstance.class));
    assertThat((Object[]) annotationElementDefaultValue(defaults, "numbers")).containsExactly(1, 2);
    assertThat(annotationElementDefaultValue(defaults, "absent")).isNull();
    assertThat(Symbol.MethodSymbol.UNKNOWN_METHOD.annotationElementDefaultValue()).isNull();
  }

  private static Object annotationElementDefaultValue(Symbol.TypeSymbol annotation, String name) {
    return ((Symbol.MethodSymbol) annotation.lookupSymbols(name).iterator().next()).annotationElementDefaultValue();
  }

  @Test
  void test() {
    JavaTree.CompilationUnitTreeImpl cu = test("class C { C m(C p) throws Exception { return null; } }");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    MethodTreeImpl m = (MethodTreeImpl) c.members().get(0);
    Symbol.MethodSymbol symbol = ((Symbol.MethodSymbol) symbolOf(m));
    assertThat(symbol.parameterTypes())
      .containsOnly(((Type) typeOf(c)));
    assertThat(symbol.declarationParameters())
      .containsExactly(m.parameters().get(0).symbol());
    assertThat(symbol.returnType())
      .isSameAs(((Symbol.TypeSymbol) typeOf(c).symbol()));
    assertThat(symbol.thrownTypes())
      .containsOnly(sema(cu).getClassType("java.lang.Exception"));
  }

  @Test
  void test_with_generics() {
    JavaTree.CompilationUnitTreeImpl cu = test("class C { <T> void m(T p) { m(1); } }");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    MethodTreeImpl m = (MethodTreeImpl) c.members().get(0);
    Symbol.MethodSymbol declarationSymbol = m.symbol();
    Type declarationParameterType = m.parameters().get(0).symbol().type();
    assertThat(declarationSymbol.parameterTypes())
      .containsOnly(declarationParameterType);
    Symbol declarationParameterSymbol = m.parameters().get(0).symbol();
    assertThat(declarationSymbol.declarationParameters())
      .containsExactly(declarationParameterSymbol);

    MethodInvocationTree invocationWithString = (MethodInvocationTree) ((ExpressionStatementTree) m.block().body().get(0)).expression();
    Symbol.MethodSymbol invocationSymbol = invocationWithString.methodSymbol();
    // Parameter types are not always the same as the one from the declaration: it can be the actual type and not the generic (Integer instead of T).
    Type invocationArgument = invocationSymbol.parameterTypes().get(0);
    assertThat(invocationArgument)
      .isNotEqualTo(declarationParameterType);
    assertThat(invocationArgument.name()).isEqualTo("Integer");
    // However, the symbols of the declaration should be the same
    invocationSymbol.declarationParameters().containsAll(
      declarationSymbol.declarationParameters()
    );
  }

  @Test
  void test_declaration_not_in_code() {
    JavaTree.CompilationUnitTreeImpl cu = test("class C {" +
      "void m() {" +
      "    \"1a\".startsWith(\"1\");\n" +
      "    \"2b\".startsWith(\"2\"); } " +
      "}");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    MethodTreeImpl m = (MethodTreeImpl) c.members().get(0);

    List<StatementTree> body = m.block().body();
    MethodInvocationTree startWith1 = (MethodInvocationTree) ((ExpressionStatementTree) body.get(0)).expression();
    MethodInvocationTree startWith2 = (MethodInvocationTree) ((ExpressionStatementTree) body.get(1)).expression();

    Symbol.MethodSymbol symbol1 = startWith1.methodSymbol();
    Symbol.MethodSymbol symbol2 = startWith2.methodSymbol();

    assertThat(symbol1.parameterTypes().get(0).name()).isEqualTo("String");
    assertThat(symbol2.parameterTypes().get(0).name()).isEqualTo("String");

    Symbol symbolParam1 = symbol1.declarationParameters().get(0);
    Symbol symbolParam2 = symbol2.declarationParameters().get(0);
    assertThat(symbolParam1).isInstanceOf(Symbol.VariableSymbol.class)
      .isInstanceOf(Symbol.VariableSymbol.class);
    assertThat(symbolParam2).isInstanceOf(Symbol.VariableSymbol.class)
      .isInstanceOf(Symbol.VariableSymbol.class);

    assertThat(symbolParam1.isVariableSymbol()).isTrue();
    assertThat(symbolParam2.isVariableSymbol()).isTrue();

    Symbol.VariableSymbol parameterSymbol1 = (Symbol.VariableSymbol) symbolParam1;
    Symbol.VariableSymbol parameterSymbol2 = (Symbol.VariableSymbol) symbolParam2;

    assertThat(parameterSymbol1).isSameAs(parameterSymbol2);

    assertThat(parameterSymbol1.name()).isEqualTo("arg0");
    assertThat(parameterSymbol1.owner().name()).isEqualTo("startsWith");
    assertThat(parameterSymbol1.type().name()).isEqualTo("String");
    assertThat(parameterSymbol1.isUnknown()).isFalse();
    assertThat(parameterSymbol1.enclosingClass()).isEqualTo(parameterSymbol1.owner().enclosingClass());
    assertThat(parameterSymbol1.usages()).isEmpty();
    assertThat(parameterSymbol1.declaration()).isNull();
    assertThat(parameterSymbol1.metadata()).isSameAs(parameterSymbol2.metadata());
    assertThat(parameterSymbol1.isVariableSymbol()).isTrue();
    assertThat(parameterSymbol1.isLocalVariable()).isFalse();
    assertThat(parameterSymbol1.isFinal()).isFalse();
  }

  @Test
  void test_forward_declaration_in_the_same_batch() {
    List<InputFile> inputFiles = Arrays.asList(
      TestUtils.inputFile("src/test/files/semantic/Main.java"),
      TestUtils.inputFile("src/test/files/semantic/Dependency.java"),
      TestUtils.inputFile("src/test/files/semantic/GenericDependency.java"),
      TestUtils.inputFile("src/test/files/semantic/Nullable.java"));
    List<String> processed = new ArrayList<>();
    PsiParserConfig.create(JParserConfig.Mode.BATCH, JParserConfig.MAXIMUM_SUPPORTED_JAVA_VERSION, JParserTestUtils.DEFAULT_CLASSPATH, false)
      .parse(inputFiles, () -> false, new AnalysisProgress(inputFiles.size()), (inputFile, result) -> {
        processed.add(inputFile.filename());
        if (inputFile.filename().equals("Main.java")) {
          assertThatCode(() -> {
            ClassTreeImpl classA = (ClassTreeImpl) result.get().types().get(0);
            MethodTreeImpl methodM = (MethodTreeImpl) classA.members().get(0);
            List<StatementTree> body = methodM.block().body();

            // Even if Dependency.java and GenericDependency.java will be analyzed after Main.java
            // the semantic API of Main.java should have a @Nullable annotation on the
            // method parameter declaration of
            // Dependency#m(@Nullable Object param)
            // GenericDependency#m(@Nullable T param)
            MethodInvocationTree dependencyInvocation = (MethodInvocationTree) ((ExpressionStatementTree) body.get(0)).expression();
            Symbol dependencyParamDeclaration = dependencyInvocation.methodSymbol().declarationParameters().get(0);
            assertThat(dependencyParamDeclaration.owner().owner().name()).isEqualTo("Dependency");
            assertThat(dependencyParamDeclaration.metadata().isAnnotatedWith("semantic.Nullable")).isTrue();

            MethodInvocationTree genericDependencyInvocation = (MethodInvocationTree) ((ExpressionStatementTree) body.get(1)).expression();
            Symbol genericDependencyParamDeclaration = genericDependencyInvocation.methodSymbol().declarationParameters().get(0);
            assertThat(genericDependencyParamDeclaration.owner().owner().name()).isEqualTo("GenericDependency");
            assertThat(genericDependencyParamDeclaration.metadata().isAnnotatedWith("semantic.Nullable")).isTrue();
          }).doesNotThrowAnyException();
        }
      });
    assertThat(processed).containsExactly("Main.java", "Dependency.java", "GenericDependency.java", "Nullable.java");
  }

  @Test
  void testSingleInheritance() {
    JavaTree.CompilationUnitTreeImpl cu = test(""
      + "interface A { void a(); }\n"
      + "class Clazz implements A { public void a() {} }");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(1);
    MethodTreeImpl m = (MethodTreeImpl) c.members().get(0);
    Symbol.MethodSymbol symbol = ((Symbol.MethodSymbol) symbolOf(m));

    assertThat(symbol.overriddenSymbols()).containsOnly(retrieveMethodSymbol("A", "a", cu));
  }

  @Test
  void testClassInheritanceChainOnlyFindsDirectOverride() {
    JavaTree.CompilationUnitTreeImpl cu = test("""
      interface Interface { void a(); }
      class A implements Interface { public void a() {} }
      class B extends A { public void a() {} }
      class C extends B { public void a() {} }
      """);
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(3);
    MethodTreeImpl m = (MethodTreeImpl) c.members().get(0);
    Symbol.MethodSymbol symbol = ((Symbol.MethodSymbol) symbolOf(m));

    assertThat(symbol.overriddenSymbols()).containsExactly(
      retrieveMethodSymbol("B", "a", cu),
      retrieveMethodSymbol("A", "a", cu),
      retrieveMethodSymbol("Interface", "a", cu));
  }

  @Test
  void testObjectExtensionFromClass() {
    JavaTree.CompilationUnitTreeImpl cu = test("class Clazz { public boolean equals(Object other) { return false; } }");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    MethodTreeImpl m = (MethodTreeImpl) c.members().get(0);
    Symbol.MethodSymbol symbol = ((Symbol.MethodSymbol) symbolOf(m));

    assertThat(symbol.overriddenSymbols()).containsExactly(objectEqualsMethod(sema(cu)));
  }

  @Test
  void testObjectExtensionFromInterface() {
    JavaTree.CompilationUnitTreeImpl cu = test("interface Interface { boolean equals(Object other) { return false; } }");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    MethodTreeImpl m = (MethodTreeImpl) c.members().get(0);
    Symbol.MethodSymbol symbol = ((Symbol.MethodSymbol) symbolOf(m));

    assertThat(symbol.overriddenSymbols()).containsExactly(objectEqualsMethod(sema(cu)));
  }

  private static Symbol.MethodSymbol objectEqualsMethod(Sema sema) {
    return sema.getClassType("java.lang.Object").symbol().lookupSymbols("equals").stream()
      .map(Symbol.MethodSymbol.class::cast).findFirst().orElseThrow();
  }

  @Test
  void testMultipleInheritance() {
    JavaTree.CompilationUnitTreeImpl cu = test("""
      interface A { void a(); }
      interface B { void a(); }
      class C implements A, B { public void a() { } }
      """);
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(2);
    MethodTreeImpl m = (MethodTreeImpl) c.members().get(0);
    Symbol.MethodSymbol symbol = ((Symbol.MethodSymbol) symbolOf(m));

    assertThat(symbol.overriddenSymbols()).containsExactly(
      retrieveMethodSymbol("A", "a", cu),
      retrieveMethodSymbol("B", "a", cu));
  }

  @Test
  void testUnknownInheritance() {
    JavaTree.CompilationUnitTreeImpl cu = test(""
      + "interface KnownInterface { void a(); }\n"
      + "class C implements KnownInterface, UnknownInterface { public void a() { } }");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(1);
    MethodTreeImpl m = (MethodTreeImpl) c.members().get(0);
    Symbol.MethodSymbol symbol = ((Symbol.MethodSymbol) symbolOf(m));

    assertThat(symbol.overriddenSymbols()).containsExactly(
      retrieveMethodSymbol("KnownInterface", "a", cu));
  }

  @Test
  void testUnknownExtends() {
    JavaTree.CompilationUnitTreeImpl cu = test(""
      + "class B extends Unknown { public void a() { } }");
    ClassTreeImpl a = (ClassTreeImpl) cu.types().get(0);
    MethodTreeImpl m = (MethodTreeImpl) a.members().get(0);
    Symbol.MethodSymbol symbol = ((Symbol.MethodSymbol) symbolOf(m));
    assertThat(symbol.overriddenSymbols()).isEmpty();
  }

  @Test
  void testMultipleInheritanceWithExtensionOfObject() {
    JavaTree.CompilationUnitTreeImpl cu = test("""
      interface A { boolean equals(Object other); }
      interface B { boolean equals(Object other); }
      interface C extends B { }
      class Clazz implements A, C { public boolean equals(Object other) { return false; } }
      """);
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(3);
    MethodTreeImpl m = (MethodTreeImpl) c.members().get(0);
    Symbol.MethodSymbol symbol = ((Symbol.MethodSymbol) symbolOf(m));

    assertThat(symbol.overriddenSymbols()).containsExactly(
      objectEqualsMethod(sema(cu)),
      retrieveMethodSymbol("A", "equals", cu),
      retrieveMethodSymbol("B", "equals", cu));
  }

  private static Symbol.MethodSymbol retrieveMethodSymbol(String className, String methodName, CompilationUnitTreeImpl cu) {
    MethodMatchers methodMatcher = MethodMatchers.create()
      .ofTypes(className)
      .names(methodName)
      .withAnyParameters()
      .build();
    return cu.types().stream()
      .filter(ClassTreeImpl.class::isInstance)
      .map(ClassTreeImpl.class::cast)
      .map(ClassTreeImpl::symbol)
      .map(Symbol.TypeSymbol::memberSymbols)
      .flatMap(Collection::stream)
      .filter(methodMatcher::matches)
      .map(Symbol.MethodSymbol.class::cast)
      .findFirst()
      .orElseThrow(() -> new IllegalStateException("No method could be found with the given name in the given class"));
  }

  @Test
  void signature() {
    JavaTree.CompilationUnitTreeImpl cu = test("package org.example; class C { C() {} <T> Object m(Object p1, Object[] p2, T p3) { return m(null, null, 42); } }");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    MethodTreeImpl constructor = (MethodTreeImpl) c.members().get(0);
    MethodTreeImpl method = (MethodTreeImpl) c.members().get(1);
    ReturnStatementTreeImpl s = (ReturnStatementTreeImpl) Objects.requireNonNull(method.block()).body().get(0);
    MethodInvocationTreeImpl methodInvocation = Objects.requireNonNull((MethodInvocationTreeImpl) s.expression());
    assertThat(((Symbol.MethodSymbol) symbolOf(constructor)).signature())
      .isEqualTo(constructor.symbol().signature())
      .isEqualTo("org.example.C#<init>()V");
    assertThat(((Symbol.MethodSymbol) symbolOf(method)).signature())
      .isEqualTo(method.symbol().signature())
      .isEqualTo("org.example.C#m(Ljava/lang/Object;[Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
    assertThat(((Symbol.MethodSymbol) symbolOf(methodInvocation)).signature())
      .isEqualTo(method.symbol().signature())
      .isEqualTo("org.example.C#m(Ljava/lang/Object;[Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
  }

  @Test
  void isOverridableTest(){
    Symbol.MethodSymbol jms = getJMethodSymbolFromClassText("class C { C m() { return null; } }");
    assertThat(jms.isOverridable()).isTrue();
    jms = getJMethodSymbolFromClassText("class C { private C m() { return null; } }");
    assertThat(jms.isOverridable()).isFalse();
    jms = getJMethodSymbolFromClassText("class C { static C m() { return null; } }");
    assertThat(jms.isOverridable()).isFalse();
    jms = getJMethodSymbolFromClassText("class C { final C m() { return null; } }");
    assertThat(jms.isOverridable()).isFalse();
    jms = getJMethodSymbolFromClassText("final class C { C m() { return null; } }");
    assertThat(jms.isOverridable()).isFalse();
  }

  @Test
  void isParametrizedMethodTest(){
    Symbol.MethodSymbol jms = getJMethodSymbolFromClassText("class C { C m() { return null; } }");
    assertThat(jms.isParametrizedMethod()).isFalse();
    jms = getJMethodSymbolFromClassText("class C { <T> C m() { return null; } }");
    assertThat(jms.isParametrizedMethod()).isTrue();
    jms = getJMethodSymbolFromClassText("class C { <T> Collection<T> m(T t) { return null; } }");
    assertThat(jms.isParametrizedMethod()).isTrue();
  }

  @Test
  void isDefaultMethodTest(){
    Symbol.MethodSymbol jms = getJMethodSymbolFromClassText("class C { C m() { return null; } }");
    assertThat(jms.isDefaultMethod()).isFalse();
    jms = getJMethodSymbolFromClassText("interface I { default C m() { return null; } }");
    assertThat(jms.isDefaultMethod()).isTrue();
  }

  @Test
  void isSynchronizedMethodTest(){
    Symbol.MethodSymbol jms = getJMethodSymbolFromClassText("class C { C m() { return null; } }");
    assertThat(jms.isSynchronizedMethod()).isFalse();
    jms = getJMethodSymbolFromClassText("class C { synchronized C m() { return null; } }");
    assertThat(jms.isSynchronizedMethod()).isTrue();
  }

  @Test
  void isVarArgsTest(){
    Symbol.MethodSymbol jms = getJMethodSymbolFromClassText("class C { C m() { return null; } }");
    assertThat(jms.isVarArgsMethod()).isFalse();
    jms = getJMethodSymbolFromClassText("class C { C m(String... s) { return null; } }");
    assertThat(jms.isVarArgsMethod()).isTrue();
  }

  @Test
  void testParameterDeclarationsOfCompactConstructor() {
    JavaTree.CompilationUnitTreeImpl cu = test("""
      record TestSonar(String arg1, String arg2, String arg3, String arg4, long arg5, String arg6) {
        public TestSonar {}
        public static void f() {
          new TestSonar(null, null, null, null, 0L, null);
        }
      }
      """);

    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    MethodTreeImpl m = (MethodTreeImpl) c.members().get(1);
    ExpressionStatementTree statementTree = ((ExpressionStatementTreeImpl) m.block().body().get(0));
    NewClassTreeImpl newClassExpression = ((NewClassTreeImpl) statementTree.expression());
    Symbol.MethodSymbol symbol = ((Symbol.MethodSymbol) newClassExpression.methodSymbol());

    assertThat(symbol.declarationParameters()).hasSize(6);
  }

  private static Symbol.MethodSymbol getJMethodSymbolFromClassText(String classText){
    return ((ClassTreeImpl) test(classText).types().get(0)).members().stream()
      .filter(MethodTreeImpl.class::isInstance).map(MethodTreeImpl.class::cast).findFirst().orElseThrow().symbol();
  }
  private static CompilationUnitTreeImpl test(String source) {
    return (CompilationUnitTreeImpl) JParserTestUtils.parse(source);
  }

  @Nested
  class IsNativeMethod {
    private final JavaTree.CompilationUnitTreeImpl cu = test("""
        class A {
          native void foo();
          void bar() { }
        }
        """);
    private final ClassTreeImpl a = firstClass(cu);

    @Test
    void isNativeMethod() {
      MethodTreeImpl nativeMethod = firstMethod(a);
      assertThat(nativeMethod.symbol().isNativeMethod()).isTrue();
    }

    @Test
    void not_native() {
      MethodTreeImpl nonNativeMethod = nthMethod(a, 1);
      assertThat(nonNativeMethod.symbol().isNativeMethod()).isFalse();
    }

    @Test
    void unknown_method_is_not_native() {
      assertThat(Symbol.MethodSymbol.UNKNOWN_METHOD.isNativeMethod()).isFalse();
    }
  }

  @Test
  void isLambda() {
    JavaTree.CompilationUnitTreeImpl cu = test("""
        class A {
          void foo() { }
          java.util.function.IntConsumer bar = i -> { };
        }
        """);
    ClassTreeImpl a = firstClass(cu);

    MethodTreeImpl foo = firstMethod(a);
    var fooSymbol = (Symbol.MethodSymbol) foo.symbol();
    assertFalse(((PsiMethodSymbol) fooSymbol).isLambda());
    assertThat(fooSymbol.toString()).isNotEmpty();

    var barAssignment = (VariableTree) a.members().get(1);
    var initializer = (LambdaExpressionTree) barAssignment.initializer();
    var lambdaSymbol = (Symbol.MethodSymbol) initializer.symbol();
    assertTrue(((PsiMethodSymbol) lambdaSymbol).isLambda());
    assertThat(lambdaSymbol.toString()).isNotEmpty();
  }
}
