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
package org.sonar.java;

import com.google.common.collect.ImmutableList;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.PersistenceMode;
import org.sonar.api.measures.RangeDistributionBuilder;
import org.sonar.api.resources.File;
import org.sonar.api.resources.Project;
import org.sonar.java.ast.visitors.AccessorVisitorST;
import org.sonar.java.ast.visitors.ComplexityVisitorST;
import org.sonar.java.ast.visitors.SubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

public class Measurer extends SubscriptionVisitor {

  private static final Number[] LIMITS = {1, 2, 4, 6, 8, 10, 12};

  private final SensorContext sensorContext;
  private final Project project;
  private File sonarFile;
  private int methods;
  private int accessors;
  private int complexityInMethods;
  private RangeDistributionBuilder methodComplexityDistribution;

  private final Deque<ClassTree> classTrees = new LinkedList<ClassTree>();
  private final AccessorVisitorST accessorVisitorST;
  private final ComplexityVisitorST complexityVisitorST;

  public Measurer(Project project, SensorContext context) {
    this.project = project;
    this.sensorContext = context;
    accessorVisitorST = new AccessorVisitorST();
    complexityVisitorST = new ComplexityVisitorST();
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.CLASS, Tree.Kind.INTERFACE, Tree.Kind.ENUM, Tree.Kind.ANNOTATION_TYPE,
        Tree.Kind.METHOD, Tree.Kind.CONSTRUCTOR);
  }


  @Override
  public void scanFile(JavaFileScannerContext context) {
    sonarFile = File.fromIOFile(context.getFile(), project);
    classTrees.clear();
    methods = 0;
    complexityInMethods = 0;
    accessors = 0;
    methodComplexityDistribution = new RangeDistributionBuilder(CoreMetrics.FUNCTION_COMPLEXITY_DISTRIBUTION, LIMITS);
    super.scanFile(context);
    //leave file.
    saveMetricOnFile(CoreMetrics.FUNCTIONS, methods);
    saveMetricOnFile(CoreMetrics.ACCESSORS, accessors);
    saveMetricOnFile(CoreMetrics.COMPLEXITY_IN_FUNCTIONS, complexityInMethods);
    sensorContext.saveMeasure(sonarFile, methodComplexityDistribution.build(true).setPersistenceMode(PersistenceMode.MEMORY));

  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Tree.Kind.CLASS) || tree.is(Tree.Kind.INTERFACE) || tree.is(Tree.Kind.ENUM) || tree.is(Tree.Kind.ANNOTATION_TYPE)) {
      classTrees.push((ClassTree) tree);
    }
    if (tree.is(Tree.Kind.METHOD) || tree.is(Tree.Kind.CONSTRUCTOR)) {
      //don't count methods in anonymous classes.
      if (classTrees.peek().simpleName() != null) {
        MethodTree methodTree = (MethodTree) tree;
        if (accessorVisitorST.isAccessor(classTrees.peek(), methodTree)) {
          accessors++;
        } else {
          methods++;
          int methodComplexity = complexityVisitorST.scan(classTrees.peek(), methodTree);
          methodComplexityDistribution.add(methodComplexity);
          complexityInMethods += methodComplexity;
        }
      }
    }

  }

  @Override
  public void leaveNode(Tree tree) {
    if (tree.is(Tree.Kind.CLASS) || tree.is(Tree.Kind.INTERFACE) || tree.is(Tree.Kind.ENUM) || tree.is(Tree.Kind.ANNOTATION_TYPE)) {
      classTrees.pop();
    }
  }

  private void saveMetricOnFile(Metric metric, double value) {
    sensorContext.saveMeasure(sonarFile, new Measure(metric, value));

  }


}
