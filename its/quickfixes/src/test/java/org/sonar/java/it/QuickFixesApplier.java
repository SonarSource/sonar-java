/*
 * SonarQube Java
 * Copyright (C) 2024-2024 SonarSource SA
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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

  private static final Logger LOG = LoggerFactory.getLogger(QuickFixesApplier.class);
  private static final List<File> DEFAULT_CLASSPATH;
  private static final Set<Integer> EDITED_LINES = new HashSet<>();

  private Map<Path, List<JavaQuickFix>> pathsToQuickfixes;

  static {
    Path path = Paths.get("target/test-classpath.txt".replace('/', File.separatorChar));
    DEFAULT_CLASSPATH = TestClasspathUtils.loadFromFile(path.toString());
  }

  public void verifyAll(List<InputFile> files) throws IOException {
    List<JavaFileScanner> visitors = new ArrayList<>(ChecksListWithQuickFix.checks);
    SonarComponents sonarComponents = sonarComponents();
    VisitorsBridgeForQuickFixes visitorsBridge = new VisitorsBridgeForQuickFixes(visitors, DEFAULT_CLASSPATH, sonarComponents, new JavaVersionImpl(21));

    JavaAstScanner astScanner = new JavaAstScanner(sonarComponents);
    astScanner.setVisitorBridge(visitorsBridge);

    astScanner.scan(files);

    pathsToQuickfixes = visitorsBridge.getQuickFixes();

    for (var fileToQfs : pathsToQuickfixes.entrySet()) {
      EDITED_LINES.clear();
      var path = fileToQfs.getKey();
      var quickfixes = new ArrayList<>(fileToQfs.getValue());
      if (quickfixes.isEmpty()) {
        continue;
      }
      quickfixes.sort(Comparator.comparing(qf -> qf.getTextEdits().get(0).getTextSpan()));
      String content = fileContent(path);
      Map<Integer, Integer> lineOffsets = computeLineOffsets(content);
      content = applyQuickFixes(content, lineOffsets, quickfixes);
      Files.write(path, content.getBytes());
    }
  }

  private Map<Integer, Integer> computeLineOffsets(String content) {
    Map<Integer, Integer> lineOffsets = new HashMap<>();
    lineOffsets.put(1, 0); // first line starts at index 0 (0-based index
    int line = 2;
    for (int i = 0; i < content.length(); i++) {
      if (content.charAt(i) == '\n') {
        lineOffsets.put(line, i);
        line++;
      }
    }
    return lineOffsets;
  }

  private String applyQuickFixes(String fileContent, Map<Integer, Integer> lineOffsets, List<JavaQuickFix> quickFixes) {
    String result = fileContent;
    for (var quickFix : quickFixes) {
      if (!isQuickfixApplicable(quickFix)) {
        continue;
      }
      var edits = new ArrayList<>(quickFix.getTextEdits());
      edits.sort((e1, e2) -> e2.getTextSpan().compareTo(e1.getTextSpan()));
      for (var edit : edits) {
        int sl = edit.getTextSpan().startLine;
        int sc = edit.getTextSpan().startCharacter + 1;
        int el = edit.getTextSpan().endLine;
        int ec = edit.getTextSpan().endCharacter + 1;
        try {
          int startOfReplacement = lineOffsets.get(sl) + sc;
          int endOfReplacement = lineOffsets.get(el) + ec;
          result = result.substring(0, startOfReplacement) + edit.getReplacement() + result.substring(endOfReplacement);
          IntStream.range(sl, el + 1).forEach(EDITED_LINES::add);
        } catch (Exception e) {
          LOG.error("ERROR  \n {} \napplying quickfixes for: \n{}", e.getMessage(), fileContent);
        }
      }
    }
    return result;
  }

  private static boolean isQuickfixApplicable(JavaQuickFix quickFix) {
    for (var edit : quickFix.getTextEdits()) {
      int sl = edit.getTextSpan().startLine;
      int el = edit.getTextSpan().endLine;
      if (EDITED_LINES.contains(sl) || EDITED_LINES.contains(el)) {
        return false;
      }
    }
    return true;
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

  private static String fileContent(Path path) {
    try {
      return new String(Files.readAllBytes(path));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public int getQuickfixesCount() {
    return pathsToQuickfixes.values().stream().mapToInt(Collection::size).sum();
  }

}
