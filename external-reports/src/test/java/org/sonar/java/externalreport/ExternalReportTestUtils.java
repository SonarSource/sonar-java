/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
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
package org.sonar.java.externalreport;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.Plugin;
import org.sonar.api.SonarQubeSide;
import org.sonar.api.SonarRuntime;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.internal.SonarRuntimeImpl;
import org.sonar.api.utils.Version;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

public final class ExternalReportTestUtils {

  private ExternalReportTestUtils() {
    // utility class
  }

  public static Plugin.Context sensorContext(int major, int minor) {
    SonarRuntime runtime = SonarRuntimeImpl.forSonarQube(Version.create(major, minor), SonarQubeSide.SERVER);
    return new Plugin.Context(runtime);
  }

  public static void assertNoErrorWarnDebugLogs(LogTester logTester) {
    assertThat(logTester.logs(LoggerLevel.ERROR)).isEmpty();
    assertThat(logTester.logs(LoggerLevel.WARN)).isEmpty();
    assertThat(logTester.logs(LoggerLevel.DEBUG)).isEmpty();
  }

  public static String onlyOneLogElement(List<String> elements) {
    assertThat(elements).hasSize(1);
    return elements.get(0);
  }

  public static File generateReport(Path projectDir, TemporaryFolder tmp, String fileName) throws IOException {
    Path filePath = projectDir.resolve(fileName);
    if (!filePath.toFile().exists()) {
      return filePath.toFile();
    }
    String reportData = new String(Files.readAllBytes(filePath), UTF_8);
    reportData = reportData.replace("${PROJECT_DIR}", projectDir.toRealPath() + File.separator);
    File reportFile = tmp.newFile(fileName).getCanonicalFile();
    Files.write(reportFile.toPath(), reportData.getBytes(UTF_8));
    return reportFile;
  }

  public static SensorContextTester createContext(Path projectDir, int majorVersion, int minorVersion) throws IOException {
    SensorContextTester context = SensorContextTester.create(projectDir);

    try (Stream<Path> pathStream = Files.walk(projectDir)) {
      pathStream
        .filter(path -> !path.toFile().isDirectory())
        .forEach(path -> addFileToContext(context, projectDir, path));
    }

    context.setRuntime(SonarRuntimeImpl.forSonarQube(Version.create(majorVersion, minorVersion), SonarQubeSide.SERVER));
    return context;
  }

  private static void addFileToContext(SensorContextTester context, Path projectDir, Path file) {
    try {
      String projectId = projectDir.getFileName().toString() + "-project";
      context.fileSystem().add(TestInputFileBuilder.create(projectId, projectDir.toFile(), file.toFile())
        .setCharset(UTF_8)
        .setLanguage(file.toString().substring(file.toString().lastIndexOf('.') + 1))
        .setContents(new String(Files.readAllBytes(file), UTF_8))
        .setType(InputFile.Type.MAIN)
        .build());
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

}
