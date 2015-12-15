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
import org.sonar.java.AnalyzerMessage;

import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

public class UnusedPrivateMethodCheckTest {

  private final UnusedPrivateMethodCheck check = new UnusedPrivateMethodCheck();

  @Test
  public void test() {
    List<AnalyzerMessage> messages = BytecodeFixture.scan("src/test/java/org/sonar/java/checks/targets/UnusedPrivateMethod.java", check);
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
    List<AnalyzerMessage> issues = BytecodeFixture.scan("src/test/resources/Lambdas.java", check);
    assertThat(issues).isEmpty();
  }

}
