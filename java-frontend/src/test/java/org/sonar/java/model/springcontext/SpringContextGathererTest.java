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
package org.sonar.java.model.springcontext;

import java.io.File;
import java.util.List;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.java.SonarComponents;
import org.sonar.java.TestUtils;
import org.sonar.java.model.JParserTestUtils;
import org.sonar.java.model.VisitorsBridge;
import org.sonar.java.test.classpath.TestClasspathUtils;
import org.sonar.plugins.java.api.JavaCheck;

abstract class SpringContextGathererTest {

  protected SpringContextModelGatherer gatherer;
  protected SpringContextModel model;

  protected void scan(String... filePaths) {
    scan(TestClasspathUtils.DEFAULT_MODULE.getClassPath(), SensorContextTester.create(new File("")), filePaths);
  }

  protected void scan(List<File> classpath, String... filePaths) {
    scan(classpath, SensorContextTester.create(new File("")), filePaths);
  }

  protected void scan(SensorContextTester ctx, String... filePaths) {
    scan(TestClasspathUtils.DEFAULT_MODULE.getClassPath(), ctx, filePaths);
  }

  protected void scan(List<File> classpath, SensorContextTester ctx, String... filePaths) {
    var sonarComponents = new SonarComponents(null, null, null, null, null, null);
    sonarComponents.setSensorContext(ctx);
    sonarComponents.setSpringContextModel(model);


    VisitorsBridge visitorsBridge = new VisitorsBridge(List.of((JavaCheck) gatherer), classpath, sonarComponents);
    for (String filePath : filePaths) {
      File file = new File(filePath);
      var compilationUnit = JParserTestUtils.parse(file, classpath);
      visitorsBridge.setCurrentFile(TestUtils.inputFile(file));
      visitorsBridge.visitFile(compilationUnit, false);
    }
    visitorsBridge.endOfAnalysis();
  }
}
