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
import org.sonar.squidbridge.api.SourceClass;
import org.sonar.squidbridge.api.SourceCode;
import org.sonar.squidbridge.api.SourceCodeEdge;
import org.sonar.squidbridge.api.SourceCodeEdgeUsage;
import org.sonar.squidbridge.api.SourceFile;
import org.sonar.squidbridge.api.SourcePackage;

public class DependenciesVisitor extends BytecodeVisitor {

  private SourceClass fromSourceClass;
  private final DirectedGraph<SourceCode, SourceCodeEdge> graph;

  public DependenciesVisitor(DirectedGraph<SourceCode, SourceCodeEdge> graph) {
    this.graph = graph;
  }

  @Override
  public void visitClass(AsmClass asmClass) {
    this.fromSourceClass = getSourceClass(asmClass);
  }

  @Override
  public void visitEdge(AsmEdge edge) {
    AsmClass toAsmClass = edge.getTargetAsmClass();
    SourceClass toSourceClass = getSourceClass(toAsmClass);
    switch (edge.getUsage()) {
      case EXTENDS:
        link(fromSourceClass, toSourceClass, SourceCodeEdgeUsage.EXTENDS);
        break;
      case IMPLEMENTS:
        link(fromSourceClass, toSourceClass, SourceCodeEdgeUsage.IMPLEMENTS);
        break;
      default:
        link(fromSourceClass, toSourceClass, SourceCodeEdgeUsage.USES);
        break;
    }
  }

  private void link(SourceClass from, SourceClass to, SourceCodeEdgeUsage link) {
    if (canWeLinkNodes(from, to) && graph.getEdge(from, to) == null) {
      SourceCodeEdge edge = new SourceCodeEdge(from, to, link);
      graph.addEdge(edge);
      SourceCodeEdge fileEdge = createEdgeBetweenParents(SourceFile.class, from, to, edge);
      createEdgeBetweenParents(SourcePackage.class, from, to, fileEdge);
    }
  }

  private SourceCodeEdge createEdgeBetweenParents(Class<? extends SourceCode> type, SourceClass from, SourceClass to,
    SourceCodeEdge rootEdge) {
    SourceCode fromParent = from.getParent(type);
    SourceCode toParent = to.getParent(type);
    SourceCodeEdge parentEdge = null;
    if (canWeLinkNodes(fromParent, toParent) && rootEdge != null) {
      if (graph.getEdge(fromParent, toParent) == null) {
        parentEdge = new SourceCodeEdge(fromParent, toParent, SourceCodeEdgeUsage.USES);
        parentEdge.addRootEdge(rootEdge);
        graph.addEdge(parentEdge);
      } else {
        parentEdge = graph.getEdge(fromParent, toParent);
        parentEdge.addRootEdge(rootEdge);
      }
    }
    return parentEdge;
  }

  private boolean canWeLinkNodes(SourceCode from, SourceCode to) {
    return from != null && to != null && !from.equals(to);
  }

  @Override
  public String toString() {
    return "deperecated dependencies metrics";
  }

}
