/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
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

import com.google.common.collect.Lists;
import com.sonar.sslr.api.typed.ActionParser;
import org.junit.Test;
import org.sonar.java.ast.parser.JavaParser;
import org.sonar.java.resolve.Flags;
import org.sonar.java.resolve.JavaSymbol;
import org.sonar.java.resolve.SemanticModel;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;

import java.io.File;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

public class MethodTreeImplTest {

  private final ActionParser p = JavaParser.createParser(StandardCharsets.UTF_8);

  @Test
  public void override_without_annotation_should_be_detected() {
    CompilationUnitTree cut = createTree("interface T { int m(); } class A implements T { int m(){return 0;}}");
    ClassTree interfaze = (ClassTree) cut.types().get(0);
    MethodTreeImpl methodInterface = (MethodTreeImpl) interfaze.members().get(0);
    ClassTree clazz = (ClassTree) cut.types().get(1);
    MethodTreeImpl methodClazz = (MethodTreeImpl) clazz.members().get(0);
    assertThat(methodInterface.isOverriding()).isFalse();
    assertThat(methodClazz.isOverriding()).isTrue();
  }

  @Test
  public void override_with_generic_parameters_should_be_detected() throws Exception {
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
  public void hiding_of_static_methods() {
    CompilationUnitTree cut = createTree("class A { static void foo() {} } class B extends A { void foo(){} } ");
    ClassTree clazz = (ClassTree) cut.types().get(1);
    MethodTreeImpl methodTree = (MethodTreeImpl) clazz.members().get(0);

    assertThat(methodTree.isOverriding()).isFalse();
  }

  @Test
  public void override_from_object_should_be_detected() {
    MethodTreeImpl method = getUniqueMethod("class A { String toString(){return \"\";}}");
    assertThat(method.isOverriding()).isTrue();
  }

  @Test
  public void override_unknown() {
    MethodTreeImpl method = getUniqueMethod("class A extends Unknown { void foo(){}}");
    assertThat(method.isOverriding()).isNull();
  }

  @Test
  public void static_method_cannot_be_overriden() {
    assertThat(getUniqueMethod("class A{ static void m(){}}").isOverriding()).isFalse();
  }

  @Test
  public void private_method_cannot_be_overriden() {
    assertThat(getUniqueMethod("class A{ private void m(){}}").isOverriding()).isFalse();
  }

  @Test
  public void override_annotated_method_should_be_overriden() {
    assertThat(getUniqueMethod("class A{ @Override void m(){}}").isOverriding()).isTrue();
    assertThat(getUniqueMethod("class A{ @Foo @Override void m(){}}").isOverriding()).isTrue();
    assertThat(getUniqueMethod("class A{ @java.lang.Override void m(){}}").isOverriding()).isTrue();
    assertThat(getUniqueMethod("class A{ @cutom.namespace.Override void m(){}}").isOverriding()).isFalse();
    assertThat(getUniqueMethod("class A{ @foo.bar.lang.Override void m(){}}").isOverriding()).isFalse();
    assertThat(getUniqueMethod("class A{ @foo.lang.Override void m(){}}").isOverriding()).isFalse();
    assertThat(getUniqueMethod("class A{ @Foo void m(){}}").isOverriding()).isFalse();
  }

  @Test
  public void symbol_not_set_should_lead_to_null_result() throws Exception {
    CompilationUnitTree cut = (CompilationUnitTree) p.parse("class A { String toString(){return \"\";}}");
    MethodTreeImpl methodTree = (MethodTreeImpl) ((ClassTree) cut.types().get(0)).members().get(0);
    assertThat(methodTree.isOverriding()).isNull();
  }

  @Test
  public void has_all_syntax_token() {
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
  public void is_main_method() throws Exception {
    assertThat(getUniqueMethod("class A { public static void main(String[] args){} }").isMainMethod()).isTrue();
    assertThat(getUniqueMethod("class A { public static void main(String... args){} }").isMainMethod()).isTrue();
    assertThat(getUniqueMethod("class A { public void main(String[] args){} }").isMainMethod()).isFalse();
    assertThat(getUniqueMethod("class A { static void main(String[] args){} }").isMainMethod()).isFalse();
    assertThat(getUniqueMethod("class A { public static void amain(String[] args){} }").isMainMethod()).isFalse();
    assertThat(getUniqueMethod("class A { public static void main(String args){} }").isMainMethod()).isFalse();
    assertThat(getUniqueMethod("class A { public static int main(String[] args){} }").isMainMethod()).isFalse();
    assertThat(getUniqueMethod("class A { public static void main(String[] args, String[] second){} }").isMainMethod()).isFalse();
  }

  @Test
  public void is_equals_method() throws Exception {
    assertThat(getUniqueMethod("class A { public boolean equals(Object o){} }").isEqualsMethod()).isTrue();
    assertThat(getUniqueMethod("class A { private boolean equals(Object o){} }").isEqualsMethod()).isFalse();
    assertThat(getUniqueMethod("class A { public static boolean equals(Object o){} }").isEqualsMethod()).isFalse();
    assertThat(getUniqueMethod("class A { public boolean equal(Object o){} }").isEqualsMethod()).isFalse();
    assertThat(getUniqueMethod("class A { public boolean equals(Object o, int a){} }").isEqualsMethod()).isFalse();
    assertThat(getUniqueMethod("class A { public boolean equals(int a){} }").isEqualsMethod()).isFalse();
    assertThat(getUniqueMethod("class equals { public equals(Object o){} }").isEqualsMethod()).isFalse();
    assertThat(getUniqueMethod("interface I { public abstract boolean equals(Object o); }").isEqualsMethod()).isTrue();
  }

  @Test
  public void is_hashcode_method() throws Exception {
    assertThat(getUniqueMethod("class A { public int hashCode(){} }").isHashCodeMethod()).isTrue();
    assertThat(getUniqueMethod("class A { public static int hashCode(){} }").isHashCodeMethod()).isFalse();
    assertThat(getUniqueMethod("class A { private int hashCode(){} }").isHashCodeMethod()).isFalse();
    assertThat(getUniqueMethod("class A { public int hashcode(){} }").isHashCodeMethod()).isFalse();
    assertThat(getUniqueMethod("class A { public boolean hashCode(){} }").isHashCodeMethod()).isFalse();
    assertThat(getUniqueMethod("class A { public int hashCode(int a){} }").isHashCodeMethod()).isFalse();
  }

  @Test
  public void is_toString_method() throws Exception {
    assertThat(getUniqueMethod("class A { public String toString(){} }").isToStringMethod()).isTrue();
    assertThat(getUniqueMethod("class A { public java.lang.String toString(){} }").isToStringMethod()).isTrue();
    assertThat(getUniqueMethod("class A { public static String toString(){} }").isToStringMethod()).isFalse();
    assertThat(getUniqueMethod("class A { private String toString(){} }").isToStringMethod()).isFalse();
    assertThat(getUniqueMethod("class A { public String tostring(){} }").isToStringMethod()).isFalse();
    assertThat(getUniqueMethod("class A { public int toString(){} }").isToStringMethod()).isFalse();
    assertThat(getUniqueMethod("class A { public String toString(String foo){} }").isToStringMethod()).isFalse();
  }

  @Test
  public void varargs_flag() {
    JavaSymbol.MethodJavaSymbol methodSymbol = (JavaSymbol.MethodJavaSymbol) getUniqueMethod("class A { public static void main(String[] args){} }").symbol();
    assertThat((methodSymbol.flags() & Flags.VARARGS) != 0).isFalse();
    methodSymbol = (JavaSymbol.MethodJavaSymbol) getUniqueMethod("class A { public static void main(String... args){} }").symbol();
    assertThat((methodSymbol.flags() & Flags.VARARGS) != 0).isTrue();
  }

  private MethodTreeImpl getUniqueMethod(String code) {
    CompilationUnitTree cut = createTree(code);
    return (MethodTreeImpl) ((ClassTree) cut.types().get(0)).members().get(0);
  }

  private CompilationUnitTree createTree(String code) {
    CompilationUnitTree compilationUnitTree = (CompilationUnitTree) p.parse(code);
    SemanticModel.createFor(compilationUnitTree, Lists.<File>newArrayList());
    return compilationUnitTree;
  }

}
