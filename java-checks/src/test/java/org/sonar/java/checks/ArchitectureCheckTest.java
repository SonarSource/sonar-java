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
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.sonar.java.bytecode.asm.AsmClass;
import org.sonar.java.bytecode.asm.AsmEdge;
import org.sonar.plugins.java.api.JavaResourceLocator;
import org.sonar.squidbridge.api.CheckMessage;
import org.sonar.squidbridge.api.SourceFile;
import org.sonar.squidbridge.checks.CheckMessagesVerifier;
import org.sonar.squidbridge.indexer.SquidIndex;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ArchitectureCheckTest {

  private final ArchitectureCheck check = new ArchitectureCheck();

  @Test
  public void test() {
    check.setToClasses("java.**.Pattern");
    SourceFile file = BytecodeFixture.scan("ArchitectureConstraint", check);
    CheckMessagesVerifier.verify(file.getCheckMessages())
      .next().atLine(27).withMessage("org/sonar/java/checks/targets/ArchitectureConstraint must not use java/util/regex/Pattern")
      .noMore();
  }

  @Test
  public void test2() {
    check.setFromClasses("com.**");
    check.setToClasses("java.**.Pattern");
    SourceFile file = BytecodeFixture.scan("ArchitectureConstraint", check);
    CheckMessagesVerifier.verify(file.getCheckMessages())
      .noMore();
  }

  @Test
  public void test_source_line_is_null_if_not_available () throws Exception {
    ArchitectureCheck archCheck = new ArchitectureCheck();
    archCheck.setFromClasses("**");
    archCheck.setToClasses("**");
    JavaResourceLocator jrl = mock(JavaResourceLocator.class);
    when(jrl.findSourceFileKeyByClassName(anyString())).thenReturn("key");
    archCheck.setJavaResourceLocator(jrl);

    SquidIndex squidIndex = mock(SquidIndex.class);
    SourceFile sourceFile = mock(SourceFile.class);
    when(squidIndex.search("key")).thenReturn(sourceFile);

    archCheck.setSquidIndex(squidIndex);
    AsmEdge asmEdge = Mockito.mock(AsmEdge.class);
    when(asmEdge.getSourceLineNumber()).thenReturn(0);
    when(asmEdge.getTargetAsmClass()).thenReturn(new AsmClass("bannedClass"));

    AsmClass myClass = new AsmClass("myClass");
    archCheck.visitClass(myClass);
    archCheck.visitEdge(asmEdge);
    archCheck.leaveClass(myClass);

    ArgumentCaptor<CheckMessage> messageCaptor = ArgumentCaptor.forClass(CheckMessage.class);
    verify(sourceFile, times(1)).log(messageCaptor.capture());

    CheckMessage value = messageCaptor.getValue();
    assertThat(value.getLine()).isNull();

  }

}
