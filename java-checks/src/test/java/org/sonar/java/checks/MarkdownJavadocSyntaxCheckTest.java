/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
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
package org.sonar.java.checks;

import java.util.regex.Matcher;
import org.junit.jupiter.api.Test;
import org.sonar.java.checks.verifier.CheckVerifier;
import org.sonar.java.checks.verifier.TestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.java.checks.MarkdownJavadocSyntaxCheck.endIndexOfTag;

class MarkdownJavadocSyntaxCheckTest {

  @Test
  void test() {
    CheckVerifier.newVerifier()
      .onFile(TestUtils.mainCodeSourcesPath("checks/MarkdownJavadocSyntaxCheckSample.java"))
      .withCheck(new MarkdownJavadocSyntaxCheck())
      .verifyIssues();
  }

  @Test
  void testPattern() {
    String input = "<p>foo<b>bar</b>";
    Matcher matcher = MarkdownJavadocSyntaxCheck.NON_MARKDOWN_JAVADOC_PATTERN.matcher(input);
    matcher.region(3, input.length());

    assertThat(matcher.find()).isTrue();
    assertThat(matcher.group(0)).isEqualTo("<b>");
  }

  @Test
  void replaceQuotedCodeWithBlanks() {
    String javadoc = "foo`<b>`bar`<i>`baz```\n<ul>\n<li>\n```<pre>\n\n`a`quz";
    String withoutQuotedCode = MarkdownJavadocSyntaxCheck.replaceQuotedCodeWithBlanks(javadoc);
    assertThat(withoutQuotedCode).isEqualTo("foo     bar     baz   \n    \n    \n   <pre>\n\n   quz");
  }

  @Test
  void replaceQuotedCodeWithBlanks_unclosedTag() {
    String javadoc = "foo``` ";
    String nonQuotedCode = MarkdownJavadocSyntaxCheck.replaceQuotedCodeWithBlanks(javadoc);
    assertThat(nonQuotedCode).isEqualTo("foo    ");
  }

  @Test
  void removeEscapedCode_noNonQuoted() {
    String javadoc = "`<b>`";
    String withoutQuotedCode = MarkdownJavadocSyntaxCheck.replaceQuotedCodeWithBlanks(javadoc);
    assertThat(withoutQuotedCode).isEqualTo("     ");
  }

  @Test
  void findEndOfMarkdowQuote() {
    String javadoc = "```int f(){return `0`;}```";
    int end = MarkdownJavadocSyntaxCheck.findEndOfMarkdownQuote(javadoc, 0);
    assertThat(end).isEqualTo(javadoc.length());
  }

  @Test
  void findEndOfMarkdowQuote_emptyCode() {
    String javadoc = "  ``````  ";
    int end = MarkdownJavadocSyntaxCheck.findEndOfMarkdownQuote(javadoc, 2);
    assertThat(end).isEqualTo(8);
  }

  @Test
  void findEndOfMarkdowQuote_singleQuote() {
    String javadoc = "`foo`  ";
    int end = MarkdownJavadocSyntaxCheck.findEndOfMarkdownQuote(javadoc, 0);
    assertThat(end).isEqualTo(5);
  }

  @Test
  void findEndOfMarkdowQuote_unclosed() {
    String javadoc = "```foo`  ";
    int end = MarkdownJavadocSyntaxCheck.findEndOfMarkdownQuote(javadoc, 0);
    assertThat(end).isEqualTo(-1);
  }

  @Test
  void findEndOfMarkdowQuote_unclosedSingleQuote() {
    String javadoc = "foo`bar  ";
    int end = MarkdownJavadocSyntaxCheck.findEndOfMarkdownQuote(javadoc, 3);
    assertThat(end).isEqualTo(-1);
  }

  @Test
  void testEndIndexOfTag() {
    String javadoc = "<p>bar";
    Matcher matcher = MarkdownJavadocSyntaxCheck.NON_MARKDOWN_JAVADOC_PATTERN.matcher(javadoc);
    assertThat(matcher.find()).isTrue();
    int index = endIndexOfTag(matcher, javadoc);
    assertThat(index).isEqualTo(3);
  }

  @Test
  void testEndIndexOfTag_atCode() {
    String javadoc = "{@code {} {}}";
    Matcher matcher = MarkdownJavadocSyntaxCheck.NON_MARKDOWN_JAVADOC_PATTERN.matcher(javadoc);
    assertThat(matcher.find()).isTrue();
    int index = endIndexOfTag(matcher, javadoc);
    assertThat(index).isEqualTo(javadoc.length());
  }

  @Test
  void testEndIndexOfTag_unclosedTag() {
    String javadoc = "{@code {} {}";
    Matcher matcher = MarkdownJavadocSyntaxCheck.NON_MARKDOWN_JAVADOC_PATTERN.matcher(javadoc);
    assertThat(matcher.find()).isTrue();
    int index = endIndexOfTag(matcher, javadoc);
    assertThat(index).isEqualTo(javadoc.length());
  }
}
