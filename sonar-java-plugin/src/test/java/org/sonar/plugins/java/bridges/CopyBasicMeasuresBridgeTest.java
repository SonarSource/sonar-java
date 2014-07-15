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

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.resources.Resource;
import org.sonar.java.ast.api.JavaMetric;
import org.sonar.squidbridge.api.SourceFile;
import org.sonar.squidbridge.measures.Metric;

import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class CopyBasicMeasuresBridgeTest {

  private final CopyBasicMeasuresBridge bridge = new CopyBasicMeasuresBridge();
  private final SensorContext context = mock(SensorContext.class);

  @Before
  public void setup() {
    bridge.setContext(context);
  }

  @Test
  public void bytecode_not_needed() {
    assertThat(bridge.needsBytecode()).isFalse();
  }

  @Test
  public void test() {
    Resource sonarFile = mock(Resource.class);
    SourceFile squidFile = new SourceFile("file");
    squidFile.setMeasure(JavaMetric.LINES_OF_CODE, 1);
    squidFile.setMeasure(JavaMetric.LINES, 2);
    squidFile.setMeasure(JavaMetric.COMMENT_LINES_WITHOUT_HEADER, 3);
    squidFile.setMeasure(JavaMetric.STATEMENTS, 4);
    squidFile.setMeasure(JavaMetric.CLASSES, 6);
    squidFile.setMeasure(JavaMetric.COMPLEXITY, 7);
    squidFile.setMeasure(Metric.PUBLIC_API, 8);

    bridge.onFile(squidFile, sonarFile);

    ArgumentCaptor<Measure> measureCaptor = ArgumentCaptor.forClass(Measure.class);
    verify(context, times(10)).saveMeasure(eq(sonarFile), measureCaptor.capture());
    verifyNoMoreInteractions(context);

    List<Measure> measures = measureCaptor.getAllValues();

    assertThat(measures.get(0).getMetric()).isSameAs(CoreMetrics.NCLOC);
    assertThat(measures.get(0).getValue()).isEqualTo(1);

    assertThat(measures.get(1).getMetric()).isSameAs(CoreMetrics.LINES);
    assertThat(measures.get(1).getValue()).isEqualTo(2);

    assertThat(measures.get(2).getMetric()).isSameAs(CoreMetrics.COMMENT_LINES);
    assertThat(measures.get(2).getValue()).isEqualTo(3);

    assertThat(measures.get(3).getMetric()).isSameAs(CoreMetrics.STATEMENTS);
    assertThat(measures.get(3).getValue()).isEqualTo(4);

    assertThat(measures.get(4).getMetric()).isSameAs(CoreMetrics.CLASSES);
    assertThat(measures.get(4).getValue()).isEqualTo(6);

    assertThat(measures.get(5).getMetric()).isSameAs(CoreMetrics.COMPLEXITY);
    assertThat(measures.get(5).getValue()).isEqualTo(7);

    assertThat(measures.get(6).getMetric()).isSameAs(CoreMetrics.FILE_COMPLEXITY_DISTRIBUTION);

    assertThat(measures.get(7).getMetric()).isSameAs(CoreMetrics.PUBLIC_API);
    assertThat(measures.get(7).getValue()).isEqualTo(8);

    assertThat(measures.get(8).getMetric()).isSameAs(CoreMetrics.PUBLIC_DOCUMENTED_API_DENSITY);

    assertThat(measures.get(9).getMetric()).isSameAs(CoreMetrics.PUBLIC_UNDOCUMENTED_API);
  }

}
