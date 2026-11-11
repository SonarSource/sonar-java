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

import org.eclipse.jdt.core.dom.ITypeBinding;
import org.junit.jupiter.api.Test;
import org.sonar.java.model.JParserTestUtils;
import org.sonar.java.model.JavaTree;
import org.sonar.java.model.declaration.ClassTreeImpl;
import org.sonar.plugins.java.api.semantic.Type;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.sonar.java.jdt.JDTTestUtils.sema;
import static org.sonar.java.jdt.JDTTestUtils.typeBinding;

class JDTTypeSymbolTest {

  @Test
  void testSuperClassWhenSemanticCanNotResolveObjectType() {
    var cu = test("interface I{}");
    var i = (ClassTreeImpl) cu.types().get(0);

    JDTSema sematicThatCanNotResolveObjectType = spy(sema(cu));
    when(sematicThatCanNotResolveObjectType.resolveType("java.lang.Object")).thenReturn(null);
    var typeSymbol = new JDTTypeSymbol(sematicThatCanNotResolveObjectType, typeBinding(i));
    assertThat(typeSymbol.superClass()).isSameAs(Type.UNKNOWN);
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

  @Test
  void recovered_type_has_no_super_types() {
    var cu = test("class C {}");
    var c = (ClassTreeImpl) cu.types().get(0);
    ITypeBinding binding = spy(typeBinding(c));
    when(binding.isRecovered()).thenReturn(true);
    assertThat(new JDTTypeSymbol(sema(cu), binding).superTypes()).isEmpty();
  }

  @Test
  void recovered_annotation_is_not_an_annotation() {
    assertThat(getJTypeSymbolFromClassText("@interface A {}", true).isAnnotation()).isFalse();
  }
}
