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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Stream;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.java.model.JParser;
import org.sonar.java.model.JParserConfig;
import org.sonar.plugins.java.api.JavaVersion;

import static org.assertj.core.api.Assertions.assertThat;

public class QuickFixesResolutionTest {

  private static final Logger LOG = LoggerFactory.getLogger(QuickFixesResolutionTest.class);
  private static final JavaVersion VERSION = JParserConfig.MAXIMUM_SUPPORTED_JAVA_VERSION;
  private static final Path PROJECT_LOCATION = Paths.get("../../java-checks-test-sources/");

  @ClassRule
  public static TemporaryFolder tmpProjectClone = new TemporaryFolder();

  @Test
  public void checkRspecMapping() {
    List<String> actualRulesWithQuickfixImplementation = QuickFixTestUtils.RULE_KEYS_IMPLEMENTING_QUICKFIXES;
    List<String> ruleWithQuickfixMetadata = QuickFixTestUtils.RULE_KEYS_WITH_QUICKFIX_METADATA;
    assertThat(actualRulesWithQuickfixImplementation)
      .as("Rules metadata does not correspond to checks actually implementing quickfixes.")
      .containsExactlyInAnyOrderElementsOf(ruleWithQuickfixMetadata);
  }

  @Test
  public void testParsingAfterQuickfixes() throws Exception {
    cloneJavaCheckTestSources();
    List<InputFile> files = QuickFixTestUtils.collectJavaFiles(tmpProjectClone.getRoot().getAbsolutePath());
    new QuickFixesResolver().scanAndApplyQuickFixes(files);
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

  private static boolean validateFilesStillParse(List<InputFile> inputFiles) {
    for (InputFile inputFile : inputFiles) {
      try {
        JParser.parse(JParserConfig.Mode.FILE_BY_FILE.create(VERSION, List.of()).astParser(), VERSION.toString(), inputFile.filename(), inputFile.contents());
      } catch (IOException ioException) {
        LOG.error("Unable to read contents for file {}", inputFile.filename());
        return false;
      }
    }
    LOG.info("All analyzed files can still be parsed after quickfixes were applied.");
    return true;
  }

}
