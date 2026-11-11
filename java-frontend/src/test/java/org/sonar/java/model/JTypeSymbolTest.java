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
import org.sonar.java.model.declaration.ClassTreeImpl;
import org.sonar.java.model.declaration.MethodTreeImpl;
import org.sonar.java.model.declaration.VariableTreeImpl;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.java.model.JParserTestUtils.sema;
import static org.sonar.java.model.JParserTestUtils.typeOf;

class JTypeSymbolTest {

  @Test
  void superClass() {
    var cu = test("interface I {} class C implements I {} class C2 extends C {} class A { Object[] array; }");
    Type object = sema(cu).getClassType("java.lang.Object");
    var i = (ClassTreeImpl) cu.types().get(0);
    var c = (ClassTreeImpl) cu.types().get(1);
    var c2 = (ClassTreeImpl) cu.types().get(2);
    var a = (ClassTreeImpl) cu.types().get(3);
    var array = (VariableTreeImpl) a.members().get(0);

    assertThat(object.symbol().superClass()).isNull();
    assertThat(i.symbol().superClass()).isSameAs(object);
    assertThat(c.symbol().superClass()).isSameAs(object);
    assertThat(c2.symbol().superClass()).isSameAs(c.symbol().type());
    assertThat(array.symbol().type().symbol().superClass()).isSameAs(object);
  }

  @Test
  void interfaces() {
    var cu = test("interface I {} class C implements I {}");
    var i = (ClassTreeImpl) cu.types().get(0);
    var c = (ClassTreeImpl) cu.types().get(1);
    assertThat(i.symbol().interfaces()).isEmpty();
    assertThat(c.symbol().interfaces()).containsOnly(i.symbol().type());
  }

  @Test
  void memberSymbols() {
    JavaTree.CompilationUnitTreeImpl cu = test("class C { int f; C() {} class N {} }");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    VariableTreeImpl f = (VariableTreeImpl) c.members().get(0);
    MethodTreeImpl m = (MethodTreeImpl) c.members().get(1);
    ClassTreeImpl n = (ClassTreeImpl) c.members().get(2);
    assertThat(typeOf(c).symbol().memberSymbols())
      .containsOnly(
        f.symbol(),
        m.symbol(),
        typeOf(n).symbol()
      );
  }

  @Test
  void superTypesTest() {
    JavaTree.CompilationUnitTreeImpl cu = test("interface I { } class C implements I { } class C2 extends C { }");
    Type javaLangObject = sema(cu).getClassType("java.lang.Object");
    assertThat(javaLangObject.symbol().superTypes()).isEmpty();

    ClassTreeImpl interfaceI = (ClassTreeImpl) cu.types().get(0);
    Symbol.TypeSymbol interfaceITypeSymbol = typeOf(interfaceI).symbol();
    assertThat(interfaceITypeSymbol.superTypes()).isEmpty();
    assertThat(interfaceITypeSymbol.superTypes()).isEmpty(); // repeat call to cover cache

    ClassTreeImpl classC = (ClassTreeImpl) cu.types().get(1);
    Symbol.TypeSymbol classCTypeSymbol = typeOf(classC).symbol();
    assertThat(classCTypeSymbol.superTypes()).containsExactly(interfaceITypeSymbol.type(), javaLangObject);

    ClassTreeImpl classC2 = (ClassTreeImpl) cu.types().get(2);
    Symbol.TypeSymbol classC2TypeSymbol = typeOf(classC2).symbol();
    assertThat(classC2TypeSymbol.superTypes()).containsExactly(classCTypeSymbol.type(), interfaceITypeSymbol.type(), javaLangObject);
  }

  @Test
  void outermostClassTest() {
    JavaTree.CompilationUnitTreeImpl cu = test("class C { class N {} }");
    ClassTreeImpl outerClass = (ClassTreeImpl) cu.types().get(0);
    ClassTreeImpl innerClass = (ClassTreeImpl) outerClass.members().get(0);
    Symbol.TypeSymbol innerClassSymbol = typeOf(innerClass).symbol();
    Symbol.TypeSymbol outerClassSymbol = typeOf(outerClass).symbol();
    assertThat(innerClassSymbol.outermostClass()).isSameAs(outerClassSymbol);
    assertThat(innerClassSymbol.outermostClass()).isSameAs(outerClassSymbol);
  }

  @Test
  void isAnnotationTest() {
    Symbol.TypeSymbol annotationSymbol = typeSymbolFromClassText("@interface A { }");
    assertThat(annotationSymbol.isAnnotation()).isTrue();
    Symbol.TypeSymbol classSymbol = typeSymbolFromClassText("class C { }");
    assertThat(classSymbol.isAnnotation()).isFalse();
  }

  @Test
  void isEffectivelyFinalTest() {
    var cu = test("class C { String m() { return super.toString(); } }");
    var c = (ClassTreeImpl) cu.types().get(0);
    var m = (MethodTreeImpl) c.members().get(0);
    var statement = (ReturnStatementTree) m.block().body().get(0);
    var invocation = (MethodInvocationTree) statement.expression();
    var select = (MemberSelectExpressionTree) invocation.methodSelect();
    var keywordSuper = (IdentifierTree) select.expression();

    assertThat(((Symbol.VariableSymbol) keywordSuper.symbol()).isEffectivelyFinal()).isFalse();
  }

  private static Symbol.TypeSymbol typeSymbolFromClassText(String classText) {
    var cu = test(classText);
    return ((ClassTreeImpl) cu.types().get(0)).symbol();
  }

  private static JavaTree.CompilationUnitTreeImpl test(String source) {
    return (JavaTree.CompilationUnitTreeImpl) JParserTestUtils.parse(source);
  }

}
