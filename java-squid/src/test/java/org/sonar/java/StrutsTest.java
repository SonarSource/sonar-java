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
import org.fest.assertions.Delta;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sonar.java.ast.api.JavaMetric;
import org.sonar.squid.api.SourceCodeSearchEngine;
import org.sonar.squid.api.SourceProject;
import org.sonar.squid.indexer.QueryByType;
import org.sonar.squid.measures.Metric;

import java.io.File;
import java.util.Collections;

import static org.fest.assertions.Assertions.assertThat;

public class StrutsTest {

  private static JavaSquid squid;
  private static SourceProject project;

  @BeforeClass
  public static void init() {
    File prjDir = new File("target/test-projects/struts-core-1.3.9");
    File srcDir = new File(prjDir, "src");
    File binDir = new File(prjDir, "bin");

    JavaConfiguration conf = new JavaConfiguration(Charsets.UTF_8);
    squid = new JavaSquid(conf);
    squid.scan(Collections.singleton(srcDir), Collections.singleton(binDir));

    SourceCodeSearchEngine index = squid.getIndex();
    project = (SourceProject) index.search(new QueryByType(SourceProject.class)).iterator().next();
  }

  // FIXME compare with previous values
  @Test
  public void measures_on_project() throws Exception {
    assertThat(project.getInt(JavaMetric.PACKAGES)).isEqualTo(15);
    assertThat(project.getInt(JavaMetric.FILES)).isEqualTo(134);
    // TODO assertEquals(37, project.getInt(Metric.ANONYMOUS_INNER_CLASSES));
    assertThat(project.getInt(JavaMetric.CLASSES)).isEqualTo(146);
    // TODO assertEquals(27, project.getInt(Metric.INTERFACES));
    // TODO assertEquals(33, project.getInt(Metric.ABSTRACT_CLASSES));
    assertThat(project.getInt(JavaMetric.METHODS) + project.getInt(JavaMetric.ACCESSORS)).isEqualTo(1485);
    assertThat(project.getInt(JavaMetric.METHODS)).isEqualTo(1178);
    assertThat(project.getInt(JavaMetric.ACCESSORS)).isEqualTo(307);
    assertThat(project.getInt(JavaMetric.LINES)).isEqualTo(32744);
    assertThat(project.getInt(JavaMetric.LINES_OF_CODE)).isEqualTo(14007);
    // TODO assertEquals(6426, project.getInt(Metric.BLANK_LINES));
    assertThat(project.getInt(JavaMetric.STATEMENTS)).isEqualTo(6895);
    assertThat(project.getInt(JavaMetric.COMPLEXITY)).isEqualTo(3547);
    // TODO assertEquals(4668, project.getInt(Metric.BRANCHES));
    assertThat(project.getInt(JavaMetric.COMMENT_LINES)).isEqualTo(9573);
    assertThat(project.getInt(JavaMetric.COMMENT_BLANK_LINES)).isEqualTo(4682);
    // TODO assertEquals(17908, project.getInt(Metric.COMMENT_LINES_WITHOUT_HEADER));
    // TODO assertEquals(0.40, project.getDouble(Metric.COMMENT_LINES_DENSITY), 0.01);
    assertThat(project.getInt(Metric.PUBLIC_API)).isEqualTo(1392);
    assertThat(project.getInt(Metric.PUBLIC_DOC_API)).isEqualTo(903);
    assertThat(project.getDouble(Metric.PUBLIC_DOCUMENTED_API_DENSITY)).isEqualTo(0.64, Delta.delta(0.01));
  }

}
