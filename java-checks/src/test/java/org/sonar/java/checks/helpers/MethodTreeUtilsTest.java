/*
 * SonarQube Java
 * Copyright (C) 2012-2023 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.java.checks.helpers;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MethodTreeUtilsTest {

  @Test
  void is_main_method() {
    assertTrue(MethodTreeUtils.isMainMethod(parseMethod("class A { public static void main(String[] args){} }")));
    assertTrue(MethodTreeUtils.isMainMethod(parseMethod("class A { public static void main(String... args){} }")));
    assertFalse(MethodTreeUtils.isMainMethod(parseMethod("class A { public void main(String[] args){} }")));
    assertFalse(MethodTreeUtils.isMainMethod(parseMethod("class A { static void main(String[] args){} }")));
    assertFalse(MethodTreeUtils.isMainMethod(parseMethod("class A { public static void amain(String[] args){} }")));
    assertFalse(MethodTreeUtils.isMainMethod(parseMethod("class A { public static void main(String args){} }")));
    assertFalse(MethodTreeUtils.isMainMethod(parseMethod("class A { public static int main(String[] args){} }")));
    assertFalse(MethodTreeUtils.isMainMethod(parseMethod("class A { public static void main(String[] args, String[] second){} }")));
  }

  @Test
  void is_equals_method() {
    assertTrue(MethodTreeUtils.isEqualsMethod(parseMethod("class A { public boolean equals(Object o){} }")));
    assertFalse(MethodTreeUtils.isEqualsMethod(parseMethod("class A { private boolean equals(Object o){} }")));
    assertFalse(MethodTreeUtils.isEqualsMethod(parseMethod("class A { public static boolean equals(Object o){} }")));
    assertFalse(MethodTreeUtils.isEqualsMethod(parseMethod("class A { public boolean equal(Object o){} }")));
    assertFalse(MethodTreeUtils.isEqualsMethod(parseMethod("class A { public boolean equals(Object o, int a){} }")));
    assertFalse(MethodTreeUtils.isEqualsMethod(parseMethod("class A { public boolean equals(int a){} }")));
    assertFalse(MethodTreeUtils.isEqualsMethod(parseMethod("class equals { public equals(Object o){} }")));
    assertTrue(MethodTreeUtils.isEqualsMethod(parseMethod("interface I { public abstract boolean equals(Object o); }")));
  }

  @Test
  void is_hashcode_method() {
    assertTrue(MethodTreeUtils.isHashCodeMethod(parseMethod("class A { public int hashCode(){} }")));
    assertFalse(MethodTreeUtils.isHashCodeMethod(parseMethod("class A { public static int hashCode(){} }")));
    assertFalse(MethodTreeUtils.isHashCodeMethod(parseMethod("class A { private int hashCode(){} }")));
    assertFalse(MethodTreeUtils.isHashCodeMethod(parseMethod("class A { public int hashcode(){} }")));
    assertFalse(MethodTreeUtils.isHashCodeMethod(parseMethod("class A { public boolean hashCode(){} }")));
    assertFalse(MethodTreeUtils.isHashCodeMethod(parseMethod("class A { public int hashCode(int a){} }")));
  }

  @Test
  void is_setter_method() {
    assertTrue(MethodTreeUtils.isSetterMethod(parseMethod("class A { public void setFoo(String foo){} }")));
    assertFalse(MethodTreeUtils.isSetterMethod(parseMethod("class A { public static void setFoo(String foo){} }")));
    assertFalse(MethodTreeUtils.isSetterMethod(parseMethod("class A { public int setFoo(String foo){} }")));
    assertFalse(MethodTreeUtils.isSetterMethod(parseMethod("class A { public void setFoo(String foo, int a){} }")));
    assertFalse(MethodTreeUtils.isSetterMethod(parseMethod("class A { public void setFoo(){} }")));
    assertFalse(MethodTreeUtils.isSetterMethod(parseMethod("class A { public void foo(){} }")));
    assertFalse(MethodTreeUtils.isSetterMethod(parseMethod("class A { public void foo(String foo){} }")));
  }

  @Test
  void consecutive_and_subsequent_method_invocation() {
    List<MethodInvocationTree> methodInvocationList = new ArrayList<>();
    parseMethod("class A { void m(){ this.a1.a2.toString().toUpperCase().length(); int x = (getClass()).getMethods().length; } A a1; A a2; }")
      .block()
      .accept(new BaseTreeVisitor() {
        @Override
        public void visitMethodInvocation(MethodInvocationTree tree) {
          super.visitMethodInvocation(tree);
          methodInvocationList.add(tree);
        }
      });

    assertThat(methodInvocationList).hasSize(5);
    MethodInvocationTree toStringMethod = methodInvocationList.get(0);
    MethodInvocationTree toUpperCaseMethod = methodInvocationList.get(1);
    MethodInvocationTree lengthMethod = methodInvocationList.get(2);
    MethodInvocationTree getClassMethod = methodInvocationList.get(3);
    MethodInvocationTree getMethodsMethod = methodInvocationList.get(4);

    assertThat(toStringMethod.methodSymbol().name()).isEqualTo("toString");
    assertThat(toUpperCaseMethod.methodSymbol().name()).isEqualTo("toUpperCase");
    assertThat(lengthMethod.methodSymbol().name()).isEqualTo("length");
    assertThat(getClassMethod.methodSymbol().name()).isEqualTo("getClass");
    assertThat(getMethodsMethod.methodSymbol().name()).isEqualTo("getMethods");

    MemberSelectExpressionTree thisA1A2 = (MemberSelectExpressionTree) ((MemberSelectExpressionTree) toStringMethod.methodSelect()).expression();
    MemberSelectExpressionTree thisA1 = (MemberSelectExpressionTree) thisA1A2.expression();
    IdentifierTree a2 = thisA1A2.identifier();
    IdentifierTree a1 = thisA1.identifier();
    IdentifierTree keywordThis = (IdentifierTree) thisA1.expression();

    assertThat(MethodTreeUtils.consecutiveMethodInvocation(keywordThis)).isEmpty();
    assertThat(MethodTreeUtils.consecutiveMethodInvocation(a1)).isEmpty();
    assertThat(MethodTreeUtils.consecutiveMethodInvocation(a2)).containsSame(toStringMethod);

    assertThat(MethodTreeUtils.consecutiveMethodInvocation(toStringMethod)).containsSame(toUpperCaseMethod);
    assertThat(MethodTreeUtils.consecutiveMethodInvocation(toUpperCaseMethod)).containsSame(lengthMethod);
    assertThat(MethodTreeUtils.consecutiveMethodInvocation(lengthMethod)).isEmpty();
    assertThat(MethodTreeUtils.consecutiveMethodInvocation(getClassMethod)).containsSame(getMethodsMethod);
    assertThat(MethodTreeUtils.consecutiveMethodInvocation(getMethodsMethod)).isEmpty();

    MethodMatchers toUpperCaseMethodMatchers = MethodMatchers.create()
      .ofTypes("java.lang.String").names("toUpperCase").addWithoutParametersMatcher().build();
    MethodMatchers lengthMethodMatchers = MethodMatchers.create()
      .ofTypes("java.lang.String").names("length").addWithoutParametersMatcher().build();

    assertThat(MethodTreeUtils.subsequentMethodInvocation(toStringMethod, lengthMethodMatchers)).containsSame(lengthMethod);
    assertThat(MethodTreeUtils.subsequentMethodInvocation(toStringMethod, toUpperCaseMethodMatchers)).containsSame(toUpperCaseMethod);
    assertThat(MethodTreeUtils.subsequentMethodInvocation(toUpperCaseMethod, lengthMethodMatchers)).containsSame(lengthMethod);
    assertThat(MethodTreeUtils.subsequentMethodInvocation(toUpperCaseMethod, toUpperCaseMethodMatchers)).isEmpty();
    assertThat(MethodTreeUtils.subsequentMethodInvocation(lengthMethod, toUpperCaseMethodMatchers)).isEmpty();
    assertThat(MethodTreeUtils.subsequentMethodInvocation(lengthMethod, lengthMethodMatchers)).isEmpty();

    // coverage
    assertThat(MethodTreeUtils.hasKind(null, Tree.Kind.METHOD_INVOCATION)).isFalse();
    assertThat(MethodTreeUtils.consecutiveMethodInvocation(((MemberSelectExpressionTree) toStringMethod.parent()).identifier())).isEmpty();
  }

  private MethodTree parseMethod(String code) {
    CompilationUnitTree compilationUnitTree = JParserTestUtils.parse(code);
    ClassTree classTree = (ClassTree) compilationUnitTree.types().get(0);
    return (MethodTree) classTree.members().get(0);
  }

}
