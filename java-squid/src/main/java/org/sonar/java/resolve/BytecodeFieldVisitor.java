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
package org.sonar.java.resolve;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Opcodes;
import org.sonar.java.resolve.Symbol.VariableSymbol;

public class BytecodeFieldVisitor extends FieldVisitor {

  private final VariableSymbol fieldSymbol;
  private final BytecodeVisitor bytecodeVisitor;

  BytecodeFieldVisitor(VariableSymbol fieldSymbol, BytecodeVisitor bytecodeVisitor) {
    super(Opcodes.ASM5);
    this.fieldSymbol = fieldSymbol;
    this.bytecodeVisitor = bytecodeVisitor;
  }

  @Override
  public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
    Type annotationType = bytecodeVisitor.convertAsmType(org.objectweb.asm.Type.getType(desc));
    AnnotationInstance annotationInstance = new AnnotationInstance(annotationType.getSymbol());
    fieldSymbol.metadata().addAnnotation(annotationInstance);
    return new BytecodeAnnotationVisitor(annotationInstance, bytecodeVisitor);
  }

}
