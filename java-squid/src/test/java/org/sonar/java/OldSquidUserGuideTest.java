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

import org.fest.assertions.Delta;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sonar.java.ast.JavaAstScanner;
import org.sonar.java.squid.JavaSquidConfiguration;
import org.sonar.squid.Squid;
import org.sonar.squid.api.SourceMethod;
import org.sonar.squid.api.SourceProject;
import org.sonar.squid.indexer.QueryByMeasure;
import org.sonar.squid.indexer.QueryByMeasure.Operator;
import org.sonar.squid.indexer.QueryByType;
import org.sonar.squid.measures.Metric;

import java.io.File;

import static org.fest.assertions.Assertions.assertThat;

public class OldSquidUserGuideTest {

  private static Squid squid;
  private static SourceProject project;

  @BeforeClass
  public static void init() {
    File srcDir = new File("target/test-projects/commons-collections-3.2.1/src");

    squid = new Squid(new JavaSquidConfiguration());
    squid.register(JavaAstScanner.class).scanDirectory(srcDir);

    project = squid.aggregate();
  }

  /**
   * SONAR-3712
   */
  private static final int STATEMENTS_CORRECTION =
      1 /* empty statement not counted */
      - 14 /* field counted, however should not */
      + 427 /* call of superclass constructor not counted */
      - 16 /* synchronized method counted, however should not */;

  @Test
  public void measures_on_project() throws Exception {
    assertThat(project.getInt(Metric.PACKAGES)).isEqualTo(12);
    assertThat(project.getInt(Metric.FILES)).isEqualTo(273);
    assertThat(project.getInt(Metric.ANONYMOUS_INNER_CLASSES)).isEqualTo(37);
    assertThat(project.getInt(Metric.CLASSES)).isEqualTo(412);
    assertThat(project.getInt(Metric.INTERFACES)).isEqualTo(27);
    assertThat(project.getInt(Metric.ABSTRACT_CLASSES)).isEqualTo(33);
    assertThat(project.getInt(Metric.METHODS)).isEqualTo(3805);
    assertThat(squid.search(new QueryByType(SourceMethod.class), new QueryByMeasure(Metric.ACCESSORS, Operator.EQUALS, 0)).size()).isEqualTo(3805);
    assertThat(project.getInt(Metric.ACCESSORS)).isEqualTo(69);
    assertThat(project.getInt(Metric.LINES)).isEqualTo(63852);
    assertThat(project.getInt(Metric.LINES_OF_CODE)).isEqualTo(26323);
    assertThat(project.getInt(Metric.BLANK_LINES)).isEqualTo(6426);
    assertThat(project.getInt(Metric.STATEMENTS) + STATEMENTS_CORRECTION).isEqualTo(12666);
    assertThat(project.getInt(Metric.COMPLEXITY)).isEqualTo(8475);
    assertThat(project.getInt(Metric.BRANCHES)).isEqualTo(4668);
    assertThat(project.getInt(Metric.COMMENT_LINES)).isEqualTo(21184);
    assertThat(project.getInt(Metric.COMMENT_BLANK_LINES)).isEqualTo(9995);
    assertThat(project.getInt(Metric.COMMENT_LINES_WITHOUT_HEADER)).isEqualTo(17908);
    assertThat(project.getDouble(Metric.COMMENT_LINES_DENSITY)).isEqualTo(0.4, Delta.delta(0.01));
    assertThat(project.getInt(Metric.PUBLIC_API)).isEqualTo(3257);
    assertThat(project.getInt(Metric.PUBLIC_DOC_API)).isEqualTo(2008);
  }

}
