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
package org.sonar.java;

import com.sonarsource.scanner.engine.sensor.test.fixtures.SensorContextTester;
import com.sonarsource.scanner.engine.sensor.test.fixtures.TestInputFileBuilder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.batch.sensor.cache.ReadCache;
import org.sonar.api.batch.sensor.cache.WriteCache;
import org.sonar.api.issue.NoSonarFilter;
import org.sonar.api.measures.FileLinesContext;
import org.sonar.api.measures.FileLinesContextFactory;
import org.sonar.java.classpath.ClasspathForMain;
import org.sonar.java.classpath.ClasspathForTest;
import org.sonar.java.classpath.ClasspathProperties;
import org.sonar.java.model.JavaVersionImpl;
import org.sonar.java.telemetry.NoOpTelemetry;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.JavaResourceLocator;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.scanner.extension.PropertyDefinitions;
import org.sonar.scanner.plugin.api.impl.config.MapSettings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SourcePathAnalysisTest {

  @TempDir
  Path temp;

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void source_paths_resolve_main_and_test_dependencies_without_analyzing_them(boolean fileByFile) throws Exception {
    writeSource("a/a/Api.java", "package a; public class Api { public static String value() { return null; } }");
    writeSource("testDependency/t/TestOnly.java", "package t; public class TestOnly {}");
    Path main = writeSource("b/B.java", "class B { String value() { return a.Api.value(); } void hidden(t.TestOnly value) {} }");
    Path secondMain = writeSource("b/C.java", "class C { String value() { return a.Api.value(); } void hidden(t.TestOnly value) {} }");
    Path test = writeSource("b/BTest.java", "class BTest { String value() { return a.Api.value(); } void visible(t.TestOnly value) {} }");

    var settings = settings();
    settings.setProperty(ClasspathProperties.SONAR_JAVA_SOURCEPATH, new String[] {"b", "a"});
    settings.setProperty(ClasspathProperties.SONAR_JAVA_TEST_SOURCEPATH, new String[] {"b", "a", "testDependency"});
    settings.setProperty(SonarComponents.SONAR_FILE_BY_FILE, fileByFile);
    settings.setProperty(SonarComponents.SONAR_BATCH_SIZE_KEY, 0);

    var context = SensorContextTester.create(temp.toFile()).setSettings(settings);
    InputFile mainFile = addInput(context, main, InputFile.Type.MAIN);
    InputFile secondMainFile = addInput(context, secondMain, InputFile.Type.MAIN);
    InputFile testFile = addInput(context, test, InputFile.Type.TEST);

    List<String> scanned = new ArrayList<>();
    JavaFileScanner check = new SemanticCheck(scannerContext -> {
      scanned.add(scannerContext.getInputFile().filename());
      ClassTree type = (ClassTree) scannerContext.getTree().types().getFirst();
      MethodTree method = (MethodTree) type.members().getFirst();
      var invocation = (MethodInvocationTree) ((ReturnStatementTree) method.block().body().getFirst()).expression();
      assertThat(invocation.methodSymbol().isUnknown()).isFalse();
      assertThat(invocation.symbolType().fullyQualifiedName()).isEqualTo("java.lang.String");
      MethodTree visibility = (MethodTree) type.members().get(1);
      assertThat(visibility.symbol().parameterTypes().getFirst().isUnknown()).isEqualTo(!type.simpleName().name().endsWith("Test"));
    });

    var components = components(context, settings);
    components.testChecks().add(check);
    frontend(context, components, check).scan(List.of(mainFile, secondMainFile), List.of(testFile), List.of());
    assertThat(scanned).containsExactly("B.java", "C.java", "BTest.java");
    assertThat(context.measure(mainFile.key(), "classes").value()).isEqualTo(1);
    assertThat(context.measure(secondMainFile.key(), "classes").value()).isEqualTo(1);
    assertThat(context.allIssues()).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void changing_dependency_sources_reanalyzes_unchanged_consumers(boolean testSourcePath) throws Exception {
    Path dependency = writeSource("a/a/Api.java", "package a; public class Api { public static String value() { return null; } }");
    Path consumer = writeSource("b/B.java", "class B { Object value() { return a.Api.value(); } }");

    var settings = settings();
    settings.setProperty(testSourcePath ? ClasspathProperties.SONAR_JAVA_TEST_SOURCEPATH : ClasspathProperties.SONAR_JAVA_SOURCEPATH,
      new String[] {"b", "a"});
    settings.setProperty(SonarComponents.SONAR_CAN_SKIP_UNCHANGED_FILES_KEY, true);

    List<String> returnTypes = new ArrayList<>();
    JavaFileScanner check = new SemanticCheck(scannerContext -> {
      ClassTree type = (ClassTree) scannerContext.getTree().types().getFirst();
      var method = (MethodTree) type.members().getFirst();
      var invocation = (MethodInvocationTree) ((ReturnStatementTree) method.block().body().getFirst()).expression();
      returnTypes.add(invocation.symbolType().fullyQualifiedName());
    });

    for (String returnType : List.of("String", "Integer")) {
      Files.writeString(dependency, "package a; public class Api { public static " + returnType + " value() { return null; } }");
      var context = SensorContextTester.create(temp.toFile()).setSettings(settings);
      context.setCacheEnabled(true);
      context.setPreviousCache(mock(ReadCache.class));
      context.setNextCache(mock(WriteCache.class));
      InputFile input = addInput(context, consumer, testSourcePath ? InputFile.Type.TEST : InputFile.Type.MAIN);
      var components = components(context, settings);
      assertThat(components.canSkipUnchangedFiles()).isFalse();
      components.testChecks().add(check);
      List<InputFile> mainFiles = testSourcePath ? List.of() : List.of(input);
      List<InputFile> testFiles = testSourcePath ? List.of(input) : List.of();
      frontend(context, components, check).scan(mainFiles, testFiles, List.of());
    }
    assertThat(returnTypes).containsExactly("java.lang.String", "java.lang.Integer");
  }

  private Path writeSource(String relativePath, String source) throws IOException {
    Path file = temp.resolve(relativePath);
    Files.createDirectories(file.getParent());
    return Files.writeString(file, source);
  }

  private InputFile addInput(SensorContextTester context, Path path, InputFile.Type type) throws IOException {
    InputFile file = new TestInputFileBuilder("b", temp.toFile(), path.toFile())
      .setContents(Files.readString(path))
      .setCharset(StandardCharsets.UTF_8)
      .setLanguage("java")
      .setType(type)
      .setStatus(InputFile.Status.SAME)
      .build();
    context.fileSystem().add(file);
    return file;
  }

  private static SonarComponents components(SensorContextTester context, MapSettings settings) {
    var lines = mock(FileLinesContextFactory.class);
    var fileLinesContext = mock(FileLinesContext.class);
    when(lines.createFor(any(InputFile.class))).thenReturn(fileLinesContext);
    var components = new SonarComponents(lines, context.fileSystem(),
      new ClasspathForMain(settings.asConfig(), context.fileSystem()),
      new ClasspathForTest(settings.asConfig(), context.fileSystem()), mock(CheckFactory.class), mock(ActiveRules.class));
    components.setSensorContext(context);
    return components;
  }

  private static JavaFrontend frontend(SensorContextTester context, SonarComponents components, JavaFileScanner check) {
    return new JavaFrontend(new JavaVersionImpl(17), components, new Measurer(context, mock(NoSonarFilter.class)),
      new NoOpTelemetry(), mock(JavaResourceLocator.class), null, check);
  }

  private static MapSettings settings() {
    var definitions = new PropertyDefinitions();
    ClasspathProperties.getProperties().forEach(definitions::addComponent);
    return new MapSettings(definitions).setProperty(SonarComponents.FAIL_ON_EXCEPTION_KEY, true);
  }

  private record SemanticCheck(Consumer<JavaFileScannerContext> action) implements JavaFileScanner {

    @Override
      public void scanFile(JavaFileScannerContext context) {
        action.accept(context);
      }
    }

}
