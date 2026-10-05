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

import com.google.gson.Gson;
import com.sonarsource.scanner.engine.sensor.test.fixtures.SensorContextTester;
import com.sonarsource.scanner.engine.sensor.test.fixtures.TestInputFileBuilder;
import com.sonarsource.scanner.engine.sensor.test.fixtures.TestSonarRuntime;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.event.Level;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.sensor.issue.Issue;
import org.sonar.api.issue.NoSonarFilter;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;
import org.sonar.api.utils.Version;
import org.sonar.java.SonarComponents;
import org.sonar.java.model.springcontext.BeanDefinitionHolder;
import org.sonar.java.model.springcontext.InjectionPoint;
import org.sonar.java.model.springcontext.ProfileExpression;
import org.sonar.java.model.springcontext.SpringContextGatheringModel;
import org.sonar.java.model.springcontext.SpringContextModel;
import org.sonar.java.reporting.AnalyzerMessage.TextSpan;
import org.sonar.java.telemetry.DefaultTelemetry;
import org.sonar.java.telemetry.NoOpTelemetry;
import org.sonar.java.telemetry.Telemetry;
import org.sonar.plugins.java.api.JavaResourceLocator;
import org.sonar.scanner.plugin.api.impl.config.MapSettings;
import org.sonar.scanner.plugin.api.impl.rule.ActiveRulesBuilder;
import org.sonar.scanner.plugin.api.impl.rule.NewActiveRule;
import org.sonar.scanner.plugin.api.impl.sensor.DefaultSensorDescriptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

class SpringContextModelSensorTest {

  private static final String MODULE_KEY = "module";
  private static final String MODEL_PATH = "state/context.json";
  private static final String DEFAULT_MODEL_PATH = ".sonar/spring-context-model.json";
  private static final RuleKey S9352_RULE_KEY = RuleKey.of("java", "S9352");
  private static final Gson GSON = new Gson();

  @TempDir
  Path tempDir;

  @RegisterExtension
  public LogTesterJUnit5 logTester = new LogTesterJUnit5();

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
    MapSettings settings = new MapSettings();
    settings.setProperty(SonarComponents.SONAR_CAN_SKIP_UNCHANGED_FILES_KEY, true);
    settings.setProperty(JavaSensor.SPRING_CONTEXT_MODEL_PATH_PROPERTY, MODEL_PATH);
    context.setSettings(settings);
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
    var savedModel = SpringContextModelPersistence.load(tempDir.resolve(MODEL_PATH));
    assertThat(savedModel.filesData().get(MODULE_KEY)).hasSize(4);
  }

  @Test
  void removes_unvisited_files_before_building_and_saving_the_model_on_full_analysis() {
    SensorContextTester context = configuredContext();
    context.setActiveRules(activeRulesWithS9352());
    InputFile analyzedFile = fakeInputFile(context, "Consumer.java");
    String type = "example.Service";
    var gatheringModel = new SpringContextGatheringModel();
    gatheringModel.collectBeans(MODULE_KEY, "unvisited", null, List.of(gatheredBean("oldService", type, Map.of())));
    gatheringModel.collectPackages(MODULE_KEY, "unvisited", null, Set.of("stale.package"));
    registerBean(gatheringModel, type, "currentService", analyzedFile);
    registerDependency(gatheringModel, type, "contextAware", analyzedFile);

    var telemetry = new DefaultTelemetry();
    sensor(gatheringModel, telemetry).execute(context);

    assertThat(context.allIssues()).isEmpty();
    assertThat(gatheringModel.filesData().get(MODULE_KEY)).containsOnlyKeys("currentService", "consumer");
    assertThat(telemetry.toMap())
      .containsEntry("java.spring.bean_count", "2")
      .containsEntry("java.spring.component_scan_package_count", "0");
    var savedModel = SpringContextModelPersistence.load(tempDir.resolve(MODEL_PATH));
    assertThat(savedModel.filesData().get(MODULE_KEY)).containsOnlyKeys("currentService", "consumer");
  }

  @Test
  void keeps_unvisited_files_for_incremental_and_sonarlint_analyses() {
    SensorContextTester incrementalContext = SensorContextTester.create(tempDir);
    MapSettings settings = new MapSettings();
    settings.setProperty(SonarComponents.SONAR_CAN_SKIP_UNCHANGED_FILES_KEY, true);
    settings.setProperty(JavaSensor.SPRING_CONTEXT_MODEL_PATH_PROPERTY, MODEL_PATH);
    incrementalContext.setSettings(settings);
    SensorContextTester sonarLintContext = SensorContextTester.create(tempDir)
      .setRuntime(TestSonarRuntime.forSonarLint(Version.create(6, 7)));
    sonarLintContext.setSettings(new MapSettings().setProperty(JavaSensor.SPRING_CONTEXT_MODEL_PATH_PROPERTY, MODEL_PATH));

    for (SensorContextTester context : List.of(incrementalContext, sonarLintContext)) {
      var gatheringModel = new SpringContextGatheringModel();
      gatheringModel.collectBeans(MODULE_KEY, "unvisited", null, List.of(gatheredBean("oldService", "example.Service", Map.of())));
      gatheringModel.collectPackages(MODULE_KEY, "unvisited", null, Set.of("stale.package"));

      sensor(gatheringModel, new NoOpTelemetry()).execute(context);

      assertThat(gatheringModel.filesData().get(MODULE_KEY)).containsOnlyKeys("unvisited");
      var savedModel = SpringContextModelPersistence.load(tempDir.resolve(MODEL_PATH));
      assertThat(savedModel.filesData().get(MODULE_KEY)).containsOnlyKeys("unvisited");
    }
  }

  @Test
  void reports_issues_when_saving_fails() throws IOException {
    SensorContextTester context = configuredContext();
    context.setSettings(new MapSettings().setProperty(JavaSensor.SPRING_CONTEXT_MODEL_PATH_PROPERTY, "blocked/context.json"));
    context.setActiveRules(activeRulesWithS9352());
    Files.writeString(tempDir.resolve("blocked"), "file");
    var gatheringModel = new SpringContextGatheringModel();
    InputFile inputFile = fakeInputFile(context, "UnresolvedConsumer.java");
    String type = "org.springframework.context.ApplicationContextAware";
    registerBean(gatheringModel, type, "componentOne", inputFile);
    registerBean(gatheringModel, type, "componentTwo", inputFile);
    registerDependency(gatheringModel, type, "contextAware", inputFile);

    sensor(gatheringModel, new NoOpTelemetry()).execute(context);

    assertThat(context.allIssues()).hasSize(1);
    assertThat(logTester.logs(Level.WARN)).anyMatch(message -> message.contains("Unable to save Spring context model to "));
  }

  @Test
  void keeps_unvisited_files_when_scan_mode_cannot_be_determined() {
    SensorContextTester context = spy(SensorContextTester.create(tempDir));
    doThrow(new NoSuchMethodError("canSkipUnchangedFiles is unavailable")).when(context).canSkipUnchangedFiles();
    var gatheringModel = new SpringContextGatheringModel();
    gatheringModel.collectBeans(MODULE_KEY, "unvisited", null, List.of(gatheredBean("oldService", "example.Service", Map.of())));

    sensor(gatheringModel, new NoOpTelemetry()).execute(context);

    assertThat(gatheringModel.filesData().get(MODULE_KEY)).containsOnlyKeys("unvisited");
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

  @Test
  void restores_updates_and_saves_one_model_across_module_contexts_during_incremental_analysis() throws IOException {
    Path moduleA = Files.createDirectory(tempDir.resolve("module-a"));
    Path moduleB = Files.createDirectory(tempDir.resolve("module-b"));
    Path modelPath = tempDir.resolve(MODEL_PATH);
    Files.createDirectories(modelPath.getParent());

    InputFile analyzedFile = new TestInputFileBuilder("module-a", "src/A.java").setContents("class A {}").build();
    String analyzedFileKey = analyzedFile.key();
    String untouchedFileKey = "module-b:src/B.java";
    var previousBean = bean("previous");
    var untouchedBean = bean("untouched");
    var initialModel = new SpringContextGatheringModel();
    initialModel.collectBeans("module-a", analyzedFileKey, null, List.of(previousBean));
    initialModel.collectPackages("module-a", analyzedFileKey, null, Set.of("previous.package"));
    initialModel.collectBeans("module-b", untouchedFileKey, null, List.of(untouchedBean));
    initialModel.collectPackages("module-b", untouchedFileKey, null, Set.of("untouched.package"));
    Files.writeString(modelPath, GSON.toJson(initialModel));

    var gatheringModel = new SpringContextGatheringModel();
    JavaSensor sensor = javaSensor(gatheringModel);
    sensor.execute(javaSensorContext(moduleA, MODEL_PATH, true));
    assertThat(gatheringModel.filesData()).containsOnlyKeys("module-a", "module-b");

    var updatedBean = bean("updated");
    gatheringModel.collectBeans("module-a", analyzedFileKey, analyzedFile, List.of(updatedBean));
    gatheringModel.collectPackages("module-a", analyzedFileKey, analyzedFile, Set.of("updated.package"));

    Files.writeString(modelPath, "{");
    sensor.execute(javaSensorContext(moduleB, MODEL_PATH, true));
    assertThat(SpringContextModelPersistence.modelPath(javaSensorContext(moduleB, MODEL_PATH, false), tempDir.toFile())).isEqualTo(modelPath);
    assertThat(gatheringModel.filesData().get("module-a").get(analyzedFileKey).inputFile()).isSameAs(analyzedFile);
    SpringContextModel projectModel = SpringContextModel.of(gatheringModel);
    assertThat(projectModel.getBeanDefinitionRegistry().getByName("previous")).isEmpty();
    assertThat(projectModel.getBeanDefinitionRegistry().getByName("updated")).hasSize(1);
    assertThat(projectModel.getBeanDefinitionRegistry().getByName("untouched")).hasSize(1);
    assertThat(projectModel.getProjectPackageScan().getPackagesForModule("module-b")).containsExactly("untouched.package");

    sensor(gatheringModel, new NoOpTelemetry()).execute(javaSensorContext(tempDir, MODEL_PATH, true));

    SpringContextGatheringModel saved = GSON.fromJson(Files.readString(modelPath), SpringContextGatheringModel.class);
    assertThat(saved.filesData()).containsOnlyKeys("module-a", "module-b");
    assertThat(saved.filesData().get("module-a").get(analyzedFileKey).beans()).containsExactly(updatedBean);
    assertThat(saved.filesData().get("module-a").get(analyzedFileKey).packages()).containsExactly("updated.package");
    assertThat(saved.filesData().get("module-b").get(untouchedFileKey).beans()).containsExactly(untouchedBean);
    assertThat(saved.filesData().get("module-b").get(untouchedFileKey).packages()).containsExactly("untouched.package");
  }

  @Test
  void restores_from_the_default_path_when_the_property_is_unset_or_blank() throws IOException {
    Path defaultPath = tempDir.resolve(DEFAULT_MODEL_PATH);
    Files.createDirectories(defaultPath.getParent());
    var previousModel = new SpringContextGatheringModel();
    previousModel.collectBeans("previous-module", "previous-file", null, List.of(bean("previous")));
    Files.writeString(defaultPath, GSON.toJson(previousModel));

    for (SensorContextTester analysisContext : List.of(javaSensorContext(tempDir), javaSensorContext(tempDir, " ", false))) {
      var gatheringModel = new SpringContextGatheringModel();
      javaSensor(gatheringModel).execute(analysisContext);
      assertThat(gatheringModel.isRestored()).isTrue();
      assertThat(gatheringModel.filesData().get("previous-module")).containsKey("previous-file");
    }
  }

  @Test
  void saves_to_the_default_path_when_the_property_is_unset_or_blank() {
    Path defaultPath = tempDir.resolve(DEFAULT_MODEL_PATH);
    List<SensorContextTester> contexts = List.of(javaSensorContext(tempDir), javaSensorContext(tempDir, " ", false));

    for (int i = 0; i < contexts.size(); i++) {
      SensorContextTester analysisContext = contexts.get(i);
      String beanName = "component" + i;
      var gatheringModel = new SpringContextGatheringModel();
      registerBean(gatheringModel, "example.Service", beanName, fakeInputFile(analysisContext, "Component.java"));

      sensor(gatheringModel, new NoOpTelemetry()).execute(analysisContext);

      assertThat(defaultPath).exists();
      assertThat(SpringContextModelPersistence.load(defaultPath).filesData().get(MODULE_KEY)).containsOnlyKeys(beanName);
    }
  }

  @Test
  void resolves_the_configured_path_from_the_root_project() throws IOException {
    Path module = Files.createDirectory(tempDir.resolve("module-a"));
    SensorContextTester moduleContext = javaSensorContext(module, MODEL_PATH, false);
    ProjectDefinition rootProject = ProjectDefinition.create();
    rootProject.setBaseDir(tempDir.toFile());
    ProjectDefinition moduleProject = ProjectDefinition.create();
    moduleProject.setBaseDir(module.toFile());
    rootProject.addSubProject(moduleProject);
    SonarComponents components = new SonarComponents(null, moduleContext.fileSystem(), null, null, null, null, moduleProject);

    assertThat(components.projectLevelBaseDir()).isEqualTo(tempDir.toFile());
    assertThat(SpringContextModelPersistence.modelPath(moduleContext, components.projectLevelBaseDir()))
      .isEqualTo(tempDir.resolve(MODEL_PATH));
    moduleContext.setSettings(new MapSettings());
    assertThat(SpringContextModelPersistence.modelPath(moduleContext, components.projectLevelBaseDir()))
      .isEqualTo(tempDir.resolve(DEFAULT_MODEL_PATH));
  }

  @Test
  void preserves_an_absolute_configured_path() {
    Path absolutePath = tempDir.resolve(MODEL_PATH);

    assertThat(SpringContextModelPersistence.modelPath(javaSensorContext(tempDir, absolutePath.toString(), false), tempDir.toFile()))
      .isEqualTo(absolutePath);
  }

  @Test
  void rejects_invalid_json_without_overwriting_it() throws IOException {
    Path modelPath = tempDir.resolve(MODEL_PATH);
    Files.createDirectories(modelPath.getParent());
    Files.writeString(modelPath, "{");

    assertThatThrownBy(() -> SpringContextModelPersistence.load(modelPath))
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining(modelPath.toString());
    assertThat(Files.readString(modelPath)).isEqualTo("{");
  }

  @Test
  void saves_a_new_model_in_a_new_directory() {
    Path modelPath = tempDir.resolve(MODEL_PATH);

    SpringContextModelPersistence.save(modelPath, new SpringContextGatheringModel());

    assertThat(modelPath).exists();
    assertThat(SpringContextModelPersistence.load(modelPath).filesData()).isEmpty();
  }

  private SpringContextModelSensor sensor(SpringContextGatheringModel gatheringModel, Telemetry telemetry) {
    return new SpringContextModelSensor(telemetry, gatheringModel);
  }

  private SensorContextTester configuredContext() {
    SensorContextTester context = SensorContextTester.create(tempDir);
    context.setSettings(new MapSettings().setProperty(JavaSensor.SPRING_CONTEXT_MODEL_PATH_PROPERTY, MODEL_PATH));
    return context;
  }

  private JavaSensor javaSensor(SpringContextGatheringModel gatheringModel) {
    SonarComponents components = mock(SonarComponents.class);
    when(components.projectLevelBaseDir()).thenReturn(tempDir.toFile());
    when(components.mainChecks()).thenReturn(List.of());
    when(components.testChecks()).thenReturn(List.of());
    when(components.jspChecks()).thenReturn(List.of());
    when(components.getJavaClasspath()).thenReturn(List.of());
    when(components.getJavaTestClasspath()).thenReturn(List.of());
    when(components.getJspClasspath()).thenReturn(List.of());
    return new JavaSensor(components, mock(JavaResourceLocator.class), mock(NoSonarFilter.class), null, new NoOpTelemetry(), gatheringModel);
  }

  private static SensorContextTester javaSensorContext(Path baseDirectory) {
    SensorContextTester context = SensorContextTester.create(baseDirectory);
    context.fileSystem().setWorkDir(baseDirectory);
    return context;
  }

  private static SensorContextTester javaSensorContext(Path baseDirectory, String configuredPath, boolean incremental) {
    SensorContextTester context = javaSensorContext(baseDirectory);
    MapSettings settings = new MapSettings();
    settings.setProperty(JavaSensor.SPRING_CONTEXT_MODEL_PATH_PROPERTY, configuredPath);
    if (incremental) {
      settings.setProperty(SonarComponents.SONAR_CAN_SKIP_UNCHANGED_FILES_KEY, true);
    }
    context.setSettings(settings);
    return context;
  }

  private static BeanDefinitionHolder.InputFileData bean(String name) {
    return new BeanDefinitionHolder.InputFileData(name, "example.Bean", "example", new TextSpan(1, 0, 1, 4),
      false, ProfileExpression.UNCONDITIONAL, null, Map.of(), Set.of("example.Bean"));
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
