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
import org.junit.jupiter.api.Test;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.java.JavaFrontend;
import org.sonar.java.Measurer;
import org.sonar.java.SonarComponents;
import org.sonar.java.TestUtils;
import org.sonar.java.model.JavaVersionImpl;
import org.sonar.java.telemetry.NoOpTelemetry;
import org.sonar.java.test.classpath.TestClasspathUtils;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.plugins.java.api.JavaResourceLocator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SpringContextModelTest {

  @Test
  void testInitialization() {
    SpringContextModel model = new SpringContextModel();

    assertNotNull(model.getBeanDefinitionRegistry(), "BeanDefinitionRegistry should be initialized");
    assertNotNull(model.getProjectPackageScan(), "ProjectPackageScan should be initialized");
    assertNotNull(model.getTypeToBeansIndex(), "TypeToBeansIndex should be initialized");
    assertNotNull(model.getEntityClassToPropertiesIndex(), "EntityClassToPropertiesIndex should be initialized");
  }

  @Test
  void scan_fills_project_package_scan_in_spring_context_model() {
    SonarComponents sonarComponents = TestUtils.mockSonarComponents();
    when(sonarComponents.getJavaClasspath()).thenReturn(TestClasspathUtils.DEFAULT_MODULE.getClassPath());
    when(sonarComponents.getJavaTestClasspath()).thenReturn(TestClasspathUtils.DEFAULT_MODULE.getClassPath());
    when(sonarComponents.getModuleKey()).thenReturn("a");

    var gatheringModel = new SpringContextGatheringModel();
    var telemetry = new NoOpTelemetry();
    List<JavaCheck> testGatherers = List.copyOf(SpringContextModelGatherers.getAllGatherers(gatheringModel, telemetry));
    when(sonarComponents.testChecks()).thenReturn(testGatherers);
    var mainGatherers = SpringContextModelGatherers.getAllGatherers(gatheringModel, telemetry).toArray(new JavaCheck[0]);
    JavaFrontend frontend = new JavaFrontend(new JavaVersionImpl(), sonarComponents, mock(Measurer.class), telemetry, mock(JavaResourceLocator.class), null,
      mainGatherers);
    frontend.scan(
      List.of(TestUtils.inputFile("src/test/files/springcontext/SpringBootApp.java")),
      List.of(TestUtils.inputFile("", new File("src/test/files/springcontext/SpringContextComponent.java"), InputFile.Type.TEST)),
      List.of()
    );
    SpringContextModel springContextModel = SpringContextModel.of(gatheringModel);

    assertThat(springContextModel.getProjectPackageScan().getModules()).isNotEmpty();
    assertThat(springContextModel.getProjectPackageScan().getPackagesForModule("a"))
      .containsExactly("springcontext");
    assertThat(springContextModel.getBeanDefinitionRegistry().getByName("springContextComponent"))
      .hasSize(1)
      .first()
      .satisfies(bean -> assertThat(bean.getType()).isEqualTo("springcontext.SpringContextComponent"));
  }

}
