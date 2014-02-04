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
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.resources.Java;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.Scopes;
import org.sonar.java.api.JavaMethod;

/**
 * @deprecated usage of {@link JavaMethod} should be removed for SQ 4.2 (SONARJAVA-438)
 */
@Deprecated
public final class FunctionsDecorator implements Decorator {

  public void decorate(Resource resource, DecoratorContext context) {
    if (Scopes.isProgramUnit(resource)) {
      int methods = 0, accessors = 0;
      double complexityInMethods = 0;
      for (DecoratorContext child : context.getChildren()) {
        if (child.getResource() instanceof JavaMethod) {
          if (((JavaMethod) child.getResource()).isAccessor()) {
            accessors++;
          } else {
            methods++;
            complexityInMethods += child.getMeasure(CoreMetrics.COMPLEXITY).getValue();
          }
        }
      }
      context.saveMeasure(new Measure(CoreMetrics.FUNCTIONS, (double) methods));
      context.saveMeasure(new Measure(CoreMetrics.ACCESSORS, (double) accessors));
      context.saveMeasure(new Measure(CoreMetrics.COMPLEXITY_IN_FUNCTIONS, complexityInMethods));
    }
  }

  public boolean shouldExecuteOnProject(Project project) {
    return Java.KEY.equals(project.getLanguageKey());
  }

}
