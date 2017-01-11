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

import com.google.common.collect.Lists;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.List;

public class BytecodeAnnotationVisitor extends AnnotationVisitor {
  private final AnnotationInstanceResolve annotationInstance;
  private final BytecodeVisitor bytecodeVisitor;

  public BytecodeAnnotationVisitor(AnnotationInstanceResolve annotationInstance, BytecodeVisitor bytecodeVisitor) {
    super(Opcodes.ASM5);
    this.annotationInstance = annotationInstance;
    this.bytecodeVisitor = bytecodeVisitor;
  }

  private void addValue(String name, Object value) {
    annotationInstance.addValue(new AnnotationValueResolve(name, value));
  }

  @Override
  public void visit(String name, Object value) {
    addValue(name, value);
  }

  @Override
  public AnnotationVisitor visitAnnotation(String name, String desc) {
    JavaSymbol.TypeJavaSymbol annotationSymbol = getSymbol(desc);
    return new BytecodeAnnotationVisitor(new AnnotationInstanceResolve(annotationSymbol), bytecodeVisitor);
  }

  @Override
  public void visitEnum(String name, String desc, String value) {
    List<JavaSymbol> lookup = getSymbol(desc).members().lookup(value);
    for (JavaSymbol symbol : lookup) {
      if (symbol.isKind(JavaSymbol.VAR)) {
        addValue(name, symbol);
      }
    }
  }

  @Override
  public AnnotationVisitor visitArray(final String name) {
    final List<Object> valuesList = Lists.newArrayList();
    //TODO handle arrays of annotation and arrays of enum values.
    return new AnnotationVisitor(Opcodes.ASM5, this) {
      @Override
      public void visit(String name, Object value) {
        valuesList.add(value);
      }

      @Override
      public void visitEnd() {
        addValue(name, valuesList.toArray());
      }
    };
  }

  private JavaSymbol.TypeJavaSymbol getSymbol(String desc) {
    return bytecodeVisitor.convertAsmType(Type.getType(desc)).getSymbol();
  }
}
