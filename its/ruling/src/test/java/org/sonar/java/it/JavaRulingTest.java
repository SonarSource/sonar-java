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
package org.sonar.java.it;

import com.google.common.base.Splitter;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.Build;
import com.sonar.orchestrator.build.BuildResult;
import com.sonar.orchestrator.build.MavenBuild;
import com.sonar.orchestrator.build.SonarScanner;
import com.sonar.orchestrator.container.Server;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.locator.MavenLocation;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import no.finn.lambdacompanion.Try;
import org.apache.commons.lang.StringUtils;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.Fail;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.wsclient.SonarClient;

public class JavaRulingTest {

  private static final int LOGS_NUMBER_LINES = 200;
  private static final Logger LOG = LoggerFactory.getLogger(JavaRulingTest.class);

  // by default all rules are enabled, if you want to enable just a subset of rules you can specify the list of
  // rule keys from the command line using "rules" property, i.e. mvn test -Drules=S100,S101
  private static final ImmutableSet<String> SUBSET_OF_ENABLED_RULES = ImmutableSet.copyOf(
      Splitter.on(',').trimResults().omitEmptyStrings().splitToList(
          System.getProperty("rules", "")
      )
  );

  @ClassRule
  public static TemporaryFolder TMP_DUMP_OLD_FOLDER = new TemporaryFolder();

  private static Path effectiveDumpOldFolder;

  @ClassRule
  public static Orchestrator orchestrator = Orchestrator.builderEnv()
    .setSonarVersion(System.getProperty("sonar.runtimeVersion", "LATEST_RELEASE[7.9]"))
    .addPlugin(FileLocation.byWildcardMavenFilename(new File("../../sonar-java-plugin/target"), "sonar-java-plugin-*.jar"))
    .addPlugin(MavenLocation.of("org.sonarsource.sonar-lits-plugin","sonar-lits-plugin", "0.8.0.1209"))
    .build();
  private static final Gson GSON = new Gson();

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
      "CallToDeprecatedMethod",
      "CycleBetweenPackages",
      // disable because it generates too many issues, performance reasons
      "LeftCurlyBraceStartLineCheck"
      );
    Set<String> activatedRuleKeys = new HashSet<>();
    ProfileGenerator.generate(orchestrator, rulesParameters, disabledRules, SUBSET_OF_ENABLED_RULES, activatedRuleKeys);
    instantiateTemplateRule("S2253", "stringToCharArray", "className=\"java.lang.String\";methodName=\"toCharArray\"", activatedRuleKeys);
    instantiateTemplateRule("S4011", "longDate", "className=\"java.util.Date\";argumentTypes=\"long\"", activatedRuleKeys);
    instantiateTemplateRule("S124", "commentRegexTest", "regularExpression=\"(?i).*TODO\\(user\\).*\";message=\"bad user\"", activatedRuleKeys);
    instantiateTemplateRule("S3417", "doNotUseCommonsCollections", "dependencyName=\"commons-collections:*\";", activatedRuleKeys);
    instantiateTemplateRule("S3417", "doNotUseJunitBefore4", "dependencyName=\"junit:junit\";version=\"*-3.9.9\"", activatedRuleKeys);
    instantiateTemplateRule("S3546", "InstancesOfNewControllerClosedWithDone",
      "factoryMethod=\"org.sonar.api.server.ws.WebService$Context#createController\";closingMethod=\"org.sonar.api.server.ws.WebService$NewController#done\"", activatedRuleKeys);
    instantiateTemplateRule("S3546", "JsonWriterNotClosed",
      "factoryMethod=\"org.sonar.api.server.ws.Response#newJsonWriter\";closingMethod=\"org.sonar.api.utils.text.JsonWriter#close\"", activatedRuleKeys);

    SUBSET_OF_ENABLED_RULES.stream()
      .filter(ruleKey -> !activatedRuleKeys.contains(ruleKey))
      .forEach(ruleKey -> Fail.fail("Specified rule does not exist: " + ruleKey));

    prepareDumpOldFolder();
  }

  private static void prepareDumpOldFolder() {
    Path allRulesFolder = Paths.get("src/test/resources");
    if (SUBSET_OF_ENABLED_RULES.isEmpty()) {
      effectiveDumpOldFolder = allRulesFolder.toAbsolutePath();
    } else {
      effectiveDumpOldFolder = TMP_DUMP_OLD_FOLDER.getRoot().toPath().toAbsolutePath();
      Try.of(() -> Files.list(allRulesFolder)).orElseThrow(Throwables::propagate)
        .filter(p -> p.toFile().isDirectory())
        .forEach(srcProjectDir -> copyDumpSubset(srcProjectDir, effectiveDumpOldFolder.resolve(srcProjectDir.getFileName())));
    }
  }

  private static void copyDumpSubset(Path srcProjectDir, Path dstProjectDir) {
    Try.of(() -> Files.createDirectory(dstProjectDir)).orElseThrow(Throwables::propagate);
    SUBSET_OF_ENABLED_RULES.stream()
      .map(ruleKey -> srcProjectDir.resolve("squid-" + ruleKey + ".json"))
      .filter(p -> p.toFile().exists())
      .forEach(srcJsonFile -> Try.of(() -> Files.copy(srcJsonFile, dstProjectDir.resolve(srcJsonFile.getFileName()), StandardCopyOption.REPLACE_EXISTING))
        .orElseThrow(Throwables::propagate));
  }

  @Test
  public void guava() throws Exception {
    String projectName = "guava";
    MavenBuild build = test_project("com.google.guava:guava", projectName);
    executeBuildWithCommonProperties(build, projectName);
  }

  @Test
  public void apache_commons_beanutils() throws Exception {
    String projectName = "commons-beanutils";
    MavenBuild build = test_project("commons-beanutils:commons-beanutils", projectName);
    executeBuildWithCommonProperties(build, projectName);
  }

  @Test
  public void sonarqube_server() throws Exception {
    // sonarqube-6.5/server/sonar-server (v.6.5)
    String projectName = "sonar-server";
    MavenBuild build = test_project("org.sonarsource.sonarqube:sonar-server", "sonarqube-6.5/server", projectName);
    executeBuildWithCommonProperties(build, projectName);
  }

  @Test
  public void jboss_ejb3_tutorial() throws Exception {
    // https://github.com/jbossejb3/jboss-ejb3-tutorial (18/01/2015)
    String projectName = "jboss-ejb3-tutorial";
    prepareProject(projectName, projectName);
    SonarScanner build = SonarScanner.create(FileLocation.of("../sources/jboss-ejb3-tutorial").getFile())
      .setProjectKey(projectName)
      .setProjectName(projectName)
      .setProjectVersion("0.1.0-SNAPSHOT")
      .setSourceEncoding("UTF-8")
      .setSourceDirs(".")
      .setDebugLogs(true)
      // Dummy sonar.java.binaries to pass validation
      .setProperty("sonar.java.binaries", "asynch")
      .setProperty("sonar.java.source", "1.5");
    executeDebugBuildWithCommonProperties(build, projectName);
  }

  /**
   * Relevant to test lack of semantic, because we don't construct semantic for files in java/lang package.
   */
  @Test
  public void jdk_1_6_source() throws Exception {
    String projectName = "jdk6";
    prepareProject(projectName, projectName);
    SonarScanner build = SonarScanner.create(FileLocation.of("../sources/jdk6").getFile())
      .setProjectKey(projectName)
      .setProjectName(projectName)
      .setProjectVersion("0.1.0-SNAPSHOT")
      .setSourceEncoding("UTF-8")
      .setSourceDirs(".")
      .setProperty("sonar.java.source", "1.5")
      // Dummy sonar.java.binaries to pass validation
      .setProperty("sonar.java.binaries", "launcher")
      .setProperty("sonar.inclusions", "java/**/*.java");
    executeBuildWithCommonProperties(build, projectName);
  }

  private static MavenBuild test_project(String projectKey, String projectName) throws IOException {
    return test_project(projectKey, null, projectName);
  }

  private static MavenBuild test_project(String projectKey, @Nullable String path, String projectName) throws IOException {
    String pomLocation = "../sources/" + (path != null ? path + "/" : "") + projectName + "/pom.xml";
    File pomFile = FileLocation.of(pomLocation).getFile().getCanonicalFile();
    prepareProject(projectKey, projectName);
    MavenBuild mavenBuild = MavenBuild.create().setPom(pomFile).setCleanPackageSonarGoals().addArgument("-DskipTests");
    mavenBuild.setProperty("sonar.projectKey", projectKey);
    return mavenBuild;
  }

  private static void prepareProject(String projectKey, String projectName) {
    orchestrator.getServer().provisionProject(projectKey, projectName);
    orchestrator.getServer().associateProjectToQualityProfile(projectKey, "java", "rules");
  }

  private static void executeDebugBuildWithCommonProperties(Build<?> build, String projectName) throws IOException {
    executeBuildWithCommonProperties(build, projectName, true);
  }

  private static void executeBuildWithCommonProperties(Build<?> build, String projectName) throws IOException {
    executeBuildWithCommonProperties(build, projectName, false);
  }

  private static void executeBuildWithCommonProperties(Build<?> build, String projectName, boolean buildQuietly) throws IOException {
    build.setProperty("sonar.cpd.exclusions", "**/*")
      .setProperty("sonar.import_unknown_files", "true")
      .setProperty("sonar.skipPackageDesign", "true")
      .setProperty("dump.old", effectiveDumpOldFolder.resolve(projectName).toString())
      .setProperty("dump.new", FileLocation.of("target/actual/" + projectName).getFile().getAbsolutePath())
      .setProperty("lits.differences", litsDifferencesPath(projectName))
      .setProperty("sonar.java.xfile", "true")
      .setProperty("sonar.java.failOnException", "true");
    BuildResult buildResult;
    if (buildQuietly) {
      // if build fail, ruling job is not violently interrupted, allowing time to dump SQ logs
      buildResult = orchestrator.executeBuildQuietly(build);
    } else {
      buildResult = orchestrator.executeBuild(build);
    }
    if (buildResult.isSuccess()) {
      assertNoDifferences(projectName);
    } else {
      dumpServerLogs();
      Fail.fail("Build failure for project: " + projectName);
    }
  }

  private static void dumpServerLogs() throws IOException {
    Server server = orchestrator.getServer();
    LOG.error("::::::::::::::::::::::::::::::::::: DUMPING SERVER LOGS :::::::::::::::::::::::::::::::::::");
    dumpServerLogLastLines(server.getAppLogs());
    dumpServerLogLastLines(server.getCeLogs());
    dumpServerLogLastLines(server.getEsLogs());
    dumpServerLogLastLines(server.getWebLogs());
  }

  private static void dumpServerLogLastLines(File logFile) throws IOException {
    if (!logFile.exists()) {
      return;
    }
    List<String> logs = Files.readAllLines(logFile.toPath());
    int nbLines = logs.size();
    if (nbLines > LOGS_NUMBER_LINES) {
      logs = logs.subList(nbLines - LOGS_NUMBER_LINES, nbLines);
    }
    LOG.error("=================================== START " + logFile.getName() + " ===================================");
    LOG.error(System.lineSeparator() + logs.stream().collect(Collectors.joining(System.lineSeparator())));
    LOG.error("===================================== END " + logFile.getName() + " ===================================");
  }

  private static String litsDifferencesPath(String projectName) {
    return FileLocation.of("target/" + projectName + "_differences").getFile().getAbsolutePath();
  }

  private static void assertNoDifferences(String projectName) throws IOException {
    String differences = new String(Files.readAllBytes(Paths.get(litsDifferencesPath(projectName))), StandardCharsets.UTF_8);
    Assertions.assertThat(differences).isEmpty();
  }

  private static void instantiateTemplateRule(String ruleTemplateKey, String instantiationKey, String params, Set<String> activatedRuleKeys) {
    if (!SUBSET_OF_ENABLED_RULES.isEmpty() && !SUBSET_OF_ENABLED_RULES.contains(instantiationKey)) {
      return;
    }
    activatedRuleKeys.add(instantiationKey);
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
    String post = sonarClient.get("api/qualityprofiles/search");
    String profileKey = null;
    Map map = GSON.fromJson(post, Map.class);
    for (Map qp : ((List<Map>) map.get("profiles"))) {
      if ("rules".equals(qp.get("name"))) {
        profileKey = (String) qp.get("key");
        break;
      }
    }
    if (StringUtils.isEmpty(profileKey)) {
      LOG.error("Could not retrieve profile key : Template rule " + ruleTemplateKey + " has not been activated");
    } else {
      String activateRuleResponse = sonarClient.post("api/qualityprofiles/activate_rule", ImmutableMap.of(
        "profile_key", profileKey,
        "rule_key", "squid:" + instantiationKey,
        "severity", "INFO",
        "params", ""));
      LOG.warn(activateRuleResponse);
    }
  }

}
