/*
 * SonarQube Java
 * Copyright (C) 2013-2024 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.java.it;

import com.sonar.sslr.api.RecognitionException;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.config.Configuration;
import org.sonar.java.SonarComponents;
import org.sonar.java.ast.JavaAstScanner;
import org.sonar.java.checks.verifier.internal.InternalSensorContext;
import org.sonar.java.classpath.ClasspathForMain;
import org.sonar.java.classpath.ClasspathForTest;
import org.sonar.java.model.JavaVersionImpl;
import org.sonar.java.reporting.JavaQuickFix;
import org.sonar.java.test.classpath.TestClasspathUtils;
import org.sonar.plugins.java.api.JavaFileScanner;

public class QuickFixesApplier {


  private static final String DEFAULT_CLASSPATH_LOCATION = "../../java-checks-test-sources/default/target/test-classpath.txt";
  private static final List<File> DEFAULT_CLASSPATH;

  static {
    Path path = Paths.get(DEFAULT_CLASSPATH_LOCATION.replace('/', File.separatorChar));
    DEFAULT_CLASSPATH = TestClasspathUtils.loadFromFile(path.toString());
  }

  public void verifyAll(List<InputFile> files) {
    List<JavaFileScanner> visitors = new ArrayList<>(ChecksListWithQuickFix.checks);
    SonarComponents sonarComponents = sonarComponents();
    VisitorsBridgeForQuickFixTests visitorsBridge = new VisitorsBridgeForQuickFixTests(visitors, DEFAULT_CLASSPATH, sonarComponents, new JavaVersionImpl(21));

    JavaAstScanner astScanner = new JavaAstScanner(sonarComponents);
    astScanner.setVisitorBridge(visitorsBridge);

    List<InputFile> filesToParse = files;
    astScanner.scan(filesToParse);

    var filesToQFLocations = visitorsBridge.getQuickFixesLocations();
    var quickFixes = visitorsBridge.getQuickFixes();

    for(var fileToQfs : filesToQFLocations.entrySet()) {
      var path = fileToQfs.getKey();
      var locations = fileToQfs.getValue();
      locations.sort(Comparator.comparingInt(l -> l.startLine));
      for (var location : locations) {
        var qfs = quickFixes.get(location); // quick fixes for this location on file `path`
        applyQuickFixes(path, qfs);
      }
    }
  }

  private void applyQuickFixes(Path file, List<JavaQuickFix> quickFixes) {
    for(var quickFix : quickFixes) {
      quickFix.getTextEdits().forEach(edit -> {
        int sl = edit.getTextSpan().startLine;
        int sc = edit.getTextSpan().startCharacter;
        int el = edit.getTextSpan().endLine;
        int ec = edit.getTextSpan().endCharacter;

      });
    }
  }

  private SonarComponents sonarComponents() {
    SensorContext sensorContext;
    sensorContext = new InternalSensorContext();
    FileSystem fileSystem = sensorContext.fileSystem();
    Configuration config = sensorContext.config();

    ClasspathForMain classpathForMain = new ClasspathForMain(config, fileSystem);
    ClasspathForTest classpathForTest = new ClasspathForTest(config, fileSystem);

    SonarComponents sonarComponents = new SonarComponents(null, fileSystem, classpathForMain, classpathForTest, null, null) {
      @Override
      public boolean reportAnalysisError(RecognitionException re, InputFile inputFile) {
        throw new AssertionError(String.format("Should not fail analysis (%s)", re.getMessage()));
      }

      @Override
      public boolean canSkipUnchangedFiles() {
        return false;
      }
    };
    sonarComponents.setSensorContext(sensorContext);
    return sonarComponents;
  }

}
