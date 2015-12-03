/*
 * SonarQube Java
 * Copyright (C) 2013 SonarSource
 * sonarqube@googlegroups.com
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.java.it;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.io.Files;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.MavenBuild;
import com.sonar.orchestrator.locator.FileLocation;
import org.fest.assertions.Fail;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.wsclient.SonarClient;

import javax.annotation.Nullable;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JavaRulingTest {

  private static final Logger LOG = LoggerFactory.getLogger(JavaRulingTest.class);

  @ClassRule
  public static Orchestrator orchestrator = Orchestrator.builderEnv()
  // workaround until https://jira.sonarsource.com/browse/PM-27 PM-28 are fixed
  .addPlugin(FileLocation.of(Iterables.getOnlyElement(Arrays.asList(new File("../../sonar-java-plugin/target/").listFiles(new FilenameFilter() {
    @Override
    public boolean accept(File dir, String name) {
      return name.endsWith(".jar") && !name.endsWith("-sources.jar");
    }
  }))).getAbsolutePath()))
  .setOrchestratorProperty("litsVersion", "0.5")
  .addPlugin("lits")
  .build();

  @BeforeClass
  public static void prepare_quality_profiles() {
    ImmutableMap<String, ImmutableMap<String, String>> rulesParameters = ImmutableMap.<String, ImmutableMap<String, String>>builder()
      .put(
        "IndentationCheck",
        ImmutableMap.of("indentationLevel", "4"))
      .put(
        "S1451",
        ImmutableMap.of(
          "headerFormat",
          "\n/*\n" +
            " * Copyright (c) 1998, 2006, Oracle and/or its affiliates. All rights reserved.\n" +
          " * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms."))
      .build();
    ImmutableSet<String> disabledRules = ImmutableSet.of(
      // disable bytecodeVisitor rules
      "UnusedPrivateMethod",
      "CallToDeprecatedMethod",
      "CycleBetweenPackages",
      // disable because it generates too many issues, performance reasons
      "LeftCurlyBraceStartLineCheck"
      );
    ProfileGenerator.generate(orchestrator, "java", "squid", rulesParameters, disabledRules);
    instantiateTemplateRule("S2253", "stringToCharArray", "className=\"java.lang.String\";methodName=\"toCharArray\"");
    instantiateTemplateRule("ArchitecturalConstraint", "doNotUseJavaIoFile", "fromClasses=\"**\";toClasses=\"java.io.File\"");
  }

  @Test
  public void guava() throws Exception {
    test_project("com.google.guava:guava", "guava");
  }

  @Test
  public void apache_commons_beanutils() throws Exception {
    test_project("commons-beanutils:commons-beanutils", "commons-beanutils");
  }

  @Test
  public void fluent_http() throws Exception {
    test_project("net.code-story:http", "fluent-http");
  }

  @Test
  public void java_squid() throws Exception {
    // sonar-java/java-squid (v3.6)
    test_project("org.sonarsource.java:java-squid", "java-squid");
  }

  @Test
  public void sonarqube_server() throws Exception {
    // sonarqube/server/sonar-server (v.5.1.2)
    test_project("org.codehaus.sonar:sonar-server", "sonarqube/server", "sonar-server");
  }

  private static void test_project(String projectKey, String projectName) throws IOException {
    test_project(projectKey, null, projectName);
  }

  private static void test_project(String projectKey, @Nullable String path, String projectName) throws IOException {
    String pomLocation = "../sources/" + (path != null ? path + "/" : "") + projectName + "/pom.xml";
    File pomFile = FileLocation.of(pomLocation).getFile();
    orchestrator.getServer().provisionProject(projectKey, projectName);
    orchestrator.getServer().associateProjectToQualityProfile(projectKey, "java", "rules");
    MavenBuild mavenBuild = MavenBuild.create().setPom(pomFile).setCleanPackageSonarGoals().addArgument("-DskipTests")
      .setProperty("sonar.cpd.skip", "true")
      .setProperty("sonar.skipPackageDesign", "true")
      .setProperty("sonar.analysis.mode", "preview")
      .setProperty("sonar.issuesReport.html.enable", "true")
      .setProperty("sonar.issuesReport.html.location", htmlReportPath(projectName))
      .setProperty("dump.old", FileLocation.of("src/test/resources/" + projectName).getFile().getAbsolutePath())
      .setProperty("dump.new", FileLocation.of("target/actual/" + projectName).getFile().getAbsolutePath())
      .setProperty("lits.differences", litsDifferencesPath(projectName));
    orchestrator.executeBuild(mavenBuild);
    assertNoDifferences(projectName);
  }

  private static String litsDifferencesPath(String projectName) {
    return FileLocation.of("target/" + projectName + "_differences").getFile().getAbsolutePath();
  }

  private static String htmlReportPath(String projectName) {
    return FileLocation.of("target/" + projectName + "_issue-report").getFile().getAbsolutePath();
  }

  private static void assertNoDifferences(String projectName) throws IOException {
    String differences = Files.toString(new File(litsDifferencesPath(projectName)), StandardCharsets.UTF_8);
    if (!differences.isEmpty()) {
      throw Fail.fail(differences + " -> file://" + htmlReportPath(projectName) + "/issues-report.html");
    }
  }

  private static void instantiateTemplateRule(String ruleTemplateKey, String instantiationKey, String params) {
    SonarClient sonarClient = orchestrator.getServer().adminWsClient();
    sonarClient.post("/api/rules/create", ImmutableMap.<String, Object>builder()
      .put("name", instantiationKey)
      .put("markdown_description", instantiationKey)
      .put("severity", "INFO")
      .put("status", "READY")
      .put("template_key", "squid:" + ruleTemplateKey)
      .put("custom_key", instantiationKey)
      .put("prevent_reactivation", "true")
      .put("params", "name=\"" + instantiationKey + "\";key=\"" + instantiationKey + "\";markdown_description=\"" + instantiationKey + "\";" + params)
      .build());
    String post = sonarClient.get("api/rules/app");
    Pattern pattern = Pattern.compile("java-rules-\\d+");
    Matcher matcher = pattern.matcher(post);
    if (matcher.find()) {
      String profilekey = matcher.group();
      sonarClient.post("api/qualityprofiles/activate_rule", ImmutableMap.<String, Object>of(
        "profile_key", profilekey,
        "rule_key", "squid:" + instantiationKey,
        "severity", "INFO",
        "params", ""));
    } else {
      LOG.error("Could not retrieve profile key : Template rule " + ruleTemplateKey + " has not been activated");
    }
  }

}
