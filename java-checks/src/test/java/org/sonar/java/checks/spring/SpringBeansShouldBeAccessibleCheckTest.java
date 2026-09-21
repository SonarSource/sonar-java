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
package org.sonar.java.checks.spring;

import com.sonarsource.scanner.engine.sensor.test.fixtures.TestInputFileBuilder;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.java.model.springcontext.BeanDefinitionHolder;
import org.sonar.java.model.springcontext.BeanLocation;
import org.sonar.java.model.springcontext.SpringContextModel;
import org.sonar.java.reporting.AnalyzerMessage;

import static org.assertj.core.api.Assertions.assertThat;

class SpringBeansShouldBeAccessibleCheckTest {

  private static final String MESSAGE = "'MyComponent' is not reachable by @ComponentScan or @SpringBootApplication. "
    + "Either move it to a package configured in @ComponentScan or update your @ComponentScan configuration.";

  private final SpringBeansShouldBeAccessibleCheck check = new SpringBeansShouldBeAccessibleCheck();

  @Test
  void empty_model_has_no_issues() {
    assertThat(check.execute(new SpringContextModel())).isEmpty();
  }

  @Test
  void beans_in_scanned_package_and_subpackages_are_covered() {
    SpringContextModel model = new SpringContextModel();
    model.getProjectPackageScan().addPackage("application", "com.example.app");
    registerBean(model, "component", "com.example.app.MyComponent", "components", "com.example.app", 1);
    registerBean(model, "service", "com.example.app.service.MyService", "components", "com.example.app.service", 2);

    assertThat(check.execute(model)).isEmpty();
  }

  @Test
  void uncovered_bean_creates_issue_at_bean_location() {
    SpringContextModel model = new SpringContextModel();
    model.getProjectPackageScan().addPackage("application", "com.example.app");
    BeanDefinitionHolder bean = registerBean(
      model, "component", "com.example.other.MyComponent", "components", "com.example.other", 7);

    assertThat(check.execute(model))
      .containsExactly(new SpringContextIssue(bean.getLocation(), MESSAGE));
  }

  @Test
  void similarly_prefixed_package_is_not_covered() {
    SpringContextModel model = new SpringContextModel();
    model.getProjectPackageScan().addPackage("application", "com.example.app");
    registerBean(model, "component", "com.example.application.MyComponent", "components", "com.example.application", 1);

    assertThat(check.execute(model)).singleElement()
      .extracting(SpringContextIssue::message)
      .isEqualTo(MESSAGE);
  }

  @Test
  void scan_configuration_in_another_module_covers_bean() {
    SpringContextModel model = new SpringContextModel();
    model.getProjectPackageScan().addPackage("application", "com.example.library");
    registerBean(model, "component", "com.example.library.MyComponent", "library", "com.example.library", 1);

    assertThat(check.execute(model)).isEmpty();
  }

  @Test
  void bean_is_uncovered_when_no_module_scans_its_package() {
    SpringContextModel model = new SpringContextModel();
    model.getProjectPackageScan().addPackages("application", List.of("com.example.app", "com.example.shared"));
    model.getProjectPackageScan().addPackage("second-application", "org.example.app");
    registerBean(model, "component", "com.example.library.MyComponent", "library", "com.example.library", 1);

    assertThat(check.execute(model)).hasSize(1);
  }

  @Test
  void no_scan_configuration_leaves_all_beans_uncovered() {
    SpringContextModel model = new SpringContextModel();
    registerBean(model, "component", "com.example.MyComponent", "components", "com.example", 1);
    registerBean(model, "service", "com.example.MyService", "components", "com.example", 2);

    assertThat(check.execute(model)).hasSize(2);
  }

  @Test
  void aliases_at_same_location_create_one_issue() {
    SpringContextModel model = new SpringContextModel();
    InputFile inputFile = dummyInputFile("com/example/MyConfiguration.java");
    BeanLocation location = new BeanLocation(inputFile, new AnalyzerMessage.TextSpan(5, 2, 5, 16));
    registerBean(model, "firstAlias", "com.example.MyComponent", "components", "com.example", location);
    registerBean(model, "secondAlias", "com.example.MyComponent", "components", "com.example", location);

    assertThat(check.execute(model)).hasSize(1);
  }

  @Test
  void nested_class_message_uses_simple_name() {
    SpringContextModel model = new SpringContextModel();
    registerBean(model, "inner", "com.example.Outer$MyComponent", "components", "com.example", 1);

    assertThat(check.execute(model)).singleElement()
      .extracting(SpringContextIssue::message)
      .isEqualTo(MESSAGE);
  }

  private static BeanDefinitionHolder registerBean(SpringContextModel model, String beanName, String type, String module, String beanPackage, int line) {
    BeanLocation location = new BeanLocation(dummyInputFile(type.replace('.', '/') + ".java"), new AnalyzerMessage.TextSpan(line));
    return registerBean(model, beanName, type, module, beanPackage, location);
  }

  private static BeanDefinitionHolder registerBean(SpringContextModel model, String beanName, String type, String module, String beanPackage, BeanLocation location) {
    BeanDefinitionHolder bean = new BeanDefinitionHolder.Builder(type, module, beanPackage, location).build();
    model.getBeanDefinitionRegistry().addBeanDefinition(beanName, bean);
    return bean;
  }

  private static InputFile dummyInputFile(String path) {
    return new TestInputFileBuilder("", path)
      .setLanguage("java")
      .setType(InputFile.Type.MAIN)
      .build();
  }
}
