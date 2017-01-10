/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
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
package org.sonar.java.checks;

import org.junit.Test;
import org.sonar.java.AnalyzerMessage;
import org.sonar.java.ast.JavaAstScanner;
import org.sonar.java.model.VisitorsBridgeForTests;

import java.io.File;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class ParsingErrorCheckTest {

  @Test
  public void test() {
    VisitorsBridgeForTests visitorsBridge = new VisitorsBridgeForTests(new ParsingErrorCheck());
    JavaAstScanner.scanSingleFileForTests(new File("src/test/files/checks/ParsingError.java"), visitorsBridge);
    Set<AnalyzerMessage> issues = visitorsBridge.lastCreatedTestContext().getIssues();
    assertThat(issues).hasSize(1);
    AnalyzerMessage issue = issues.iterator().next();
    assertThat(issue.getLine()).isEqualTo(1);
    assertThat(issue.getMessage()).isEqualTo("Parse error");
  }

}
