/*
 * SonarQube Java
 * Copyright (C) 2024-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
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
    LOG.info("Cloning java-checks-test-sources to temporary folder.");
    cloneJavaCheckTestSources();
    List<InputFile> files = QuickFixTestUtils.collectJavaFiles(tmpProjectClone.getRoot().getAbsolutePath());
    LOG.info("Analyzing and applying quickfixes to all files.");
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
    boolean allFilesParse = true;
    for (InputFile inputFile : inputFiles) {
      try {
        JParser.parse(JParserConfig.Mode.FILE_BY_FILE.create(VERSION, List.of()).astParser(), VERSION.toString(), inputFile.filename(), inputFile.contents());
      } catch (IOException ioException) {
        LOG.error("Unable to read contents for file {}", inputFile.filename());
        allFilesParse = false;
      }
    }
    if(allFilesParse){
      LOG.info("All files still parse after applying quickfixes.");
    }
    return allFilesParse;
  }

}
