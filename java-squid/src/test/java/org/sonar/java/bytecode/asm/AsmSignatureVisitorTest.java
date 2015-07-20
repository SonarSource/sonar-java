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

import org.junit.Before;
import org.junit.Test;
import org.objectweb.asm.signature.SignatureReader;

import static org.fest.assertions.Assertions.assertThat;

public class AsmSignatureVisitorTest {

  private AsmSignatureVisitor visitor;

  @Before
  public void init() {
    visitor = new AsmSignatureVisitor();
  }

  @Test
  public void analyzeFieldSignatureWithGenerics() {
    String signature = "Ljava/util/List<-Ljava/lang/Integer;>;";
    new SignatureReader(signature).accept(visitor);
    assertThat(visitor.getInternalNames()).containsOnly("java/util/List", "java/lang/Integer");
  }

  @Test
  public void analyseFieldSignatureWithoutGenerics() {
    String signature = "Ljava/util/List;";
    new SignatureReader(signature).accept(visitor);
    assertThat(visitor.getInternalNames()).containsOnly("java/util/List");
  }

  @Test
  public void analyseFieldSignatureWithoutObjects() {
    String signature = "I";
    new SignatureReader(signature).accept(visitor);
    assertThat(visitor.getInternalNames()).hasSize(0);
  }

  @Test
  public void analyzeMethodSignatureWithGenerics() {
    String signature = "(Ljava/util/List<-Ljava/lang/Integer;>;)Ljava/lang/Number;";
    new SignatureReader(signature).accept(visitor);
    assertThat(visitor.getInternalNames()).containsOnly("java/util/List", "java/lang/Integer", "java/lang/Number");
  }

  @Test
  public void analyseMethodSignatureWithoutGenerics() {
    String signature = "([I)Ljava/lang/String;";
    new SignatureReader(signature).accept(visitor);
    assertThat(visitor.getInternalNames()).containsOnly("java/lang/String");
  }

  @Test
  public void analyseMethodSignatureWithoutObjects() {
    String signature = "([B)I";
    new SignatureReader(signature).accept(visitor);
    assertThat(visitor.getInternalNames()).hasSize(0);
  }

}
