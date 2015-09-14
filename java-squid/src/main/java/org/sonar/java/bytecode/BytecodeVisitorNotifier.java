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
package org.sonar.java.bytecode;

import org.sonar.java.bytecode.asm.AsmClass;
import org.sonar.java.bytecode.asm.AsmEdge;
import org.sonar.java.bytecode.asm.AsmField;
import org.sonar.java.bytecode.asm.AsmMethod;
import org.sonar.java.bytecode.asm.AsmResource;
import org.sonar.java.bytecode.visitor.BytecodeVisitor;
import org.sonar.plugins.java.api.JavaResourceLocator;
import org.sonar.squidbridge.indexer.SquidIndex;

public class BytecodeVisitorNotifier {

  private final AsmClass asmClass;
  private final BytecodeVisitor[] bytecodeVisitors;

  public BytecodeVisitorNotifier(AsmClass asmClass, BytecodeVisitor[] bytecodeVisitors) {
    this.asmClass = asmClass;
    this.bytecodeVisitors = new BytecodeVisitor[bytecodeVisitors.length];
    System.arraycopy(bytecodeVisitors, 0, this.bytecodeVisitors, 0, bytecodeVisitors.length);
  }

  public void notifyVisitors(SquidIndex indexer, JavaResourceLocator javaResourceLocator) {
    for (BytecodeVisitor visitor : bytecodeVisitors) {
      visitor.setSquidIndex(indexer);
      visitor.setJavaResourceLocator(javaResourceLocator);
    }
    callVisitClass();
    callVisitMethodAndFieldAndEdge();
    callLeaveClass();
  }

  private void callVisitMethodAndFieldAndEdge() {
    callVisitEdgeForSpecificAsmResource(asmClass);
    for (AsmMethod method : asmClass.getMethods()) {
      callVisitMethod(method);
      callVisitEdgeForSpecificAsmResource(method);
    }
    for (AsmField field : asmClass.getFields()) {
      callVisitField(field);
      callVisitEdgeForSpecificAsmResource(field);
    }
  }

  private void callVisitEdgeForSpecificAsmResource(AsmResource resource) {
    for (AsmEdge edge : resource.getOutgoingEdges()) {
      for (BytecodeVisitor visitor : bytecodeVisitors) {
        visitor.visitEdge(edge);
      }
    }
  }

  private void callVisitMethod(AsmMethod asmMethod) {
    for (BytecodeVisitor visitor : bytecodeVisitors) {
      visitor.visitMethod(asmMethod);
    }
  }

  private void callVisitField(AsmField asmField) {
    for (BytecodeVisitor visitor : bytecodeVisitors) {
      visitor.visitField(asmField);
    }
  }

  private void callVisitClass() {
    for (BytecodeVisitor visitor : bytecodeVisitors) {
      visitor.visitClass(asmClass);
    }
  }

  private void callLeaveClass() {
    for (BytecodeVisitor visitor : bytecodeVisitors) {
      visitor.leaveClass(asmClass);
    }
  }

}
