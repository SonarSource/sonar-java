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

import com.google.common.base.Charsets;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import org.fest.assertions.Delta;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.measures.Measure;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.ProjectFileSystem;
import org.sonar.java.ast.api.JavaMetric;
import org.sonar.squidbridge.api.CodeVisitor;
import org.sonar.squidbridge.api.SourceCode;
import org.sonar.squidbridge.api.SourceCodeEdgeUsage;
import org.sonar.squidbridge.api.SourceCodeSearchEngine;
import org.sonar.squidbridge.api.SourceProject;
import org.sonar.squidbridge.indexer.QueryByType;
import org.sonar.squidbridge.measures.Metric;

import java.io.File;
import java.util.Collections;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SquidUserGuideTest {

  private static JavaSquid squid;
  private static SourceProject project;
  private static SensorContext context;
  private static Measurer measurer;

  @BeforeClass
  public static void init() {
    File prjDir = new File("target/test-projects/commons-collections-3.2.1");
    File srcDir = new File(prjDir, "src");
    File binDir = new File(prjDir, "bin");

    JavaConfiguration conf = new JavaConfiguration(Charsets.UTF_8);
    context = mock(SensorContext.class);
    Project sonarProject = mock(Project.class);
    ProjectFileSystem pfs = mock(ProjectFileSystem.class);
    when(pfs.getBasedir()).thenReturn(prjDir);
    when(sonarProject.getFileSystem()).thenReturn(pfs);
    measurer = new Measurer(sonarProject, context);
    squid = new JavaSquid(conf, null, measurer, new CodeVisitor[0]);
    squid.scanDirectories(Collections.singleton(srcDir), Collections.singleton(binDir));

    SourceCodeSearchEngine index = squid.getIndex();
    project = (SourceProject) index.search(new QueryByType(SourceProject.class)).iterator().next();
  }

  @Test
  public void measures_on_project() throws Exception {
    ArgumentCaptor<Measure> captor = ArgumentCaptor.forClass(Measure.class);
    verify(context, atLeastOnce()).saveMeasure(any(org.sonar.api.resources.File.class), captor.capture());
    Multiset<String> metrics = HashMultiset.create();
    for (Measure measure : captor.getAllValues()) {
      if(measure.getIntValue() != null ){
        metrics.add(measure.getMetricKey(), measure.getIntValue());
      }
    }

    assertThat(project.getInt(JavaMetric.CLASSES)).isEqualTo(412);
    assertThat(metrics.count("classes")).isEqualTo(412);
    assertThat(project.getInt(JavaMetric.METHODS) + project.getInt(Metric.ACCESSORS)).isEqualTo(3805 + 69);
    assertThat(project.getInt(JavaMetric.METHODS)).isEqualTo(3805);
    assertThat(metrics.count("functions")).isEqualTo(3693);
    assertThat(project.getInt(Metric.ACCESSORS)).isEqualTo(69);
    assertThat(metrics.count("lines")).isEqualTo(64125);
    assertThat(project.getInt(JavaMetric.LINES_OF_CODE)).isEqualTo(26323);
    assertThat(project.getInt(JavaMetric.STATEMENTS)).isEqualTo(12047);
    assertThat(metrics.count("complexity")).isEqualTo(8475 - 80 /* SONAR-3793 */- 2 /* SONAR-3794 */);
    assertThat(project.getInt(JavaMetric.COMMENT_LINES_WITHOUT_HEADER)).isEqualTo(17908);
    assertThat(project.getInt(Metric.PUBLIC_API)).isEqualTo(3257);
    assertThat(metrics.count("public_api")).isEqualTo(3221);
    assertThat(project.getInt(Metric.PUBLIC_DOC_API)).isEqualTo(2008);
    assertThat(project.getDouble(Metric.PUBLIC_DOCUMENTED_API_DENSITY)).isEqualTo(0.62, Delta.delta(0.01));
  }

  @Test
  public void getDependenciesBetweenPackages() {
    SourceCode collectionsPackage = squid.search("org/apache/commons/collections");
    SourceCode bufferPackage = squid.search("org/apache/commons/collections/buffer");
    SourceCode bidimapPackage = squid.search("org/apache/commons/collections/bidimap");
    assertThat(squid.getEdge(bidimapPackage, collectionsPackage).getUsage()).isEqualTo(SourceCodeEdgeUsage.USES);
    assertThat(squid.getEdge(collectionsPackage, bufferPackage).getUsage()).isEqualTo(SourceCodeEdgeUsage.USES);
    assertThat(squid.getEdge(collectionsPackage, bufferPackage).getRootEdges().size()).isEqualTo(7);
  }

}
