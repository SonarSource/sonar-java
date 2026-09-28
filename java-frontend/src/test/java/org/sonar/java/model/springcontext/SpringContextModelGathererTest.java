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

import com.sonarsource.scanner.engine.sensor.test.fixtures.SensorContextTester;
import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.LongSupplier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.java.SonarComponents;
import org.sonar.java.TestUtils;
import org.sonar.java.model.JParserTestUtils;
import org.sonar.java.model.VisitorsBridge;
import org.sonar.java.telemetry.DefaultTelemetry;
import org.sonar.java.telemetry.NoOpTelemetry;
import org.sonar.java.telemetry.Telemetry;
import org.sonar.java.test.classpath.TestClasspathUtils;
import org.sonar.plugins.java.api.InputFileScannerContext;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.plugins.java.api.ModuleScannerContext;
import org.sonar.plugins.java.api.Version;
import org.sonar.plugins.java.api.tree.Tree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class SpringContextModelGathererTest {

  private static final String GATHERING_TIME_KEY = "java.spring.context_model_gathering_time_ms";
  private static final String SIMPLE_CLASS = "src/test/files/model/SimpleClass.java";
  private static final List<File> CLASSPATH = TestClasspathUtils.DEFAULT_MODULE.getClassPath();
  private static final long ONE_MILLISECOND_IN_NANOS = 1_000_000L;
  private static final long SUB_MILLISECOND_IN_NANOS = 400_000L;

  /**
   * {@code SimpleClass.java} holds a single class, so scanning it goes through three measured phases:
   * one {@code visitNode}, one {@code leaveFile} and one {@code endOfAnalysis}.
   */
  private static final int PHASES_OF_A_PARSED_FILE = 3;

  private final SpringContextModel model = new SpringContextModel();

  @Test
  void gathering_time_is_reported() {
    var telemetry = new DefaultTelemetry();
    scanFile(SIMPLE_CLASS, new SampleGatherer(telemetry), CLASSPATH);
    assertThat(model.getTypeToBeansIndex().getNamesForType("com.example.MyService", "", Set.of())).containsExactly("myServiceBean");
    assertThat(telemetry.toMap().get(GATHERING_TIME_KEY)).matches("\\d+");
  }

  @Test
  void every_phase_of_a_parsed_file_is_measured() {
    var telemetry = new DefaultTelemetry();
    scanFile(SIMPLE_CLASS, new SampleGatherer(telemetry, new IncrementingNanoTime(ONE_MILLISECOND_IN_NANOS)), CLASSPATH);
    assertThat(telemetry.toMap()).containsEntry(GATHERING_TIME_KEY, String.valueOf(PHASES_OF_A_PARSED_FILE));
  }

  @Test
  void restoring_a_file_from_the_cache_is_measured() {
    var telemetry = new DefaultTelemetry();
    var gatherer = new SampleGatherer(telemetry, new IncrementingNanoTime(ONE_MILLISECOND_IN_NANOS));
    gatherer.scanWithoutParsing(mock(InputFileScannerContext.class));
    bridgeFor(gatherer, CLASSPATH).endOfAnalysis();
    assertThat(telemetry.toMap()).containsEntry(GATHERING_TIME_KEY, "2");
  }

  /**
   * Each of the three phases lasts 0.4 ms and would be truncated to 0 ms if converted on its own,
   * but together they reach 1.2 ms.
   */
  @Test
  void sub_millisecond_phases_are_summed_before_being_converted_to_millis() {
    var telemetry = new DefaultTelemetry();
    scanFile(SIMPLE_CLASS, new SampleGatherer(telemetry, new IncrementingNanoTime(SUB_MILLISECOND_IN_NANOS)), CLASSPATH);
    assertThat(telemetry.toMap()).containsEntry(GATHERING_TIME_KEY, "1");
  }

  // ---- isCompatibleWithDependencies -----------------------------------------

  @ParameterizedTest
  @ValueSource(strings = {"spring-context", "spring-beans", "spring-boot-starter", "spring-boot-starter-web"})
  void isCompatibleWithDependencies_true_when_spring_dependency_is_present(String dependency) {
    assertThat(new SampleGatherer(new NoOpTelemetry()).isCompatibleWithDependencies(finderFor(dependency))).isTrue();
  }

  @Test
  void isCompatibleWithDependencies_false_when_no_spring_dependency_is_present() {
    assertThat(new SampleGatherer(new NoOpTelemetry()).isCompatibleWithDependencies(finderFor())).isFalse();
  }

  // ---- Helpers --------------------------------------------------------------

  private void scanFile(String filePath, JavaCheck check, List<File> classpath) {
    File file = new File(filePath);
    InputFile inputFile = TestUtils.inputFile(file);
    var compilationUnit = JParserTestUtils.parse(file, classpath);

    VisitorsBridge visitorsBridge = bridgeFor(check, classpath);
    visitorsBridge.setCurrentFile(inputFile);
    visitorsBridge.visitFile(compilationUnit, false);
    visitorsBridge.endOfAnalysis();
  }

  private VisitorsBridge bridgeFor(JavaCheck check, List<File> classpath) {
    SensorContextTester sensorContextTester = SensorContextTester.create(new File(""));
    var sonarComponents = new SonarComponents(null, null, null, null, null, null);
    sonarComponents.setSensorContext(sensorContextTester);
    sonarComponents.setSpringContextModel(model);
    return new VisitorsBridge(List.of(check), classpath, sonarComponents);
  }

  private static Function<String, Optional<Version>> finderFor(String... presentDeps) {
    var available = Set.of(presentDeps);
    return name -> available.contains(name) ? Optional.of(mock(Version.class)) : Optional.empty();
  }

  static class SampleGatherer extends SpringContextModelGatherer {

    SampleGatherer(Telemetry telemetry) {
      super(telemetry);
    }

    SampleGatherer(Telemetry telemetry, LongSupplier nanoTime) {
      super(telemetry, nanoTime);
    }

    @Override
    public void gatherSpringContextData(ModuleScannerContext context, SpringContextModel springContextModel) {
      springContextModel.getTypeToBeansIndex().addBeanForType("com.example.MyService", "myServiceBean", context.getModuleKey(), "com.example");
    }

    @Override
    public List<Tree.Kind> nodesToVisit() {
      return List.of(Tree.Kind.CLASS);
    }
  }

  /**
   * Fake clock advancing by a fixed step on every read. Since a measured phase reads the clock exactly twice,
   * every phase lasts one step, and the reported duration is the number of measured phases times the step.
   */
  static class IncrementingNanoTime implements LongSupplier {

    private final long stepNanos;
    private long value;

    IncrementingNanoTime(long stepNanos) {
      this.stepNanos = stepNanos;
    }

    @Override
    public long getAsLong() {
      value += stepNanos;
      return value;
    }
  }

}
