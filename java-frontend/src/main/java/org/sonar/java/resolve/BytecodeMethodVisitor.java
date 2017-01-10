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
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.sonar.java.resolve.JavaSymbol.MethodJavaSymbol;

public class BytecodeMethodVisitor extends MethodVisitor {

  private final MethodJavaSymbol methodSymbol;
  private final BytecodeVisitor bytecodeVisitor;
  /**
   * This counter counts the number of argument annotated with java.lang.Synthetic.
   * This annotation is added by the asm library for arguments that are in the method descriptor but not in the method signature.
   * As we rely on method signature (when available) we might end up with less parameter in the method symbol than in the method descriptor (which makes sense as we want to be
   * more compliant with source than with bytecode). So we assume all synthetic params are the first parameters and count them as we visit.
   * Then we substract that number from the parameter index to be compliant with what is in MethodSymbol.
   */
  private int syntheticArgs;

  BytecodeMethodVisitor(MethodJavaSymbol methodSymbol, BytecodeVisitor bytecodeVisitor) {
    super(Opcodes.ASM5);
    this.methodSymbol = methodSymbol;
    this.bytecodeVisitor = bytecodeVisitor;
    this.syntheticArgs = 0;
  }

  @Override
  public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
    JavaType annotationType = bytecodeVisitor.convertAsmType(org.objectweb.asm.Type.getType(desc));
    AnnotationInstanceResolve annotationInstance = new AnnotationInstanceResolve(annotationType.getSymbol());
    methodSymbol.metadata().addAnnotation(annotationInstance);
    return new BytecodeAnnotationVisitor(annotationInstance, bytecodeVisitor);
  }

  @Override
  public AnnotationVisitor visitParameterAnnotation(int parameter, String desc, boolean visible) {
    JavaType annotationType = bytecodeVisitor.convertAsmType(org.objectweb.asm.Type.getType(desc));
    if (annotationType.is("java.lang.Synthetic")) {
      syntheticArgs++;
    } else {
      AnnotationInstanceResolve annotationInstance = new AnnotationInstanceResolve(annotationType.getSymbol());
      methodSymbol.getParameters().scopeSymbols().get(parameter - syntheticArgs).metadata().addAnnotation(annotationInstance);
      return new BytecodeAnnotationVisitor(annotationInstance, bytecodeVisitor);
    }
    return null;
  }
}
