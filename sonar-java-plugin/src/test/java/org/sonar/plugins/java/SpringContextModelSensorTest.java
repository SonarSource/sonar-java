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

import com.sonarsource.scanner.engine.sensor.test.fixtures.SensorContextTester;
import com.sonarsource.scanner.engine.sensor.test.fixtures.TestInputFileBuilder;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.sensor.issue.Issue;
import org.sonar.api.rule.RuleKey;
import org.sonar.java.model.springcontext.BeanDefinitionHolder;
import org.sonar.java.model.springcontext.InjectionPoint;
import org.sonar.java.model.springcontext.ProfileExpression;
import org.sonar.java.model.springcontext.SpringContextGatheringModel;
import org.sonar.java.reporting.AnalyzerMessage.TextSpan;
import org.sonar.java.telemetry.DefaultTelemetry;
import org.sonar.java.telemetry.NoOpTelemetry;
import org.sonar.java.telemetry.Telemetry;
import org.sonar.scanner.plugin.api.impl.rule.ActiveRulesBuilder;
import org.sonar.scanner.plugin.api.impl.rule.NewActiveRule;
import org.sonar.scanner.plugin.api.impl.sensor.DefaultSensorDescriptor;

import static org.assertj.core.api.Assertions.assertThat;

class SpringContextModelSensorTest {

  private static final String MODULE_KEY = "module";
  private static final RuleKey S9352_RULE_KEY = RuleKey.of("java", "S9352");

  @TempDir
  Path tempDir;

  @Test
  void test_toString() {
    DefaultSensorDescriptor descriptor = new DefaultSensorDescriptor();
    SpringContextModelSensor sensor = sensor(new SpringContextGatheringModel(), new NoOpTelemetry());
    sensor.describe(descriptor);
    assertThat(descriptor.name()).isEqualTo("Java SpringContextModelSensor");
    assertThat(descriptor.languages()).containsExactly("java", "jsp");
  }

  @Test
  void reports_an_issue_for_an_ambiguous_dependency() {
    SensorContextTester context = SensorContextTester.create(tempDir);
    context.setActiveRules(activeRulesWithS9352());
    var gatheringModel = new SpringContextGatheringModel();
    InputFile inputFile = fakeInputFile(context, "UnresolvedConsumer.java");
    String type = "org.springframework.context.ApplicationContextAware";

    registerBean(gatheringModel, type, "componentOne", inputFile);
    registerBean(gatheringModel, type, "componentTwo", inputFile);
    registerDependency(gatheringModel, type, "contextAware", inputFile);

    var telemetry = new DefaultTelemetry();
    sensor(gatheringModel, telemetry).execute(context);

    assertThat(context.allIssues()).hasSize(1);
    Issue issue = context.allIssues().iterator().next();
    assertThat(issue.ruleKey()).isEqualTo(S9352_RULE_KEY);
    assertThat(issue.primaryLocation().message())
      .isEqualTo("Multiple beans match this dependency"
        + " (componentOne, componentTwo); disambiguate it with \"@Qualifier\" or mark one bean as \"@Primary\".");
    assertThat(issue.primaryLocation().textRange().start().line()).isEqualTo(13);
    assertThat(telemetry.toMap().get("java.spring.context_checks_time_ms")).matches("\\d+");
  }

  @Test
  void does_not_report_issues_when_rule_is_not_active() {
    SensorContextTester context = SensorContextTester.create(tempDir);
    // No active rules registered: S9352 is not in the quality profile.
    var gatheringModel = new SpringContextGatheringModel();
    InputFile inputFile = fakeInputFile(context, "UnresolvedConsumer.java");
    String type = "org.springframework.context.ApplicationContextAware";

    registerBean(gatheringModel, type, "componentOne", inputFile);
    registerBean(gatheringModel, type, "componentTwo", inputFile);
    registerDependency(gatheringModel, type, "contextAware", inputFile);

    var telemetry = new DefaultTelemetry();
    sensor(gatheringModel, telemetry).execute(context);

    assertThat(context.allIssues()).isEmpty();
    assertThat(telemetry.toMap().get("java.spring.context_checks_time_ms")).matches("\\d+");
  }

  @Test
  void restored_beans_affect_analyzed_files_without_reporting_on_restored_files() {
    SensorContextTester context = SensorContextTester.create(tempDir);
    context.setActiveRules(activeRulesWithS9352());
    InputFile analyzedFile = fakeInputFile(context, "Consumer.java");
    String type = "example.Service";
    var gatheringModel = new SpringContextGatheringModel();
    gatheringModel.collectBeans(MODULE_KEY, "restored-one", null, List.of(gatheredBean("componentOne", type, Map.of())));
    gatheringModel.collectBeans(MODULE_KEY, "restored-two", null, List.of(gatheredBean("componentTwo", type, Map.of())));
    var dependency = Map.of(type, Set.of(new InjectionPoint.InputFileData("contextAware", new TextSpan(13, 13, 13, 25), false)));
    gatheringModel.collectBeans(MODULE_KEY, "restored-consumer", null, List.of(gatheredBean("oldConsumer", "example.OldConsumer", dependency)));
    gatheringModel.collectBeans(MODULE_KEY, analyzedFile.key(), analyzedFile, List.of(gatheredBean("consumer", "example.Consumer", dependency)));

    sensor(gatheringModel, new NoOpTelemetry()).execute(context);

    assertThat(context.allIssues()).hasSize(1);
    assertThat(context.allIssues().iterator().next().primaryLocation().inputComponent()).isEqualTo(analyzedFile);
    var savedModel = JavaSensor.loadSpringContextGatheringModel(tempDir.resolve(JavaSensor.DEFAULT_SPRING_CONTEXT_MODEL_PATH));
    assertThat(savedModel.filesData().get(MODULE_KEY)).hasSize(4);
  }

  private static ActiveRules activeRulesWithS9352() {
    return new ActiveRulesBuilder()
      .addRule(new NewActiveRule.Builder().setRuleKey(S9352_RULE_KEY).build())
      .build();
  }

  @Test
  void records_metrics_for_the_built_model() {
    SensorContextTester context = SensorContextTester.create(tempDir);
    InputFile inputFile = fakeInputFile(context, "Consumer.java");
    var gatheringModel = new SpringContextGatheringModel();
    String type = "example.Service";
    registerBean(gatheringModel, type, "componentOne", inputFile);
    registerBean(gatheringModel, type, "componentTwo", inputFile);
    registerDependency(gatheringModel, type, "contextAware", inputFile);
    gatheringModel.collectPackages(MODULE_KEY, "componentOne", inputFile, Set.of("example"));

    var telemetry = new DefaultTelemetry();
    sensor(gatheringModel, telemetry).execute(context);

    assertThat(telemetry.toMap())
      .containsEntry("java.spring.bean_count", "3")
      .containsEntry("java.spring.bean_name_count", "3")
      .containsEntry("java.spring.injection_point_count", "1")
      .containsEntry("java.spring.component_scan_package_count", "1");
    assertThat(Long.parseLong(telemetry.toMap().get("java.spring.context_model_size_bytes"))).isPositive();
    assertThat(telemetry.toMap().get("java.spring.context_model_gathering_time_ms")).matches("\\d+");
    assertThat(telemetry.toMap().get("java.spring.context_checks_time_ms")).matches("\\d+");
  }

  private SpringContextModelSensor sensor(SpringContextGatheringModel gatheringModel, Telemetry telemetry) {
    return new SpringContextModelSensor(telemetry, gatheringModel);
  }

  private static BeanDefinitionHolder.InputFileData gatheredBean(String name, String type, Map<String, Set<InjectionPoint.InputFileData>> dependencies) {
    return new BeanDefinitionHolder.InputFileData(name, type, "example", new TextSpan(5, 0, 5, 4), false,
      ProfileExpression.UNCONDITIONAL, null, dependencies, Set.of(type));
  }

  private static void registerBean(SpringContextGatheringModel model, String type, String beanName, InputFile inputFile) {
    model.collectBeans(MODULE_KEY, beanName, inputFile, List.of(gatheredBean(beanName, type, Map.of())));
  }

  private static void registerDependency(SpringContextGatheringModel model, String type, String dependencyName, InputFile inputFile) {
    var dependencies = Map.of(type, Set.of(new InjectionPoint.InputFileData(dependencyName, new TextSpan(13, 13, 13, 25), false)));
    model.collectBeans(MODULE_KEY, "consumer", inputFile, List.of(gatheredBean("consumer", "example.Consumer", dependencies)));
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
