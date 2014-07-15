/*
 * SonarQube Java
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

import org.sonar.squidbridge.checks.CheckMessagesVerifier;
import org.junit.Test;
import org.sonar.api.resources.InputFile;
import org.sonar.api.resources.InputFileUtils;
import org.sonar.java.JavaConfiguration;
import org.sonar.java.JavaSquid;
import org.sonar.squidbridge.api.CodeVisitor;
import org.sonar.squidbridge.api.SourceCode;
import org.sonar.squidbridge.api.SourceFile;
import org.sonar.squidbridge.indexer.QueryByType;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Collections;

import static org.fest.assertions.Assertions.assertThat;

public class UnusedPrivateMethodCheckTest {

  private final UnusedPrivateMethodCheck check = new UnusedPrivateMethodCheck();

  @Test
  public void test() {
    SourceFile file = BytecodeFixture.scan("UnusedPrivateMethod", check);
    CheckMessagesVerifier.verify(file.getCheckMessages())
      .next().withMessage("Private method 'unusedPrivateMethod(...)' is never used.") // TODO verify line?
      .noMore();
  }


  @Test
  public void lambdas_should_not_raise_issue() throws Exception {
    SourceFile file = scan(check);
    CheckMessagesVerifier.verify(file.getCheckMessages())
        .noMore();
  }

  public static SourceFile scan(CodeVisitor visitor) {
    File baseDir = new File("src/test/resources/");
    InputFile sourceFile = InputFileUtils.create(baseDir, new File(baseDir, "Lambdas.java"));
    File bytecodeFile = new File("target/test-classes/");

    if (!sourceFile.getFile().isFile()) {
      throw new IllegalArgumentException("File '" + sourceFile + "' not found.");
    }

    JavaSquid javaSquid = new JavaSquid(new JavaConfiguration(Charset.forName("UTF-8")), visitor);
    javaSquid.scan(Collections.singleton(sourceFile), Collections.<InputFile>emptyList(), Collections.singleton(bytecodeFile));

    Collection<SourceCode> sources = javaSquid.getIndex().search(new QueryByType(SourceFile.class));
    if (sources.size() != 1) {
      throw new IllegalStateException("Only one SourceFile was expected whereas " + sources.size() + " has been returned.");
    }
    return (SourceFile) sources.iterator().next();
  }

  @Test
  public void test_toString() {
    assertThat(check.toString()).isEqualTo("UnusedPrivateMethod rule");
  }

}
