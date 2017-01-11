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
package org.sonar.java.signature;

import com.sonar.sslr.api.typed.ActionParser;
import org.junit.Test;
import org.sonar.java.ast.parser.JavaParser;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class MethodSignatureScannerTest {

  @Test
  public void scan() {
    MethodSignature method = MethodSignatureScanner.scan("read(Ljava/lang/String;[S)V");
    assertThat(method.getMethodName()).isEqualTo("read");

    assertThat(method.getReturnType().getJvmJavaType()).isEqualTo(JvmJavaType.V);
    assertThat(method.getArgumentTypes().size()).isEqualTo(2);

    Parameter param1 = method.getArgumentTypes().get(0);
    assertThat(param1.isOject()).isTrue();
    assertThat(param1.getClassName()).isEqualTo("String");

    Parameter param2 = method.getArgumentTypes().get(1);
    assertThat(param2.isOject()).isFalse();
    assertThat(param2.isArray()).isTrue();
    assertThat(param2.getJvmJavaType()).isEqualTo(JvmJavaType.S);
  }

  @Test
  public void scanMethodWithReturnType() {
    MethodSignature method = MethodSignatureScanner.scan("read(Ljava/lang/String;S)[Ljava/util/Vector;");

    assertThat(method.getReturnType().isOject()).isTrue();
    assertThat(method.getReturnType().isArray()).isTrue();
    assertThat(method.getReturnType().getClassName()).isEqualTo("Vector");
  }

  @Test
  public void scanGenericMethod() {
    MethodSignature method = MethodSignatureScanner.scan("transactionValidation(Ljava/lang/String;Ljava/util/List<Ljava/lang/String;>;)V");

    Parameter param1 = method.getArgumentTypes().get(0);
    assertThat(param1.isOject()).isTrue();
    assertThat(param1.getClassName()).isEqualTo("String");

    Parameter param2 = method.getArgumentTypes().get(1);
    assertThat(param2.isOject()).isTrue();
    assertThat(param2.getClassName()).isEqualTo("List");
  }

  @Test
  public void scanMethodTree() {
    ActionParser p = JavaParser.createParser(StandardCharsets.UTF_8);
    List<Tree> members = ((ClassTree) ((CompilationUnitTree) p.parse("class A { " +
      "A(){} " +
      "String[] method(int a){} " +
      "int foo(String a){}" +
      "java.lang.String bar(java.lang.String a){}" +
      "String qix(List<String> list){}" +
      "}")).types().get(0)).members();
    MethodTree constructorTree = (MethodTree) members.get(0);
    MethodTree methodTree = (MethodTree) members.get(1);
    MethodTree primitiveReturnType = (MethodTree) members.get(2);
    MethodTree fullyQualifiedReturnType = (MethodTree) members.get(3);
    MethodTree genericParameter = (MethodTree) members.get(4);
    MethodSignature constructor = MethodSignatureScanner.scan(constructorTree);
    assertThat(constructor.getMethodName()).isEqualTo("<init>");
    assertThat(constructor.getReturnType().isVoid()).isTrue();

    MethodSignature method = MethodSignatureScanner.scan(methodTree);
    assertThat(method.getMethodName()).isEqualTo("method");
    assertThat(method.getReturnType().isVoid()).isFalse();
    assertThat(method.getReturnType().isArray()).isTrue();
    assertThat(method.getReturnType().getClassName()).isEqualTo("String");
    assertThat(method.getArgumentTypes().get(0).isOject()).isFalse();

    method = MethodSignatureScanner.scan(primitiveReturnType);
    assertThat(method.getMethodName()).isEqualTo("foo");
    assertThat(method.getReturnType().isVoid()).isFalse();
    assertThat(method.getReturnType().isOject()).isFalse();
    assertThat(method.getReturnType().isArray()).isFalse();
    assertThat(method.getArgumentTypes().get(0).isOject()).isTrue();
    assertThat(method.getArgumentTypes().get(0).getClassName()).isEqualTo("String");

    method = MethodSignatureScanner.scan(fullyQualifiedReturnType);
    assertThat(method.getMethodName()).isEqualTo("bar");
    assertThat(method.getReturnType().isVoid()).isFalse();
    assertThat(method.getReturnType().isOject()).isTrue();
    assertThat(method.getReturnType().isArray()).isFalse();
    assertThat(method.getReturnType().getClassName()).isEqualTo("String");
    assertThat(method.getArgumentTypes().get(0).getClassName()).isEqualTo("String");

    method = MethodSignatureScanner.scan(genericParameter);
    assertThat(method.getMethodName()).isEqualTo("qix");
    assertThat(method.getArgumentTypes().get(0).getClassName()).isEqualTo("List");
  }
}
