/*
 * SonarQube Java
 * Copyright (C) 2013-2017 SonarSource SA
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
import com.sonar.orchestrator.build.SonarRunner;
import com.sonar.orchestrator.locator.FileLocation;
import org.junit.ClassRule;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

public class JavaPerformanceTest {

  private static final String SENSOR_NAME = "JavaSquidSensor";

  @ClassRule
  public static final Orchestrator ORCHESTRATOR = Orchestrator.builderEnv()
    .addPlugin(FileLocation.byWildcardMavenFilename(new File("../../sonar-java-plugin/target"), "sonar-java-plugin-*.jar"))
    .restoreProfileAtStartup(FileLocation.of("src/test/profile.xml"))
    .build();

  @Test
  public void perform() throws IOException {
    ORCHESTRATOR.getServer().provisionProject("project", "project");
    ORCHESTRATOR.getServer().associateProjectToQualityProfile("project", "java", "no-rules");
    SonarRunner build = SonarRunner.create(FileLocation.of("../sources/jdk6").getFile())
      .setEnvironmentVariable("SONAR_RUNNER_OPTS", "-Xmx1024m -server")
      .setProperty("sonar.importSources", "false")
      .setProperty("sonar.showProfiling", "true")
      .setProjectKey("project")
      .setProjectName("project")
      .setProjectVersion("1")
      .setSourceEncoding("UTF-8")
      .setSourceDirs(".");

    BuildResult result = ORCHESTRATOR.executeBuild(build);

    double time = sensorTime(build.getProjectDir(), result.getLogs(), SENSOR_NAME);

    double expected = 170;
    assertThat(time).isEqualTo(expected, offset(expected * 0.04));
  }

  private static double sensorTime(File projectDir, String logs, String sensor) throws IOException {
    File profilingFile = new File(projectDir, ".sonar/profiling/project-profiler.properties");
    Preconditions.checkArgument(profilingFile.isFile(), "Cannot find profiling file to extract time for sensor " + sensor + ": " + profilingFile.getAbsolutePath());
    return getTimeValue(profilingFile, sensor);
  }

  private static double getTimeValue(File profilingFile, String sensor) throws IOException {
    Properties properties = new Properties();
    properties.load(new FileInputStream(profilingFile));
    String time = properties.getProperty(sensor);
    Preconditions.checkNotNull(time, "Could not find a value for property : " + sensor);
    return toMilliseconds(time);
  }

  private static double toMilliseconds(String time) {
    return TimeUnit.MILLISECONDS.toSeconds(Integer.parseInt(time));
  }

}
