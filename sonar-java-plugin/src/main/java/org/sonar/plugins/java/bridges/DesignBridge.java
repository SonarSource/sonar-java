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
package org.sonar.plugins.java.bridges;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.design.Dependency;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.PersistenceMode;
import org.sonar.api.resources.Directory;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.rules.ActiveRule;
import org.sonar.api.rules.Violation;
import org.sonar.api.utils.TimeProfiler;
import org.sonar.graph.Cycle;
import org.sonar.graph.DirectedGraph;
import org.sonar.graph.Dsm;
import org.sonar.graph.DsmTopologicalSorter;
import org.sonar.graph.Edge;
import org.sonar.graph.IncrementalCyclesAndFESSolver;
import org.sonar.graph.MinimumFeedbackEdgeSetSolver;
import org.sonar.java.checks.CycleBetweenPackagesCheck;
import org.sonar.squidbridge.api.SourcePackage;
import org.sonar.squidbridge.api.SourceProject;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public class DesignBridge extends Bridge {

  private static final Logger LOG = LoggerFactory.getLogger(DesignBridge.class);

  /*
   * This index is shared between onProject() and onPackage(). It works because onProject() is executed before onPackage().
   */
  @Override
  public boolean needsBytecode() {
    return true;
  }

  @Override
  public void onProject(SourceProject squidProject, Project sonarProject) {
    Collection<Resource> directories = DSMMapping.directories();
    TimeProfiler profiler = new TimeProfiler(LOG).start("Package design analysis");
    LOG.debug("{} packages to analyze", directories.size());

    IncrementalCyclesAndFESSolver<Resource> cyclesAndFESSolver = new IncrementalCyclesAndFESSolver<Resource>(graph, directories);
    LOG.debug("{} cycles", cyclesAndFESSolver.getCycles().size());

    Set<Edge> feedbackEdges = cyclesAndFESSolver.getFeedbackEdgeSet();
    LOG.debug("{} feedback edges", feedbackEdges.size());
    int tangles = cyclesAndFESSolver.getWeightOfFeedbackEdgeSet();

    saveViolations(feedbackEdges);
    saveDependencies();
    savePositiveMeasure(sonarProject, CoreMetrics.PACKAGE_CYCLES, cyclesAndFESSolver.getCycles().size());
    savePositiveMeasure(sonarProject, CoreMetrics.PACKAGE_FEEDBACK_EDGES, feedbackEdges.size());
    savePositiveMeasure(sonarProject, CoreMetrics.PACKAGE_TANGLES, tangles);
    savePositiveMeasure(sonarProject, CoreMetrics.PACKAGE_EDGES_WEIGHT, getEdgesWeight(directories));

    String dsmJson = serializeDsm(graph, directories, feedbackEdges);
    Measure dsmMeasure = new Measure(CoreMetrics.DEPENDENCY_MATRIX, dsmJson).setPersistenceMode(PersistenceMode.DATABASE);
    context.saveMeasure(sonarProject, dsmMeasure);

    profiler.stop();
  }

  private void savePositiveMeasure(Resource sonarResource, Metric metric, double value) {
    if (value >= 0.0) {
      context.saveMeasure(sonarResource, metric, value);
    }
  }

  @Override
  public void onPackage(SourcePackage squidPackage, Resource sonarPackage) {
    Collection<Resource> squidFiles = DSMMapping.files((Directory) sonarPackage);
    if (squidFiles != null && !squidFiles.isEmpty()) {

      IncrementalCyclesAndFESSolver<Resource> cycleDetector = new IncrementalCyclesAndFESSolver<Resource>(graph, squidFiles);
      Set<Cycle> cycles = cycleDetector.getCycles();

      MinimumFeedbackEdgeSetSolver solver = new MinimumFeedbackEdgeSetSolver(cycles);
      Set<Edge> feedbackEdges = solver.getEdges();
      int tangles = solver.getWeightOfFeedbackEdgeSet();

      savePositiveMeasure(sonarPackage, CoreMetrics.FILE_CYCLES, cycles.size());
      savePositiveMeasure(sonarPackage, CoreMetrics.FILE_FEEDBACK_EDGES, feedbackEdges.size());
      savePositiveMeasure(sonarPackage, CoreMetrics.FILE_TANGLES, tangles);
      savePositiveMeasure(sonarPackage, CoreMetrics.FILE_EDGES_WEIGHT, getEdgesWeight(squidFiles));

      String dsmJson = serializeDsm(graph, squidFiles, feedbackEdges);
      context.saveMeasure(sonarPackage, new Measure(CoreMetrics.DEPENDENCY_MATRIX, dsmJson));
    }
  }

  private double getEdgesWeight(Collection<Resource> resources) {
    List<Dependency> edges = graph.getEdges(resources);
    double total = 0.0;
    for (Dependency edge : edges) {
      total += edge.getWeight();
    }
    return total;
  }

  private String serializeDsm(DirectedGraph<Resource, Dependency> graph, Collection<Resource> squidSources, Set<Edge> feedbackEdges) {
    Dsm<Resource> dsm = new Dsm<Resource>(graph, squidSources, feedbackEdges);
    DsmTopologicalSorter.sort(dsm);
    return DsmSerializer.serialize(dsm);
  }

  private void saveViolations(Set<Edge> feedbackEdges) {
    ActiveRule rule = CycleBetweenPackagesCheck.getActiveRule(checkFactory);
    if (rule == null) {
      // Rule inactive
      return;
    }
    for (Edge feedbackEdge : feedbackEdges) {
      for (Dependency subDependency : DSMMapping.getSubDependencies((Dependency) feedbackEdge)) {
        Resource fromFile = subDependency.getFrom();
        Resource toFile = subDependency.getTo();
        Violation violation = Violation.create(rule, fromFile)
            .setMessage("Remove the dependency on the source file \"" + toFile.getLongName() + "\" to break a package cycle.")
            .setCost((double) subDependency.getWeight());
        context.saveViolation(violation);
      }
    }
  }

  private void saveDependencies() {
    for (Resource resource : graph.getVertices()) {
      for (Dependency dependency : graph.getOutgoingEdges(resource)) {
        context.saveDependency(dependency);
      }
    }
  }


}
