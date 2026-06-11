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
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

class BeanDefinitionGathererTest extends SpringContextGathererTest {

  @BeforeEach
  void setUp() {
    gatherer = new BeanDefinitionGatherer();
    model = new SpringContextModel();
  }

  // ---- Stereotype annotations -----------------------------------------------

  @ParameterizedTest(name = "{0}")
  @MethodSource("stereotypeAnnotationArguments")
  void stereotype_annotation_registers_bean(String filePath, String expectedBeanName, String expectedType) {
    scan(filePath);

    var beans = model.getBeanDefinitionRegistry().getByName(expectedBeanName);
    assertThat(beans).hasSize(1);
    assertThat(beans.get(0).getType()).isEqualTo(expectedType);
  }

  static Stream<Arguments> stereotypeAnnotationArguments() {
    return Stream.of(
      Arguments.of("src/test/files/springcontext/SimpleComponent.java",          "simpleComponent",      "checks.spring.context.SimpleComponent"),
      Arguments.of("src/test/files/springcontext/SimpleService.java",            "simpleService",        "checks.spring.context.SimpleService"),
      Arguments.of("src/test/files/springcontext/SimpleRepository.java",         "simpleRepository",     "checks.spring.context.SimpleRepository"),
      Arguments.of("src/test/files/springcontext/SimpleController.java",         "simpleController",     "checks.spring.context.SimpleController"),
      Arguments.of("src/test/files/springcontext/SimpleRestController.java",     "simpleRestController", "checks.spring.context.SimpleRestController"),
      Arguments.of("src/test/files/springcontext/SimpleConfiguration.java",      "simpleConfiguration",  "checks.spring.context.SimpleConfiguration"),
      Arguments.of("src/test/files/springcontext/ConfigurationWithBeanMethods.java", "simpleServiceBean", "org.springframework.context.ApplicationContext")
    );
  }

  // ---- Explicit bean names ---------------------------------------------------

  @Test
  void explicit_bean_name_from_annotation_value() {
    scan("src/test/files/springcontext/ExplicitNameComponent.java");

    var beans = model.getBeanDefinitionRegistry().getByName("myBean");
    assertThat(beans).hasSize(1);
    assertThat(beans.get(0).getType()).isEqualTo("checks.spring.context.ExplicitNameComponent");
    // Default name should NOT be registered
    assertThat(model.getBeanDefinitionRegistry().getByName("explicitNameComponent")).isEmpty();
  }

  // ---- @Bean methods --------------------------------------------------------

  @Test
  void bean_method_with_explicit_name() {
    scan("src/test/files/springcontext/ConfigurationWithBeanMethods.java");

    var beans = model.getBeanDefinitionRegistry().getByName("namedBean");
    assertThat(beans).hasSize(1);
    assertThat(beans.get(0).getType()).isEqualTo("org.springframework.context.ApplicationContext");
    // Method name should NOT be registered
    assertThat(model.getBeanDefinitionRegistry().getByName("namedBeanMethod")).isEmpty();
  }

  @Test
  void bean_method_with_array_of_names_uses_first_name() {
    scan("src/test/files/springcontext/ConfigurationWithBeanMethods.java");

    var beans = model.getBeanDefinitionRegistry().getByName("arrayNamedBean");
    assertThat(beans).hasSize(1);
    assertThat(beans.get(0).getType()).isEqualTo("org.springframework.context.ApplicationContext");
  }

  @Test
  void bean_method_with_empty_name_array_falls_back_to_method_name() {
    scan("src/test/files/springcontext/ConfigurationWithBeanMethods.java");

    var beans = model.getBeanDefinitionRegistry().getByName("emptyNameArrayMethod");
    assertThat(beans).hasSize(1);
    assertThat(beans.get(0).getType()).isEqualTo("org.springframework.context.ApplicationContext");
  }

  // ---- @Primary -------------------------------------------------------------

  @Test
  void primary_annotation_is_captured() {
    scan("src/test/files/springcontext/PrimaryBean.java");

    var beans = model.getBeanDefinitionRegistry().getByName("primaryBean");
    assertThat(beans).hasSize(1);
    assertThat(beans.get(0).isPrimary()).isTrue();
  }

  @Test
  void non_primary_bean_has_isPrimary_false() {
    scan("src/test/files/springcontext/SimpleComponent.java");

    var beans = model.getBeanDefinitionRegistry().getByName("simpleComponent");
    assertThat(beans).hasSize(1);
    assertThat(beans.get(0).isPrimary()).isFalse();
  }

  // ---- Anonymous / no annotation --------------------------------------------

  @Test
  void anonymous_class_is_skipped() {
    scan("src/test/files/springcontext/SpringBootAppWithAnonymousClass.java");

    // Anonymous class (no simpleName) should be skipped — it would not be registered as a bean
    // SpringBootApplication itself is not a stereotype bean
    assertThat(model.getBeanDefinitionRegistry().getByName("")).isEmpty();
  }

  @Test
  void no_spring_annotations_registers_nothing() {
    scan("src/test/files/springcontext/NoScanAnnotations.java");

    assertThat(model.getBeanDefinitionRegistry().getByName("noScanAnnotations")).isEmpty();
  }

  // ---- DependencyVersionAware -----------------------------------------------

  @Test
  void gatherer_skipped_when_spring_not_in_classpath() {
    scan(List.of(), "src/test/files/springcontext/SimpleComponent.java");

    assertThat(model.getBeanDefinitionRegistry().getByName("simpleComponent")).isEmpty();
  }

  // ---- Multiple files -------------------------------------------------------

  @Test
  void beans_from_multiple_files_are_merged() {
    scan(
      "src/test/files/springcontext/SimpleComponent.java",
      "src/test/files/springcontext/SimpleService.java"
    );

    assertThat(model.getBeanDefinitionRegistry().getByName("simpleComponent")).hasSize(1);
    assertThat(model.getBeanDefinitionRegistry().getByName("simpleService")).hasSize(1);
  }

  // ---- @Autowired dependencies ----------------------------------------------

  @ParameterizedTest(name = "{0}")
  @MethodSource("dependencyCollectionArguments")
  void dependencies_collected_as_depending_beans(String filePath, String expectedBeanName) {
    scan(filePath);

    var beans = model.getBeanDefinitionRegistry().getByName(expectedBeanName);
    assertThat(beans).hasSize(1);
    assertThat(beans.get(0).getDependingBeans())
      .containsExactlyInAnyOrder(
        "org.springframework.context.ApplicationContext",
        "org.springframework.core.env.Environment"
      );
  }

  static Stream<Arguments> dependencyCollectionArguments() {
    return Stream.of(
      Arguments.of("src/test/files/springcontext/AutowiredDependencies.java",            "autowiredDependencies"),
      Arguments.of("src/test/files/springcontext/AutowiredConstructorDependencies.java", "autowiredConstructorDependencies"),
      Arguments.of("src/test/files/springcontext/BeanMethodWithDependencies.java",       "myBean")
    );
  }

  // ---- Bean location --------------------------------------------------------

  @Test
  void bean_location_is_captured() {
    scan("src/test/files/springcontext/SimpleComponent.java");

    var beans = model.getBeanDefinitionRegistry().getByName("simpleComponent");
    assertThat(beans).hasSize(1);
    var location = beans.get(0).getLocation();
    assertThat(location).isNotNull();
    assertThat(location.inputFile()).isNotNull();
    assertThat(location.mainLocation()).isNotNull();
  }

  // ---- Bean package ---------------------------------------------------------

  @Test
  void bean_package_is_captured() {
    scan("src/test/files/springcontext/SimpleComponent.java");

    var beans = model.getBeanDefinitionRegistry().getByName("simpleComponent");
    assertThat(beans).hasSize(1);
    assertThat(beans.get(0).getBeanPackage()).isEqualTo("checks.spring.context");
  }
}