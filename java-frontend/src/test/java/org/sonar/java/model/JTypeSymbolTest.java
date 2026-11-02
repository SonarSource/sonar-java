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
import org.sonar.java.test.TestResourceScope;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@TestResourceScope.Scoped
class JTypeSymbolTest {

  @Test
  void superClass() {
    SemanticFixture fixture = SemanticFixture.scoped("interface I { } class C implements I { } class C2 extends C { }");
    Type javaLangObject = fixture.type("java.lang.Object");
    var i = fixture.declaration(0).symbol();
    var c = fixture.declaration(1).symbol();
    var c2 = fixture.declaration(2).symbol();
    assertAll(
      () -> assertThat(javaLangObject.symbol().superClass()).isNull(),
      () -> assertThat(i.superClass()).isSameAs(javaLangObject),
      () -> assertThat(c.superClass()).isSameAs(javaLangObject),
      () -> assertThat(c2.superClass()).isSameAs(c.type()),
      () -> assertThat(fixture.type("java.lang.Object[]").symbol().superClass()).isSameAs(javaLangObject)
    );
  }

  @Test
  void testSuperClassWhenSemanticCanNotResolveObjectType() {
    var cu = test("interface I{}");
    var original = (JType) ((ClassTreeImpl) cu.types().getFirst()).symbol().type();
    var broken = spy((JSema) cu.sema);
    when(broken.getClassType("java.lang.Object")).thenReturn(Type.UNKNOWN);
    var type = new JType(broken, original.psi, original.key);
    assertThat(new JTypeSymbol(broken, type, original.key).superClass()).isSameAs(Type.UNKNOWN);
  }

  @Test
  void interfaces() {
    SemanticFixture fixture = SemanticFixture.scoped("interface I { } class C implements I { }");
    var i = fixture.declaration(0).symbol();
    var c = fixture.declaration(1).symbol();
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
    assertThat(SemanticTestSupport.type(c).symbol().memberSymbols())
      .containsOnly(
        SemanticTestSupport.symbol(f),
        SemanticTestSupport.method(m),
        SemanticTestSupport.type(n).symbol()
      );
  }

  @Test
  void superTypesTest(){
    JavaTree.CompilationUnitTreeImpl cu = test("interface I { } class C implements I { } class C2 extends C { }");
    Type javaLangObject = cu.sema.getClassType("java.lang.Object");
    assertThat(javaLangObject.symbol().superTypes()).isEmpty();

    ClassTreeImpl interfaceI = (ClassTreeImpl) cu.types().get(0);
    Symbol.TypeSymbol interfaceITypeSymbol = SemanticTestSupport.type(interfaceI).symbol();
    assertThat(interfaceITypeSymbol.superTypes()).isEmpty();
    assertThat(interfaceITypeSymbol.superTypes()).isEmpty(); // repeat call to cover cache

    ClassTreeImpl classC = (ClassTreeImpl) cu.types().get(1);
    Symbol.TypeSymbol classCTypeSymbol = SemanticTestSupport.type(classC).symbol();
    assertThat(classCTypeSymbol.superTypes()).containsExactly(interfaceITypeSymbol.type(), javaLangObject);


    ClassTreeImpl classC2 = (ClassTreeImpl) cu.types().get(2);
    Symbol.TypeSymbol classC2TypeSymbol = SemanticTestSupport.type(classC2).symbol();
    assertThat(classC2TypeSymbol.superTypes()).containsExactly(classCTypeSymbol.type(), interfaceITypeSymbol.type(), javaLangObject);
  }

  @Test
  void recovered_super_types() {
    var cu = test("interface I { } class C implements I { } class C2 extends C { }");
    var original = (JType) ((ClassTreeImpl) cu.types().get(2)).symbol().type();
    var broken = spy((com.intellij.psi.PsiClassType) original.psi);
    when(broken.resolve()).thenReturn(null);
    var type = new JType((JSema) cu.sema, broken, original.key);
    assertThat(new JTypeSymbol((JSema) cu.sema, type, original.key).superTypes()).isEmpty();
  }

  @Test
  void outermostClassTest() {
    JavaTree.CompilationUnitTreeImpl cu = test("class C { class N {} }");
    ClassTreeImpl outerClass = (ClassTreeImpl) cu.types().get(0);
    ClassTreeImpl innerClass = (ClassTreeImpl) outerClass.members().get(0);
    Symbol.TypeSymbol innerClassSymbol = SemanticTestSupport.type(innerClass).symbol();
    Symbol.TypeSymbol outerClassSymbol = SemanticTestSupport.type(outerClass).symbol();
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
    var classSymbol = getJTypeSymbolFromClassText("class C { }", false);
    assertThat(((Symbol.VariableSymbol) SemanticTestSupport.specialField(classSymbol.type(), true)).isEffectivelyFinal()).isFalse();
  }

  private static Symbol.TypeSymbol getJTypeSymbolFromClassText(String classText, boolean isUnknown){
    JavaTree.CompilationUnitTreeImpl cu = test(classText);
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    if (isUnknown) {
      return Symbol.TypeSymbol.UNKNOWN_TYPE;
    }
    return c.symbol();
  }

  private static JavaTree.CompilationUnitTreeImpl test(String source) {
    return (JavaTree.CompilationUnitTreeImpl) JParserTestUtils.parse(source);
  }

}
