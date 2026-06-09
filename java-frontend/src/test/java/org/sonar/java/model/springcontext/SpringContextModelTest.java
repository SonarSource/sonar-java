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

import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.sonar.java.JavaFrontend;
import org.sonar.java.Measurer;
import org.sonar.java.SonarComponents;
import org.sonar.java.TestUtils;
import org.sonar.java.model.JavaVersionImpl;
import org.sonar.java.telemetry.NoOpTelemetry;
import org.sonar.java.test.classpath.TestClasspathUtils;
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
    assertNotNull(model.getTypeToBeanNamesIndex(), "TypeToBeanNamesIndex should be initialized");
    assertNotNull(model.getEntityClassToPropertiesIndex(), "EntityClassToPropertiesIndex should be initialized");
  }

  @Test
  void scan_fills_project_package_scan_in_spring_context_model() {
    SpringContextModel springContextModel = new SpringContextModel();
    SonarComponents sonarComponents = TestUtils.mockSonarComponents();
    when(sonarComponents.getSpringContextModel()).thenReturn(springContextModel);
    when(sonarComponents.getJavaClasspath()).thenReturn(TestClasspathUtils.DEFAULT_MODULE.getClassPath());
    when(sonarComponents.getModuleKey()).thenReturn("a");

    JavaFrontend frontend = new JavaFrontend(new JavaVersionImpl(), sonarComponents, mock(Measurer.class), new NoOpTelemetry(), mock(JavaResourceLocator.class), null);
    frontend.scan(
      Collections.singletonList(TestUtils.inputFile("src/test/files/springcontext/SpringBootApp.java")),
      Collections.emptyList(),
      Collections.emptyList()
    );

    assertThat(springContextModel.getProjectPackageScan().getModules()).isNotEmpty();
    assertThat(springContextModel.getProjectPackageScan().getPackagesForModule("a"))
      .containsExactly("springcontext");
  }

}
