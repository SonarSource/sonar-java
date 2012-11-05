/*
 * Sonar Java
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.java.checks;

import com.sonar.sslr.squid.checks.CheckMessagesVerifierRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.java.JavaAstScanner;
import org.sonar.squid.api.SourceFile;

import java.io.File;

import static org.fest.assertions.Assertions.assertThat;

public class SnippetCheckTest {

  @Rule
  public CheckMessagesVerifierRule checkMessagesVerifier = new CheckMessagesVerifierRule();

  @Test
  public void empty() {
    SnippetCheck check = new SnippetCheck();
    assertThat(check.dontExample1).isEqualTo("");
    assertThat(check.dontExample2).isEqualTo("");
    assertThat(check.doExample1).isEqualTo("");

    SourceFile file = JavaAstScanner.scanSingleFile(new File("src/test/files/checks/snippet/Empty.java"), check);
    checkMessagesVerifier.verify(file.getCheckMessages());
  }

  @Test
  public void constant() {
    SnippetCheck check = new SnippetCheck();
    check.dontExample1 = "1l";
    check.doExample1 = "1L";

    SourceFile file = JavaAstScanner.scanSingleFile(new File("src/test/files/checks/snippet/Constant.java"), check);
    checkMessagesVerifier.verify(file.getCheckMessages())
        .next().atLine(3).withMessage("This should be rewritten as: 1L");
  }

  @Test
  public void value_identical() {
    SnippetCheck check = new SnippetCheck();
    check.dontExample1 = "assertThat(value).isEqualTo(true);";
    check.doExample1 = "assertThat(value).isTrue();";

    SourceFile file = JavaAstScanner.scanSingleFile(new File("src/test/files/checks/snippet/ValueIdentical.java"), check);
    checkMessagesVerifier.verify(file.getCheckMessages())
        .next().atLine(9);
  }

  @Test
  public void value_replaced() {
    SnippetCheck check = new SnippetCheck();
    check.dontExample1 = "assertThat(value).isEqualTo(true);";
    check.doExample1 = "assertThat(value).isTrue();";
    check.dontExample2 = "assertThat(otherValue).isEqualTo(true);";

    SourceFile file = JavaAstScanner.scanSingleFile(new File("src/test/files/checks/snippet/ValueReplaced.java"), check);
    checkMessagesVerifier.verify(file.getCheckMessages())
        .next().atLine(9);
  }

  @Test
  public void value_qualified_replacement() {
    SnippetCheck check = new SnippetCheck();
    check.dontExample1 = "assertThat(value).isEqualTo(true);";
    check.doExample1 = "assertThat(value).isTrue();";
    check.dontExample2 = "assertThat(foo.bar).isEqualTo(true);";

    SourceFile file = JavaAstScanner.scanSingleFile(new File("src/test/files/checks/snippet/ValueQualifiedReplacement.java"), check);
    checkMessagesVerifier.verify(file.getCheckMessages())
        .next().atLine(9);
  }

  @Test
  public void value_method_call() {
    SnippetCheck check = new SnippetCheck();
    check.dontExample1 = "assertThat(value()).isEqualTo(true);";
    check.doExample1 = "assertThat(value).isTrue();";
    check.dontExample2 = "assertThat(hehe).isEqualTo(true);";

    SourceFile file = JavaAstScanner.scanSingleFile(new File("src/test/files/checks/snippet/ValueMethodCall.java"), check);
    checkMessagesVerifier.verify(file.getCheckMessages())
        .next().atLine(7);
  }

}
