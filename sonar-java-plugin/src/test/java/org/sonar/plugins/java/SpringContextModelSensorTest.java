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

import java.io.File;
import org.junit.jupiter.api.Test;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.sensor.internal.DefaultSensorDescriptor;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.batch.sensor.issue.Issue;
import org.sonar.api.rule.RuleKey;
import org.sonar.java.model.springcontext.BeanDefinitionHolder;
import org.sonar.java.model.springcontext.BeanLocation;
import org.sonar.java.model.springcontext.SpringContextModel;
import org.sonar.java.reporting.AnalyzerMessage.TextSpan;

import static org.assertj.core.api.Assertions.assertThat;

class SpringContextModelSensorTest {

  private static final String MODULE_KEY = "module";
  private static final String PACKAGE = "checks.spring.s9352";

  @Test
  void test_toString() {
    DefaultSensorDescriptor descriptor = new DefaultSensorDescriptor();
    SpringContextModelSensor sensor = new SpringContextModelSensor(new SpringContextModel());
    sensor.describe(descriptor);
    assertThat(descriptor.name()).isEqualTo("Java SpringContextModelSensor");
    assertThat(descriptor.languages()).containsExactly("java", "jsp");
  }

  @Test
  void reports_an_issue_for_an_ambiguous_dependency() {
    SensorContextTester context = SensorContextTester.create(new File(""));
    SpringContextModel model = new SpringContextModel();
    InputFile inputFile = fakeInputFile(context, "UnresolvedConsumer.java");
    String type = "org.springframework.context.ApplicationContextAware";

    registerBean(model, type, "componentOne", inputFile, 5, 0, 5, 12);
    registerBean(model, type, "componentTwo", inputFile, 6, 0, 6, 12);
    registerDependency(model, type, "contextAware", inputFile, 13, 13, 13, 25);

    new SpringContextModelSensor(model).execute(context);

    assertThat(context.allIssues()).hasSize(1);
    Issue issue = context.allIssues().iterator().next();
    assertThat(issue.ruleKey()).isEqualTo(RuleKey.of("java", "S9352"));
    assertThat(issue.primaryLocation().message())
      .isEqualTo("Multiple beans match this dependency"
        + " (componentOne, componentTwo); disambiguate it with \"@Qualifier\" or mark one bean as \"@Primary\".");
    assertThat(issue.primaryLocation().textRange().start().line()).isEqualTo(13);
  }

  private static void registerBean(SpringContextModel model, String type, String beanName, InputFile inputFile,
    int startLine, int startCharacter, int endLine, int endCharacter) {
    BeanLocation location = new BeanLocation(inputFile, new TextSpan(startLine, startCharacter, endLine, endCharacter));
    model.getBeanDefinitionRegistry().addBeanDefinition(beanName,
      new BeanDefinitionHolder.Builder(type, MODULE_KEY, PACKAGE, location).build());
    model.getTypeToBeanNamesIndex().addBeanForType(type, beanName);
  }

  private static void registerDependency(SpringContextModel model, String type, String dependencyName, InputFile inputFile,
    int startLine, int startCharacter, int endLine, int endCharacter) {
    BeanLocation location = new BeanLocation(inputFile, new TextSpan(startLine, startCharacter, endLine, endCharacter));
    model.getTypeToDependenciesIndex().addDependencyForType(type, dependencyName, location);
  }

  private static InputFile fakeInputFile(SensorContextTester context, String fileName) {
    String line = "// dummy source line //////\n";
    String contents = line.repeat(20);
    InputFile inputFile = new TestInputFileBuilder("", fileName)
      .setContents(contents)
      .setLanguage("java")
      .setType(InputFile.Type.MAIN)
      .build();
    context.fileSystem().add(inputFile);
    return inputFile;
  }

}
