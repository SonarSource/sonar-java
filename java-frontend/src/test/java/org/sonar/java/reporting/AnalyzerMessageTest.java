/*
 * SonarQube Java
 * Copyright (C) 2012-2022 SonarSource SA
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
package org.sonar.java.reporting;

import org.assertj.core.api.Fail;
import org.junit.jupiter.api.Test;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.java.TestUtils;
import org.sonar.java.model.JParserTestUtils;
import org.sonar.java.reporting.AnalyzerMessage.TextSpan;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.InferedTypeTree;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.VariableTree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class AnalyzerMessageTest {

  @Test
  void testAnalyzerMessage() {
    JavaCheck javaCheck = mock(JavaCheck.class);
    InputFile file = TestUtils.emptyInputFile("a");
    int line = 5;
    String message = "analyzer message";
    int cost = 3;
    AnalyzerMessage analyzerMessage = new AnalyzerMessage(javaCheck, file, line, message, cost);
    assertThat(analyzerMessage.getCheck()).isEqualTo(javaCheck);
    assertThat(analyzerMessage.getInputComponent()).isEqualTo(file);
    assertThat(analyzerMessage.getLine()).isEqualTo(line);
    assertThat(analyzerMessage.getMessage()).isEqualTo(message);
    assertThat(analyzerMessage.getCost()).isEqualTo(cost);

    AnalyzerMessage.TextSpan location = analyzerMessage.primaryLocation();
    assertThat(location).hasToString("(5:-1)-(5:-1)");
    assertThat(location.startLine).isEqualTo(line);
    assertThat(location.startCharacter).isEqualTo(-1);
    assertThat(location.endLine).isEqualTo(line);
    assertThat(location.endCharacter).isEqualTo(-1);
    assertThat(location.isEmpty()).isTrue();
  }

  @Test
  void testAnalyzerMessageOnFile2() {
    JavaCheck javaCheck = mock(JavaCheck.class);
    InputFile file = TestUtils.emptyInputFile("a");
    String message = "analyzer message";
    int cost = 3;
    AnalyzerMessage analyzerMessage = new AnalyzerMessage(javaCheck, file, -5, message, cost);
    assertThat(analyzerMessage.getCheck()).isEqualTo(javaCheck);
    assertThat(analyzerMessage.getInputComponent()).isEqualTo(file);
    assertThat(analyzerMessage.getLine()).isNull();
    assertThat(analyzerMessage.getMessage()).isEqualTo(message);
    assertThat(analyzerMessage.getCost()).isEqualTo(cost);
    assertThat(analyzerMessage.primaryLocation()).isNull();
  }

  @Test
  void emptyTextSpan() {
    // same line, same offset
    assertThat(new AnalyzerMessage.TextSpan(42, 2, 42, 2).isEmpty()).isTrue();
    // different offset
    assertThat(new AnalyzerMessage.TextSpan(42, 2, 42, 5).isEmpty()).isFalse();
    // different lines, different offset
    assertThat(new AnalyzerMessage.TextSpan(42, 2, 43, 5).isEmpty()).isFalse();
    // different lines, same offset
    assertThat(new AnalyzerMessage.TextSpan(42, 2, 43, 2).isEmpty()).isFalse();
  }

  @Test
  void textSpanOnLine() {
    assertThat(new AnalyzerMessage.TextSpan(42).onLine()).isTrue();
    assertThat(new AnalyzerMessage.TextSpan(0, -1, 0, 5).onLine()).isTrue();
    assertThat(new AnalyzerMessage.TextSpan(0, 2, 0, 2).onLine()).isFalse();
  }

  @Test
  void textSpanForTrees() {
    CompilationUnitTree cut = JParserTestUtils.parse("class A {\n}\n");
    ClassTree classTree = (ClassTree) cut.types().get(0);

    TextSpan textSpan;

    textSpan = AnalyzerMessage.textSpanFor(classTree);
    assertThat(textSpan.startLine).isEqualTo(1);
    assertThat(textSpan.startCharacter).isZero();
    assertThat(textSpan.endLine).isEqualTo(2);
    assertThat(textSpan.endCharacter).isEqualTo(1);

    textSpan = AnalyzerMessage.textSpanBetween(classTree.declarationKeyword(), classTree.openBraceToken());
    assertThat(textSpan.startLine).isEqualTo(1);
    assertThat(textSpan.startCharacter).isZero();
    assertThat(textSpan.endLine).isEqualTo(1);
    assertThat(textSpan.endCharacter).isEqualTo(9);

    textSpan = AnalyzerMessage.textSpanBetween(classTree.declarationKeyword(), true, classTree.openBraceToken(), true);
    assertThat(textSpan.startLine).isEqualTo(1);
    assertThat(textSpan.startCharacter).isZero();
    assertThat(textSpan.endLine).isEqualTo(1);
    assertThat(textSpan.endCharacter).isEqualTo(9);

    textSpan = AnalyzerMessage.textSpanBetween(classTree.declarationKeyword(), true, classTree.openBraceToken(), false);
    assertThat(textSpan.startLine).isEqualTo(1);
    assertThat(textSpan.startCharacter).isZero();
    assertThat(textSpan.endLine).isEqualTo(1);
    assertThat(textSpan.endCharacter).isEqualTo(8);

    textSpan = AnalyzerMessage.textSpanBetween(classTree.declarationKeyword(), false, classTree.openBraceToken(), true);
    assertThat(textSpan.startLine).isEqualTo(1);
    assertThat(textSpan.startCharacter).isEqualTo(5);
    assertThat(textSpan.endLine).isEqualTo(1);
    assertThat(textSpan.endCharacter).isEqualTo(9);

    textSpan = AnalyzerMessage.textSpanBetween(classTree.declarationKeyword(), false, classTree.openBraceToken(), false);
    assertThat(textSpan.startLine).isEqualTo(1);
    assertThat(textSpan.startCharacter).isEqualTo(5);
    assertThat(textSpan.endLine).isEqualTo(1);
    assertThat(textSpan.endCharacter).isEqualTo(8);
  }

  @Test
  void textSpanForTextBlocks() {
    CompilationUnitTree cut = JParserTestUtils.parse(
      "class A {\n String s = \"\"\"\n" +
      "              \n" +
      "  hello\n" +
      "  \n" +
      "  \"\"\";}\n");
    ClassTree classTree = (ClassTree) cut.types().get(0);
    VariableTree variableTree = (VariableTree) classTree.members().get(0);
    ExpressionTree textBlock = variableTree.initializer();

    TextSpan textSpan;

    textSpan = AnalyzerMessage.textSpanFor(textBlock);
    assertThat(textSpan.startLine).isEqualTo(2);
    assertThat(textSpan.startCharacter).isEqualTo(12);
    assertThat(textSpan.endLine).isEqualTo(6);
    assertThat(textSpan.endCharacter).isEqualTo(5);
  }

  @Test
  void textSpanBetweenTextBlocks() {
    CompilationUnitTree cut;
    cut = JParserTestUtils.parse(
      "class A {  " +
        "public void str() {\n" +
        "    String question2 = \"\"\"\n" +
        "              \\\"What's the point, really\n" +
        "              \\\"What's the point, really\n" +
        "              ?\"\"\"; " +
        "}" +
        "}");
    ClassTree classTree = (ClassTree) cut.types().get(0);
    VariableTree variableTree = (VariableTree) ((MethodTree)(classTree.members()).get(0)).block().body().get(0);

    TextSpan textSpan;

    textSpan = AnalyzerMessage.textSpanBetween(variableTree, variableTree.initializer());
    assertThat(textSpan.startLine).isEqualTo(2);
    assertThat(textSpan.startCharacter).isEqualTo(4);
    assertThat(textSpan.endLine).isEqualTo(5);
    assertThat(textSpan.endCharacter).isEqualTo(18);
  }

  @Test
  void shouldFailOnEmptySpans() {
    CompilationUnitTree cut = JParserTestUtils.parse("class A {\n}\n");

    try {
      AnalyzerMessage.textSpanFor(cut.eofToken());
      Fail.fail("Should have failed on empty issue location");
    } catch (Exception e) {
      assertThat(e)
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Invalid issue location: Text span is empty when trying reporting on (l:3, c:1).");
    }
  }

  @Test
  void shouldFailOnTreeWithoutParents() {
    InferedTypeTree itt = new InferedTypeTree();
    try {
      AnalyzerMessage.textSpanFor(itt);
      Fail.fail("Should have failed on empty issue location");
    } catch (Exception e) {
      assertThat(e)
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Trying to report on an empty tree with no parent");
    }
  }

  @Test
  void shouldNotFailOnEmptyTrees() {
    CompilationUnitTree cut = JParserTestUtils.parse(
      "class A {\n" +
        "  void foo(java.util.List l) {\n" +
        "    l.forEach(o -> {});\n" +
        "  }\n" +
        "}");

    MethodTree methodTree = (MethodTree) ((ClassTree) cut.types().get(0)).members().get(0);
    MethodInvocationTree mit = (MethodInvocationTree) ((ExpressionStatementTree) methodTree.block().body().get(0)).expression();
    VariableTree variableTree = ((LambdaExpressionTree) mit.arguments().get(0)).parameters().get(0);
    TypeTree type = variableTree.type();

    assertThat(type).isInstanceOf(InferedTypeTree.class);
    assertThat(AnalyzerMessage.textSpanFor(type))
      .extracting("startLine", "startCharacter", "endLine", "endCharacter")
      .contains(3, 14, 3, 15);

    assertThat(AnalyzerMessage.textSpanBetween(type, methodTree))
      .extracting("startLine", "startCharacter", "endLine", "endCharacter")
      .contains(3, 14, 4, 3);

    assertThat(AnalyzerMessage.textSpanBetween(methodTree, type))
      .extracting("startLine", "startCharacter", "endLine", "endCharacter")
      .contains(2, 2, 3, 15);
  }

  @Test
  void testAnalyzerMessageOnFile() {
    JavaCheck javaCheck = mock(JavaCheck.class);
    InputFile file = TestUtils.emptyInputFile("a");
    String message = "analyzer message";
    int cost = 3;
    AnalyzerMessage analyzerMessage = new AnalyzerMessage(javaCheck, file, null, message, cost);
    assertThat(analyzerMessage.getCheck()).isEqualTo(javaCheck);
    assertThat(analyzerMessage.getInputComponent()).isEqualTo(file);
    assertThat(analyzerMessage.getLine()).isNull();
    assertThat(analyzerMessage.getMessage()).isEqualTo(message);
    assertThat(analyzerMessage.getCost()).isEqualTo(cost);
    assertThat(analyzerMessage.primaryLocation()).isNull();
  }

  @Test
  void testAnalyzerMessageWithoutCost() {
    JavaCheck javaCheck = mock(JavaCheck.class);
    InputFile file = TestUtils.emptyInputFile("a");
    String message = "analyzer message";
    int cost = 0;
    AnalyzerMessage analyzerMessage = new AnalyzerMessage(javaCheck, file, null, message, cost);
    assertThat(analyzerMessage.getCheck()).isEqualTo(javaCheck);
    assertThat(analyzerMessage.getInputComponent()).isEqualTo(file);
    assertThat(analyzerMessage.getLine()).isNull();
    assertThat(analyzerMessage.getMessage()).isEqualTo(message);
    assertThat(analyzerMessage.getCost()).isNull();
    assertThat(analyzerMessage.primaryLocation()).isNull();
  }

  @Test
  void toString_test() throws Exception {
    JavaCheck javaCheck = mock(JavaCheck.class);
    InputFile file = TestUtils.emptyInputFile("file");
    String message = "analyzer message";
    int cost = 0;
    AnalyzerMessage analyzerMessage = new AnalyzerMessage(javaCheck, file, 12, message, cost);
    assertThat(analyzerMessage).hasToString("'analyzer message' in file:12");
    analyzerMessage = new AnalyzerMessage(javaCheck, null, null, null, cost);
    assertThat(analyzerMessage).hasToString("'null' in null:null");
  }

  @Test
  void test_text_span_equals_hashcode() {
    TextSpan textSpan1 = new TextSpan(1, 2, 3, 4);
    TextSpan textSpanSameAs1 = new TextSpan(1, 2, 3, 4);
    TextSpan textSpan2 = new TextSpan(2, 3, 4, 5);

    assertThat(textSpan1.equals(null)).isFalse();
    assertThat(textSpan1.equals(new Object())).isFalse();
    assertThat(textSpan1.equals(textSpan2)).isFalse();

    assertThat(textSpan1.equals(textSpan1)).isTrue();
    assertThat(textSpan1.equals(textSpanSameAs1)).isTrue();

    assertThat(textSpan1.hashCode()).isNotEqualTo(textSpan2.hashCode());
    assertThat(textSpan1.hashCode()).hasSameHashCodeAs(textSpan1.hashCode());
    assertThat(textSpan1.hashCode()).hasSameHashCodeAs(textSpanSameAs1.hashCode());

    assertThat(new TextSpan(1, 2, 3, 4).equals(new TextSpan(1, 99, 3, 4))).isFalse();
    assertThat(new TextSpan(1, 2, 3, 4).equals(new TextSpan(1, 2, 99, 4))).isFalse();
    assertThat(new TextSpan(1, 2, 3, 4).equals(new TextSpan(1, 2, 3, 99))).isFalse();
  }
}
