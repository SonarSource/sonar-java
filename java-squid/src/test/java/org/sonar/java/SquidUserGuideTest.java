/*
 * Sonar Java
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
import org.junit.BeforeClass;
import org.junit.Test;
import org.sonar.java.ast.api.JavaMetric;
import org.sonar.squid.api.SourceCode;
import org.sonar.squid.api.SourceCodeEdgeUsage;
import org.sonar.squid.api.SourceCodeSearchEngine;
import org.sonar.squid.api.SourceProject;
import org.sonar.squid.indexer.QueryByType;
import org.sonar.squid.measures.Metric;

import java.io.File;
import java.util.Collections;

import static org.fest.assertions.Assertions.assertThat;

public class SquidUserGuideTest {

  private static JavaSquid squid;
  private static SourceProject project;

  @BeforeClass
  public static void init() {
    File prjDir = new File("target/test-projects/commons-collections-3.2.1");
    File srcDir = new File(prjDir, "src");
    File binDir = new File(prjDir, "bin");

    JavaConfiguration conf = new JavaConfiguration(Charsets.UTF_8);
    squid = new JavaSquid(conf);
    squid.scanDirectories(Collections.singleton(srcDir), Collections.singleton(binDir));

    SourceCodeSearchEngine index = squid.getIndex();
    project = (SourceProject) index.search(new QueryByType(SourceProject.class)).iterator().next();
  }

  @Test
  public void measures_on_project() throws Exception {
    assertThat(project.getInt(JavaMetric.PACKAGES)).isEqualTo(12);
    assertThat(project.getInt(JavaMetric.FILES)).isEqualTo(273);
    // TODO assertEquals(37, project.getInt(Metric.ANONYMOUS_INNER_CLASSES));
    assertThat(project.getInt(JavaMetric.CLASSES)).isEqualTo(412);
    // TODO assertEquals(27, project.getInt(Metric.INTERFACES));
    // TODO assertEquals(33, project.getInt(Metric.ABSTRACT_CLASSES));
    assertThat(project.getInt(JavaMetric.METHODS) + project.getInt(JavaMetric.ACCESSORS)).isEqualTo(3805 + 69);
    assertThat(project.getInt(JavaMetric.METHODS)).isEqualTo(3805);
    assertThat(project.getInt(JavaMetric.ACCESSORS)).isEqualTo(69);
    assertThat(project.getInt(JavaMetric.LINES)).isEqualTo(63852);
    assertThat(project.getInt(JavaMetric.LINES_OF_CODE)).isEqualTo(26323);
    // TODO assertEquals(6426, project.getInt(Metric.BLANK_LINES));
    assertThat(project.getInt(JavaMetric.STATEMENTS)).isEqualTo(12666);
    assertThat(project.getInt(JavaMetric.COMPLEXITY)).isEqualTo(8389); // FIXME 8475
    // TODO assertEquals(4668, project.getInt(Metric.BRANCHES));
    assertThat(project.getInt(JavaMetric.COMMENT_LINES)).isEqualTo(21184);
    assertThat(project.getInt(JavaMetric.COMMENT_BLANK_LINES)).isEqualTo(9995);
    // TODO assertEquals(17908, project.getInt(Metric.COMMENT_LINES_WITHOUT_HEADER));
    // TODO assertEquals(0.40, project.getDouble(Metric.COMMENT_LINES_DENSITY), 0.01);
    // FIXME assertThat(project.getInt(Metric.PUBLIC_API)).isEqualTo(3257);
    // FIXME assertThat(project.getInt(Metric.PUBLIC_DOC_API)).isEqualTo(2008);
    // FIXME assertThat(project.getDouble(Metric.PUBLIC_DOCUMENTED_API_DENSITY)).isEqualTo(0.66, Delta.delta(0.01));
  }

  @Test
  public void getRobertCMartinOOMetrics() {
    SourceCode bufferPackage = squid.search("org/apache/commons/collections/buffer");
    assertThat(bufferPackage.getInt(JavaMetric.CLASSES)).isEqualTo(13);
    // TODO assertEquals(1, bufferPackage.getInt(Metric.ABSTRACT_CLASSES));
    assertThat(bufferPackage.getInt(Metric.CA)).isEqualTo(1);
    assertThat(bufferPackage.getInt(Metric.CE)).isEqualTo(14);
    // TODO assertEquals(0.93, bufferPackage.getDouble(Metric.INSTABILITY), 0.01);
    // TODO assertEquals(0.07, bufferPackage.getDouble(Metric.ABSTRACTNESS), 0.01);
    // TODO assertEquals(0.01, bufferPackage.getDouble(Metric.DISTANCE), 0.01);
  }

  @Test
  public void getChidamberAndKemererMetrics() {
    assertThat(squid.search("org/apache/commons/collections/bag/AbstractBagDecorator").getInt(Metric.NOC)).isEqualTo(3);
    assertThat(squid.search("org/apache/commons/collections/bag/PredicatedBag").getInt(Metric.DIT)).isEqualTo(4);
    assertThat(squid.search("org/apache/commons/collections/ArrayStack").getInt(Metric.RFC)).isEqualTo(15);
    assertThat(squid.search("org/apache/commons/collections/ArrayStack").getInt(Metric.LCOM4)).isEqualTo(3);
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
