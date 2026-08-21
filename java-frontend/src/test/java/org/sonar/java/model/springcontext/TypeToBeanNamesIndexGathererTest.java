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

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class TypeToBeanNamesIndexGathererTest extends SpringContextGathererTest {

  @BeforeEach
  void setUp() {
    gatherer = new TypeToBeanNamesIndexGatherer();
    model = new SpringContextModel();
  }

  // ---- Stereotype beans -------------------------------------------------------

  @ParameterizedTest(name = "{0}")
  @ValueSource(strings = {
    "src/test/files/springcontext/SimpleComponent.java",
    "src/test/files/springcontext/SimpleService.java",
    "src/test/files/springcontext/SimpleRepository.java",
    "src/test/files/springcontext/SimpleController.java",
    "src/test/files/springcontext/SimpleRestController.java",
    "src/test/files/springcontext/SimpleConfiguration.java"
  })
  void stereotype_bean_is_registered_under_its_own_type(String filePath) {
    scan(filePath);

    var index = model.getTypeToBeanNamesIndex();
    assertThat(index.getNamesForType("checks.spring.context." + beanClassNameFrom(filePath)))
      .isNotEmpty();
  }

  @Test
  void bean_is_registered_under_implemented_interface() {
    scan("src/test/files/springcontext/ComponentImplementingInterface.java");

    var index = model.getTypeToBeanNamesIndex();
    assertThat(index.getNamesForType("checks.spring.context.ComponentImplementingInterface"))
      .containsOnly("componentImplementingInterface");
    assertThat(index.getNamesForType("org.springframework.context.ApplicationContextAware"))
      .containsOnly("componentImplementingInterface");
  }

  @Test
  void explicit_bean_name_is_used_in_index() {
    scan("src/test/files/springcontext/ExplicitNameComponent.java");

    var index = model.getTypeToBeanNamesIndex();
    assertThat(index.getNamesForType("checks.spring.context.ExplicitNameComponent"))
      .containsOnly("myBean");
  }

  // ---- @Bean methods ----------------------------------------------------------

  @Test
  void bean_method_return_type_is_registered() {
    scan("src/test/files/springcontext/ConfigurationWithBeanMethods.java");

    var index = model.getTypeToBeanNamesIndex();
    assertThat(index.getNamesForType("org.springframework.context.ApplicationContext"))
      .contains("simpleServiceBean", "namedBean", "arrayNamedBean", "emptyNameArrayMethod");
  }

  @Test
  void bean_method_aliases_are_all_registered() {
    scan("src/test/files/springcontext/ConfigurationWithBeanMethods.java");

    // @Bean(name = {"arrayNamedBean", "alias"}) — both names must appear in the index
    var index = model.getTypeToBeanNamesIndex();
    assertThat(index.getNamesForType("org.springframework.context.ApplicationContext"))
      .contains("arrayNamedBean", "alias");
  }

  // ---- Multiple beans ---------------------------------------------------------

  @Test
  void multiple_beans_of_same_type_all_registered() {
    scan(
      "src/test/files/springcontext/SimpleComponent.java",
      "src/test/files/springcontext/SimpleService.java"
    );

    var index = model.getTypeToBeanNamesIndex();
    // Each bean appears only under its own concrete type
    assertThat(index.getNamesForType("checks.spring.context.SimpleComponent"))
      .containsOnly("simpleComponent");
    assertThat(index.getNamesForType("checks.spring.context.SimpleService"))
      .containsOnly("simpleService");
  }

  // ---- No annotation ----------------------------------------------------------

  @Test
  void non_spring_class_registers_nothing() {
    scan("src/test/files/springcontext/NoScanAnnotations.java");

    assertThat(model.getTypeToBeanNamesIndex().getNamesForType("checks.spring.context.NoScanAnnotations"))
      .isEmpty();
  }

  @Test
  void gatherer_skipped_when_spring_not_in_classpath() {
    scan(List.of(), "src/test/files/springcontext/SimpleComponent.java");

    assertThat(model.getTypeToBeanNamesIndex().getNamesForType("checks.spring.context.SimpleComponent"))
      .isEmpty();
  }

  @Test
  void anonymous_class_is_skipped() {
    scan("src/test/files/springcontext/SpringBootAppWithAnonymousClass.java");

    assertThat(model.getTypeToBeanNamesIndex().getNamesForType("")).isEmpty();
  }

  // ---- Helpers ----------------------------------------------------------------

  private static String beanClassNameFrom(String filePath) {
    return filePath.substring(filePath.lastIndexOf('/') + 1, filePath.lastIndexOf('.'));
  }
}
