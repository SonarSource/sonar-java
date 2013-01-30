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
package org.sonar.plugins.surefire;

import org.sonar.api.BatchExtension;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.resources.JavaFile;
import org.sonar.api.resources.Resource;
import org.sonar.api.test.MutableTestPlan;
import org.sonar.plugins.surefire.api.AbstractSurefireParser;
import org.sonar.plugins.surefire.data.UnitTestClassReport;
import org.sonar.plugins.surefire.data.UnitTestResult;

/**
 * @since 1.2
 */
public class SurefireJavaParser extends AbstractSurefireParser implements BatchExtension {

  private final ResourcePerspectives perspectives;

  public SurefireJavaParser(ResourcePerspectives perspectives) {
    this.perspectives = perspectives;
  }

  protected void saveResults(SensorContext context, Resource testFile, UnitTestClassReport report) {
    for (UnitTestResult unitTestResult : report.getResults()) {
      MutableTestPlan testPlan = perspectives.as(MutableTestPlan.class, testFile);
      if (testPlan != null) {
        testPlan.addTestCase(unitTestResult.getName())
            .setName(unitTestResult.getName())
            .setDurationInMs(unitTestResult.getDurationMilliseconds())
            .setStatus(unitTestResult.getStatus())
            .setMessage(unitTestResult.getMessage())
            .setStackTrace(unitTestResult.getStackTrace());
      }
    }
  }

  protected Resource<?> getUnitTestResource(String classKey) {
    return new JavaFile(classKey, true);
  }

}
