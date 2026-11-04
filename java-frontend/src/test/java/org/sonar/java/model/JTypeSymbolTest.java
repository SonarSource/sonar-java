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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.sonar.java.model.JParserTestUtils.sema;
import static org.sonar.java.model.JParserTestUtils.symbolOf;
import static org.sonar.java.model.JParserTestUtils.typeOf;

class JTypeSymbolTest {

  @Test
  void superClass() {
    JavaTree.CompilationUnitTreeImpl cu = test("interface I { } class C implements I { } class C2 extends C { }");
    Type object = sema(cu).getClassType("java.lang.Object");
    JTypeSymbol i = (JTypeSymbol) ((ClassTreeImpl) cu.types().get(0)).symbol();
    JTypeSymbol c = (JTypeSymbol) ((ClassTreeImpl) cu.types().get(1)).symbol();
    JTypeSymbol c2 = (JTypeSymbol) ((ClassTreeImpl) cu.types().get(2)).symbol();
    assertAll(
      () -> assertThat(object.symbol().superClass()).isNull(),
      () -> assertThat(i.superClass()).isSameAs(object),
      () -> assertThat(c.superClass()).isSameAs(object),
      () -> assertThat(c2.superClass().is(c.type().fullyQualifiedName())).isTrue(),
      () -> assertThat(sema(cu).getClassType("java.lang.Object[]").symbol().superClass()).isSameAs(object)
    );
  }

  @Test
  void interfaces() {
    JavaTree.CompilationUnitTreeImpl cu = test("interface I { } class C implements I { }");
    JTypeSymbol i = (JTypeSymbol) ((ClassTreeImpl) cu.types().get(0)).symbol();
    JTypeSymbol c = (JTypeSymbol) ((ClassTreeImpl) cu.types().get(1)).symbol();
    assertThat(i.interfaces()).isEmpty();
    assertThat(c.interfaces()).containsOnly(i.type());
  }

  @Test
  void memberSymbols() {
    JavaTree.CompilationUnitTreeImpl cu = test("class C { int f; C() {} class N {} }");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    VariableTreeImpl f = (VariableTreeImpl) c.members().get(0);
    MethodTreeImpl m = (MethodTreeImpl) c.members().get(1);
    ClassTreeImpl n = (ClassTreeImpl) c.members().get(2);
    assertThat(((JTypeSymbol) typeOf(c).symbol()).memberSymbols())
      .containsOnly(
        ((JVariableSymbol) symbolOf(f)),
        ((JMethodSymbol) symbolOf(m)),
        ((JTypeSymbol) typeOf(n).symbol())
      );
  }

  @Test
  void superTypesTest(){
    JavaTree.CompilationUnitTreeImpl cu = test("interface I { } class C implements I { } class C2 extends C { }");
    Type javaLangObject = sema(cu).getClassType("java.lang.Object");
    assertThat(javaLangObject.symbol().superTypes()).isEmpty();

    ClassTreeImpl interfaceI = (ClassTreeImpl) cu.types().get(0);
    JTypeSymbol interfaceITypeSymbol = ((JTypeSymbol) typeOf(interfaceI).symbol());
    assertThat(interfaceITypeSymbol.superTypes()).containsExactly(javaLangObject);
    assertThat(interfaceITypeSymbol.superTypes()).containsExactly(javaLangObject);

    ClassTreeImpl classC = (ClassTreeImpl) cu.types().get(1);
    JTypeSymbol classCTypeSymbol = ((JTypeSymbol) typeOf(classC).symbol());
    assertThat(classCTypeSymbol.superTypes()).containsExactly(interfaceITypeSymbol.type(), javaLangObject);

    ClassTreeImpl classC2 = (ClassTreeImpl) cu.types().get(2);
    JTypeSymbol classC2TypeSymbol = ((JTypeSymbol) typeOf(classC2).symbol());
    assertThat(classC2TypeSymbol.superTypes()).containsExactly(classCTypeSymbol.type() ,interfaceITypeSymbol.type(), javaLangObject);

    classC2TypeSymbol.unknown(true);
    assertThat(classC2TypeSymbol.superTypes()).isEmpty();
  }

  @Test
  void outermostClassTest() {
    JavaTree.CompilationUnitTreeImpl cu = test("class C { class N {} }");
    ClassTreeImpl outerClass = (ClassTreeImpl) cu.types().get(0);
    ClassTreeImpl innerClass = (ClassTreeImpl) outerClass.members().get(0);
    JTypeSymbol innerClassSymbol = ((JTypeSymbol) typeOf(innerClass).symbol());
    JTypeSymbol outerClassSymbol = ((JTypeSymbol) typeOf(outerClass).symbol());
    assertThat(innerClassSymbol.outermostClass()).isSameAs(outerClassSymbol);
    assertThat(innerClassSymbol.outermostClass()).isSameAs(outerClassSymbol);
  }

  @Test
  void isAnnotationTest(){
    JTypeSymbol annotationSymbol = getJTypeSymbolFromClassText("@interface A { }", true);
    assertThat(annotationSymbol.isAnnotation()).isFalse();
    annotationSymbol = getJTypeSymbolFromClassText("@interface A { }", false);
    assertThat(annotationSymbol.isAnnotation()).isTrue();
    JTypeSymbol classSymbol = getJTypeSymbolFromClassText("class C { }", false);
    assertThat(classSymbol.isAnnotation()).isFalse();
  }

  @Test
  void isEffectivelyFinalTest(){
    JavaTree.CompilationUnitTreeImpl cu = test("class C { }");
    JTypeSymbol classSymbol = (JTypeSymbol) ((ClassTreeImpl) cu.types().get(0)).symbol();
    Symbol superReceiver = sema(cu).receiverSymbol(classSymbol, "super", classSymbol.superClass());
    assertThat(superReceiver.name()).isEqualTo("super");
    assertThat(superReceiver.owner()).isSameAs(classSymbol);
  }

  private static JTypeSymbol getJTypeSymbolFromClassText(String classText, boolean isUnknown){
    JavaTree.CompilationUnitTreeImpl cu = test(classText);
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    JTypeSymbol symbol = (JTypeSymbol) c.symbol();
    if (isUnknown) {
      symbol.unknown(true);
    }
    return symbol;
  }

  private static JavaTree.CompilationUnitTreeImpl test(String source) {
    return (JavaTree.CompilationUnitTreeImpl) JParserTestUtils.parse(source);
  }

}
