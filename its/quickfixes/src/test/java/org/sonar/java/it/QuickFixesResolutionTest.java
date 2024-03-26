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

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.MavenBuild;
import com.sonar.orchestrator.locator.FileLocation;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QuickFixesResolutionTest {

  private static final Logger LOG = LoggerFactory.getLogger(QuickFixesResolutionTest.class);

  private static final String PROJECT_LOCATION = "../../java-checks-test-sources/";
  private static final Set<String> PATHS_TO_INSPECT = Set.of(
    "default/src/main/java",
    "aws/src/main/java",
    "java-17/src/main/java"
  );

  @ClassRule
  public static Orchestrator orchestrator = Orchestrator.builderEnv()
    .useDefaultAdminCredentialsForBuilds(true)
    .setSonarVersion(System.getProperty("sonar.runtimeVersion", "LATEST_RELEASE"))
    .build();

  @Test
  public void testCompilationAfterQuickfixes() throws Exception {
//    PATHS_TO_INSPECT.forEach(path -> {
//      List<File> javaFiles = collectJavaFiles(PROJECT_LOCATION + path);
//      javaFiles.forEach(file -> {
//        LOG.info("Compiling " + file);
//        // compile file
//      });
//    });

    MavenBuild mavenBuild = MavenBuild.create()
      .setPom(FileLocation.of(PROJECT_LOCATION + "pom.xml").getFile().getCanonicalFile())
      .addGoal("compile");

    orchestrator.executeBuild(mavenBuild);

  }

  private static List<File> collectJavaFiles(String directory) {
    Path start = Paths.get(directory);
    int maxDepth = Integer.MAX_VALUE; // this is to say that it should search as deep as possible
    try (Stream<Path> stream = Files.walk(start, maxDepth)) {
      return stream
        .filter(path -> path.toString().endsWith(".java"))
        .map(Path::toFile)
        .collect(Collectors.toList());
    } catch (IOException e) {
      LOG.error("Unable to read " + directory);
    }
    return Collections.emptyList();
  }

}
