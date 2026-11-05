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
package org.sonar.java.jdt;

import java.util.Objects;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.junit.jupiter.api.Test;
import org.sonar.java.model.JParserTestUtils;
import org.sonar.java.model.JavaTree;
import org.sonar.java.model.declaration.ClassTreeImpl;
import org.sonar.java.model.declaration.MethodTreeImpl;
import org.sonar.java.model.declaration.VariableTreeImpl;
import org.sonar.plugins.java.api.semantic.Type;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.sonar.java.jdt.JdtTestUtils.sema;
import static org.sonar.java.jdt.JdtTestUtils.typeBinding;
import static org.sonar.java.model.JParserTestUtils.symbolOf;
import static org.sonar.java.model.JParserTestUtils.typeOf;

class JTypeSymbolTest {

  @Test
  void superClass() {
    JavaTree.CompilationUnitTreeImpl cu = test("interface I { } class C implements I { } class C2 extends C { }");
    ITypeBinding javaLangObject = Objects.requireNonNull(sema(cu).resolveType("java.lang.Object"));
    ITypeBinding i = Objects.requireNonNull(typeBinding(((ClassTreeImpl) cu.types().get(0))));
    ITypeBinding c = Objects.requireNonNull(typeBinding(((ClassTreeImpl) cu.types().get(1))));
    ITypeBinding c2 = Objects.requireNonNull(typeBinding(((ClassTreeImpl) cu.types().get(2))));
    assertAll(
      // for java.lang.Object
      () ->
        assertThat(sema(cu).typeSymbol(javaLangObject).superClass())
          .isNull(),
      // for interfaces
      () ->
        assertThat(sema(cu).typeSymbol(i).superClass())
          .isSameAs(sema(cu).type(javaLangObject)),
      // for classes
      () ->
        assertThat(sema(cu).typeSymbol(c).superClass())
          .isSameAs(sema(cu).type(javaLangObject)),
      () ->
        assertThat(sema(cu).typeSymbol(c2).superClass())
          .isSameAs(sema(cu).type(c)),
      // for arrays
      () ->
        assertThat(sema(cu).typeSymbol(javaLangObject.createArrayType(1)).superClass())
          .isSameAs(sema(cu).type(javaLangObject))
    );
  }

  @Test
  void testSuperClassWhenSemanticCanNotResolveObjectType() {
    var cu = test("interface I{}");
    var i = (ClassTreeImpl) cu.types().get(0);

    JDTSema sematicThatCanNotResolveObjectType = spy(sema(cu));
    when(sematicThatCanNotResolveObjectType.resolveType("java.lang.Object")).thenReturn(null);
    var typeSymbol = new JDTTypeSymbol(sematicThatCanNotResolveObjectType, typeBinding(i));
    assertThat(typeSymbol.superClass()).isSameAs(Type.UNKNOWN);
  }

  @Test
  void interfaces() {
    JavaTree.CompilationUnitTreeImpl cu = test("interface I { } class C implements I { }");
    ITypeBinding i = Objects.requireNonNull(typeBinding(((ClassTreeImpl) cu.types().get(0))));
    ITypeBinding c = Objects.requireNonNull(typeBinding(((ClassTreeImpl) cu.types().get(1))));
    assertThat(sema(cu).typeSymbol(i).interfaces())
      .isEmpty();
    assertThat(sema(cu).typeSymbol(c).interfaces())
      .containsOnly(sema(cu).type(i));
  }

  @Test
  void memberSymbols() {
    JavaTree.CompilationUnitTreeImpl cu = test("class C { int f; C() {} class N {} }");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    VariableTreeImpl f = (VariableTreeImpl) c.members().get(0);
    MethodTreeImpl m = (MethodTreeImpl) c.members().get(1);
    ClassTreeImpl n = (ClassTreeImpl) c.members().get(2);
    assertThat(((JDTTypeSymbol) typeOf(c).symbol()).memberSymbols())
      .containsOnly(
        ((JDTVariableSymbol) symbolOf(f)),
        ((JDTMethodSymbol) symbolOf(m)),
        ((JDTTypeSymbol) typeOf(n).symbol())
      );
  }

  @Test
  void superTypesTest(){
    JavaTree.CompilationUnitTreeImpl cu = test("interface I { } class C implements I { } class C2 extends C { }");
    ITypeBinding javaLangObject = Objects.requireNonNull(sema(cu).resolveType("java.lang.Object"));
    assertThat(sema(cu).typeSymbol(javaLangObject).superTypes()).isEmpty();

    ClassTreeImpl interfaceI = (ClassTreeImpl) cu.types().get(0);
    JDTTypeSymbol interfaceITypeSymbol = ((JDTTypeSymbol) typeOf(interfaceI).symbol());
    assertThat(interfaceITypeSymbol.superTypes()).isEmpty();
    assertThat(interfaceITypeSymbol.superTypes()).isEmpty(); // repeat call to cover cache

    ClassTreeImpl classC = (ClassTreeImpl) cu.types().get(1);
    JDTTypeSymbol classCTypeSymbol = ((JDTTypeSymbol) typeOf(classC).symbol());
    assertThat(classCTypeSymbol.superTypes()).containsExactly(interfaceITypeSymbol.type(), sema(cu).type(javaLangObject));

    ClassTreeImpl classC2 = (ClassTreeImpl) cu.types().get(2);
    JDTTypeSymbol classC2TypeSymbol = ((JDTTypeSymbol) typeOf(classC2).symbol());
    assertThat(classC2TypeSymbol.superTypes()).containsExactly(classCTypeSymbol.type() ,interfaceITypeSymbol.type(), sema(cu).type(javaLangObject));

    ITypeBinding brokenTypeBinding = spy(typeBinding(classC2));
    when(brokenTypeBinding.isRecovered()).thenReturn(true);
    JDTTypeSymbol brokenTypeSymbol = new JDTTypeSymbol(sema(cu), brokenTypeBinding);
    assertThat(brokenTypeSymbol.superTypes()).isEmpty();
  }

  @Test
  void outermostClassTest() {
    JavaTree.CompilationUnitTreeImpl cu = test("class C { class N {} }");
    ClassTreeImpl outerClass = (ClassTreeImpl) cu.types().get(0);
    ClassTreeImpl innerClass = (ClassTreeImpl) outerClass.members().get(0);
    JDTTypeSymbol innerClassSymbol = ((JDTTypeSymbol) typeOf(innerClass).symbol());
    JDTTypeSymbol outerClassSymbol = ((JDTTypeSymbol) typeOf(outerClass).symbol());
    assertThat(innerClassSymbol.outermostClass()).isSameAs(outerClassSymbol);
    assertThat(innerClassSymbol.outermostClass()).isSameAs(outerClassSymbol);
  }

  @Test
  void isAnnotationTest(){
    JDTTypeSymbol annotationSymbol = getJTypeSymbolFromClassText("@interface A { }", true);
    assertThat(annotationSymbol.isAnnotation()).isFalse();
    annotationSymbol = getJTypeSymbolFromClassText("@interface A { }", false);
    assertThat(annotationSymbol.isAnnotation()).isTrue();
    JDTTypeSymbol classSymbol = getJTypeSymbolFromClassText("class C { }", false);
    assertThat(classSymbol.isAnnotation()).isFalse();
  }

  @Test
  void isEffectivelyFinalTest(){
    JDTTypeSymbol classSymbol = getJTypeSymbolFromClassText("class C { }", false);
    assertThat(classSymbol.superSymbol.isEffectivelyFinal()).isFalse();
  }

  private static JDTTypeSymbol getJTypeSymbolFromClassText(String classText, boolean isUnknown){
    JavaTree.CompilationUnitTreeImpl cu = test(classText);
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    if(isUnknown){
      ITypeBinding brokenTypeBinding = spy(typeBinding(c));
      when(brokenTypeBinding.isRecovered()).thenReturn(true);
      return new JDTTypeSymbol(sema(cu), brokenTypeBinding);
    }
    return new JDTTypeSymbol(sema(cu), typeBinding(c));
  }

  private static JavaTree.CompilationUnitTreeImpl test(String source) {
    return (JavaTree.CompilationUnitTreeImpl) JParserTestUtils.parse(source);
  }

}
