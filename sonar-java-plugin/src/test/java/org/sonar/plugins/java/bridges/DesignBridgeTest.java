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
package org.sonar.plugins.java.bridges;

import org.junit.Test;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.design.Dependency;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.graph.DirectedGraph;
import org.sonar.java.bytecode.visitor.ResourceMapping;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyDouble;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class DesignBridgeTest {


  @Test
  public void metrics_are_saved() throws Exception {
    SensorContext context = mock(SensorContext.class);
    DirectedGraph<Resource, Dependency> graph = mock(DirectedGraph.class);
    ResourceMapping resourceMapping = mock(ResourceMapping.class);
    ResourcePerspectives resourcePerspectives = mock(ResourcePerspectives.class);
    DesignBridge bridge = new DesignBridge(context, graph, resourceMapping, resourcePerspectives);
    bridge.saveDesign(mock(Project.class));
    verify(context, times(4)).saveMeasure(any(Resource.class), any(Metric.class), anyDouble());

  }
}
