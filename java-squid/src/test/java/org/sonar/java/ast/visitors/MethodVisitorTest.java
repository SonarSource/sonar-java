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
package org.sonar.java.ast.visitors;

import com.google.common.collect.Lists;
import org.junit.Test;
import org.sonar.java.JavaAstScanner;
import org.sonar.squidbridge.api.SourceCode;
import org.sonar.squidbridge.api.SourceMethod;

import java.io.File;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

public class MethodVisitorTest {

  @Test
  public void signatures() {
    List<SourceMethod> methods = sourceMethods(JavaAstScanner.scanSingleFile(new File("src/test/files/signatures/Signatures.java"), new MethodVisitor()));
    assertThat(methods.get(0).getName()).isEqualTo("drainTasksTo(LCollection;)I");
    assertThat(methods.get(1).getName()).isEqualTo("getQueuedThreads()LCollection;");
    assertThat(methods.get(2).getName()).isEqualTo("getRole(LString;)LList;");
    assertThat(methods.get(3).getName()).isEqualTo("instantiate(LString;LObjectName;[LObject;[LString;)LObject;");
    assertThat(methods.get(4).getName()).isEqualTo("invoke(LObject;LObject;)LObject;");
  }

  private List<SourceMethod> sourceMethods(SourceCode sourceCode) {
    List<SourceMethod> result = Lists.newArrayList();
    sourceMethods(result, sourceCode);
    return result;
  }

  private void sourceMethods(List<SourceMethod> result, SourceCode sourceCode) {
    for (SourceCode child : sourceCode.getChildren()) {
      if (child instanceof SourceMethod) {
        result.add((SourceMethod) child);
      } else {
        sourceMethods(result, child);
      }
    }
  }

}
