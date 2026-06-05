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
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.java.SonarComponents;
import org.sonar.java.TestUtils;
import org.sonar.java.model.JParserTestUtils;
import org.sonar.java.model.VisitorsBridge;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.plugins.java.api.ModuleScannerContext;
import org.sonar.plugins.java.api.tree.Tree;

import static org.assertj.core.api.Assertions.assertThat;

class SpringContextModelGathererTest {

  @Test
  void testGatherSpringContextData() {
    var model = getSpringModelAfterVisitingFile("src/test/files/model/SimpleClass.java", new SampleGatherer());
    assertThat(model.getTypeToBeanNamesIndex().getNamesForType("com.example.MyService")).containsExactly("myServiceBean");
  }

  private SpringContextModel getSpringModelAfterVisitingFile(String filePath, JavaCheck check) {
    File file = new File(filePath);
    InputFile inputFile = TestUtils.inputFile(file);
    var compilationUnit = JParserTestUtils.parse(file);
    SensorContextTester sensorContextTester = SensorContextTester.create(new File(""));
    var sonarComponents = new SonarComponents(null, null, null, null, null, null);
    sonarComponents.setSensorContext(sensorContextTester);
    sonarComponents.setSpringContextModel(new SpringContextModel());

    VisitorsBridge visitorsBridge = new VisitorsBridge(List.of(check), new ArrayList<>(), sonarComponents);
    visitorsBridge.setCurrentFile(inputFile);
    visitorsBridge.visitFile(compilationUnit, false);
    visitorsBridge.endOfAnalysis();
    return sonarComponents.getSpringContextModel();
  }

  class SampleGatherer extends SpringContextModelGatherer {

    @Override
    public void gatherSpringContextData(ModuleScannerContext context, SpringContextModel springContextModel) {
      springContextModel.getTypeToBeanNamesIndex().addBeanForType("com.example.MyService", "myServiceBean");
    }

    @Override
    public List<Tree.Kind> nodesToVisit() {
      return List.of();
    }
  }

}
