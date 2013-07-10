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

import org.fest.assertions.Delta;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sonar.java.ast.JavaAstScanner;
import org.sonar.java.squid.JavaSquidConfiguration;
import org.sonar.squid.Squid;
import org.sonar.squid.api.SourceProject;
import org.sonar.squid.measures.Metric;

import java.io.File;

import static org.fest.assertions.Assertions.assertThat;

public class StrutsOldTest {

  private static Squid squid;
  private static SourceProject project;

  @BeforeClass
  public static void init() {
    File prjDir = new File("target/test-projects/struts-core-1.3.9");
    File srcDir = new File(prjDir, "src");

    squid = new Squid(new JavaSquidConfiguration());
    squid.register(JavaAstScanner.class).scanDirectory(srcDir);

    project = squid.aggregate();
  }

  /**
   * SONAR-3712
   */
  private static final int STATEMENTS_CORRECTION = 74;

  @Test
  public void measures_on_project() throws Exception {
    assertThat(project.getInt(Metric.PACKAGES)).isEqualTo(15);
    assertThat(project.getInt(Metric.FILES)).isEqualTo(134);
    assertThat(project.getInt(Metric.ANONYMOUS_INNER_CLASSES)).isEqualTo(4);
    assertThat(project.getInt(Metric.CLASSES)).isEqualTo(146);
    assertThat(project.getInt(Metric.INTERFACES)).isEqualTo(8);
    assertThat(project.getInt(Metric.ABSTRACT_CLASSES)).isEqualTo(23);
    assertThat(project.getInt(Metric.METHODS) + project.getInt(Metric.ACCESSORS)).isEqualTo(1485);
    assertThat(project.getInt(Metric.METHODS)).isEqualTo(1437);
    assertThat(project.getInt(Metric.ACCESSORS)).isEqualTo(48);
    assertThat(project.getInt(Metric.LINES)).isEqualTo(32744);
    assertThat(project.getInt(Metric.LINES_OF_CODE)).isEqualTo(14007);
    assertThat(project.getInt(Metric.BLANK_LINES)).isEqualTo(4534);
    assertThat(project.getInt(Metric.STATEMENTS) + STATEMENTS_CORRECTION).isEqualTo(6895);
    assertThat(project.getInt(Metric.COMPLEXITY)).isEqualTo(3957);
    assertThat(project.getInt(Metric.BRANCHES)).isEqualTo(2519);
    assertThat(project.getInt(Metric.COMMENT_LINES)).isEqualTo(9573);
    assertThat(project.getInt(Metric.COMMENT_BLANK_LINES)).isEqualTo(4682);
    assertThat(project.getInt(Metric.COMMENT_LINES_WITHOUT_HEADER)).isEqualTo(7608);
    assertThat(project.getDouble(Metric.COMMENT_LINES_DENSITY)).isEqualTo(0.35, Delta.delta(0.01));
    assertThat(project.getInt(Metric.PUBLIC_API)).isEqualTo(1348);
    assertThat(project.getInt(Metric.PUBLIC_DOC_API)).isEqualTo(842);
    assertThat(project.getDouble(Metric.PUBLIC_DOCUMENTED_API_DENSITY)).isEqualTo(0.62, Delta.delta(0.01));
  }

}
