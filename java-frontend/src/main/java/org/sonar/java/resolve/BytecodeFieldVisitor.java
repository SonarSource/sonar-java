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
package org.sonar.java.resolve;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Opcodes;
import org.sonar.java.resolve.JavaSymbol.VariableJavaSymbol;

public class BytecodeFieldVisitor extends FieldVisitor {

  private final VariableJavaSymbol fieldSymbol;
  private final BytecodeVisitor bytecodeVisitor;

  BytecodeFieldVisitor(VariableJavaSymbol fieldSymbol, BytecodeVisitor bytecodeVisitor) {
    super(Opcodes.ASM5);
    this.fieldSymbol = fieldSymbol;
    this.bytecodeVisitor = bytecodeVisitor;
  }

  @Override
  public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
    JavaType annotationType = bytecodeVisitor.convertAsmType(org.objectweb.asm.Type.getType(desc));
    AnnotationInstanceResolve annotationInstance = new AnnotationInstanceResolve(annotationType.getSymbol());
    fieldSymbol.metadata().addAnnotation(annotationInstance);
    return new BytecodeAnnotationVisitor(annotationInstance, bytecodeVisitor);
  }

}
