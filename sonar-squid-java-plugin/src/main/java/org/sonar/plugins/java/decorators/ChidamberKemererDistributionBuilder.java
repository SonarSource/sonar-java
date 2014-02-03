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
package org.sonar.plugins.java.decorators;

import org.sonar.api.batch.Decorator;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.batch.DependedUpon;
import org.sonar.api.batch.DependsUpon;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.RangeDistributionBuilder;
import org.sonar.api.resources.Java;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.Scopes;

public final class ChidamberKemererDistributionBuilder implements Decorator {

  private static final Integer[] RFC_LIMITS = {0, 5, 10, 20, 30, 50, 90, 150};

  @DependedUpon
  public Metric generatesRfcDistribution() {
    return CoreMetrics.RFC_DISTRIBUTION;
  }

  @DependsUpon
  public Metric dependsInRfc() {
    return CoreMetrics.RFC;
  }

  public void decorate(Resource resource, DecoratorContext context) {
    if (shouldExecuteOn(resource)) {
      RangeDistributionBuilder rfcDistribution = new RangeDistributionBuilder(CoreMetrics.RFC_DISTRIBUTION, RFC_LIMITS);

      for (DecoratorContext childContext : context.getChildren()) {
        if (Scopes.isFile(childContext.getResource())) {
          addMeasureToDistribution(childContext, rfcDistribution, CoreMetrics.RFC);
        }
      }

      saveDistribution(context, rfcDistribution);
    }
  }

  private void addMeasureToDistribution(DecoratorContext childContext, RangeDistributionBuilder distribution, Metric metric) {
    Measure measure = childContext.getMeasure(metric);
    if (measure != null) {
      distribution.add(measure.getIntValue());
    }
  }

  private void saveDistribution(DecoratorContext context, RangeDistributionBuilder distribution) {
    Measure measure = distribution.build(false);
    if (measure != null) {
      context.saveMeasure(measure);
    }
  }

  boolean shouldExecuteOn(Resource resource) {
    return Scopes.isDirectory(resource);
  }

  public boolean shouldExecuteOnProject(Project project) {
    return Java.KEY.equals(project.getLanguageKey());
  }

}
