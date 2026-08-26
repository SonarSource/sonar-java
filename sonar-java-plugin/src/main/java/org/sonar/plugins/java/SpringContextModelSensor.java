/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.plugins.java;

import org.sonar.api.batch.Phase;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.scanner.sensor.ProjectSensor;
import org.sonar.java.GeneratedCheckList;
import org.sonar.java.checks.spring.AmbiguousDependencyCheck;
import org.sonar.java.jsp.Jasper;
import org.sonar.java.model.springcontext.BeanLocation;
import org.sonar.java.model.springcontext.SpringContextModel;
import org.sonar.java.reporting.AnalyzerMessage;

/**
 * A post-phase {@link ProjectSensor} that holds the shared {@link SpringContextModel} built during analysis.
 *
 * <p>This sensor runs after the main Java analysis phase, ensuring that all
 * {@link org.sonar.java.model.springcontext.SpringContextModelGatherer} visitors have finished populating
 * the model before it can be consumed by downstream components.
 *
 * <p>The {@link SpringContextModel} instance is injected via the IoC container and shared across all
 * components that need access to Spring context information (bean definitions, component-scan packages, etc.).
 */
@Phase(name = Phase.Name.POST)
public class SpringContextModelSensor implements ProjectSensor {

  private final SpringContextModel springContextModel;

  public SpringContextModelSensor(SpringContextModel springContextModel) {
    this.springContextModel = springContextModel;
  }

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor.onlyOnLanguages(Java.KEY, Jasper.JSP_LANGUAGE_KEY).name("Java SpringContextModelSensor");
  }

  @Override
  public void execute(SensorContext context) {
    reportAmbiguousDependencies(context);
  }

  private void reportAmbiguousDependencies(SensorContext context) {
    RuleKey ruleKey = RuleKey.of(GeneratedCheckList.REPOSITORY_KEY, "S9352");
    for (var ambiguousDependency : new AmbiguousDependencyCheck().findAmbiguousDependencies(springContextModel)) {
      BeanLocation location = ambiguousDependency.location();
      AnalyzerMessage.TextSpan span = location.mainLocation();
      NewIssue newIssue = context.newIssue().forRule(ruleKey);
      newIssue.at(newIssue.newLocation()
        .on(location.inputFile())
        .at(location.inputFile().newRange(span.startLine, span.startCharacter, span.endLine, span.endCharacter))
        .message(ambiguousDependency.message()));
      newIssue.save();
    }
  }
}

