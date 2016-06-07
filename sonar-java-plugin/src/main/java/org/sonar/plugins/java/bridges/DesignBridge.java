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
package org.sonar.plugins.java.bridges;

import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.design.Dependency;
import org.sonar.api.issue.Issuable;
import org.sonar.api.resources.Resource;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.graph.DirectedGraph;
import org.sonar.graph.Edge;
import org.sonar.graph.IncrementalCyclesAndFESSolver;
import org.sonar.java.bytecode.visitor.ResourceMapping;
import org.sonar.java.checks.CycleBetweenPackagesCheck;

import java.util.Collection;
import java.util.Set;

public class DesignBridge {

  private static final Logger LOG = Loggers.get(DesignBridge.class);

  private final DirectedGraph<Resource, Dependency> graph;
  private final ResourceMapping resourceMapping;
  private final ResourcePerspectives resourcePerspectives;

  public DesignBridge(DirectedGraph<Resource, Dependency> graph, ResourceMapping resourceMapping, ResourcePerspectives resourcePerspectives) {
    this.graph = graph;
    this.resourceMapping = resourceMapping;
    this.resourcePerspectives = resourcePerspectives;
  }

  public void saveDesign() {
    Collection<Resource> directories = resourceMapping.directories();
    LOG.debug("{} packages to analyze", directories.size());

    IncrementalCyclesAndFESSolver<Resource> cyclesAndFESSolver = new IncrementalCyclesAndFESSolver<>(graph, directories);
    LOG.debug("{} cycles", cyclesAndFESSolver.getCycles().size());

    Set<Edge> feedbackEdges = cyclesAndFESSolver.getFeedbackEdgeSet();
    LOG.debug("{} feedback edges", feedbackEdges.size());
    saveIssues(feedbackEdges);
  }

  private void saveIssues(Set<Edge> feedbackEdges) {
    for (Edge feedbackEdge : feedbackEdges) {
      for (Dependency subDependency : resourceMapping.getSubDependencies((Dependency) feedbackEdge)) {
        Resource fromFile = subDependency.getFrom();
        Resource toFile = subDependency.getTo();
        Issuable issuable = resourcePerspectives.as(Issuable.class, fromFile);
        if (issuable != null) {
          issuable.addIssue(issuable.newIssueBuilder()
              .ruleKey(CycleBetweenPackagesCheck.RULE_KEY)
              .message("Remove the dependency on the source file \"" + toFile.getLongName() + "\" to break a package cycle.")
              .build());
        }
      }
    }
  }


}
