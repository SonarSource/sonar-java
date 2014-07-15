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
import org.sonar.squidbridge.api.SourceClass;
import org.sonar.squidbridge.api.SourceFile;
import org.sonar.squidbridge.api.SourceMethod;
import org.sonar.squidbridge.measures.Metric;

import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class FunctionsBridgeTest {

  private final FunctionsBridge bridge = new FunctionsBridge();
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

    SourceClass squidClass = new SourceClass("class");
    squidClass.setMeasure(JavaMetric.CLASSES, 1);
    squidFile.addChild(squidClass);

    SourceClass squidAnonymousClass = new SourceClass("anonymousClass");
    squidFile.addChild(squidAnonymousClass);

    SourceMethod squidMethod = new SourceMethod("method");
    squidClass.addChild(squidMethod);

    SourceMethod squidAccessor = new SourceMethod("accessor");
    squidAccessor.setMeasure(Metric.ACCESSORS, 1);
    squidClass.addChild(squidAccessor);

    bridge.onFile(squidFile, sonarFile);

    ArgumentCaptor<Measure> measureCaptor = ArgumentCaptor.forClass(Measure.class);
    verify(context, times(4)).saveMeasure(eq(sonarFile), measureCaptor.capture());

    List<Measure> measures = measureCaptor.getAllValues();

    assertThat(measures.get(0).getMetric()).isSameAs(CoreMetrics.FUNCTIONS);
    assertThat(measures.get(0).getValue()).isEqualTo(1);

    assertThat(measures.get(1).getMetric()).isSameAs(CoreMetrics.ACCESSORS);
    assertThat(measures.get(1).getValue()).isEqualTo(1);

    assertThat(measures.get(2).getMetric()).isSameAs(CoreMetrics.COMPLEXITY_IN_FUNCTIONS);
    assertThat(measures.get(2).getValue()).isEqualTo(0);

    assertThat(measures.get(3).getMetric()).isSameAs(CoreMetrics.FUNCTION_COMPLEXITY_DISTRIBUTION);
  }

}
