/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
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
import org.objectweb.asm.TypePath;
import org.objectweb.asm.TypeReference;
import org.sonar.java.resolve.JavaSymbol.MethodJavaSymbol;

import static org.sonar.java.resolve.BytecodeCompleter.ASM_API_VERSION;

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
    super(ASM_API_VERSION);
    this.methodSymbol = methodSymbol;
    this.bytecodeVisitor = bytecodeVisitor;
    this.syntheticArgs = 0;
  }

  @Override
  public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
    JavaType annotationType = bytecodeVisitor.convertAsmType(org.objectweb.asm.Type.getType(desc), Flags.ANNOTATION);
    AnnotationInstanceResolve annotationInstance = new AnnotationInstanceResolve(annotationType.getSymbol());
    methodSymbol.metadata().addAnnotation(annotationInstance);
    return new BytecodeAnnotationVisitor(annotationInstance, bytecodeVisitor);
  }

  @Override
  public AnnotationVisitor visitParameterAnnotation(int parameter, String desc, boolean visible) {
    JavaType annotationType = bytecodeVisitor.convertAsmType(org.objectweb.asm.Type.getType(desc), Flags.ANNOTATION);
    if (annotationType.is("java.lang.Synthetic")) {
      syntheticArgs++;
    } else {
      AnnotationInstanceResolve annotationInstance = new AnnotationInstanceResolve(annotationType.getSymbol());
      methodSymbol.getParameters().scopeSymbols().get(parameter - syntheticArgs).metadata().addAnnotation(annotationInstance);
      return new BytecodeAnnotationVisitor(annotationInstance, bytecodeVisitor);
    }
    return null;
  }

  @Override
  public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
    TypeReference typeReference = new TypeReference(typeRef);
    switch (typeReference.getSort()) {
      case TypeReference.METHOD_FORMAL_PARAMETER:
        return visitParameterAnnotation(typeReference.getFormalParameterIndex(), descriptor, visible);
      case TypeReference.METHOD_RETURN:
        return visitAnnotation(descriptor, visible);
      default:
        // Corner case, limitation: the case METHOD_TYPE_PARAMETER is not yet supported. It happens when an annotation
        // is set on a type parameter. e.g.: <@Annotation T> Object method()
        return null;
    }
  }

  @Override
  public AnnotationVisitor visitAnnotationDefault() {
    return new AnnotationVisitor(ASM_API_VERSION) {
      @Override
      public void visit(String name, Object value) {
        methodSymbol.defaultValue = value;
        super.visit(name, value);
      }
    };
  }
}
