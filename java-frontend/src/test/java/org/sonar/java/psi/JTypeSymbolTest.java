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

import org.junit.jupiter.api.Test;
import org.sonar.java.model.JParserTestUtils;
import org.sonar.java.model.JavaTree;
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
    Type javaLangObject = sema(cu).getClassType("java.lang.Object");
    Type i = typeOf(cu.types().get(0));
    Type c = typeOf(cu.types().get(1));
    Type c2 = typeOf(cu.types().get(2));
    assertAll(
      // for java.lang.Object
      () ->
        assertThat(javaLangObject.symbol().superClass())
          .isNull(),
      // for interfaces
      () ->
        assertThat(i.symbol().superClass())
          .isSameAs(javaLangObject),
      // for classes
      () ->
        assertThat(c.symbol().superClass())
          .isSameAs(javaLangObject),
      () ->
        assertThat(c2.symbol().superClass())
          .isSameAs(c),
      // for arrays
      () ->
        assertThat(sema(cu).getClassType("java.lang.Object[]").symbol().superClass())
          .isSameAs(javaLangObject)
    );
  }

  @Test
  void interfaces() {
    JavaTree.CompilationUnitTreeImpl cu = test("interface I { } class C implements I { }");
    Type i = typeOf(cu.types().get(0));
    Type c = typeOf(cu.types().get(1));
    assertThat(i.symbol().interfaces())
      .isEmpty();
    assertThat(c.symbol().interfaces())
      .containsOnly(i);
  }

  @Test
  void memberSymbols() {
    JavaTree.CompilationUnitTreeImpl cu = test("class C { int f; C() {} class N {} }");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    VariableTreeImpl f = (VariableTreeImpl) c.members().get(0);
    MethodTreeImpl m = (MethodTreeImpl) c.members().get(1);
    ClassTreeImpl n = (ClassTreeImpl) c.members().get(2);
    assertThat(((Symbol.TypeSymbol) typeOf(c).symbol()).memberSymbols())
      .containsOnly(
        ((Symbol.VariableSymbol) symbolOf(f)),
        ((Symbol.MethodSymbol) symbolOf(m)),
        ((Symbol.TypeSymbol) typeOf(n).symbol())
      );
  }

  @Test
  void superTypesTest(){
    JavaTree.CompilationUnitTreeImpl cu = test("interface I { } class C implements I { } class C2 extends C { }");
    Type javaLangObject = sema(cu).getClassType("java.lang.Object");
    assertThat(javaLangObject.symbol().superTypes()).isEmpty();

    ClassTreeImpl interfaceI = (ClassTreeImpl) cu.types().get(0);
    Symbol.TypeSymbol interfaceITypeSymbol = ((Symbol.TypeSymbol) typeOf(interfaceI).symbol());
    assertThat(interfaceITypeSymbol.superTypes()).isEmpty();
    assertThat(interfaceITypeSymbol.superTypes()).isEmpty(); // repeat call to cover cache

    ClassTreeImpl classC = (ClassTreeImpl) cu.types().get(1);
    Symbol.TypeSymbol classCTypeSymbol = ((Symbol.TypeSymbol) typeOf(classC).symbol());
    assertThat(classCTypeSymbol.superTypes()).containsExactly(interfaceITypeSymbol.type(), javaLangObject);

    ClassTreeImpl classC2 = (ClassTreeImpl) cu.types().get(2);
    Symbol.TypeSymbol classC2TypeSymbol = ((Symbol.TypeSymbol) typeOf(classC2).symbol());
    assertThat(classC2TypeSymbol.superTypes()).containsExactly(classCTypeSymbol.type() ,interfaceITypeSymbol.type(), javaLangObject);


  }

  @Test
  void outermostClassTest() {
    JavaTree.CompilationUnitTreeImpl cu = test("class C { class N {} }");
    ClassTreeImpl outerClass = (ClassTreeImpl) cu.types().get(0);
    ClassTreeImpl innerClass = (ClassTreeImpl) outerClass.members().get(0);
    Symbol.TypeSymbol innerClassSymbol = ((Symbol.TypeSymbol) typeOf(innerClass).symbol());
    Symbol.TypeSymbol outerClassSymbol = ((Symbol.TypeSymbol) typeOf(outerClass).symbol());
    assertThat(innerClassSymbol.outermostClass()).isSameAs(outerClassSymbol);
    assertThat(innerClassSymbol.outermostClass()).isSameAs(outerClassSymbol);
  }

  @Test
  void isAnnotationTest(){
    Symbol.TypeSymbol annotationSymbol = getJTypeSymbolFromClassText("@interface A { }", true);
    assertThat(annotationSymbol.isAnnotation()).isFalse();
    annotationSymbol = getJTypeSymbolFromClassText("@interface A { }", false);
    assertThat(annotationSymbol.isAnnotation()).isTrue();
    Symbol.TypeSymbol classSymbol = getJTypeSymbolFromClassText("class C { }", false);
    assertThat(classSymbol.isAnnotation()).isFalse();
  }

  @Test
  void isEffectivelyFinalTest(){
    Symbol.TypeSymbol classSymbol = getJTypeSymbolFromClassText("class C { }", false);
    assertThat(((PsiTypeSymbol) classSymbol).superSymbol.isEffectivelyFinal()).isFalse();
  }

  private static Symbol.TypeSymbol getJTypeSymbolFromClassText(String classText, boolean isUnknown){
    JavaTree.CompilationUnitTreeImpl cu = test(classText);
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    return isUnknown ? Symbol.TypeSymbol.UNKNOWN_TYPE : c.symbol();
  }

  private static JavaTree.CompilationUnitTreeImpl test(String source) {
    return (JavaTree.CompilationUnitTreeImpl) JParserTestUtils.parse(source);
  }

}
