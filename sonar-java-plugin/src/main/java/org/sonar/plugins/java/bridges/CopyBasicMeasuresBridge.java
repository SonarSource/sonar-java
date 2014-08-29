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

import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.resources.Resource;
import org.sonar.java.ast.api.JavaMetric;
import org.sonar.squidbridge.api.SourceCode;
import org.sonar.squidbridge.api.SourceFile;
import org.sonar.squidbridge.measures.MetricDef;

public final class CopyBasicMeasuresBridge extends Bridge {

  @Override
  public void onFile(SourceFile squidFile, Resource sonarResource) {
    copy(squidFile, sonarResource, JavaMetric.LINES_OF_CODE, CoreMetrics.NCLOC);
    copy(squidFile, sonarResource, JavaMetric.COMMENT_LINES_WITHOUT_HEADER, CoreMetrics.COMMENT_LINES);
    copy(squidFile, sonarResource, JavaMetric.STATEMENTS, CoreMetrics.STATEMENTS);
  }

  private void copy(SourceCode squidResource, Resource sonarResource, MetricDef squidMetric, org.sonar.api.measures.Metric sonarMetric) {
    context.saveMeasure(sonarResource, new Measure(sonarMetric, squidResource.getDouble(squidMetric)));
  }

}
