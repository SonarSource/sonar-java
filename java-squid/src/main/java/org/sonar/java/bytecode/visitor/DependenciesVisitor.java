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

import org.sonar.graph.DirectedGraph;
import org.sonar.java.bytecode.asm.AsmClass;
import org.sonar.java.bytecode.asm.AsmEdge;
import org.sonar.squidbridge.api.SourceCode;
import org.sonar.squidbridge.api.SourceCodeEdge;
import org.sonar.squidbridge.api.SourceCodeEdgeUsage;
import org.sonar.squidbridge.api.SourceFile;

import javax.annotation.Nullable;

public class DependenciesVisitor extends BytecodeVisitor {

  @Nullable
  private SourceFile fromSourceFile;
  private final DirectedGraph<SourceCode, SourceCodeEdge> graph;

  public DependenciesVisitor(DirectedGraph<SourceCode, SourceCodeEdge> graph) {
    this.graph = graph;
  }

  @Override
  public void visitClass(AsmClass asmClass) {
    fromSourceFile = getSourceFile(asmClass);
  }

  @Override
  public void visitEdge(AsmEdge edge) {
    AsmClass toAsmClass = edge.getTargetAsmClass();
    SourceFile toSourceFile = getSourceFile(toAsmClass);
    SourceCodeEdge fileEdge = createEdge(fromSourceFile, toSourceFile, null);
    if(fromSourceFile != null && toSourceFile != null) {
      createEdge(fromSourceFile.getParent(), toSourceFile.getParent(), fileEdge);
    }
  }

  private SourceCodeEdge createEdge(@Nullable SourceCode from, @Nullable SourceCode to, @Nullable SourceCodeEdge rootEdge) {
    SourceCodeEdge parentEdge = null;
    if (canWeLinkNodes(from, to)) {
      parentEdge = graph.getEdge(from, to);
      if (parentEdge == null) {
        parentEdge = new SourceCodeEdge(from, to, SourceCodeEdgeUsage.USES);
        graph.addEdge(parentEdge);
      }
      if(rootEdge == null) {
        parentEdge.addRootEdge(parentEdge);
      } else {
        parentEdge.addRootEdge(rootEdge);
      }
    }
    return parentEdge;
  }

  private boolean canWeLinkNodes(@Nullable SourceCode from, @Nullable SourceCode to) {
    return from != null && to != null && !from.equals(to);
  }

  @Override
  public String toString() {
    return "deperecated dependencies metrics";
  }

}
