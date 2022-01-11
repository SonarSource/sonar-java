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
package org.sonar.java.checks.helpers;

import java.util.regex.Pattern;
import org.assertj.core.api.AbstractBooleanAssert;
import org.junit.jupiter.api.Test;
import org.sonarsource.analyzer.commons.regex.RegexParseResult;
import org.sonarsource.analyzer.commons.regex.ast.FlagSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.java.checks.helpers.SimplifiedRegexCharacterClassTest.parseRegex;

class RegexTreeHelperTest {

  @Test
  void intersects_one_character() {
    assertIntersects("a", "a", false).isTrue();
    assertIntersects("a", "b", true).isFalse();
    assertIntersects("a", "(?i)a", false).isTrue();
    assertIntersects("a", "A", false).isFalse();
    assertIntersects("A", "a", false).isFalse();
    assertIntersects("a", "A", false, Pattern.CASE_INSENSITIVE).isTrue();
    assertIntersects("a", "(?i)A", false).isTrue();
    assertIntersects("A", "(?i)a", false).isTrue();
    assertIntersects("(?i)a", "A", false).isTrue();
    assertIntersects("(?i)A", "a", false).isTrue();
    assertIntersects("(?i)A", "(?i)a", false).isTrue();
  }

  @Test
  void intersects_dot() {
    assertIntersects(".", "a", false).isTrue();
    assertIntersects("a", ".", true).isTrue();
    assertIntersects(".", "[a-z]", false).isTrue();
    assertIntersects("[a-z]+", ".+", false).isTrue();

    // by default [\r\n\u0085\u2028\u2029] excluded from DotTree
    assertIntersects(".", "[\r\n\u0085\u2028\u2029]", false).isFalse();
    assertIntersects(".+", "\b\f \t", false).isTrue();

    // only \n excluded when UNIX_LINES is set
    assertIntersects(".", "\n", false, Pattern.UNIX_LINES).isFalse();
    assertIntersects("(?d).", "\n", false).isFalse();
    assertIntersects(".+", "\b\f \t\r\u0085\u2028\u2029", false, Pattern.UNIX_LINES).isTrue();
    assertIntersects("(?d).+", "\b\f \t\r\u0085\u2028\u2029", false).isTrue();

    // no exclusion and UNIX_LINES is ignored when DOTALL is set
    assertIntersects(".", "\n", false, Pattern.DOTALL).isTrue();
    assertIntersects(".", "\n", false, Pattern.UNIX_LINES|Pattern.DOTALL).isTrue();
    assertIntersects("(?d).", "\n", false, Pattern.DOTALL).isTrue();
    assertIntersects("(?s).", "\n", false).isTrue();
    assertIntersects("(?ds).", "\n", false).isTrue();
    assertIntersects(".+", "\b\f \t\r\n\u0085\u2028\u2029", false, Pattern.DOTALL).isTrue();
  }

  @Test
  void intersects_one_range() {
    assertIntersects("[b-d]", "a", true).isFalse();
    assertIntersects("[b-d]", "b", false).isTrue();
    assertIntersects("[a-de-g]", "[a-g]", false).isTrue();
    assertIntersects("[a-de-g]", "e", false).isTrue();
    assertIntersects("[b-dg-i]", "e", true).isFalse();
  }

  @Test
  void intersects_sequence() {
    assertIntersects("ab", "ab", false).isTrue();
    assertIntersects("ab", "ac", false).isFalse();
    assertIntersects("a[b-d]", "ac", false).isTrue();
    assertIntersects("ac", "a[b-d]", false).isTrue();
    // Boundary not supported
    assertIntersects("^ab", "ab", false).isFalse();
    assertIntersects("ab$", "ab", false).isFalse();
    assertIntersects("()", "()", false).isFalse();
  }

  @Test
  void intersects_disjunction() {
    assertIntersects("(a|b)", "(b|c)", false).isTrue();
    assertIntersects("(a|b)", "(c|d)", false).isFalse();
    assertIntersects("(abc|aab)", "(xyz|aab)", false).isTrue();
    assertIntersects("(abc|aab)", "(xyz|xy|ab)", false).isFalse();
    assertIntersects("((a|(b|c))|(d|e|f))", "((x|y)|(a|z))", false).isTrue();
    assertIntersects("((a|(b|c))|(d|e|f))", "((x|y)|(w|z))", false).isFalse();
  }

  @Test
  void intersects_repetitions() {
    assertIntersects("a{0}", "a|b", false).isFalse();
    assertIntersects("a|b", "a{0}", false).isFalse();
    assertIntersects("a{1}", "a|b", false).isTrue();
    assertIntersects("a|b", "a{1}", false).isTrue();
    assertIntersects("a{1,}", "aa", false).isTrue();
    assertIntersects("aa", "a{1,}", false).isTrue();
    assertIntersects("a+", "aa", false).isTrue();
    assertIntersects("aa", "a+", false).isTrue();
    assertIntersects("a+b", "aa", false).isFalse();
    assertIntersects("aa", "a+b", false).isFalse();
    // finite repetition > 1 not supported
    assertIntersects("a{2}", "aa", false).isFalse();
    assertIntersects("aa", "a{2}", false).isFalse();
  }

  @Test
  void intersects_back_references() {
    // BACK_REFERENCE is not supported
    assertIntersects("(a)\\1", "aa", false).isFalse();
    assertIntersects("aa", "(a)\\1", false).isFalse();
  }

  @Test
  void intersects_sequence_prefix() {
    assertIntersects("ab", false, "abcdef", false, false).isFalse();
    assertIntersects("ab", true, "abcdef", false, false).isFalse();
    assertIntersects("ab", false, "abcdef", true, false).isTrue();

    assertIntersects("abcdef", false, "ab", false, false).isFalse();
    assertIntersects("abcdef", true, "ab", false, false).isTrue();
    assertIntersects("abcdef", false, "ab", true, false).isFalse();
  }

  @Test
  void intersects_not_supported() {
    assertIntersects("a(?<=a)", "a", true).isTrue();
    assertIntersects("a(?<=a)", "a", false).isFalse();
  }

  @Test
  void superset_of_one_character() {
    assertSupersetOf("a", "a", false).isTrue();
    assertSupersetOf("a", "b", true).isFalse();
    assertSupersetOf("a", "(?i)a", false).isFalse();
    assertSupersetOf("a", "A", false, 0).isFalse();
    assertSupersetOf("A", "a", false, 0).isFalse();
    assertSupersetOf("a", "A", false, Pattern.CASE_INSENSITIVE).isTrue();
    assertSupersetOf("a", "(?i)A", false).isFalse();
    assertSupersetOf("A", "(?i)a", false).isFalse();
    assertSupersetOf("(?i)a", "A", false).isTrue();
    assertSupersetOf("(?i)A", "a", false).isTrue();
    assertSupersetOf("(?i)A", "(?i)a", false).isTrue();
    assertSupersetOf("(?<name>X)", "X", false).isTrue();
    assertSupersetOf("X", "(?<name>X)", false).isTrue();
    assertSupersetOf("(?:X)", "X", false).isTrue();
    assertSupersetOf("X", "(?:X)", false).isTrue();
    assertSupersetOf("(?m:X)", "X", false).isTrue();
    assertSupersetOf("X", "(?m:X)", false).isTrue();
    assertSupersetOf("(?>X)", "X", false).isTrue();
    assertSupersetOf("X", "(?>X)", false).isTrue();
  }

  @Test
  void superset_of_dot() {
    assertSupersetOf(".", "a", false).isTrue();
    assertSupersetOf("a", ".", true).isFalse();
    assertSupersetOf(".", "[a-z]", false).isTrue();
    assertSupersetOf("[a-z]+", ".+", false).isFalse();

    // by default [\r\n\u0085\u2028\u2029] excluded from DotTree
    assertSupersetOf(".", "[^\r\n\u0085\u2028\u2029]", false).isTrue();
    assertSupersetOf(".+", "\b\f \t", false).isTrue();

    // only \n excluded when UNIX_LINES is set
    assertSupersetOf(".", "[^\n]", false, Pattern.UNIX_LINES).isTrue();
    assertSupersetOf(".", "\n", false, Pattern.UNIX_LINES).isFalse();
    assertSupersetOf("(?d).", "\n", false).isFalse();
    assertSupersetOf(".+", "\b\f \t\r\u0085\u2028\u2029", false, Pattern.UNIX_LINES).isTrue();
    assertSupersetOf("(?d).+", "\b\f \t\r\u0085\u2028\u2029", false).isTrue();

    // no exclusion and UNIX_LINES is ignored when DOTALL is set
    assertSupersetOf(".", "[^a]", false, Pattern.DOTALL).isTrue();
    assertSupersetOf(".", "a", false, Pattern.DOTALL).isTrue();
    assertSupersetOf(".", "\n", false, Pattern.DOTALL).isTrue();
    assertSupersetOf(".", "\n", false, Pattern.UNIX_LINES|Pattern.DOTALL).isTrue();
    assertSupersetOf("(?d).", "\n", false, Pattern.DOTALL).isTrue();
    assertSupersetOf("(?s).", "\n", false).isTrue();
    assertSupersetOf("(?ds).", "\n", false).isTrue();
    assertSupersetOf(".+", "\b\f \t\r\n\u0085\u2028\u2029", false, Pattern.DOTALL).isTrue();
  }

  @Test
  void superset_of_not_supported() {
    // positive lookahead
    assertSupersetOf("a(?=X)", "a(?=X)", false).isFalse();
    // negative lookahead
    assertSupersetOf("a(?!X)", "a(?!X)", false).isFalse();
    // positive lookbehind
    assertSupersetOf("\\w+(?<=X)", "\\w+(?<=X)", false).isFalse();
    // negative lookbehind
    assertSupersetOf("\\w+(?<!X)", "\\w+(?<!X)", false).isFalse();
  }

  @Test
  void superset_of_one_range() {
    assertSupersetOf("[b-d]", "a", true).isFalse();
    assertSupersetOf("[b-d]", "b", false).isTrue();
    assertSupersetOf("[a-de-g]", "[a-g]", false).isTrue();
    assertSupersetOf("[b-dg-i]", "[a-dg-i]", true).isFalse();
  }

  @Test
  void superset_of_sequence() {
    assertSupersetOf("xy", "xy", false).isTrue();
    assertSupersetOf("xy", "xz", false).isFalse();
    assertSupersetOf("a[b-d]", "ac", false).isTrue();
    // Boundary not supported
    assertSupersetOf("^ab", "ab", false).isFalse();
    assertSupersetOf("ab$", "ab", false).isFalse();
    assertSupersetOf("ab", "^ab", true).isTrue();
    assertSupersetOf("ab", "ab$", true).isTrue();
    assertSupersetOf("()", "()", false).isFalse();
  }

  @Test
  void superset_of_disjunction() {
    assertSupersetOf("a|b|c", "a|b", false).isTrue();
    assertSupersetOf("a|b|c", "a|b|c", false).isTrue();
    assertSupersetOf("a|b|c", "a|b|c|d", false).isFalse();
    assertSupersetOf("((a|(b|c))|(d|e|f))", "a|b", false).isTrue();
    assertSupersetOf("((a|(b|c))|(d|e|f))", "a|b|c|d|e|f", false).isTrue();
    assertSupersetOf("((a|(b|c))|(d|e|f))", "a|b|c|X|d|e|f", false).isFalse();
    assertSupersetOf("((a|(b|c))|(d|e|f))", "((a|f)|(b|e))", false).isTrue();
  }

  @Test
  void superset_of_back_references() {
    // BACK_REFERENCE is not supported
    assertSupersetOf("(a)\\1", "aa", false).isFalse();
    assertSupersetOf("aa", "(a)\\1", false).isFalse();
  }

  @Test
  void superset_of_repetitions() {
    assertSupersetOf("a{0}", "a|b", false).isFalse();
    assertSupersetOf("a|b", "a{0}", false).isFalse();
    assertSupersetOf("a{1}", "a", false).isTrue();
    assertSupersetOf("a+", "a", false).isTrue();
    assertSupersetOf("a*", "a", false).isTrue();
    assertSupersetOf("a|b", "a{1}", false).isTrue();
    assertSupersetOf("a*b", "aab", false).isTrue();
    assertSupersetOf("a+b", "aaab", false).isTrue();
    assertSupersetOf("ab+", "ab", false).isTrue();
    assertSupersetOf("a{1,}b", "aab", false).isTrue();
    assertSupersetOf("aab", "a{1,}b", false).isFalse();
    // finite repetition > 1 not supported
    assertSupersetOf("aa", "a+", false).isFalse();
    assertSupersetOf("a{2}", "aa", false).isFalse();
    assertSupersetOf("aa", "a{2}", false).isFalse();
  }

  @Test
  void superset_of_prefix() {
    assertSupersetOf("x", false, "xy", false, false).isFalse();
    assertSupersetOf("x", true, "xy", false, false).isFalse();
    assertSupersetOf("x", false, "xy", true, false).isTrue();
    assertSupersetOf("x+", false, "xy", true, false).isTrue();
    assertSupersetOf("xy+", false, "xy", true, false).isTrue();
    assertSupersetOf("x", false, "x+y", true, false).isTrue();
    assertSupersetOf("x", false, "x*y", true, false).isFalse();
    assertSupersetOf("\\d+", false, "789", true, false).isTrue();
    assertSupersetOf("\\d+", false, "(7|x)89", true, false).isFalse();

    assertSupersetOf("xy", false, "x", false, false).isFalse();
    assertSupersetOf("xy", true, "x", false, false).isTrue();
    assertSupersetOf("xy", false, "x", true, false).isFalse();

    assertSupersetOf("abc", false, "abcdef", true, false).isTrue();
    assertSupersetOf("abc", true, "abcdef", false, false).isFalse();

    assertSupersetOf("[aeoiu]", false, "[a-z]", true, false).isFalse();
    assertSupersetOf("[a-z]", false, "[aeiou]", true, false).isTrue();
  }

  @Test
  void superset_of_prefix_disjunction() {
    assertSupersetOf("x(y|z)", false, "xy", true, false).isTrue();
    assertSupersetOf("x(y|z)", false, "xyy", true, false).isTrue();
    assertSupersetOf("x(y|z)", false, "xzy", true, false).isTrue();
    assertSupersetOf("x(y|z)", false, "xxy", true, false).isFalse();

    assertSupersetOf("x(y*)z", false, "x", true, false).isFalse();
    assertSupersetOf("x(y*)z", false, "xz", true, false).isTrue();
    assertSupersetOf("x(y*)z", false, "xzx", true, false).isTrue();
    assertSupersetOf("x(y*)z", false, "xyzx", true, false).isTrue();
    assertSupersetOf("x(y*)z", false, "xyyyyzx", true, false).isTrue();
    assertSupersetOf("x(y*)z", false, "xyyyyxx", true, false).isFalse();

    // xy is dead code
    assertSupersetOf("xy", false, "(xy|ab)", true, false).isFalse();
    assertSupersetOf("xy|ab", false, "(xy|ab)", true, false).isTrue();
    assertSupersetOf("xy|a", false, "(xy|ab)", true, false).isTrue();
  }

  @Test
  void intersects_and_superset_of_complex_expression() {
    String shortEmailRegex = "[a-zA-Z0-9_.+-]+@(?:[a-zA-Z0-9-]+\\.)*[a-zA-Z0-9-]+";
    String longEmailSample = "donaudampfschiffahrtsgesellschaftskapitan@armeeserver\\.de";

    assertIntersects(shortEmailRegex, longEmailSample, false).isTrue();
    assertSupersetOf(shortEmailRegex, longEmailSample, false).isTrue();
    assertSupersetOf(shortEmailRegex, "paul@mail\\.server\\.com", false).isTrue();
    assertSupersetOf(shortEmailRegex, "paul@mail\\.\\.com", false).isFalse();
    assertSupersetOf(shortEmailRegex, "paul@mail\\.com", false).isTrue();
    assertSupersetOf(shortEmailRegex, "paul@mail\\.", false).isFalse();
    assertSupersetOf(shortEmailRegex, "paul@server", false).isTrue();
    assertSupersetOf(shortEmailRegex, "@server", false).isFalse();
    assertSupersetOf(shortEmailRegex, "paul", false).isFalse();

    // from SonarQube EmailValidator Pattern VALID_EMAIL_ADDRESS_REGEX
    String longEmailRegex = "(?:(?:\\r\\n)?[ \\t])*(?:(?:(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+" +
      "|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*)" +
      "(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@," +
      ";:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*))*@(?:(?:\\r\\n)?[ \\" +
      "t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([" +
      "^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\0" +
      "31]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?" +
      "[ \\t])*))*|(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]" +
      "))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*)*\\<(?:(?:\\r\\n)?[ \\t])*(?:@(?:[^(" +
      ")<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r" +
      "\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:" +
      "(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))" +
      "*(?:,@(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@," +
      ";:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;" +
      ":\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|" +
      "\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*)*:(?:(?:\\r\\n)?[ \\t])*)?(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\" +
      "r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r" +
      "\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(" +
      "?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*))*@(?:" +
      "(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\"." +
      "\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\" +
      "[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\]" +
      "(?:(?:\\r\\n)?[ \\t])*))*\\>(?:(?:\\r\\n)?[ \\t])*)|(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\" +
      "t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t" +
      "])*)*:(?:(?:\\r\\n)?[ \\t])*(?:(?:(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"" +
      "()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\" +
      "r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\" +
      "]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*))*@(?:(?:\\r\\n)?[ \\t])*(?:[^()<>" +
      "@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\" +
      "]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\" +
      "r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*|(?:" +
      "[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\" +
      "r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*)*\\<(?:(?:\\r\\n)?[ \\t])*(?:@(?:[^()<>@,;:\\\\\".\\" +
      "[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\]" +
      "(?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t]" +
      ")+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*(?:,@(?:(?:\\r\\" +
      "n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]])" +
      ")|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\" +
      "000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\" +
      "r\\n)?[ \\t])*))*)*:(?:(?:\\r\\n)?[ \\t])*)?(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z" +
      "|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*)(?:\\" +
      ".(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\" +
      "\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*))*@(?:(?:\\r\\n)?[ \\t])*(" +
      "?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\" +
      "]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?" +
      ":(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t]" +
      ")*))*\\>(?:(?:\\r\\n)?[ \\t])*)(?:,\\s*(?:(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(" +
      "?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*)(?:\\." +
      "(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\" +
      "\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*))*@(?:(?:\\r\\n)?[ \\t])*(" +
      "?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\" +
      "]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?" +
      ":(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t]" +
      ")*))*|(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(" +
      "?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*)*\\<(?:(?:\\r\\n)?[ \\t])*(?:@(?:[^()<>@,;" +
      ":\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|" +
      "\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r" +
      "\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*(?:,@" +
      "(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\"" +
      ".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\"." +
      "\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\" +
      "](?:(?:\\r\\n)?[ \\t])*))*)*:(?:(?:\\r\\n)?[ \\t])*)?(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\" +
      "t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t" +
      "])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()" +
      "<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*))*@(?:(?:\\r\\n)" +
      "?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|" +
      "\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000" +
      "-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\" +
      "n)?[ \\t])*))*\\>(?:(?:\\r\\n)?[ \\t])*))*)?;\\s*)";

    assertIntersects(longEmailRegex, longEmailRegex, false).isTrue();
    assertIntersects(longEmailRegex, longEmailRegex, true).isTrue();

    // All the following assertions should match only "isTrue()" once we support all types of transition
    assertIntersects(longEmailRegex, longEmailSample, false).isFalse();
    assertIntersects(longEmailRegex, longEmailSample, true).isTrue();

    assertSupersetOf(longEmailRegex, longEmailRegex, false).isFalse();
    assertSupersetOf(longEmailRegex, longEmailRegex, true).isTrue();

    assertSupersetOf(longEmailRegex, longEmailSample, false).isFalse();
    assertSupersetOf(longEmailRegex, longEmailSample, true).isTrue();
  }

  private static AbstractBooleanAssert<?> assertIntersects(String set1, String set2, boolean defaultAnswer) {
    return assertIntersects(set1, false, set2, false, defaultAnswer, 0);
  }

  private static AbstractBooleanAssert<?> assertIntersects(String set1, String set2, boolean defaultAnswer, int flags) {
    return assertIntersects(set1, false, set2, false, defaultAnswer, flags);
  }

  private static AbstractBooleanAssert<?> assertIntersects(
    String set1, boolean set1AllowPrefix,
    String set2, boolean set2AllowPrefix,
    boolean defaultAnswer) {
    return assertIntersects(set1, set1AllowPrefix, set2, set2AllowPrefix, defaultAnswer, 0);
  }

  private static AbstractBooleanAssert<?> assertIntersects(
    String set1, boolean set1AllowPrefix,
    String set2, boolean set2AllowPrefix,
    boolean defaultAnswer, int flags) {
    SubAutomaton sub1 = parseSubAutomaton(set1, set1AllowPrefix, flags);
    SubAutomaton sub2 = parseSubAutomaton(set2, set2AllowPrefix, flags);
    return assertThat(RegexTreeHelper.intersects(sub1, sub2, defaultAnswer));
  }

  private static AbstractBooleanAssert<?> assertSupersetOf(String superset, String subset, boolean defaultAnswer) {
    return assertSupersetOf(superset, false, subset, false, defaultAnswer, 0);
  }

  private static AbstractBooleanAssert<?> assertSupersetOf(String superset, String subset, boolean defaultAnswer, int flags) {
    return assertSupersetOf(superset, false, subset, false, defaultAnswer, flags);
  }

  private static AbstractBooleanAssert<?> assertSupersetOf(
    String superset, boolean supersetAllowPrefix,
    String subset, boolean subsetAllowPrefix,
    boolean defaultAnswer) {
    return assertSupersetOf(superset, supersetAllowPrefix, subset, subsetAllowPrefix, defaultAnswer, 0);
  }

  private static AbstractBooleanAssert<?> assertSupersetOf(
    String superset, boolean supersetAllowPrefix,
    String subset, boolean subsetAllowPrefix,
    boolean defaultAnswer, int flags) {
    SubAutomaton supersetSub = parseSubAutomaton(superset, supersetAllowPrefix, flags);
    SubAutomaton subsetSub = parseSubAutomaton(subset, subsetAllowPrefix, flags);
    return assertThat(RegexTreeHelper.supersetOf(supersetSub, subsetSub, defaultAnswer));
  }

  static SubAutomaton parseSubAutomaton(String stringLiteral, boolean allowPrefix, int flags) {
    FlagSet flagSet = new FlagSet(flags);
    RegexParseResult result = parseRegex(stringLiteral, flagSet);
    return new SubAutomaton(result.getStartState(), result.getFinalState(), allowPrefix);
  }

}
