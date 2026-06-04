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
import org.sonar.api.scanner.sensor.ProjectSensor;
import org.sonar.java.jsp.Jasper;
import org.sonar.java.model.springcontext.SpringContextModel;

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
    System.out.println(springContextModel);
  }
}
