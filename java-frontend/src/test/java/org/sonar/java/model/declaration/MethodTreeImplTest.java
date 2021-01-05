/*
 * SonarQube Java
 * Copyright (C) 2012-2021 SonarSource SA
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
package org.sonar.java.model.declaration;

import org.junit.jupiter.api.Test;
import org.sonar.java.model.JParserTestUtils;
import org.sonar.java.model.JUtils;
import org.sonar.plugins.java.api.cfg.ControlFlowGraph;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.MethodTree;

import static org.assertj.core.api.Assertions.assertThat;

class MethodTreeImplTest {

  @Test
  void override_without_annotation_should_be_detected() {
    CompilationUnitTree cut = createTree("interface T { int m(); } class A implements T { public int m(){return 0;}}");
    ClassTree interfaze = (ClassTree) cut.types().get(0);
    MethodTreeImpl methodInterface = (MethodTreeImpl) interfaze.members().get(0);
    ClassTree clazz = (ClassTree) cut.types().get(1);
    MethodTreeImpl methodClazz = (MethodTreeImpl) clazz.members().get(0);
    assertThat(methodInterface.isOverriding()).isFalse();
    assertThat(methodClazz.isOverriding()).isTrue();
  }

  @Test
  void override_with_generic_parameters_should_be_detected() throws Exception {
    CompilationUnitTree cut = createTree("public class ReferenceQueue<T> {\n" +
        "\n" +
        "    private static class Null extends ReferenceQueue {\n" +
        "        boolean enqueue(Reference r) {\n" +
        "            return false;\n" +
        "        }\n" +
        "    }\n" +
        "  boolean enqueue(Reference<? extends T> r) {}}" +
        "public abstract class Reference<T> {}");

    ClassTree innerClass = (ClassTree) cut.types().get(0);
    MethodTreeImpl methodInterface = (MethodTreeImpl) ((ClassTree) innerClass.members().get(0)).members().get(0);

    assertThat(methodInterface.isOverriding()).isTrue();

  }

  @Test
  void hiding_of_static_methods() {
    CompilationUnitTree cut = createTree("class A { static void foo() {} } class B extends A { void foo(){} } ");
    ClassTree clazz = (ClassTree) cut.types().get(1);
    MethodTreeImpl methodTree = (MethodTreeImpl) clazz.members().get(0);

    assertThat(methodTree.isOverriding()).isFalse();
  }

  @Test
  void override_from_object_should_be_detected() {
    MethodTreeImpl method = getUniqueMethod("class A { public String toString(){return \"\";}}");
    assertThat(method.isOverriding()).isTrue();
  }

  @Test
  void override_unknown() {
    MethodTreeImpl method = getUniqueMethod("class A extends Unknown { void foo(){}}");
    assertThat(method.isOverriding()).isFalse();
  }

  @Test
  void static_method_cannot_be_overriden() {
    assertThat(getUniqueMethod("class A{ static void m(){}}").isOverriding()).isFalse();
  }

  @Test
  void private_method_cannot_be_overriden() {
    assertThat(getUniqueMethod("class A{ private void m(){}}").isOverriding()).isFalse();
  }

  @Test
  void override_annotated_method_should_be_overriden() {
    assertThat(getUniqueMethod("class A{ @Override void m(){}}").isOverriding()).isTrue();
    assertThat(getUniqueMethod("class A{ @Foo @Override void m(){}}").isOverriding()).isTrue();
    assertThat(getUniqueMethod("class A{ @java.lang.Override void m(){}}").isOverriding()).isTrue();
    assertThat(getUniqueMethod("class A{ @cutom.namespace.Override void m(){}}").isOverriding()).isFalse();
    assertThat(getUniqueMethod("class A{ @foo.bar.lang.Override void m(){}}").isOverriding()).isFalse();
    assertThat(getUniqueMethod("class A{ @foo.lang.Override void m(){}}").isOverriding()).isFalse();
    assertThat(getUniqueMethod("class A{ @Foo void m(){}}").isOverriding()).isFalse();
  }

  @Test
  void compute_cfg() {
    MethodTree methodWithoutBody = getUniqueMethod("interface A { void foo(int arg) throws Exception; }");
    ControlFlowGraph cfg = methodWithoutBody.cfg();
    assertThat(cfg).isNull();

    MethodTree method = getUniqueMethod("class A { void foo(int arg) throws Exception { }}");
    cfg = method.cfg();
    assertThat(cfg).isNotNull();
    assertThat(method.cfg()).isSameAs(cfg);
  }

  @Test
  void has_all_syntax_token() {
    MethodTreeImpl method = getUniqueMethod("class A { public void foo(int arg) throws Exception {} }");
    assertThat(method.openParenToken()).isNotNull();
    assertThat(method.closeParenToken()).isNotNull();
    assertThat(method.semicolonToken()).isNull();
    assertThat(method.throwsToken()).isNotNull();

    method = getUniqueMethod("abstract class A { public abstract void foo(int arg); }");
    assertThat(method.openParenToken()).isNotNull();
    assertThat(method.closeParenToken()).isNotNull();
    assertThat(method.semicolonToken()).isNotNull();
    assertThat(method.throwsToken()).isNull();
  }

  @Test
  void varargs_flag() {
    Symbol.MethodSymbol methodSymbol = getUniqueMethod("class A { public static void main(String[] args){} }").symbol();
    assertThat(JUtils.isVarArgsMethod(methodSymbol)).isFalse();
    methodSymbol = getUniqueMethod("class A { public static void main(String... args){} }").symbol();
    assertThat(JUtils.isVarArgsMethod(methodSymbol)).isTrue();
  }

  private static MethodTreeImpl getUniqueMethod(String code) {
    CompilationUnitTree cut = createTree(code);
    return (MethodTreeImpl) ((ClassTree) cut.types().get(0)).members().get(0);
  }

  private static CompilationUnitTree createTree(String code) {
    return JParserTestUtils.parse(code);
  }

}
