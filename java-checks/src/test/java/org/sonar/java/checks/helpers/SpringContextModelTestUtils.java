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
package org.sonar.java.checks.helpers;

import com.sonarsource.scanner.engine.sensor.test.fixtures.SensorContextTester;
import com.sonarsource.scanner.engine.sensor.test.fixtures.TestInputFileBuilder;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.function.UnaryOperator;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.java.SonarComponents;
import org.sonar.java.checks.verifier.TestUtils;
import org.sonar.java.model.JParser;
import org.sonar.java.model.JParserConfig;
import org.sonar.java.model.VisitorsBridge;
import org.sonar.java.model.springcontext.BeanDefinitionGatherer;
import org.sonar.java.model.springcontext.BeanDefinitionHolder;
import org.sonar.java.model.springcontext.BeanDefinitionKind;
import org.sonar.java.model.springcontext.BeanLocation;
import org.sonar.java.model.springcontext.ComponentScanPackageGatherer;
import org.sonar.java.model.springcontext.ProfileExpression;
import org.sonar.java.model.springcontext.SpringContextGatheringModel;
import org.sonar.java.model.springcontext.SpringContextModel;
import org.sonar.java.reporting.AnalyzerMessage;
import org.sonar.java.telemetry.NoOpTelemetry;
import org.sonar.java.test.classpath.TestClasspathUtils;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;

/**
 * Builds a {@link SpringContextModel} for the tests of rules that consume one instead of visiting an AST, either
 * by registering its contents directly or by running the gatherers over real sample files.
 *
 * <p>Registering directly keeps a test focused on the rule's own logic and can express states no single set of
 * parsed files can, such as a bean reached by a scan declared in another module. Running the gatherers instead
 * covers the seam between what they collect and what the rule expects of it. Both constructions live here so that
 * a new component on {@link BeanDefinitionHolder} or {@link BeanLocation} only has to be accounted for once.
 */
public final class SpringContextModelTestUtils {

  private SpringContextModelTestUtils() {
  }

  /**
   * An input file that exists only as a path: enough to anchor a {@link BeanLocation}, with no contents to parse.
   */
  public static InputFile dummyInputFile(String path) {
    return new TestInputFileBuilder("", path)
      .setLanguage("java")
      .setType(InputFile.Type.MAIN)
      .build();
  }

  public static BeanLocation beanLocation(String path, int line) {
    return beanLocation(dummyInputFile(path), line);
  }

  public static BeanLocation beanLocation(InputFile file, int line) {
    return new BeanLocation(file, new AnalyzerMessage.TextSpan(line));
  }

  /**
   * Registers a bean declared by a stereotype annotation, the kind most rules care about.
   */
  public static BeanDefinitionHolder registerBean(SpringContextModel model, String beanName, String type, String module, String beanPackage,
    BeanLocation location) {
    return registerBean(model, beanName, type, module, beanPackage, BeanDefinitionKind.STEREOTYPE, location, UnaryOperator.identity());
  }

  public static BeanDefinitionHolder registerBean(SpringContextModel model, String beanName, String type, String module, String beanPackage,
    BeanDefinitionKind kind, BeanLocation location) {
    return registerBean(model, beanName, type, module, beanPackage, kind, location, UnaryOperator.identity());
  }

  /**
   * Registers a stereotype bean whose builder the customizer completes, for the metadata that decides autowiring:
   * {@code @Qualifier}, {@code @Primary}, {@code @Profile}.
   */
  public static BeanDefinitionHolder registerBean(SpringContextModel model, String beanName, String type, String module, String beanPackage,
    BeanLocation location, UnaryOperator<BeanDefinitionHolder.Builder> customizer) {
    return registerBean(model, beanName, type, module, beanPackage, BeanDefinitionKind.STEREOTYPE, location, customizer);
  }

  public static BeanDefinitionHolder registerBean(SpringContextModel model, String beanName, String type, String module, String beanPackage,
    BeanDefinitionKind kind, BeanLocation location, UnaryOperator<BeanDefinitionHolder.Builder> customizer) {
    BeanDefinitionHolder bean = customizer.apply(new BeanDefinitionHolder.Builder(type, kind, module, beanPackage, location)).build();
    model.getBeanDefinitionRegistry().addBeanDefinition(beanName, bean);
    return bean;
  }

  /**
   * Indexes a registered bean by its type. Only rules that resolve a dependency to its candidates read that index,
   * so registering a bean does not do this on its own.
   */
  public static void indexBeanByType(SpringContextModel model, String beanName, String type, String module, String beanPackage) {
    model.getTypeToBeansIndex().addBeanForType(type, beanName, module, beanPackage);
  }

  public static void registerInjectionPoint(SpringContextModel model, String type, String fieldName, String module,
    ProfileExpression consumerProfile, BeanLocation location) {
    model.getTypeToDependenciesIndex().addDependencyForType(type, fieldName, module, consumerProfile, location, false);
  }

  /**
   * Runs the Spring context gatherers over the given sample files into a single, freshly built model, mirroring how
   * {@code JavaSensor} drives them during a real analysis, without needing java-frontend's test-only scanning
   * helpers. All files land in one module, so a model built this way cannot express a cross-module scenario.
   *
   * @param basePath          directory holding the sample files, relative to the main test sources root
   * @param relativeFilePaths the sample files to analyze, relative to {@code basePath}
   */
  public static SpringContextModel buildModel(String basePath, String... relativeFilePaths) {
    return buildModel(Arrays.stream(relativeFilePaths)
      .map(relativeFilePath -> TestUtils.mainCodeSourcesPath(basePath + relativeFilePath))
      .toList());
  }

  private static SpringContextModel buildModel(List<String> filePaths) {
    List<File> classpath = TestClasspathUtils.DEFAULT_MODULE.getClassPath();
    SonarComponents sonarComponents = new SonarComponents(null, null, null, null, null, null);
    sonarComponents.setSensorContext(SensorContextTester.create(new File("")));
    var gatheringModel = new SpringContextGatheringModel();
    List<JavaCheck> gatherers = List.of(
      new BeanDefinitionGatherer(gatheringModel, new NoOpTelemetry()),
      new ComponentScanPackageGatherer(gatheringModel, new NoOpTelemetry()));
    VisitorsBridge visitorsBridge = new VisitorsBridge(gatherers, classpath, sonarComponents);
    for (String filePath : filePaths) {
      File file = new File(filePath);
      CompilationUnitTree compilationUnit = parse(file, classpath);
      visitorsBridge.setCurrentFile(inputFile(file));
      visitorsBridge.visitFile(compilationUnit, false);
    }
    visitorsBridge.endOfAnalysis();
    return SpringContextModel.of(gatheringModel);
  }

  private static InputFile inputFile(File file) {
    try {
      return new TestInputFileBuilder("", file.getParentFile(), file)
        .setContents(Files.readString(file.toPath(), StandardCharsets.UTF_8))
        .setCharset(StandardCharsets.UTF_8)
        .setLanguage("java")
        .setType(InputFile.Type.MAIN)
        .build();
    } catch (IOException e) {
      throw new IllegalStateException("Unable to read file '" + file.getAbsolutePath() + "'", e);
    }
  }

  private static CompilationUnitTree parse(File file, List<File> classpath) {
    String source;
    try {
      source = Files.readString(file.toPath(), StandardCharsets.UTF_8);
    } catch (Exception e) {
      throw new IllegalStateException("Unable to read file '" + file.getAbsolutePath() + "'", e);
    }
    JavaVersion version = JParserConfig.MAXIMUM_SUPPORTED_JAVA_VERSION;
    return JParser.parse(JParserConfig.Mode.FILE_BY_FILE.create(version, classpath).astParser(), version.toString(), file.getName(), source);
  }
}
