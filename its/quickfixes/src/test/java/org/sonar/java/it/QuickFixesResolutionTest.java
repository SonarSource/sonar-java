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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.java.checks.verifier.TestUtils;
import org.sonar.java.model.JParser;
import org.sonar.java.model.JParserConfig;
import org.sonar.plugins.java.api.JavaVersion;

import static org.assertj.core.api.Assertions.assertThat;

public class QuickFixesResolutionTest {

  private static final Logger LOG = LoggerFactory.getLogger(QuickFixesResolutionTest.class);
  private static final JavaVersion version = JParserConfig.MAXIMUM_SUPPORTED_JAVA_VERSION;
  private static final Path PROJECT_LOCATION = Paths.get("../../java-checks-test-sources/");

  @ClassRule
  public static TemporaryFolder tmpProjectClone = new TemporaryFolder();

  @Test
  public void checkRspecMapping() {
    Pattern pattern = Pattern.compile("@Rule\\(key\\s*=\\s*\"(.*?)\"\\)");
    Path javaChecksPath = Paths.get("../../java-checks/src/main/java/org/sonar/java/checks/");
    List<String> actualQuickfixImplementations = new ArrayList<>();
    try (Stream<Path> paths = Files.walk(javaChecksPath)) {
      paths.forEach(path -> {
        if (path.toString().endsWith(".java")) {
          try {
            String content = new String(Files.readAllBytes(path));
            if (content.contains("QuickFixHelper") || content.contains("JavaQuickFix")) {
              var matcher = pattern.matcher(content);
              if (matcher.find()) {
                String ruleKey = matcher.group(1);
                LOG.info("Rule {} was detected to be implementing quickfixes", ruleKey);
                actualQuickfixImplementations.add(ruleKey);
              }
            }
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        }
      });
    } catch (IOException e) {
      LOG.error("Error reading java-checks folder", e);
    }
    assertThat(actualQuickfixImplementations).containsExactlyInAnyOrder(ChecksListWithQuickFix.QUICKFIX_KEYS.toArray(new String[0]));
  }

  @Test
  public void testCompilationAfterQuickfixes() throws Exception {
    cloneJavaCheckTestSources();
    var applier = new QuickFixesApplier();
    List<InputFile> files = collectJavaFiles(tmpProjectClone.getRoot().getAbsolutePath());
    LOG.info("Starting analysis of {} files", files.size());
    applier.verifyAll(files);
    LOG.info("Analysis complete with {} quickfixes found", applier.getQuickfixesCount());
    assertThat(validateFilesStillParse(files)).isTrue();
  }

  private static void cloneJavaCheckTestSources() throws Exception {
    try (Stream<Path> paths = Files.walk(PROJECT_LOCATION)) {
      paths.forEach(source -> {
        try {
          if (!source.toString().contains("non-compiling")) {
            Path target = tmpProjectClone.getRoot().toPath().resolve(PROJECT_LOCATION.relativize(source));
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
          }
        } catch (IOException e) {
          throw new RuntimeException(e.getMessage(), e);
        }
      });
    }
  }

  private static List<InputFile> collectJavaFiles(String directory) {
    Path start = Paths.get(directory);
    int maxDepth = Integer.MAX_VALUE; // this is to say that it should search as deep as possible
    try (Stream<Path> stream = Files.walk(start, maxDepth)) {
      return stream
        .filter(path -> path.toString().endsWith(".java"))
        .map(path -> TestUtils.inputFile(path.toFile()))
        .toList();
    } catch (IOException e) {
      LOG.error("Unable to read {}", directory);
    }
    return Collections.emptyList();
  }

  private static boolean validateFilesStillParse(List<InputFile> inputFiles) {
    for (InputFile inputFile : inputFiles) {
      try {
        JParser.parse(JParserConfig.Mode.FILE_BY_FILE.create(version, List.of()).astParser(), version.toString(), inputFile.filename(), inputFile.contents());
      } catch (IOException ioException) {
        LOG.error("Unable to read contents for file {}", inputFile.filename());
        return false;
      }
    }
    LOG.info("All analyzed files can still be parsed after quickfixes were applied.");
    return true;
  }

}
