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

import java.util.ArrayList;
import java.util.List;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Type;

import static org.sonar.java.resolve.BytecodeCompleter.ASM_API_VERSION;

public class BytecodeAnnotationVisitor extends AnnotationVisitor {
  private final AnnotationInstanceResolve annotationInstance;
  private final BytecodeVisitor bytecodeVisitor;

  public BytecodeAnnotationVisitor(AnnotationInstanceResolve annotationInstance, BytecodeVisitor bytecodeVisitor) {
    super(ASM_API_VERSION);
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
    JavaSymbol.TypeJavaSymbol sym = getSymbol(desc);
    if(sym.completing) {
      sym.callbackOnceComplete(() -> addSymbolAsValue(name, value, sym));
      return;
    }
    addSymbolAsValue(name, value, sym);
  }

  private void addSymbolAsValue(String name, String value, JavaSymbol.TypeJavaSymbol sym) {
    sym.members().lookup(value).stream()
      .filter(symbol -> symbol.isKind(JavaSymbol.VAR))
      .forEach(symbol -> addValue(name, symbol));
  }

  @Override
  public AnnotationVisitor visitArray(final String name) {
    final List<Object> valuesList = new ArrayList<>();
    // TODO handle arrays of annotation
    return new AnnotationVisitor(ASM_API_VERSION, this) {
      @Override
      public void visit(String name, Object value) {
        valuesList.add(value);
      }

      @Override
      public void visitEnum(String name, String desc, String value) {
        getSymbol(desc).members().lookup(value).stream()
          .filter(symbol -> symbol.isKind(JavaSymbol.VAR))
          .forEach(valuesList::add);
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
