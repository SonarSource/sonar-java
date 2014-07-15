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
import org.sonar.api.measures.PersistenceMode;
import org.sonar.api.measures.RangeDistributionBuilder;
import org.sonar.api.resources.Resource;
import org.sonar.java.ast.api.JavaMetric;
import org.sonar.squidbridge.api.SourceClass;
import org.sonar.squidbridge.api.SourceCode;
import org.sonar.squidbridge.api.SourceFile;
import org.sonar.squidbridge.api.SourceMethod;

public class FunctionsBridge extends Bridge {

  private static final Number[] LIMITS = { 1, 2, 4, 6, 8, 10, 12 };

  private int methods;
  private int accessors;
  private int complexityInMethods;
  private RangeDistributionBuilder methodComplexityDistribution;

  @Override
  public void onFile(SourceFile squidFile, Resource sonarFile) {
    methods = 0;
    complexityInMethods = 0;
    accessors = 0;
    methodComplexityDistribution = new RangeDistributionBuilder(CoreMetrics.FUNCTION_COMPLEXITY_DISTRIBUTION, LIMITS);

    visit(squidFile);

    context.saveMeasure(sonarFile, new Measure(CoreMetrics.FUNCTIONS, (double) methods));
    context.saveMeasure(sonarFile, new Measure(CoreMetrics.ACCESSORS, (double) accessors));
    context.saveMeasure(sonarFile, new Measure(CoreMetrics.COMPLEXITY_IN_FUNCTIONS, (double) complexityInMethods));
    context.saveMeasure(sonarFile, methodComplexityDistribution.build(true).setPersistenceMode(PersistenceMode.MEMORY));
  }

  private void visit(SourceCode squidCode) {
    if (squidCode.hasChildren()) {
      for (SourceCode code : squidCode.getChildren()) {
        if (code.isType(SourceClass.class)) {
          if (isNotAnonymousInnerClass(code)) {
            visit(code);
          }
        } else if (code.isType(SourceMethod.class)) {
          if (((SourceMethod) code).isAccessor()) {
            accessors++;
          } else {
            methods++;
            int complexity = code.getInt(JavaMetric.COMPLEXITY);
            complexityInMethods += complexity;
            methodComplexityDistribution.add(complexity);
          }
          visit(code);
        }
      }
    }
  }

  /**
   * @see org.sonar.java.ast.visitors.AnonymousInnerClassVisitor
   */
  private boolean isNotAnonymousInnerClass(SourceCode squidClass) {
    return squidClass.getInt(JavaMetric.CLASSES) >= 1;
  }

}
