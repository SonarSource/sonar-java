/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
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

import org.sonar.api.design.Dependency;
import org.sonar.api.resources.Resource;
import org.sonar.graph.DirectedGraph;
import org.sonar.java.bytecode.asm.AsmClass;
import org.sonar.java.bytecode.asm.AsmEdge;
import org.sonar.plugins.java.api.JavaResourceLocator;

import javax.annotation.Nullable;

public class DependenciesVisitor extends BytecodeVisitor {

  @Nullable
  private Resource fromResource;
  private final DirectedGraph<Resource, Dependency> graph;
  private ResourceMapping resourceMapping;

  public DependenciesVisitor(DirectedGraph<Resource, Dependency> graph) {
    this.graph = graph;
  }

  @Override
  public void setJavaResourceLocator(JavaResourceLocator javaResourceLocator) {
    resourceMapping = javaResourceLocator.getResourceMapping();
    super.setJavaResourceLocator(javaResourceLocator);
  }

  @Override
  public void visitClass(AsmClass asmClass) {
    fromResource = getResource(asmClass);
  }

  @Override
  public void visitEdge(AsmEdge edge) {
    AsmClass toAsmClass = edge.getTargetAsmClass();
    Resource toResource = getResource(toAsmClass);
    Dependency fileEdge = createDependency(fromResource, toResource, null);
    if (fromResource != null && toResource != null) {
      createDependency(fromResource.getParent(), toResource.getParent(), fileEdge);
    }
  }


  private Dependency createDependency(@Nullable Resource from, @Nullable Resource to, @Nullable Dependency subDependency) {
    Dependency dependency = null;
    if (canWeLinkNodes(from, to)) {
      dependency = graph.getEdge(from, to);
      if (dependency == null) {
        dependency = new Dependency(from, to).setUsage("USES");
        dependency.setWeight(1);
        graph.addEdge(dependency);
      }
      if (subDependency != null && !resourceMapping.getSubDependencies(dependency).contains(subDependency)) {
        resourceMapping.addSubDependency(dependency, subDependency);
        dependency.setWeight(dependency.getWeight() + 1);
        subDependency.setParent(dependency);
      }
    }
    return dependency;
  }

  private static boolean canWeLinkNodes(@Nullable Resource from, @Nullable Resource to) {
    return from != null && to != null && !from.equals(to);
  }

  private Resource getResource(AsmClass asmClass) {
    return javaResourceLocator.findResourceByClassName(asmClass.getInternalName());
  }

}
