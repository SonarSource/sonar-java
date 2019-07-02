/*
 * SonarQube Java
 * Copyright (C) 2013-2019 SonarSource SA
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
import com.google.common.base.Preconditions;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.BuildResult;
import com.sonar.orchestrator.build.SonarScanner;
import com.sonar.orchestrator.locator.FileLocation;
import java.io.File;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.ClassRule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

public class JavaPerformanceTest {

  @ClassRule
  public static final Orchestrator ORCHESTRATOR = Orchestrator.builderEnv()
    .setSonarVersion(System.getProperty("sonar.runtimeVersion", "LATEST_RELEASE[7.9]"))
    .addPlugin(FileLocation.byWildcardMavenFilename(new File("../../sonar-java-plugin/target"), "sonar-java-plugin-*.jar"))
    .restoreProfileAtStartup(FileLocation.of("src/test/profile.xml"))
    .build();

  @Test
  public void perform() {
    ORCHESTRATOR.getServer().provisionProject("project", "project");
    ORCHESTRATOR.getServer().associateProjectToQualityProfile("project", "java", "no-rules");
    SonarScanner build = SonarScanner.create(FileLocation.of("../sources/jdk6").getFile())
      .setEnvironmentVariable("SONAR_RUNNER_OPTS", "-Xmx1024m -server")
      .setProperty("sonar.importSources", "false")
      // Dummy sonar.java.binaries to pass validation
      .setProperty("sonar.java.binaries", "launcher")
      .setProperty("sonar.preloadFileMetadata", "true")
      .setProjectKey("project")
      .setProjectName("project")
      .setProjectVersion("1")
      .setSourceEncoding("UTF-8")
      .setSourceDirs(".");

    BuildResult result = ORCHESTRATOR.executeBuild(build);

    double time = sensorTime(result.getLogs());

    double expected = 170;
    assertThat(time).isEqualTo(expected, offset(expected * 0.06));
  }


  private static double sensorTime(String logs) {
    Pattern pattern = Pattern.compile("Sensor JavaSquidSensor \\[java\\] \\(done\\) \\| time=(\\d++)ms");
    Matcher matcher = pattern.matcher(logs);

    Preconditions.checkArgument(matcher.find(), "Unable to extract the timing of sensor \"JavaSquidSensor\" from the logs");
    double result = (double) TimeUnit.MILLISECONDS.toSeconds(Integer.parseInt(matcher.group(1)));
    Preconditions.checkArgument(!matcher.find(), "Found several potential timings of sensor \"JavaSquidSensor\" in the logs");
    return result;
  }

}
