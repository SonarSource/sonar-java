/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.sonar.java.se.utils.JParserTestUtils;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SELiteralUtilsTest {

  static List<VariableTree> variables;

  @BeforeAll
  static void beforeAll() {
    File file = new File("src/test/java/org/sonar/java/model/SELiteralUtilsTest.java");
    CompilationUnitTree tree = JParserTestUtils.parse(file);
    ClassTree classTree = (ClassTree) tree.types().get(0);
    variables = classTree.members().stream()
      .filter(member -> member.is(Tree.Kind.VARIABLE))
      .map(VariableTree.class::cast)
      .toList();
  }

  /**
   * The variables below are used in the setUp method above to create the 'variables' list which is used in tests
   */
  int x1 = 42;
  int x2 = -7;
  int x3 = +3;
  int x4 = 42 + x1;
  int x5 = -x1;
  int x6 = 0xff;
  int x7 = 0b0100;
  int x8 = 56_78;
  int x9 = 0XFF;
  int x10 = 0B1100110;
  int x11 = 0xff000000;

  long y1 = 42;
  long y2 = 42L;
  long y3 = -7;
  long y4 = -7L;
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
  long y17 = 0XFFL;
  long y18 = 0B1100110L;

  String s1 = "";
  String s2 = " ";
  String s3 = "not_empty";
  String s4 = "\n";

  @Test
  void private_constructor() throws Exception {
    Constructor<SELiteralUtils> constructor = SELiteralUtils.class.getDeclaredConstructor();
    assertThatThrownBy(constructor::newInstance).isInstanceOf(IllegalAccessException.class);
    constructor.setAccessible(true);
    constructor.newInstance();
  }

  @Test
  void test_int_and_long_value() {
    Integer[] expectedIntegerValues = {42, -7, 3, null, null, 0xff, 0b0100, 5678, 0xFF, 0b1100110, 0xff000000};
    Long[] expectedLongValues = {42L, 42L, -7L, -7L, +3L, +3L, null, null, 0xFFL, null, null, null,
      Long.MAX_VALUE, Long.MAX_VALUE, 0b11010010_01101001_10010100_10010010L, 10010L, 0xFFL, 0b1100110L};
    int i = 0;
    int j = 0;

    for (VariableTree variableTree : variables) {
      if (variableTree.simpleName().name().startsWith("x")) {
        assertThat(SELiteralUtils.intLiteralValue(variableTree.initializer())).isEqualTo(expectedIntegerValues[i++]);
      } else if (variableTree.simpleName().name().startsWith("y")) {
        assertThat(SELiteralUtils.longLiteralValue(variableTree.initializer())).isEqualTo(expectedLongValues[j++]);
      }
    }
  }

  /**
   * Binary, hex and octal int literals are allowed when they fit into 32-bits (jls11 - §3.10.1)
   */
  @Test
  void testLargeBinary() {
    // 32 bit masks
    assertThat(SELiteralUtils.intLiteralValue(getIntLiteral("0b1111_1111_1111_1111_0000_0000_0000_0000"))).isEqualTo(0b1111_1111_1111_1111_0000_0000_0000_0000);
    assertThat(SELiteralUtils.intLiteralValue(getIntLiteral("0b0111_1111_1111_1111_0000_0000_0000_0000"))).isEqualTo(0b0111_1111_1111_1111_0000_0000_0000_0000);

    // 32 bits numbers padded with zeros
    assertThat(SELiteralUtils.intLiteralValue(getIntLiteral("0b0000_1111_1111_1111_1111_0000_0000_0000_0000"))).isEqualTo(0b0000_1111_1111_1111_1111_0000_0000_0000_0000);
    assertThat(SELiteralUtils.intLiteralValue(getIntLiteral("0x00FFF0000"))).isEqualTo(0x00FFF0000);

    // hexa
    assertThat(SELiteralUtils.intLiteralValue(getIntLiteral("0xFFFF0000"))).isEqualTo(0xFFFF0000);
    assertThat(SELiteralUtils.intLiteralValue(getIntLiteral("0x7FFF0000"))).isEqualTo(0x7FFF0000);
  }

  private ExpressionTree getIntLiteral(String intLiteral) {
    return ((BinaryExpressionTree) getReturnExpression("int foo() { return " + intLiteral + " & 42; }")).leftOperand();
  }

  @Test
  void testTrimLongSuffix() {
    assertThat(SELiteralUtils.trimLongSuffix("")).isEmpty();
    String longValue = "12345";
    assertThat(SELiteralUtils.trimLongSuffix(longValue)).isEqualTo(longValue);
    assertThat(SELiteralUtils.trimLongSuffix(longValue + "l")).isEqualTo(longValue);
    assertThat(SELiteralUtils.trimLongSuffix(longValue + "L")).isEqualTo(longValue);
  }

  @Test
  void testTrimQuotes() {
    assertThat(SELiteralUtils.trimQuotes("\"test\"")).isEqualTo("test");
    assertThat(SELiteralUtils.trimQuotes("\"\"\"test\"\"\"")).isEqualTo("test");
  }

  @Test
  void testIsTextBlock() {
    assertThat(SELiteralUtils.isTextBlock("\"test\"")).isFalse();
    assertThat(SELiteralUtils.isTextBlock("\"\"\"test\"\"\"")).isTrue();
  }

  @Test
  void isTrue_withNonBooleanLiteral_returnsFalse() {
    ExpressionTree tree = getFirstExpression("void foo(java.util.Properties props){ props.setProperty(\"myKey\", \"myValue\"); }");
    assertThat(SELiteralUtils.isTrue(tree)).isFalse();
  }

  @Test
  void isFalse_withNonBooleanLiteral_returnsFalse() {
    ExpressionTree tree = getFirstExpression("void foo(java.util.Properties props){ props.setProperty(\"myKey\", \"myValue\"); }");
    assertThat(SELiteralUtils.isFalse(tree)).isFalse();
  }

  @Test
  void isTrue_withFalseValue_returnsFalse() {
    LiteralTree falseTree = (LiteralTree) getReturnExpression("void foo(){ return false; }");
    assertThat(SELiteralUtils.isTrue(falseTree)).isFalse();
  }

  @Test
  void isFalse_withTrueValue_returnsFalse() {
    LiteralTree trueTree = (LiteralTree) getReturnExpression("void foo(){ return true; }");
    assertThat(SELiteralUtils.isFalse(trueTree)).isFalse();
  }

  @Test
  void isTrue_withExpectedValue_returnsTrue() {
    LiteralTree trueTree = (LiteralTree) getReturnExpression("void foo(){ return true; }");
    assertThat(SELiteralUtils.isTrue(trueTree)).isTrue();
  }

  @Test
  void isFalse_withExpectedValue_returnsTrue() {
    LiteralTree falseTree = (LiteralTree) getReturnExpression("void foo(){ return false; }");
    assertThat(SELiteralUtils.isFalse(falseTree)).isTrue();
  }

  @Test
  void getAsStringValue_for_string() {

    LiteralTree intLiteral = getLiteral("123");
    assertThat(SELiteralUtils.getAsStringValue(intLiteral)).isEqualTo("123");

    LiteralTree stringLiteral = getLiteral("\"ABC\"");
    assertThat(SELiteralUtils.getAsStringValue(stringLiteral)).isEqualTo("ABC");

    LiteralTree textBlock = getLiteral("\"\"\"\nABC\"\"\"");
    assertThat(SELiteralUtils.getAsStringValue(textBlock)).isEqualTo("ABC");

    LiteralTree multilineString = getLiteral("\"ABC\\nABC\"");
    assertThat(SELiteralUtils.getAsStringValue(multilineString)).isEqualTo("ABC\\nABC");

    LiteralTree multilineTB = getLiteral("\"\"\"\n      ABC\n      ABC\"\"\"");
    assertThat(SELiteralUtils.getAsStringValue(multilineTB)).isEqualTo("ABC\nABC");

    LiteralTree multilineIndentInTB = getLiteral("\"\"\"\n      ABC\n    ABC\"\"\"");
    assertThat(SELiteralUtils.getAsStringValue(multilineIndentInTB)).isEqualTo("  ABC\nABC");

    LiteralTree textBlockWithTab = getLiteral("\"\"\"\n      \tABC\"\"\"");
    assertThat(SELiteralUtils.getAsStringValue(textBlockWithTab)).isEqualTo("ABC");

    LiteralTree textBlockWithEmptyLines = getLiteral("\"\"\"\n\n\n      \tABC\"\"\"");
    assertThat(SELiteralUtils.getAsStringValue(textBlockWithEmptyLines)).isEqualTo("\n\nABC");

    LiteralTree textBlockWithNewLines = getLiteral("\"\"\"\n\n\n      \tABC\\n\"\"\"");
    assertThat(SELiteralUtils.getAsStringValue(textBlockWithNewLines)).isEqualTo("\n\nABC\\n");

    LiteralTree textBlockWithTrailingSpaces = getLiteral("\"\"\"\n\n\n      \tABC                  \"\"\"");
    assertThat(SELiteralUtils.getAsStringValue(textBlockWithTrailingSpaces)).isEqualTo("\n\nABC                  ");

    LiteralTree textBlockWithTrailingAndLeadingSpaces = getLiteral("\"\"\"\n\n\n      \tABC   \n       ABC     ABC                  \"\"\"");
    assertThat(SELiteralUtils.getAsStringValue(textBlockWithTrailingAndLeadingSpaces)).isEqualTo("\n\nABC\nABC     ABC                  ");

    LiteralTree textBlockWithQuotesOnNewLine = getLiteral("\"\"\"\n     ABC\n     ABC\n  \"\"\"");
    assertThat(SELiteralUtils.getAsStringValue(textBlockWithQuotesOnNewLine)).isEqualTo("   ABC\n   ABC\n");
  }

  @Test
  void indentationOfTextBlock() {
    String[] noIndentation = {"\"\"\"", "abc", "\"\"\""};
    assertThat(SELiteralUtils.indentationOfTextBlock(noIndentation)).isZero();
    String[] lastLineNotIndented = {"\"\"\"", "    abc", "\"\"\""};
    assertThat(SELiteralUtils.indentationOfTextBlock(lastLineNotIndented)).isZero();
    String[] indented = {"\"\"\"", "    abc", "    \"\"\""};
    assertThat(SELiteralUtils.indentationOfTextBlock(indented)).isEqualTo(4);
    String[] tabsAndFormFeeds = {"\"\"\"", "\t\tabc", "\f\f\"\"\""};
    assertThat(SELiteralUtils.indentationOfTextBlock(tabsAndFormFeeds)).isEqualTo(2);
    String[] withEmptyLine = {"\"\"\"", "    abc", "", "    \"\"\""};
    assertThat(SELiteralUtils.indentationOfTextBlock(withEmptyLine)).isEqualTo(4);
    String[] withIndentedEmptyLine = {"\"\"\"", "    abc", " \t\f", "    \"\"\""};
    assertThat(SELiteralUtils.indentationOfTextBlock(withIndentedEmptyLine)).isEqualTo(4);
  }

  private ExpressionTree getFirstExpression(String code) {
    ClassTree firstType = getClassTree(code);
    StatementTree firstStatement = ((MethodTree) firstType.members().get(0)).block().body().get(0);
    return ((ExpressionStatementTree) firstStatement).expression();
  }

  private ExpressionTree getReturnExpression(String code) {
    ClassTree firstType = getClassTree(code);
    ReturnStatementTree returnExpression = (ReturnStatementTree) ((MethodTree) firstType.members().get(0)).block().body().get(0);
    return returnExpression.expression();
  }

  private ClassTree getClassTree(String code) {
    CompilationUnitTree compilationUnitTree = JParserTestUtils.parse("class A { " + code + "}");
    return (ClassTree) compilationUnitTree.types().get(0);
  }

  private LiteralTree getLiteral(String code) {
    ClassTree classTree = getClassTree("Object o = " + code + ";");
    return (LiteralTree) ((VariableTree) classTree.members().get(0)).initializer();
  }

}
