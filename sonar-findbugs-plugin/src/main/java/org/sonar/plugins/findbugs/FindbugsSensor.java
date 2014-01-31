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
package org.sonar.plugins.findbugs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Java;
import org.sonar.api.resources.JavaFile;
import org.sonar.api.resources.Project;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.Violation;

import java.util.Collection;

public class FindbugsSensor implements Sensor {

  private static final Logger LOG = LoggerFactory.getLogger(FindbugsSensor.class);

  private RulesProfile profile;
  private RuleFinder ruleFinder;
  private FindbugsExecutor executor;

  public FindbugsSensor(RulesProfile profile, RuleFinder ruleFinder, FindbugsExecutor executor) {
    this.profile = profile;
    this.ruleFinder = ruleFinder;
    this.executor = executor;
  }

  public boolean shouldExecuteOnProject(Project project) {
    return !project.getFileSystem().mainFiles(Java.KEY).isEmpty()
        && !profile.getActiveRulesByRepository(FindbugsConstants.REPOSITORY_KEY).isEmpty();
  }

  public void analyse(Project project, SensorContext context) {
    Collection<ReportedBug> collection = executor.execute();

    for (ReportedBug bugInstance : collection) {
      Rule rule = ruleFinder.findByKey(FindbugsConstants.REPOSITORY_KEY, bugInstance.getType());
      if (rule == null) {
        // ignore violations from report, if rule not activated in Sonar
        LOG.warn("Findbugs rule '{}' not active in Sonar.", bugInstance.getType());
        continue;
      }

      String longMessage = bugInstance.getMessage();
      String className = bugInstance.getClassName();
      int start = bugInstance.getStartLine();

      JavaFile resource = new JavaFile(getSonarJavaFileKey(className));
      if (context.getResource(resource) != null) {
        Violation violation = Violation.create(rule, resource)
            .setMessage(longMessage);
        if (start > 0) {
          violation.setLineId(start);
        }
        context.saveViolation(violation);
      }
    }
  }

  private static String getSonarJavaFileKey(String className) {
    if (className.indexOf('$') > -1) {
      return className.substring(0, className.indexOf('$'));
    }
    return className;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }

}
