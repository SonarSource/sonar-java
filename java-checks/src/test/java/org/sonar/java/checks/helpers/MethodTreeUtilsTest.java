/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.checks.helpers;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.sonar.java.model.expression.LiteralTreeImpl;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.ParenthesizedTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.sonar.java.checks.helpers.MethodTreeUtils.lamdaArgumentAt;
import static org.sonar.java.checks.helpers.MethodTreeUtils.parentMethodInvocationOfArgumentAtPos;

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

  @Test
  void parent_method_invocation_of_argument_at_pos() {
    CompilationUnitTree compilationUnitTree = JParserTestUtils.parse("""
      class A {
        int field1 = 1;
        int field2 = Math.max((2), 3);
        Thread field3 = new Thread("4");
      }
      """);
    ClassTree classTree = (ClassTree) compilationUnitTree.types().get(0);
    List<Tree> members = classTree.members();
    LiteralTree literal1 = (LiteralTree) ((VariableTree) members.get(0)).initializer();
    MethodInvocationTree mathMax = (MethodInvocationTree) ((VariableTree) members.get(1)).initializer();
    LiteralTree literal2 = (LiteralTree) ((ParenthesizedTree) mathMax.arguments().get(0)).expression();
    LiteralTree literal3 = (LiteralTree) mathMax.arguments().get(1);
    NewClassTree newThread = (NewClassTree) ((VariableTree) members.get(2)).initializer();
    LiteralTree literal4 = (LiteralTree) newThread.arguments().get(0);
    LiteralTreeImpl expressionWithoutParent = new LiteralTreeImpl(Tree.Kind.STRING_LITERAL, null);

    assertThat(parentMethodInvocationOfArgumentAtPos(null, 0)).isNull();
    assertThat(parentMethodInvocationOfArgumentAtPos(expressionWithoutParent, 0)).isNull();
    assertThat(parentMethodInvocationOfArgumentAtPos(literal1, 0)).isNull();
    assertThat(parentMethodInvocationOfArgumentAtPos(literal2, 0)).isSameAs(mathMax);
    assertThat(parentMethodInvocationOfArgumentAtPos(literal2, 1)).isNull();
    assertThat(parentMethodInvocationOfArgumentAtPos(literal2, 2)).isNull();
    assertThat(parentMethodInvocationOfArgumentAtPos(literal3, 0)).isNull();
    assertThat(parentMethodInvocationOfArgumentAtPos(literal3, 1)).isSameAs(mathMax);
    assertThat(parentMethodInvocationOfArgumentAtPos(literal4, 0)).isNull();
  }

  @Test
  void lamda_argument_ar() {
    CompilationUnitTree compilationUnitTree = JParserTestUtils.parse("""
      class A {
        Runnable lambda1 = () -> {};
        java.util.function.Consumer<String> lambda2 = a -> {};
      }
      """);
    ClassTree classTree = (ClassTree) compilationUnitTree.types().get(0);
    List<Tree> members = classTree.members();
    LambdaExpressionTree lambda1 = (LambdaExpressionTree) ((VariableTree) members.get(0)).initializer();
    LambdaExpressionTree lambda2 = (LambdaExpressionTree) ((VariableTree) members.get(1)).initializer();

    assertThat(lamdaArgumentAt(null, 0)).isNull();
    assertThat(lamdaArgumentAt(lambda1, 0)).isNull();
    assertThat(lamdaArgumentAt(lambda2, 0)).isSameAs(lambda2.parameters().get(0));
    assertThat(lamdaArgumentAt(lambda2, 1)).isNull();
  }

  private MethodTree parseMethod(String code) {
    CompilationUnitTree compilationUnitTree = JParserTestUtils.parse(code);
    ClassTree classTree = (ClassTree) compilationUnitTree.types().get(0);
    return (MethodTree) classTree.members().get(0);
  }

}
