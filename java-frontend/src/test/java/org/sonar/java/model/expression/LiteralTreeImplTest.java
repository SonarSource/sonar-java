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
package org.sonar.java.model.expression;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.model.JParserTestUtils;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BooleanLiteralTree;
import org.sonar.plugins.java.api.tree.CharLiteralTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.DoubleLiteralTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.FloatLiteralTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.IntLiteralTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.LongLiteralTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.StringLiteralTree;
import org.sonar.plugins.java.api.tree.Tree;

import static org.assertj.core.api.Assertions.assertThat;

class LiteralTreeImplTest {

  private static CompilationUnitTree currentSourceCodeCache;

  @Test
  void null_literal() {
    var literal = (LiteralTree) parseTree(null).tree();
    assertThat(literal.value()).isEqualTo("null");
    assertThat(literal.unquotedValue()).isEqualTo("null");
    assertThat(literal.parsedValue()).isNull();
  }

  @Test
  void null_literal_values() {
    assertResolveAsConstantMatch(parseTrees(
      // we don't support cast, so we accidentally have the good answer for null literal
      (String) null));
  }

  @Test
  void boolean_literal() {
    var falseLiteral = (BooleanLiteralTree) parseTree(false).tree();
    assertThat(falseLiteral.value()).isEqualTo("false");
    assertThat(falseLiteral.unquotedValue()).isEqualTo("false");
    assertThat(falseLiteral.parsedValue()).isInstanceOf(Boolean.class).isSameAs(Boolean.FALSE);
    assertThat(falseLiteral.booleanValue()).isFalse();

    var trueLiteral = (BooleanLiteralTree) parseTree(true).tree();
    assertThat(trueLiteral.value()).isEqualTo("true");
    assertThat(trueLiteral.unquotedValue()).isEqualTo("true");
    assertThat(trueLiteral.parsedValue()).isInstanceOf(Boolean.class).isSameAs(Boolean.TRUE);
    assertThat(trueLiteral.booleanValue()).isTrue();
  }

  @Test
  void boolean_literal_values() {
    assertResolveAsConstantMatch(parseTrees(
      !true,
      !false));

    // limitation, not yet supported:
    assertResolveAsConstantIsNull(parseTrees(
      true && true,
      true && false,
      false && true,
      false && false,
      true || true,
      true || false,
      false || true,
      false || false));
  }

  @Test
  void int_literal() {
    var literal = (IntLiteralTree) parseTree(1_000).tree();
    assertThat(literal.value()).isEqualTo("1_000");
    assertThat(literal.unquotedValue()).isEqualTo("1_000");
    assertThat(literal.parsedValue()).isInstanceOf(Integer.class).isEqualTo(1000);
    assertThat(literal.intValue()).isEqualTo(1000);
  }

  @Test
  void int_literal_values() {
    assertLiteralParsedValuesAndResolveAsConstantMatch(parseTrees(
      0,
      1,
      1_000_000,
      // Integer.MIN_VALUE,
      0x80000000,
      // Integer.MAX_VALUE,
      0x7fffffff,
      2147483647,
      0x7fffffff,
      0xfffffffe,
      // -1
      0xffffffff));
    assertResolveAsConstantMatch(parseTrees(
      0,
      1,
      -1,
      1_000_000,
      -1_000_000,
      // Integer.MIN_VALUE,
      0x80000000,
      -0x80000000,
      -2147483648,
      // Integer.MAX_VALUE,
      0x7fffffff,
      -0x7fffffff,
      2147483647,
      0x7fffffff,
      -0x7fffffff,
      0xfffffffe,
      -0xfffffffe,
      -0xffffffff,
      // -1
      0xffffffff,
      0 + 0,
      1 + 1,
      (1 + (1)),
      0xffffffff + 1,
      0xffffffff - 1,
      10 - 2,
      2 * -3,
      0xffffffff * 2,
      0xffffffff * 0xffffffff));
  }

  @Test
  void long_literal() {
    var literal = (LongLiteralTree) parseTree(1_000L).tree();
    assertThat(literal.value()).isEqualTo("1_000L");
    assertThat(literal.unquotedValue()).isEqualTo("1_000L");
    assertThat(literal.parsedValue()).isInstanceOf(Long.class).isEqualTo(1000L);
    assertThat(literal.longValue()).isEqualTo(1000L);
  }

  @Test
  void long_literal_values() {
    assertLiteralParsedValuesAndResolveAsConstantMatch(parseTrees(
      0L,
      1L,
      1_000_000L,
      // Long.MIN_VALUE,
      0x8000000000000000L,
      // Long.MAX_VALUE,
      0x7fffffffffffffffL,
      0x7fFfFfFfFfFfFfFfL,
      9223372036854775807L,
      0xfffffffffffffffeL,
      // -1
      0xffffffffffffffffL));
    assertResolveAsConstantMatch(parseTrees(
      0L,
      1L,
      -1L,
      1_000_000L,
      -1_000_000L,
      4_000_000_000_000_000_000L,
      // Integer.MIN_VALUE,
      0x8000000000000000L,
      -9223372036854775808L,
      // Long.MAX_VALUE,
      0x7fffffffffffffffL,
      -0x7fffffffffffffffL,
      0x7fFfFfFfFfFfFfFfL,
      -0x7fFfFfFfFfFfFfFfL,
      9223372036854775807L,
      -9223372036854775807L,
      0xfffffffffffffffeL,
      -0xfffffffffffffffeL,
      // -1
      0xffffffffffffffffL));

    // limitation, not yet supported:
    assertResolveAsConstantIsNull(parseTrees(
      0L + 0L,
      1L + 1L,
      (1L + (1L)),
      0xffffffffffffffffL + 1L,
      0xffffffffffffffffL - 1L,
      10L - 2L,
      2L * -3L,
      0xffffffffffffffffL * 2L,
      0xffffffffffffffffL * 0xffffffffffffffffL));
  }

  @Test
  void float_literal() {
    var literal = (FloatLiteralTree) parseTree(1_000.0000f).tree();
    assertThat(literal.value()).isEqualTo("1_000.0000f");
    assertThat(literal.unquotedValue()).isEqualTo("1_000.0000f");
    assertThat(literal.parsedValue()).isInstanceOf(Float.class).isEqualTo(1000.0f);
    assertThat(literal.floatValue()).isEqualTo(1000.0f);
  }

  @Test
  void float_literal_values() {
    assertLiteralParsedValuesAndResolveAsConstantMatch(parseTrees(
      0.0,
      0.0f,
      1f,
      1.0,
      1_000_000.0f,
      // Long.MIN_VALUE,
      0x0.000002P-126f,
      1.4e-45f,
      // Long.MAX_VALUE,
      0x1.fffffeP+127f,
      3.4028235e+38f,
      1.1f,
      .0f));
    assertResolveAsConstantMatch(parseTrees(
      (1.1f)));

    // limitation, not yet supported:
    assertResolveAsConstantIsNull(parseTrees(
      -0.0f,
      -1f,
      -1_000_000.0f,
      // Float.MIN_VALUE,
      -0x0.000002P-126f,
      -1.4e-45f,
      // Float.MAX_VALUE,
      -0x1.fffffeP+127f,
      -3.4028235e+38f,
      -1.1f,
      (-1.1f),
      -.0f,
      0.0f + 0.0f,
      1.0f + 1.0f,
      (1.0f + (1.0f)),
      3.4028235e+38f + 1.0e+38f,
      1.4e-45f - 1.0e-45f,
      3.4028235e+38f * -2.0f));
  }

  @Test
  void double_literal() {
    var literal = (DoubleLiteralTree) parseTree(1_000.0000d).tree();
    assertThat(literal.value()).isEqualTo("1_000.0000d");
    assertThat(literal.unquotedValue()).isEqualTo("1_000.0000d");
    assertThat(literal.parsedValue()).isInstanceOf(Double.class).isEqualTo(1000.0);
    assertThat(literal.doubleValue()).isEqualTo(1000.0);
  }

  @Test
  void double_literal_values() {
    assertLiteralParsedValuesAndResolveAsConstantMatch(parseTrees(
      0.0d,
      1d,
      1_000_000.0d,
      // Double.MIN_VALUE,
      0x0.0000000000001P-1022,
      4.9e-324,
      0x1.0p-1022,
      0x0.000002P-126d,
      1.4e-45d,
      // Double.MAX_VALUE,
      0x1.fffffffffffffP+1023,
      1.7976931348623157e+308,
      0x1.fffffeP+127d,
      3.4028235e+38d,
      1.1d,
      .0f));
    assertResolveAsConstantMatch(parseTrees(
      (1.1d)));

    // limitation, not yet supported:
    assertResolveAsConstantIsNull(parseTrees(
      -0.0d,
      -1d,
      -1_000_000.0d,
      // Long.MIN_VALUE,
      -0x0.000002P-126d,
      -1.4e-45d,
      // Long.MAX_VALUE,
      -0x1.fffffeP+127d,
      -3.4028235e+38d,
      -1.1d,
      (-1.1d),
      -.0d,
      0.0d + 0.0d,
      1.0d + 1.0d,
      (1.0d + (1.0d)),
      3.4028235e+38d + 1.0e+38d,
      1.4e-45d - 1.0e-45d,
      3.4028235e+38d * -2.0d));
  }

  @Test
  void char_literal() {
    var lit = (CharLiteralTree) parseTree('a').tree();
    assertThat(lit.value()).isEqualTo("'a'");
    assertThat(lit.unquotedValue()).isEqualTo("a");
    assertThat(lit.parsedValue()).isEqualTo('a').isInstanceOf(Character.class);
    assertThat(lit.charValue()).isEqualTo('a');

    lit = (CharLiteralTree) parseTree('\b').tree();
    assertThat(lit.value()).isEqualTo("'\\b'");
    assertThat(lit.unquotedValue()).isEqualTo("\\b");
    assertThat(lit.parsedValue()).isEqualTo('\b').isInstanceOf(Character.class);
    assertThat(lit.charValue()).isEqualTo('\b');

    lit = (CharLiteralTree) parseTree('\u0041').tree();
    assertThat(lit.value()).isEqualTo("'\\u0041'");
    assertThat(lit.unquotedValue()).isEqualTo("\\u0041");
    assertThat(lit.parsedValue()).isEqualTo('A').isInstanceOf(Character.class);
    assertThat(lit.charValue()).isEqualTo('A');

    lit = (CharLiteralTree) parseTree('\101').tree();
    assertThat(lit.value()).isEqualTo("'\\101'");
    assertThat(lit.unquotedValue()).isEqualTo("\\101");
    assertThat(lit.parsedValue()).isEqualTo('A').isInstanceOf(Character.class);
    assertThat(lit.charValue()).isEqualTo('A');
  }

  @Test
  void char_literal_values() {
    // limitation: Bug in the ECJ parser, it fails to parse this file if we add in the following arguments the char literal '\s'
    assertLiteralParsedValuesAndResolveAsConstantMatch(parseTrees(
      '0',
      'a',
      ' ',
      '\b',
      '\t',
      '\n',
      '\f',
      '\r',
      '\"',
      '\'',
      '\\',
      '\0',
      '\00',
      '\000',
      '\377',
      '\u0000',
      '\u1234',
      '\uffff'));
    assertResolveAsConstantMatch(parseTrees(
      ('a')));

    // limitation, not yet supported:
    assertResolveAsConstantIsNull(parseTrees(
      'a' + 'a',
      'a' - 2));
  }

  @Test
  void string_literal() {
    var empty = (StringLiteralTree) parseTree("").tree();
    assertThat(empty.value()).isEqualTo("\"\"");
    assertThat(empty.unquotedValue()).isEmpty();
    assertThat(empty.parsedValue()).isEqualTo("");
    assertThat(empty.stringValue()).isEmpty();

    var letters = (StringLiteralTree) parseTree("abcdef").tree();
    assertThat(letters.value()).isEqualTo("\"abcdef\"");
    assertThat(letters.unquotedValue()).isEqualTo("abcdef");
    assertThat(letters.parsedValue()).isEqualTo("abcdef");
    assertThat(letters.stringValue()).isEqualTo("abcdef");

    var newLine = (StringLiteralTree) parseTree(" abc '\s'\n def\n").tree();
    assertThat(newLine.value()).isEqualTo("\" abc '\\s'\\n def\\n\"");
    assertThat(newLine.unquotedValue()).isEqualTo(" abc '\\s'\\n def\\n");
    assertThat(newLine.parsedValue()).isEqualTo(" abc ' '\n def\n");
    assertThat(newLine.stringValue()).isEqualTo(" abc ' '\n def\n");

    var unicodeEscape = (StringLiteralTree) parseTree(" \u0061 \u0062 \u0063 ").tree();
    assertThat(unicodeEscape.value()).isEqualTo("\" \\u0061 \\u0062 \\u0063 \"");
    assertThat(unicodeEscape.unquotedValue()).isEqualTo(" \\u0061 \\u0062 \\u0063 ");
    assertThat(unicodeEscape.parsedValue()).isEqualTo(" a b c ");
    assertThat(unicodeEscape.stringValue()).isEqualTo(" a b c ");

    var octalEscape = (StringLiteralTree) parseTree(" \101 \102 \103 ").tree();
    assertThat(octalEscape.value()).isEqualTo("\" \\101 \\102 \\103 \"");
    assertThat(octalEscape.unquotedValue()).isEqualTo(" \\101 \\102 \\103 ");
    assertThat(octalEscape.parsedValue()).isEqualTo(" A B C ");
    assertThat(octalEscape.stringValue()).isEqualTo(" A B C ");

    var escapeLimits = (StringLiteralTree) parseTree(" \b \s \t \n \f \r \" \' \\ \0 \00 \000 \377 \u0000 \uffff ").tree();
    assertThat(escapeLimits.value())
      .isEqualTo("\" \\b \\s \\t \\n \\f \\r \\\" \\' \\\\ \\0 \\00 \\000 \\377 \\u0000 \\uffff \"");
    assertThat(escapeLimits.unquotedValue())
      .isEqualTo(" \\b \\s \\t \\n \\f \\r \\\" \\' \\\\ \\0 \\00 \\000 \\377 \\u0000 \\uffff ");
    assertThat(escapeLimits.parsedValue())
      .isEqualTo(" \b \s \t \n \f \r \" \' \\ \0 \00 \000 \377 \u0000 \uffff ");
    assertThat(escapeLimits.stringValue())
      .isEqualTo(" \b \s \t \n \f \r \" \' \\ \0 \00 \000 \377 \u0000 \uffff ");
  }

  @Test
  void string_literal_values() {
    assertLiteralParsedValuesAndResolveAsConstantMatch(parseTrees(
      "",
      "a",
      "abc",
      "a\n\"\'c\n",
      "'\u0040\u0041'",
      "\"\u26A0a\012cdef\"",
      "1\r\n2\r\n3\r\n",
      "\\n\\t",
      "\0\0\0"));
    assertResolveAsConstantMatch(parseTrees(
      "a" + "b",
      "a" + 'b',
      "a" + 2,
      ("a"),
      ("abc"),
      ("a" + "\n")));

    // limitation, not yet supported:
    assertResolveAsConstantIsNull(parseTrees(
      "a" + null));
  }

  @Test
  void text_block_literal() {
    var empty = (StringLiteralTree) parseTree("""
      """).tree();
    assertThat(empty.value()).isEqualTo("\"\"\"\n      \"\"\"");
    assertThat(empty.unquotedValue()).isEmpty();
    assertThat(empty.parsedValue()).isEqualTo("");
    assertThat(empty.stringValue()).isEmpty();

    var letters = (StringLiteralTree) parseTree("""
      abcdef  """).tree();
    assertThat(letters.value())
      .isEqualTo("\"\"\"\n      abcdef  \"\"\"");
    assertThat(letters.unquotedValue())
      .isEqualTo("abcdef");
    assertThat(letters.parsedValue())
      .isEqualTo("abcdef");

    var severalLines = (StringLiteralTree) parseTree("""
        abc '\s'
        def
      """).tree();
    assertThat(severalLines.value())
      .isEqualTo("\"\"\"\n        abc '\\s'\n        def\n      \"\"\"");
    assertThat(severalLines.unquotedValue())
      .isEqualTo("  abc '\\s'\n  def\n");
    assertThat(severalLines.parsedValue())
      .isEqualTo("  abc ' '\n  def\n");

    var lineContinuation = (StringLiteralTree) parseTree("""
       abc \
       def \
      """).tree();
    assertThat(lineContinuation.value())
      .isEqualTo("\"\"\"\n       abc \\\n       def \\\n      \"\"\"");
    assertThat(lineContinuation.unquotedValue())
      // We don't remove the \\\n (line continuation) from the content
      .isEqualTo(" abc \\\n def \\\n");
    assertThat(lineContinuation.parsedValue())
      .isEqualTo(" abc  def ");

    var unicodeEscape = (StringLiteralTree) parseTree("""
      \u0061 \u0062 \u0063""").tree();
    assertThat(unicodeEscape.value())
      .isEqualTo("\"\"\"\n      \\u0061 \\u0062 \\u0063\"\"\"");
    assertThat(unicodeEscape.unquotedValue())
      .isEqualTo("\\u0061 \\u0062 \\u0063");
    assertThat(unicodeEscape.parsedValue())
      .isEqualTo("a b c");
    assertThat(unicodeEscape.stringValue())
      .isEqualTo("a b c");
  }

  @Test
  void text_block_literal_escapes() {

    var octalEscape = (StringLiteralTree) parseTree("""
      \101 \102 \103""").tree();
    assertThat(octalEscape.value())
      .isEqualTo("\"\"\"\n      \\101 \\102 \\103\"\"\"");
    assertThat(octalEscape.unquotedValue())
      .isEqualTo("\\101 \\102 \\103");
    assertThat(octalEscape.parsedValue())
      .isEqualTo("A B C");
    assertThat(octalEscape.stringValue())
      .isEqualTo("A B C");

    TreeAndValue ecjBug1 = parseTree("""
      before \'\" after""");
    var ecjBug1Literal = (StringLiteralTree) ecjBug1.tree();
    assertThat(ecjBug1Literal.value())
      .isEqualTo("\"\"\"\n      before \\'\\\" after\"\"\"");
    assertThat(ecjBug1Literal.unquotedValue())
      .isEqualTo("before \\'\\\" after");
    assertThat(ecjBug1Literal.parsedValue())
      // BUG in the ECJ parser, it replaces \'\" with '\ instead of '"
      .isNotEqualTo(ecjBug1.value()).isEqualTo("before \'\\after");
    // Knowing the Java parses it correctly
    assertThat(ecjBug1.value()).isEqualTo("before \'\" after");

    TreeAndValue ecjBug2 = parseTree("""
        \
      """);
    var ecjBug2Literal = (StringLiteralTree) ecjBug2.tree();
    assertThat(ecjBug2Literal.value())
      .isEqualTo("\"\"\"\n        \\\n      \"\"\"");
    assertThat(ecjBug2Literal.unquotedValue())
      .isEqualTo("  \\\n");
    assertThat(ecjBug2Literal.parsedValue())
      // BUG in the ECJ parser, it does not keep the 2 spaces
      .isNotEqualTo(ecjBug2.value()).isEqualTo("");
    // Knowing the Java parses it as 2 spaces
    assertThat(ecjBug2.value()).isEqualTo("  ");
  }

  @Test
  void text_block_literal_values() {
    assertLiteralParsedValuesAndResolveAsConstantMatch(parseTrees(
      // empty text box
      """
        """,
      // text block without indentation
      """
        line 1
        line 2
        """,
      // text block with 2 space indentation
      """
          line 1
          line 2
        """,
      // text block with escaped characters
      """
        \u26A0 line\t1
        \u26A0 line\t2
        """,
      // text block with line continuation
      """
          with continuation \
          end \
        """,
      // empty string with line continuations:
      """
        \
        """,
      // text block with all escaped characters
      """
        \" \b \s \t \n \f \r \' \\ \0 \00 \000 \377 \u0000 \uffff """,
      // text block with ending whitespace
      """
          line 3  \s
          line 4  \s
        """,
      // text block without a new line at the end
      """
        line 5  \s
        line 6""",
      // text block with tailing space before the end are trimmed
      """
        line 7  \s
        line 8  """));
    assertResolveAsConstantMatch(parseTrees(
      """
        line 9
        """ + "line10\n"));

    // limitation, not yet supported:
    assertResolveAsConstantIsNull(parseTrees(
      """
        \s""" + null));
  }

  private void assertLiteralParsedValuesAndResolveAsConstantMatch(List<TreeAndValue> treeAndValues) {
    assertLiteralParsedValuesMatch(treeAndValues);
    assertResolveAsConstantMatch(treeAndValues);
  }

  private void assertLiteralParsedValuesMatch(List<TreeAndValue> treeAndValues) {
    for (TreeAndValue treeAndValue : treeAndValues) {
      assertThat(treeAndValue.tree())
        .describedAs(treeAndValue.toString())
        .isInstanceOf(LiteralTree.class);
      assertThat(((LiteralTree) treeAndValue.tree()).parsedValue())
        .describedAs(treeAndValue.toString())
        .isEqualTo(treeAndValue.value());
    }
  }

  private void assertResolveAsConstantMatch(List<TreeAndValue> treeAndValues) {
    for (TreeAndValue treeAndValue : treeAndValues) {
      assertThat(ExpressionUtils.resolveAsConstant(treeAndValue.tree()))
        .describedAs(treeAndValue.toString())
        .isEqualTo(treeAndValue.value());
    }
  }

  private void assertResolveAsConstantIsNull(List<TreeAndValue> treeAndValues) {
    for (TreeAndValue treeAndValue : treeAndValues) {
      assertThat(ExpressionUtils.resolveAsConstant(treeAndValue.tree()))
        .describedAs(treeAndValue.toString())
        .isNull();
    }
  }

  record TreeAndValue(ExpressionTree tree, Object value) {
    @Override
    public String toString() {
      return "At line " + lineOf(tree) +
        ", tree: " + tree.getClass().getSimpleName() +
        ", value = (" + (value != null ? value.getClass().getSimpleName() : "Object") + ") " + value;
    }
  }

  private static List<TreeAndValue> parseTrees(Object... values) {
    return parseTrees(callerLineNumber(), currentMethodName(), values);
  }

  private static TreeAndValue parseTree(Object value) {
    return parseTrees(callerLineNumber(), currentMethodName(), new Object[] {value}).get(0);
  }

  private static List<TreeAndValue> parseTrees(int line, String methodName, Object... values) {
    Arguments arguments = parseMethodArguments(line, methodName, values);
    if (arguments.size() != values.length) {
      throw new IllegalStateException("Expected the same number of arguments as values, but found: " + arguments.size() + " vs " + values.length);
    }
    var result = new ArrayList<TreeAndValue>();
    for (int i = 0; i < arguments.size(); i++) {
      result.add(new TreeAndValue(arguments.get(i), values[i]));
    }
    return result;
  }

  private static Arguments parseMethodArguments(int line, String methodName, Object... values) {
    Arguments arguments = findArgumentsOfMethod(line, methodName);
    if (arguments.size() != values.length) {
      throw new IllegalStateException("Expected the same number of arguments as values, but found: " + arguments.size() + " vs " + values.length);
    }
    return arguments;
  }

  private static Arguments findArgumentsOfMethod(int line, String methodName) {
    var array = new ArrayList<Arguments>();
    parseCurrentSourceCode().accept(new BaseTreeVisitor() {
      @Override
      public void visitMethodInvocation(MethodInvocationTree tree) {
        if (tree.methodSelect() instanceof IdentifierTree identifier && identifier.name().equals(methodName) && lineOf(identifier) == line) {
          array.add(tree.arguments());
        }
        super.visitMethodInvocation(tree);
      }
    });
    if (array.size() != 1) {
      throw new IllegalStateException("Expected exactly one method " + methodName + " at line " + line + ", but found: " + array.size());
    }
    return array.get(0);
  }

  private static int lineOf(Tree tree) {
    return tree.firstToken().range().start().line();
  }

  private static CompilationUnitTree parseCurrentSourceCode() {
    if (currentSourceCodeCache == null) {
      Path thisJavaFilePath = Path.of("src", "test", "java",
        LiteralTreeImplTest.class.getName().replace('.', '/').concat(".java"));
      currentSourceCodeCache = JParserTestUtils.parse(thisJavaFilePath.toFile());
    }
    return currentSourceCodeCache;
  }

  private static int callerLineNumber() {
    return Thread.currentThread().getStackTrace()[3].getLineNumber();
  }

  private static String currentMethodName() {
    return Thread.currentThread().getStackTrace()[2].getMethodName();
  }

}
