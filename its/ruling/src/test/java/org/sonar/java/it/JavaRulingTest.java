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

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.OrchestratorBuilder;
import com.sonar.orchestrator.build.Build;
import com.sonar.orchestrator.build.BuildResult;
import com.sonar.orchestrator.build.MavenBuild;
import com.sonar.orchestrator.build.SonarScanner;
import com.sonar.orchestrator.container.Edition;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.api.Fail;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.java.test.classpath.TestClasspathUtils;
import org.sonarqube.ws.Qualityprofiles.SearchWsResponse.QualityProfile;
import org.sonarqube.ws.client.HttpConnector;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.WsClientFactories;
import org.sonarqube.ws.client.qualityprofiles.ActivateRuleRequest;
import org.sonarqube.ws.client.qualityprofiles.SearchRequest;
import org.sonarqube.ws.client.rules.CreateRequest;

import static org.assertj.core.api.Assertions.assertThat;

public class JavaRulingTest {

  private static final int LOGS_NUMBER_LINES = 200;
  private static final Logger LOG = LoggerFactory.getLogger(JavaRulingTest.class);

  private static final String INCREMENTAL_ANALYSIS_KEY = "sonar.java.skipUnchanged";
  private static final String SONAR_CACHING_ENABLED_KEY = "sonar.analysisCache.enabled";

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

  public static boolean isCommunityEditionTestsOnly() {
    return "true".equals(System.getProperty("communityEditionTestsOnly"));
  }
  @ClassRule
  public static final Orchestrator ORCHESTRATOR = createOrchestrator();

  private static Orchestrator createOrchestrator() {
    OrchestratorBuilder orchestratorBuilder = Orchestrator.builderEnv()
      .useDefaultAdminCredentialsForBuilds(true)
      .setSonarVersion(System.getProperty("sonar.runtimeVersion", "LATEST_RELEASE[10.3]"))
      .addPlugin(FileLocation.of(TestClasspathUtils.findModuleJarPath("../../sonar-java-plugin").toFile()))
      .addPlugin(MavenLocation.of("org.sonarsource.sonar-lits-plugin", "sonar-lits-plugin", "0.11.0.2659"))
      .addPlugin(FileLocation.of(TestClasspathUtils.findModuleJarPath("../../java-symbolic-execution/java-symbolic-execution-plugin").toFile()));

    if (isCommunityEditionTestsOnly()) {
      orchestratorBuilder.setEdition(Edition.COMMUNITY);
    } else {
      orchestratorBuilder.setEdition(Edition.DEVELOPER)
        .activateLicense();
    }
    return orchestratorBuilder.build();
  }

  @BeforeClass
  public static void prepare_quality_profiles() throws Exception {
    ImmutableMap<String, ImmutableMap<String, String>> rulesParameters = ImmutableMap.<String, ImmutableMap<String, String>>builder()
      .put(
        "S1120",
        ImmutableMap.of("indentationLevel", "4"))
      .put(
        "S1451",
        ImmutableMap.of(
          "headerFormat",
          "\n/*\n" +
            " * Copyright (c) 1998, 2006, Oracle and/or its affiliates. All rights reserved.\n" +
          " * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms."))
      .put("S5961", ImmutableMap.of("MaximumAssertionNumber", "50"))
      .put("S6539", ImmutableMap.of("couplingThreshold", "20"))
      .build();
    ImmutableSet<String> disabledRules = ImmutableSet.of(
      "S1874",
      "CycleBetweenPackages",
      // disable because it generates too many issues, performance reasons
      "S1106"
      );
    Set<String> activatedRuleKeys = new HashSet<>();
    ProfileGenerator.generate(ORCHESTRATOR, rulesParameters, disabledRules, SUBSET_OF_ENABLED_RULES, activatedRuleKeys);
    instantiateTemplateRule("S2253", "stringToCharArray", "className=\"java.lang.String\";methodName=\"toCharArray\"", activatedRuleKeys);
    instantiateTemplateRule("S4011", "longDate", "className=\"java.util.Date\";argumentTypes=\"long\"", activatedRuleKeys);
    instantiateTemplateRule("S124", "commentRegexTest", "regularExpression=\"(?i).*TODO\\(user\\).*\";message=\"bad user\"", activatedRuleKeys);
    instantiateTemplateRule("S3546", "InstancesOfNewControllerClosedWithDone",
      "factoryMethod=\"org.sonar.api.server.ws.WebService$Context#createController\";closingMethod=\"org.sonar.api.server.ws.WebService$NewController#done\"", activatedRuleKeys);
    instantiateTemplateRule("S3546", "JsonWriterNotClosed",
      "factoryMethod=\"org.sonar.api.server.ws.Response#newJsonWriter\";closingMethod=\"org.sonar.api.utils.text.JsonWriter#close\"", activatedRuleKeys);

    SUBSET_OF_ENABLED_RULES.stream()
      .filter(ruleKey -> !activatedRuleKeys.contains(ruleKey))
      .forEach(ruleKey -> Fail.fail("Specified rule does not exist: " + ruleKey));

    prepareDumpOldFolder();
  }

  @AfterClass
  public static void afterAllAnalysis() throws IOException {
    PerformanceStatistics.generate(Paths.get("target","performance"));
  }

  private static void prepareDumpOldFolder() throws Exception {
    Path allRulesFolder = Paths.get("src/test/resources");
    if (SUBSET_OF_ENABLED_RULES.isEmpty()) {
      effectiveDumpOldFolder = allRulesFolder.toAbsolutePath();
    } else {
      effectiveDumpOldFolder = TMP_DUMP_OLD_FOLDER.getRoot().toPath().toAbsolutePath();
      Files.list(allRulesFolder)
        .filter(p -> p.toFile().isDirectory())
        .forEach(srcProjectDir -> copyDumpSubset(srcProjectDir, effectiveDumpOldFolder.resolve(srcProjectDir.getFileName())));
    }
  }

  private static void copyDumpSubset(Path srcProjectDir, Path dstProjectDir) {
    try {
      Files.createDirectory(dstProjectDir);
    } catch (IOException e) {
      throw new IllegalStateException("Unable to create directory: " + dstProjectDir.toString());
    }
    SUBSET_OF_ENABLED_RULES.stream()
      .map(ruleKey -> srcProjectDir.resolve("java-" + ruleKey + ".json"))
      .filter(p -> p.toFile().exists())
      .forEach(srcJsonFile -> copyFile(srcJsonFile, dstProjectDir));
  }

  private static void copyFile(Path source, Path targetDir) {
    try {
      Files.copy(source, targetDir.resolve(source.getFileName()), StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException e) {
      throw new IllegalStateException("Unable to copy file: " + source.toString());
    }
  }

  @Test
  public void guava() throws Exception {
    String projectName = "guava";
    MavenBuild build = test_project("com.google.guava:guava", projectName);
    build
      // by default guava is compatible with java 6, however this is not supported with JDK 17
      .setProperty("java.version", "1.7")
      .setProperty("maven.javadoc.skip", "true")
      // use batch
      .setProperty("sonar.java.experimental.batchModeSizeInKB", "8192");
    executeBuildWithCommonProperties(build, projectName);
  }

  @Test
  public void apache_commons_beanutils() throws Exception {
    String projectName = "commons-beanutils";
    MavenBuild build = test_project("commons-beanutils:commons-beanutils", projectName);
    build
      // by default it can not be built with jdk 17 without changing some plugin versions
      .setProperty("maven-bundle-plugin.version", "5.1.4")
      // use batch
      .setProperty("sonar.java.experimental.batchModeSizeInKB", "8192");
    executeBuildWithCommonProperties(build, projectName);
  }

  @Test
  public void eclipse_jetty_incremental() throws Exception {
    if (isCommunityEditionTestsOnly()) {
      return;
    }

    List<String> dirs = Arrays.asList("jetty-http/", "jetty-io/", "jetty-jmx/", "jetty-server/", "jetty-slf4j-impl/", "jetty-util/", "jetty-util-ajax/", "jetty-xml/", "tests/jetty-http-tools/");

    String mainBranchSourceCode = "eclipse-jetty";
    String mainBinaries = dirs.stream().map(dir -> FileLocation.of("../sources/" + mainBranchSourceCode + "/" + dir + "target/classes"))
      .map(JavaRulingTest::getFileLocationAbsolutePath)
      .collect(Collectors.joining(","));

    final var mainBranch = "eclipse-jetty-main";

    MavenBuild branchBuild = test_project("org.eclipse.jetty:jetty-project", mainBranchSourceCode)
      // re-define binaries from initial maven build
      .setProperty("sonar.java.binaries", mainBinaries)
      .setProperty("sonar.exclusions", "jetty-server/src/main/java/org/eclipse/jetty/server/HttpInput.java," +
        "jetty-osgi/jetty-osgi-boot/src/main/java/org/eclipse/jetty/osgi/boot/internal/serverfactory/ServerInstanceWrapper.java")
      .addArgument("-Dpmd.skip=true")
      .addArgument("-Dcheckstyle.skip=true")
      // Set up incremental analysis
      .setProperties(
        "sonar.branch.name", mainBranch,
        "sonar.scm.provider", "git",
        "sonar.scm.disabled", "false",
        INCREMENTAL_ANALYSIS_KEY, "true",
        SONAR_CACHING_ENABLED_KEY, "true"
      );

    var before1 = System.currentTimeMillis();
    executeBuildWithCommonProperties(branchBuild, mainBranchSourceCode);
    var after1 = System.currentTimeMillis();
    var time1 = after1 - before1;

    // Huge PR
    String prSourceCode = "eclipse-jetty-similar-to-main";
    String prBinaries = dirs.stream().map(dir -> FileLocation.of("../sources/" + prSourceCode + "/" + dir + "target/classes"))
      .map(JavaRulingTest::getFileLocationAbsolutePath)
      .collect(Collectors.joining(","));

    final var prBranch = "eclipse-jetty-same-issues-as-main";

    MavenBuild prBuild = test_existing_project("org.eclipse.jetty:jetty-project", prSourceCode)
      // re-define binaries from initial maven build
      .setProperty("sonar.java.binaries", prBinaries)
      .setProperty("sonar.exclusions", "jetty-server/src/main/java/org/eclipse/jetty/server/HttpInput.java," +
        "jetty-osgi/jetty-osgi-boot/src/main/java/org/eclipse/jetty/osgi/boot/internal/serverfactory/ServerInstanceWrapper.java")
      .addArgument("-Dpmd.skip=true")
      .addArgument("-Dcheckstyle.skip=true")
      // Set up incremental analysis
      .setProperties(
        "sonar.pullrequest.key", prBranch,
        "sonar.pullrequest.branch", prBranch,
        "sonar.pullrequest.base", mainBranch,
        "sonar.scm.provider", "git",
        "sonar.scm.disabled", "false",
        INCREMENTAL_ANALYSIS_KEY, "true",
        SONAR_CACHING_ENABLED_KEY, "true",
        "sonar.java.ignoreUnnamedModuleForSplitPackage", "true"
      );

    var before2 = System.currentTimeMillis();
    executeBuildWithCommonProperties(prBuild, prSourceCode);
    var after2 = System.currentTimeMillis();
    var time2 = after2 - before2;

    // Small PR
    String smallPrSourceCode = "eclipse-jetty-similar-to-main-small";
    String smallPrBinaries = dirs.stream().map(dir -> FileLocation.of("../sources/" + smallPrSourceCode + "/" + dir + "target/classes"))
      .map(JavaRulingTest::getFileLocationAbsolutePath)
      .collect(Collectors.joining(","));

    final var smallPrBranch = "eclipse-jetty-same-issues-as-main-small";

    MavenBuild smallPrBuild = test_existing_project("org.eclipse.jetty:jetty-project", smallPrSourceCode)
      // re-define binaries from initial maven build
      .setProperty("sonar.java.binaries", smallPrBinaries)
      .setProperty("sonar.exclusions", "jetty-server/src/main/java/org/eclipse/jetty/server/HttpInput.java," +
        "jetty-osgi/jetty-osgi-boot/src/main/java/org/eclipse/jetty/osgi/boot/internal/serverfactory/ServerInstanceWrapper.java")
      .addArgument("-Dpmd.skip=true")
      .addArgument("-Dcheckstyle.skip=true")
      // Set up incremental analysis
      .setProperties(
        "sonar.pullrequest.key", smallPrBranch,
        "sonar.pullrequest.branch", smallPrBranch,
        "sonar.pullrequest.base", mainBranch,
        "sonar.scm.provider", "git",
        "sonar.scm.disabled", "false",
        INCREMENTAL_ANALYSIS_KEY, "true",
        SONAR_CACHING_ENABLED_KEY, "true"
      );

    var before3 = System.currentTimeMillis();
    executeBuildWithCommonProperties(smallPrBuild, smallPrSourceCode);
    var after3 = System.currentTimeMillis();
    var time3 = after3 - before3;

    // Results
    assertThat(time2).isLessThan(time1);
    assertThat(time3)
      .isLessThan(time1)
      .isLessThan(time2);
  }

  private static String getFileLocationAbsolutePath(FileLocation location) {
    try {
      return location.getFile().getCanonicalFile().getAbsolutePath();
    } catch (IOException e) {
      return "";
    }
  }

  @Test
  public void sonarqube_server() throws Exception {
    // sonarqube-6.5/server/sonar-server (v.6.5)
    String projectName = "sonar-server";
    MavenBuild build = test_project("org.sonarsource.sonarqube:sonar-server", "sonarqube-6.5/server", projectName)
      .setProperty("sonar.java.fileByFile", "true");
    executeBuildWithCommonProperties(build, projectName);
  }

  @Test
  public void jboss_ejb3_tutorial() throws Exception {
    // https://github.com/jbossejb3/jboss-ejb3-tutorial (18/01/2015)
    String projectName = "jboss-ejb3-tutorial";
    prepareProject(projectName, projectName);
    SonarScanner build = SonarScanner.create(FileLocation.of("../sources/jboss-ejb3-tutorial").getFile())
      .setProperty("sonar.java.fileByFile", "true")
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

  @Test
  public void regex_examples() throws IOException {
    String projectName = "regex-examples";
    MavenBuild build = test_project("org.regex-examples:regex-examples", projectName)
      .setProperty("sonar.java.fileByFile", "true");
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

  private static MavenBuild test_existing_project(String projectKey, String projectName) throws IOException {
    String pomLocation = "../sources/" + projectName + "/pom.xml";
    File pomFile = FileLocation.of(pomLocation).getFile().getCanonicalFile();
    //prepareProject(projectKey, projectName);
    MavenBuild mavenBuild = MavenBuild.create().setPom(pomFile).setCleanPackageSonarGoals().addArgument("-DskipTests");
    mavenBuild.setProperty("sonar.projectKey", projectKey);
    return mavenBuild;
  }

  private static void prepareProject(String projectKey, String projectName) {
    ORCHESTRATOR.getServer().provisionProject(projectKey, projectName);
    ORCHESTRATOR.getServer().associateProjectToQualityProfile(projectKey, "java", "rules");
  }

  private static void executeDebugBuildWithCommonProperties(Build<?> build, String projectName) throws IOException {
    executeBuildWithCommonProperties(build, projectName, true);
  }

  private static void executeBuildWithCommonProperties(Build<?> build, String projectName) throws IOException {
    executeBuildWithCommonProperties(build, projectName, false);
  }

  private static void executeBuildWithCommonProperties(Build<?> build, String projectName, boolean buildQuietly) throws IOException {
    build.setProperty("sonar.cpd.exclusions", "**/*")
      .setProperty("sonar.java.performance.measure", "true")
      .setProperty("sonar.java.performance.measure.path", "target/performance/sonar.java.performance.measure.json")
      .setProperty("sonar.import_unknown_files", "true")
      .setProperty("sonar.skipPackageDesign", "true")
      .setProperty("sonar.lits.dump.old", effectiveDumpOldFolder.resolve(projectName).toString())
      .setProperty("sonar.lits.dump.new", FileLocation.of("target/actual/" + projectName).getFile().getAbsolutePath())
      .setProperty("sonar.lits.differences", litsDifferencesPath(projectName))
      .setProperty("sonar.internal.analysis.failFast", "true");
    BuildResult buildResult;
    if (buildQuietly) {
      // if build fail, ruling job is not violently interrupted, allowing time to dump SQ logs
      buildResult = ORCHESTRATOR.executeBuildQuietly(build);
    } else {
      buildResult = ORCHESTRATOR.executeBuild(build);
    }
    if (buildResult.isSuccess()) {
      assertNoDifferences(projectName);
    } else {
      dumpServerLogs();
      Fail.fail("Build failure for project: " + projectName);
    }
  }

  private static void dumpServerLogs() throws IOException {
    Server server = ORCHESTRATOR.getServer();
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
    assertThat(differences).isEmpty();
  }

  private static void instantiateTemplateRule(String ruleTemplateKey, String instantiationKey, String params, Set<String> activatedRuleKeys) {
    if (!SUBSET_OF_ENABLED_RULES.isEmpty() && !SUBSET_OF_ENABLED_RULES.contains(instantiationKey)) {
      return;
    }
    activatedRuleKeys.add(instantiationKey);
    newAdminWsClient(ORCHESTRATOR)
      .rules()
      .create(new CreateRequest()
      .setName(instantiationKey)
      .setMarkdownDescription(instantiationKey)
      .setSeverity("INFO")
      .setStatus("READY")
      .setTemplateKey("java:" + ruleTemplateKey)
      .setCustomKey(instantiationKey)
      .setPreventReactivation("true")
      .setParams(Arrays.asList(("name=\"" + instantiationKey + "\";key=\"" + instantiationKey + "\";" +
        "markdown_description=\"" + instantiationKey + "\";" + params).split(";", 0))));

    String profileKey = newAdminWsClient(ORCHESTRATOR).qualityprofiles()
      .search(new SearchRequest())
      .getProfilesList().stream()
      .filter(qualityProfile -> "rules".equals(qualityProfile.getName()))
      .map(QualityProfile::getKey)
      .findFirst()
      .orElse(null);

    if (StringUtils.isEmpty(profileKey)) {
      LOG.error("Could not retrieve profile key : Template rule " + ruleTemplateKey + " has not been activated");
    } else {
      String ruleKey = "java:" + instantiationKey;
      newAdminWsClient(ORCHESTRATOR).qualityprofiles()
        .activateRule(new ActivateRuleRequest()
          .setKey(profileKey)
          .setRule(ruleKey)
          .setSeverity("INFO")
          .setParams(Collections.emptyList()));
      LOG.info(String.format("Successfully activated template rule '%s'", ruleKey));
    }
  }

  static WsClient newAdminWsClient(Orchestrator orchestrator) {
    return WsClientFactories.getDefault().newClient(HttpConnector.newBuilder()
      .credentials(Server.ADMIN_LOGIN, Server.ADMIN_PASSWORD)
      .url(orchestrator.getServer().getUrl())
      .build());
  }
}
