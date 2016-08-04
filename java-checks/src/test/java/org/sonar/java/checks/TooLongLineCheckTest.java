/*
 * SonarQube Java
 * Copyright (C) 2012-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.java.checks;

import org.junit.Test;
import org.sonar.java.checks.verifier.JavaCheckVerifier;

public class TooLongLineCheckTest {

  private static final String BASEDIR = "src/test/files/checks/TooLongLine_S00103_Check";

  TooLongLineCheck check = new TooLongLineCheck();

  @Test
  public void test() {
    check.maximumLineLength = 20;
    JavaCheckVerifier.verify(BASEDIR + "/LineLength.java", check);
  }

  @Test
  public void test_with_empty_import_on_first_line() {
    check.maximumLineLength = 20;
    JavaCheckVerifier.verify(BASEDIR + "/LineLengthEmptyStatementInImport.java", check);
  }

  @Test
  public void test_with_no_import() {
    check.maximumLineLength = 20;
    JavaCheckVerifier.verify(BASEDIR + "/LineLengthNoImport.java", check);
  }

  @Test
  public void test_link_within_block_comment_noncompliant() {
    check.maximumLineLength = 100;
    JavaCheckVerifier.verify(BASEDIR + "/LineLengthLinkWithinBlockCommentNoncompliant.java", check);
  }

  @Test
  public void test_link_within_block_comment() {
    check.maximumLineLength = 20;
    JavaCheckVerifier.verifyNoIssue(BASEDIR + "/LineLengthLinkWithinBlockComment.java", check);
  }

  @Test
  public void test_link_within_inline_comment_noncompliant() {
    check.maximumLineLength = 100;
    JavaCheckVerifier.verify(BASEDIR + "/LineLengthLinkWithinInlineCommentNoncompliant.java", check);
  }

  @Test
  public void test_link_within_inline_comment() {
    check.maximumLineLength = 20;
    JavaCheckVerifier.verifyNoIssue(BASEDIR + "/LineLengthLinkWithinInlineComment.java", check);
  }

  @Test
  public void test_see_within_block_comment_noncompliant() {
    check.maximumLineLength = 100;
    JavaCheckVerifier.verify(BASEDIR + "/LineLengthSeeWithinBlockCommentNoncompliant.java", check);
  }

  @Test
  public void test_see_within_block_comment() {
    check.maximumLineLength = 20;
    JavaCheckVerifier.verifyNoIssue(BASEDIR + "/LineLengthSeeWithinBlockComment.java", check);
  }

  @Test
  public void test_see_within_inline_comment_noncompliant() {
    check.maximumLineLength = 100;
    JavaCheckVerifier.verify(BASEDIR + "/LineLengthSeeWithinInlineCommentNoncompliant.java", check);
  }

  @Test
  public void test_see_within_inline_comment() {
    check.maximumLineLength = 20;
    JavaCheckVerifier.verifyNoIssue(BASEDIR + "/LineLengthSeeWithinInlineComment.java", check);
  }
}
