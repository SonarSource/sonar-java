/*
 * SonarQube Java
 * Copyright (C) 2012-2018 SonarSource SA
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
package org.sonar.java.model;

import org.junit.BeforeClass;
import org.junit.Test;
import org.sonar.java.ast.parser.JavaParser;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class LiteralUtilsTest {

  static List<VariableTree> variables;

  @BeforeClass
  public static void setUp() {
    File file = new File("src/test/java/org/sonar/java/model/LiteralUtilsTest.java");
    CompilationUnitTree tree = (CompilationUnitTree) JavaParser.createParser().parse(file);
    ClassTree classTree = (ClassTree) tree.types().get(0);
    variables = classTree.members().stream()
      .filter(member -> member.is(Tree.Kind.VARIABLE))
      .map(VariableTree.class::cast)
      .collect(Collectors.toList());
  }

  int x1 = 42;
  int x2 = -7;
  int x3 = +3;
  int x4 = 42 + x1;
  int x5 = -x1;
  int x6 = 0xff;
  int x7 = 0b0100;
  int x8 = 56_78;

  long y1 = 42;
  long y2 = 42L;
  long y3 = -7;
  long y4 = -7l;
  long y5 = +3;
  long y6 = +3L;
  long y7 = 42 + y1;
  long y8 = -y1;
  long y9 = 0xFFL;
  long y10 = 0xFFFFFFFFFFFFFFFFL;
  long y11 = 0xFFFFFFFFFFFFFFFEL;
  long y12 = 0x8000000000000000L;
  long y13 = 0x7FFFFFFFFFFFFFFFL;
  long y14 = 0x7FFF_FFFF_FFFF_FFFFL;
  long y15 = 0b11010010_01101001_10010100_10010010;
  long y16 = 100_10;

  String s1 = "";
  String s2 = " ";
  String s3 = "not_empty";
  String s4 = "\n";

  @Test
  public void test_int_and_long_value() throws Exception {
    Integer[] expectedIntegerValues = {42, -7, 3, null, null, null, null, 5678};
    Long[] expectedLongValues = {42L, 42L, -7L, -7L, +3L, +3L, null, null, 255L, null, null, null, Long.MAX_VALUE, Long.MAX_VALUE, null, 10010L};
    int i = 0;
    int j = 0;

    for (VariableTree variableTree : variables) {
      if (variableTree.simpleName().name().startsWith("x")) {
        assertThat(LiteralUtils.intLiteralValue(variableTree.initializer())).isEqualTo(expectedIntegerValues[i++]);
      } else if (variableTree.simpleName().name().startsWith("y")) {
        assertThat(LiteralUtils.longLiteralValue(variableTree.initializer())).isEqualTo(expectedLongValues[j++]);
      }
    }
  }

  @Test
  public void testTrimLongSuffix() throws Exception {
    assertThat(LiteralUtils.trimLongSuffix("")).isEqualTo("");
    String longValue = "12345";
    assertThat(LiteralUtils.trimLongSuffix(longValue)).isEqualTo(longValue);
    assertThat(LiteralUtils.trimLongSuffix(longValue + "l")).isEqualTo(longValue);
    assertThat(LiteralUtils.trimLongSuffix(longValue + "L")).isEqualTo(longValue);
  }

  @Test
  public void testEmptyString() {
    boolean[] expectedStringEmptyResult = {true, false, false, false};
    int i = 0;
    for (VariableTree variableTree : variables) {
      if (variableTree.simpleName().name().startsWith("s")) {
        assertThat(LiteralUtils.isEmptyString(variableTree.initializer())).isEqualTo(expectedStringEmptyResult[i++]);
      }
    }
    Optional<VariableTree> nonStringVariable = variables.stream().filter(v -> v.simpleName().name().startsWith("x")).findFirst();
    assertThat(nonStringVariable).isPresent();
    assertThat(LiteralUtils.isEmptyString(nonStringVariable.get().initializer())).isFalse();
  }

  @Test
  public void testTrimQuotes() {
    assertThat(LiteralUtils.trimQuotes("\"test\"")).isEqualTo("test");
  }

}
