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
package org.sonar.java.bytecode.visitor;

import org.sonar.java.bytecode.asm.AsmClass;
import org.sonar.java.bytecode.asm.AsmEdge;
import org.sonar.java.bytecode.asm.AsmMethod;
import org.sonar.squid.api.SourceCodeEdgeUsage;
import org.sonar.squid.measures.Metric;

import java.util.HashSet;
import java.util.Set;

public class RFCVisitor extends BytecodeVisitor {

  private AsmClass asmClass;
  private Set<AsmMethod> distinctCallToExternalMethods;
  private int rfc = 0;

  @Override
  public void visitClass(AsmClass asmClass) {
    rfc = 0;
    this.asmClass = asmClass;
    distinctCallToExternalMethods = new HashSet<AsmMethod>();
  }

  @Override
  public void visitMethod(AsmMethod asmMethod) {
    if (asmMethod.isBodyLoaded() && !asmMethod.isAccessor()) {
      rfc++;
    }
  }

  @Override
  public void visitEdge(AsmEdge edge) {
    if (edge.getTargetAsmClass() != asmClass && edge.getUsage() == SourceCodeEdgeUsage.CALLS_METHOD
        && !((AsmMethod) edge.getTo()).isAccessor()) {
      distinctCallToExternalMethods.add((AsmMethod) edge.getTo());
    }
  }

  @Override
  public void leaveClass(AsmClass asmClass) {
    rfc += distinctCallToExternalMethods.size();
    getSourceClass(asmClass).add(Metric.RFC, rfc);

    if (isMainPublicClassInFile(asmClass)) {
      getSourceFile(asmClass).add(Metric.RFC, rfc);
    }
  }

}
