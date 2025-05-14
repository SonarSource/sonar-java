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
package org.sonar.java.checks;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.sonar.java.checks.verifier.CheckVerifier;
import org.sonar.java.checks.verifier.TestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class MarkdownJavadocSyntaxCheckTest {

  @Test
  void test() {
    CheckVerifier.newVerifier()
      .onFile(TestUtils.mainCodeSourcesPath("checks/MarkdownJavadocSyntaxCheckSample.java"))
      .withCheck(new MarkdownJavadocSyntaxCheck())
      .verifyIssues();
  }

  @Test
  void removeQuotedCode() {
    String javadoc = "foo`<b>`bar`<i>`baz```\n<ul>\n<li>\n```<pre>\n\n`a`quz";
    List<String> strings = MarkdownJavadocSyntaxCheck.removeQuotedCode(javadoc);
    assertThat(strings).containsExactly("foo", "bar", "baz", "<pre>\n\n", "quz");
  }

  @Test
  void removeQuotedCode_unclosedTag() {
    String javadoc = "foo``` ";
    List<String> strings = MarkdownJavadocSyntaxCheck.removeQuotedCode(javadoc);
    assertThat(strings).containsExactly("foo");
  }

  /**
   * Checking that in the case of a String containing only a quoted bit of code, the result of {@link MarkdownJavadocSyntaxCheck#removeQuotedCode(String)}
   * contains two empty strings: one for the part before and one for the part after the quoted part.
   */
  @Test
  void removeEscapedCode_noNonQuoted() {
    String javadoc = "`<b>`";
    List<String> strings = MarkdownJavadocSyntaxCheck.removeQuotedCode(javadoc);
    assertThat(strings).containsExactly("", "");
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
}
