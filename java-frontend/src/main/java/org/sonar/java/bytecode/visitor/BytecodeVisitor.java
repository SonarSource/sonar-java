/*
 * SonarQube Java
 * Copyright (C) 2012-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.java.bytecode.visitor;

import org.sonar.api.resources.Resource;
import org.sonar.java.bytecode.asm.AsmClass;
import org.sonar.java.bytecode.asm.AsmEdge;
import org.sonar.java.bytecode.asm.AsmField;
import org.sonar.java.bytecode.asm.AsmMethod;
import org.sonar.java.signature.MethodSignature;
import org.sonar.java.signature.MethodSignaturePrinter;
import org.sonar.java.signature.MethodSignatureScanner;
import org.sonar.plugins.java.api.JavaCheck;

import javax.annotation.Nullable;

public abstract class BytecodeVisitor implements JavaCheck {
  BytecodeContext context;

  public void visitClass(AsmClass asmClass) {
  }

  public void visitMethod(AsmMethod asmMethod) {
  }

  public void visitField(AsmField asmField) {
  }

  public void visitEdge(AsmEdge asmEdge) {

  }

  public void leaveClass(AsmClass asmClass) {
  }

  public void setContext(BytecodeContext context) {
    this.context = context;
  }

  protected BytecodeContext getContext() {
    return context;
  }

  @Nullable
  protected final Resource getSourceFile(AsmClass asmClass) {
    return context.getJavaResourceLocator().findResourceByClassName(asmClass.getInternalName());
  }

  protected final int getMethodLineNumber(AsmMethod asmMethod) {
    MethodSignature methodSignature = MethodSignatureScanner.scan(asmMethod.getGenericKey());
    AsmClass asmClass = asmMethod.getParent();
    Integer result = context.getJavaResourceLocator().getMethodStartLine(asmClass.getInternalName() + "#" + MethodSignaturePrinter.print(methodSignature));
    if (result != null) {
      return result;
    }
    return -1;
  }

}
