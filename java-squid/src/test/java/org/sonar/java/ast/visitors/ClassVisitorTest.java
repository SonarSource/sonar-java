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

import org.junit.Test;
import org.sonar.java.JavaAstScanner;
import org.sonar.squidbridge.api.SourceClass;
import org.sonar.squidbridge.api.SourceCode;
import org.sonar.squidbridge.api.SourceFile;
import org.sonar.squidbridge.api.SourceMethod;

import java.io.File;

import static org.fest.assertions.Assertions.assertThat;

public class ClassVisitorTest {

  @Test
  public void resolve_class_names() throws Exception {
    //class visitor is called by JavaAstScanner, so call with no visitor
    SourceFile file = JavaAstScanner.scanSingleFile(new File("src/test/files/metrics/ClassNames.java"));
    assertThat(file.getChildren()).hasSize(1);
    SourceClass enclosing = (SourceClass) file.getChildren().iterator().next();
    assertThat(enclosing.getName()).isEqualTo("A");
    SourceClass BClass = null;
    SourceMethod sourceMethod = null;
    for(SourceCode sourceCode : enclosing.getChildren()) {
      if(sourceCode.isType(SourceClass.class)) {
        BClass = (SourceClass) sourceCode;
      } else {
        sourceMethod = (SourceMethod) sourceCode;
      }
    }
    assertThat(BClass.getKey()).isEqualTo("A$B");
    assertThat(sourceMethod.getChildren()).hasSize(1);
    assertThat(sourceMethod.getChildren().iterator().next().getKey()).isEqualTo("A$2local");
    assertThat(BClass.getChildren().iterator().next().getChildren().iterator().next().getKey()).isEqualTo("A$B$1local");

  }
}
