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
package org.sonar.java;

import org.junit.Test;
import org.sonar.plugins.java.api.JavaCheck;

import java.io.File;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class AnalyzerMessageTest {

  @Test
  public void testAnalyzerMessage() {
    JavaCheck javaCheck = mock(JavaCheck.class);
    File file = new File("a");
    int line = 5;
    String message = "analyzer message";
    int cost = 3;
    AnalyzerMessage analyzerMessage = new AnalyzerMessage(javaCheck, file, line, message, cost);
    assertThat(analyzerMessage.getCheck()).isEqualTo(javaCheck);
    assertThat(analyzerMessage.getFile()).isEqualTo(file);
    assertThat(analyzerMessage.getLine()).isEqualTo(line);
    assertThat(analyzerMessage.getMessage()).isEqualTo(message);
    assertThat(analyzerMessage.getCost()).isEqualTo(cost);

    AnalyzerMessage.TextSpan location = analyzerMessage.primaryLocation();
    assertThat(location.startLine).isEqualTo(line);
    assertThat(location.startCharacter).isEqualTo(-1);
    assertThat(location.endLine).isEqualTo(line);
    assertThat(location.endCharacter).isEqualTo(-1);
    assertThat(location.toString()).isEqualTo("(5:-1)-(5:-1)");
  }

  @Test
  public void testAnalyzerMessageOnFile2() {
    JavaCheck javaCheck = mock(JavaCheck.class);
    File file = new File("a");
    String message = "analyzer message";
    int cost = 3;
    AnalyzerMessage analyzerMessage = new AnalyzerMessage(javaCheck, file, -5, message, cost);
    assertThat(analyzerMessage.getCheck()).isEqualTo(javaCheck);
    assertThat(analyzerMessage.getFile()).isEqualTo(file);
    assertThat(analyzerMessage.getLine()).isEqualTo(null);
    assertThat(analyzerMessage.getMessage()).isEqualTo(message);
    assertThat(analyzerMessage.getCost()).isEqualTo(cost);
    assertThat(analyzerMessage.primaryLocation()).isNull();
  }

  @Test
   public void testAnalyzerMessageOnFile() {
    JavaCheck javaCheck = mock(JavaCheck.class);
    File file = new File("a");
    String message = "analyzer message";
    int cost = 3;
    AnalyzerMessage analyzerMessage = new AnalyzerMessage(javaCheck, file, null, message, cost);
    assertThat(analyzerMessage.getCheck()).isEqualTo(javaCheck);
    assertThat(analyzerMessage.getFile()).isEqualTo(file);
    assertThat(analyzerMessage.getLine()).isEqualTo(null);
    assertThat(analyzerMessage.getMessage()).isEqualTo(message);
    assertThat(analyzerMessage.getCost()).isEqualTo(cost);
    assertThat(analyzerMessage.primaryLocation()).isNull();
  }

  @Test
  public void testAnalyzerMessageWithoutCost() {
    JavaCheck javaCheck = mock(JavaCheck.class);
    File file = new File("a");
    String message = "analyzer message";
    int cost = 0;
    AnalyzerMessage analyzerMessage = new AnalyzerMessage(javaCheck, file, null, message, cost);
    assertThat(analyzerMessage.getCheck()).isEqualTo(javaCheck);
    assertThat(analyzerMessage.getFile()).isEqualTo(file);
    assertThat(analyzerMessage.getLine()).isEqualTo(null);
    assertThat(analyzerMessage.getMessage()).isEqualTo(message);
    assertThat(analyzerMessage.getCost()).isNull();
    assertThat(analyzerMessage.primaryLocation()).isNull();
  }
}
