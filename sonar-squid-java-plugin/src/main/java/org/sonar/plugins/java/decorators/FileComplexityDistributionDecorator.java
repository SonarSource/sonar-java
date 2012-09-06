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
package org.sonar.plugins.java.decorators;

import org.sonar.api.batch.Decorator;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.batch.DependedUpon;
import org.sonar.api.batch.DependsUpon;
import org.sonar.api.measures.*;
import org.sonar.api.resources.Java;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.Scopes;

/**
 * @since 3.0
 */
public class FileComplexityDistributionDecorator implements Decorator {

  private static final Number[] LIMITS = {0, 5, 10, 20, 30, 60, 90};

  @DependsUpon
  public Metric dependOnComplexity() {
    return CoreMetrics.COMPLEXITY;
  }

  @DependedUpon
  public Metric generatesComplexityDistribution() {
    return CoreMetrics.FILE_COMPLEXITY_DISTRIBUTION;
  }

  public boolean shouldExecuteOnProject(Project project) {
    return Java.KEY.equals(project.getLanguageKey());
  }

  private static boolean shouldExecuteOn(Resource resource, DecoratorContext context) {
    return Scopes.isFile(resource) && context.getMeasure(CoreMetrics.COMPLEXITY) != null;
  }

  public void decorate(Resource resource, DecoratorContext context) {
    if (shouldExecuteOn(resource, context)) {
      RangeDistributionBuilder builder = new RangeDistributionBuilder(CoreMetrics.FILE_COMPLEXITY_DISTRIBUTION, LIMITS);
      Measure complexity = context.getMeasure(CoreMetrics.COMPLEXITY);
      builder.add(complexity.getValue());
      Measure measure = builder.build(true);
      measure.setPersistenceMode(PersistenceMode.MEMORY);
      context.saveMeasure(measure);
    }
  }

}
