/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
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

import com.sonar.sslr.api.typed.ActionParser;
import org.junit.Test;
import org.sonar.java.ast.parser.JavaParser;
import org.sonar.java.bytecode.loader.SquidClassLoader;
import org.sonar.java.resolve.SemanticModel;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.MethodTree;

import java.util.Collections;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MethodTreeUtilsTest {

  @Test
  public void is_main_method() {
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
  public void is_equals_method() {
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
  public void is_hashcode_method() {
    assertTrue(MethodTreeUtils.isHashCodeMethod(parseMethod("class A { public int hashCode(){} }")));
    assertFalse(MethodTreeUtils.isHashCodeMethod(parseMethod("class A { public static int hashCode(){} }")));
    assertFalse(MethodTreeUtils.isHashCodeMethod(parseMethod("class A { private int hashCode(){} }")));
    assertFalse(MethodTreeUtils.isHashCodeMethod(parseMethod("class A { public int hashcode(){} }")));
    assertFalse(MethodTreeUtils.isHashCodeMethod(parseMethod("class A { public boolean hashCode(){} }")));
    assertFalse(MethodTreeUtils.isHashCodeMethod(parseMethod("class A { public int hashCode(int a){} }")));
  }

  private MethodTree parseMethod(String code) {
    ActionParser parser = JavaParser.createParser();
    CompilationUnitTree compilationUnitTree = (CompilationUnitTree) parser.parse(code);
    SemanticModel.createFor(compilationUnitTree, new SquidClassLoader(Collections.emptyList()));
    ClassTree classTree = (ClassTree) compilationUnitTree.types().get(0);
    return (MethodTree) classTree.members().get(0);
  }

}
