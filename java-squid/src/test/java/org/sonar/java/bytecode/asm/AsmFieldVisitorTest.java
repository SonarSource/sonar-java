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
package org.sonar.java.bytecode.asm;

import org.junit.Test;
import org.sonar.java.bytecode.ClassLoaderBuilder;

import java.io.File;

import static org.fest.assertions.Assertions.assertThat;

public class AsmFieldVisitorTest {

  @Test
  public void testVisitStringField() {
    AsmClassProviderImpl classProvider = new AsmClassProviderImpl(ClassLoaderBuilder.create(new File("src/test/files/bytecode/bin/")));
    AsmClass fileClass = classProvider.getClass("tags/SourceFile");
    assertThat(fileClass.getFields().size()).isEqualTo(5);
    AsmField field = fileClass.getField("path");
    assertThat(field.isStatic()).isTrue();
    assertThat(field.isPublic()).isTrue();
  }

}
