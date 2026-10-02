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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.issue.NoSonarFilter;
import org.sonar.java.SonarComponents;
import org.sonar.java.model.springcontext.BeanDefinitionHolder;
import org.sonar.java.model.springcontext.ProfileExpression;
import org.sonar.java.model.springcontext.SpringContextGatheringModel;
import org.sonar.java.model.springcontext.SpringContextModel;
import org.sonar.java.reporting.AnalyzerMessage.TextSpan;
import org.sonar.java.telemetry.NoOpTelemetry;
import org.sonar.plugins.java.api.JavaResourceLocator;
import org.sonar.scanner.plugin.api.impl.config.MapSettings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SpringContextGatheringModelTest {

  private static final String CONTEXT_PATH = "state/context.json";
  private static final Gson GSON = new Gson();

  @TempDir
  Path root;

  @Test
  void restores_updates_and_saves_one_model_across_module_contexts() throws IOException {
    Path moduleA = Files.createDirectory(root.resolve("module-a"));
    Path moduleB = Files.createDirectory(root.resolve("module-b"));
    Path modelPath = root.resolve(CONTEXT_PATH);
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
    JavaSensor sensor = sensor(gatheringModel);
    sensor.execute(context(moduleA, CONTEXT_PATH));
    assertThat(gatheringModel.filesData()).containsOnlyKeys("module-a", "module-b");

    var updatedBean = bean("updated");
    gatheringModel.collectBeans("module-a", analyzedFileKey, analyzedFile, List.of(updatedBean));
    gatheringModel.collectPackages("module-a", analyzedFileKey, analyzedFile, Set.of("updated.package"));

    Files.writeString(modelPath, "{");
    sensor.execute(context(moduleB, CONTEXT_PATH));
    assertThat(JavaSensor.springContextGatheringModelPath(context(moduleB, CONTEXT_PATH), root.toFile())).isEqualTo(modelPath);
    assertThat(gatheringModel.filesData().get("module-a").get(analyzedFileKey).inputFile()).isSameAs(analyzedFile);
    SpringContextModel projectModel = SpringContextModel.of(gatheringModel);
    assertThat(projectModel.getBeanDefinitionRegistry().getByName("previous")).isEmpty();
    assertThat(projectModel.getBeanDefinitionRegistry().getByName("updated")).hasSize(1);
    assertThat(projectModel.getBeanDefinitionRegistry().getByName("untouched")).hasSize(1);
    assertThat(projectModel.getProjectPackageScan().getPackagesForModule("module-b")).containsExactly("untouched.package");

    new SpringContextModelSensor(new NoOpTelemetry(), gatheringModel).execute(context(root, CONTEXT_PATH));

    SpringContextGatheringModel saved = GSON.fromJson(Files.readString(modelPath), SpringContextGatheringModel.class);
    assertThat(saved.filesData()).containsOnlyKeys("module-a", "module-b");
    assertThat(saved.filesData().get("module-a").get(analyzedFileKey).beans()).containsExactly(updatedBean);
    assertThat(saved.filesData().get("module-a").get(analyzedFileKey).packages()).containsExactly("updated.package");
    assertThat(saved.filesData().get("module-b").get(untouchedFileKey).beans()).containsExactly(untouchedBean);
    assertThat(saved.filesData().get("module-b").get(untouchedFileKey).packages()).containsExactly("untouched.package");
  }

  @Test
  void resolves_the_default_path_from_the_root_project() throws IOException {
    Path module = Files.createDirectory(root.resolve("module-a"));

    assertThat(JavaSensor.springContextGatheringModelPath(SensorContextTester.create(module), components().projectLevelBaseDir()))
      .isEqualTo(root.resolve(JavaSensor.DEFAULT_SPRING_CONTEXT_MODEL_PATH));
  }

  @Test
  void rejects_invalid_json_without_overwriting_it() throws IOException {
    Path modelPath = root.resolve(CONTEXT_PATH);
    Files.createDirectories(modelPath.getParent());
    Files.writeString(modelPath, "{");

    assertThatThrownBy(() -> JavaSensor.loadSpringContextGatheringModel(modelPath))
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining(modelPath.toString());
    assertThat(Files.readString(modelPath)).isEqualTo("{");
  }

  @Test
  void saves_a_new_model_in_a_new_directory() {
    Path modelPath = root.resolve(CONTEXT_PATH);

    SpringContextModelSensor.saveSpringContextGatheringModel(modelPath, new SpringContextGatheringModel());

    assertThat(modelPath).exists();
    assertThat(JavaSensor.loadSpringContextGatheringModel(modelPath).filesData()).isEmpty();
  }

  private SonarComponents components() {
    return new SonarComponents(null, SensorContextTester.create(root).fileSystem(), null, null, null, null);
  }

  private JavaSensor sensor(SpringContextGatheringModel gatheringModel) {
    SonarComponents components = mock(SonarComponents.class);
    when(components.projectLevelBaseDir()).thenReturn(root.toFile());
    when(components.mainChecks()).thenReturn(List.of());
    when(components.testChecks()).thenReturn(List.of());
    when(components.jspChecks()).thenReturn(List.of());
    when(components.getJavaClasspath()).thenReturn(List.of());
    when(components.getJavaTestClasspath()).thenReturn(List.of());
    when(components.getJspClasspath()).thenReturn(List.of());
    return new JavaSensor(components, mock(JavaResourceLocator.class), mock(NoSonarFilter.class), null, new NoOpTelemetry(), gatheringModel);
  }

  private static SensorContextTester context(Path baseDirectory, String configuredPath) {
    SensorContextTester context = SensorContextTester.create(baseDirectory);
    context.fileSystem().setWorkDir(baseDirectory);
    MapSettings settings = new MapSettings();
    settings.setProperty(JavaSensor.SPRING_CONTEXT_MODEL_PATH_PROPERTY, configuredPath);
    context.setSettings(settings);
    return context;
  }

  private static BeanDefinitionHolder.InputFileData bean(String name) {
    return new BeanDefinitionHolder.InputFileData(name, "example.Bean", "example", new TextSpan(1, 0, 1, 4),
      false, ProfileExpression.UNCONDITIONAL, null, Map.of(), Set.of("example.Bean"));
  }
}
