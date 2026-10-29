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
import java.util.Set;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.java.SonarComponents;
import org.sonar.java.TestUtils;
import org.sonar.java.model.JParserTestUtils;
import org.sonar.java.model.VisitorsBridge;
import org.sonar.java.test.TestResourceScope;
import org.sonar.java.test.classpath.TestClasspathUtils;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.plugins.java.api.caching.CacheContext;
import org.sonar.plugins.java.api.caching.JavaReadCache;
import org.sonar.plugins.java.api.caching.JavaWriteCache;
import org.sonar.scanner.plugin.api.impl.config.MapSettings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@TestResourceScope.Scoped
abstract class SpringContextGathererTest {

  protected SpringContextModelGatherer gatherer;
  protected SpringContextModel model;

  /**
   * The sample files of these tests live under {@code src/test/files/}, a path that
   * {@link org.sonar.java.utils.JavaFileTypeClassifier}'s heuristic would read as test scope, which would stop the
   * gatherers from collecting anything. Declaring {@code sonar.tests} disables that heuristic, as it does in a real
   * analysis where the scanner already assigns {@link InputFile.Type#TEST} itself.
   */
  protected static SensorContextTester mainScopeSensorContext() {
    return SensorContextTester.create(new File("")).setSettings(new MapSettings().setProperty("sonar.tests", "src/test/java"));
  }

  protected void scan(String... filePaths) {
    scan(TestClasspathUtils.DEFAULT_MODULE.getClassPath(), mainScopeSensorContext(), filePaths);
  }

  protected void scan(List<File> classpath, String... filePaths) {
    scan(classpath, mainScopeSensorContext(), filePaths);
  }

  protected void scan(SensorContextTester ctx, String... filePaths) {
    scan(TestClasspathUtils.DEFAULT_MODULE.getClassPath(), ctx, filePaths);
  }

  /**
   * Scans the given files as the scanner would when the build tool places them in the test source set.
   */
  protected void scanAsTestScope(String... filePaths) {
    scan(TestClasspathUtils.DEFAULT_MODULE.getClassPath(), mainScopeSensorContext(), InputFile.Type.TEST, filePaths);
  }

  protected void scan(List<File> classpath, SensorContextTester ctx, String... filePaths) {
    scan(classpath, ctx, InputFile.Type.MAIN, filePaths);
  }

  protected void scan(List<File> classpath, SensorContextTester ctx, InputFile.Type type, String... filePaths) {
    var sonarComponents = new SonarComponents(null, null, null, null, null, null);
    sonarComponents.setSensorContext(ctx);
    VisitorsBridge visitorsBridge = new VisitorsBridge(List.of((JavaCheck) gatherer), classpath, sonarComponents);
    for (String filePath : filePaths) {
      File file = new File(filePath);
      var compilationUnit = JParserTestUtils.parse(file, classpath);
      visitorsBridge.setCurrentFile(TestUtils.inputFile("", file, type));
      visitorsBridge.visitFile(compilationUnit, false);
    }
    visitorsBridge.endOfAnalysis();
    model = SpringContextModel.of(gatherer.springContextGatheringModel);
  }

  protected static CacheContext mockCacheContext(JavaReadCache readCache, JavaWriteCache writeCache) {
    CacheContext cacheContext = mock(CacheContext.class);
    when(cacheContext.isCacheEnabled()).thenReturn(true);
    when(cacheContext.getReadCache()).thenReturn(readCache);
    when(cacheContext.getWriteCache()).thenReturn(writeCache);
    return cacheContext;
  }

  protected static void assertInjectionPoint(Set<InjectionPoint> injectionPoints, String expectedName,
    InputFile expectedInputFile, int expectedLine) {
    assertThat(injectionPoints).hasSize(1);
    var point = injectionPoints.iterator().next();
    assertThat(point.name()).isEqualTo(expectedName);
    assertThat(point.location().inputFile()).isEqualTo(expectedInputFile);
    assertThat(point.location().mainLocation().startLine).isEqualTo(expectedLine);
  }
}
