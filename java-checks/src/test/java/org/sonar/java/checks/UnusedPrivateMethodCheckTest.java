/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
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

import org.junit.Test;
import org.sonar.api.resources.Resource;
import org.sonar.java.AnalyzerMessage;
import org.sonar.java.JavaConfiguration;
import org.sonar.java.JavaSquid;
import org.sonar.java.bytecode.visitor.DefaultBytecodeContext;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.plugins.java.api.JavaResourceLocator;
import org.sonar.squidbridge.api.CodeVisitor;
import org.sonar.squidbridge.api.SourceCode;
import org.sonar.squidbridge.api.SourceFile;
import org.sonar.squidbridge.indexer.QueryByType;

import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class UnusedPrivateMethodCheckTest {

  private final UnusedPrivateMethodCheck check = new UnusedPrivateMethodCheck();

  public static List<AnalyzerMessage> scan(CodeVisitor visitor) {
    File baseDir = new File("src/test/resources/");
    File bytecodeFile = new File("target/test-classes/");

    final File file = new File(baseDir, "Lambdas.java");
    final JavaResourceLocator javaResourceLocator = mock(JavaResourceLocator.class);
    final List<AnalyzerMessage> analyzerMessages = new ArrayList<>();
    JavaSquid javaSquid = new JavaSquid(
      new JavaConfiguration(Charset.forName("UTF-8")), null, null, javaResourceLocator, new DefaultBytecodeContext(javaResourceLocator) {
      @Override
      public void reportIssue(JavaCheck check, Resource resource, String message, int line) {
        analyzerMessages.add(new AnalyzerMessage(check, file, line, message, 0));
      }
    }, visitor);
    javaSquid.scan(Collections.singleton(file), Collections.<File>emptyList(), Collections.singleton(bytecodeFile));

    Collection<SourceCode> sources = javaSquid.getIndex().search(new QueryByType(SourceFile.class));
    if (sources.size() != 1) {
      throw new IllegalStateException("Only one SourceFile was expected whereas " + sources.size() + " has been returned.");
    }
    return analyzerMessages;
  }

  @Test
  public void test() {
    List<AnalyzerMessage> messages = BytecodeFixture.scan("UnusedPrivateMethod", check);
    assertThat(messages).hasSize(4);
    for (AnalyzerMessage message : messages) {
      Integer line = message.getLine();
      if (line == null) {
        checkMessage(message, "Private constructor 'org.sonar.java.checks.targets.UnusedPrivateMethod$A(UnusedPrivateMethod)' is never used.");
        continue;
      }
      switch (line) {
        case 54:
          checkMessage(message, "Private method 'unusedPrivateMethod' is never used.");
          break;
        case 57:
          checkMessage(message, "Private method 'unusedPrivateMethod' is never used.");
          break;
        case 67:
          checkMessage(message, "Private constructor 'org.sonar.java.checks.targets.UnusedPrivateMethod$Attribute(String,String[],int)' is never used.");
          break;
        default:
          throw new IllegalStateException("Unexpected " + line);
      }
    }
  }

  private static void checkMessage(AnalyzerMessage analyzerMessage, String message) {
    assertThat(analyzerMessage.getMessage()).as("on: " + analyzerMessage).isEqualTo(message);
  }

  @Test
  public void lambdas_should_not_raise_issue() throws Exception {
    List<AnalyzerMessage> issues = scan(check);
    assertThat(issues).isEmpty();
  }

}
