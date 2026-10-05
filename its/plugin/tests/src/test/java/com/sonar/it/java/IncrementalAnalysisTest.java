package com.sonar.it.java;

import com.sonar.it.java.suite.JavaTestSuite;
import com.sonar.it.java.suite.TestUtils;
import com.sonar.orchestrator.build.BuildResult;
import com.sonar.orchestrator.build.MavenBuild;
import com.sonar.orchestrator.container.Edition;
import com.sonar.orchestrator.junit4.OrchestratorRule;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

public class IncrementalAnalysisTest {

  private static final String PROJECT_KEY = "org.sonarsource.it.projects:incremental-eclipse-jetty";
  private static final String MAIN_BRANCH = "eclipse-jetty-main";
  private static final List<String> MODULES = List.of(
    "jetty-http", "jetty-io", "jetty-jmx", "jetty-server", "jetty-slf4j-impl", "jetty-util", "jetty-util-ajax", "jetty-xml", "tests/jetty-http-tools");
  private static final String CAN_SKIP_UNCHANGED_FILES_LOG =
    "The Java analyzer is running in a context where unchanged files can be skipped.";
  private static final Pattern FILES_LEVERAGED_FROM_CACHE = Pattern.compile(
    "The Java analyzer was able to leverage cached data from previous analyses for (\\d+) out of (\\d+) files\\.");
  private static final Logger LOG = LoggerFactory.getLogger(IncrementalAnalysisTest.class);

  @ClassRule
  public static final OrchestratorRule ORCHESTRATOR = createOrchestrator();

  private static OrchestratorRule createOrchestrator() {
    if (Boolean.getBoolean("communityEditionTestsOnly")) {
      return null;
    }
    return OrchestratorRule.builderEnv()
      .useDefaultAdminCredentialsForBuilds(true)
      .setSonarVersion(System.getProperty("sonar.runtimeVersion", "LATEST_RELEASE"))
      .setEdition(Edition.ENTERPRISE_LW)
      .activateLicense()
      .addPlugin(JavaTestSuite.JAVA_PLUGIN_LOCATION)
      .build();
  }

  @Test
  public void eclipse_jetty_incremental() throws IOException {
    if (Boolean.getBoolean("communityEditionTestsOnly")) {
      return;
    }

    TestUtils.provisionProject(ORCHESTRATOR, PROJECT_KEY, "incremental-eclipse-jetty", "java", "Sonar way");
    AnalysisResult mainBranchResult = analyze(jettyBuild("eclipse-jetty")
      .setProperty("sonar.branch.name", MAIN_BRANCH));
    AnalysisResult largePrResult = analyze(pullRequestBuild("eclipse-jetty-similar-to-main", "eclipse-jetty-same-issues-as-main")
      .setProperty("sonar.java.ignoreUnnamedModuleForSplitPackage", "true"));
    AnalysisResult smallPrResult = analyze(pullRequestBuild("eclipse-jetty-similar-to-main-small", "eclipse-jetty-same-issues-as-main-small"));

    CacheUsage mainBranchUsage = cacheUsage(mainBranchResult.buildResult());
    logCacheUsage("Main branch", mainBranchUsage);
    assertThat(mainBranchUsage.total())
      .as("The main branch analysis should report cache usage")
      .isPositive();
    assertThat(mainBranchUsage.fromCache())
      .as("The first analysis of the main branch has no previous analysis to leverage")
      .isZero();
    assertCacheWasLeveraged(largePrResult.buildResult(), "Large PR", 0.5);
    assertCacheWasLeveraged(smallPrResult.buildResult(), "Small PR", 0.9);

    assertThat(largePrResult.durationMillis())
      .as("Large PR incremental analysis should not be significantly slower than main branch analysis")
      .isLessThan((long) (mainBranchResult.durationMillis() * 1.25));
    assertThat(smallPrResult.durationMillis())
      .as("Small PR incremental analysis should be faster than main branch analysis")
      .isLessThan((long) (mainBranchResult.durationMillis() * 0.90))
      .as("Small PR incremental analysis should be faster than large PR incremental analysis")
      .isLessThan((long) (largePrResult.durationMillis() * 0.95));
  }

  private static MavenBuild pullRequestBuild(String sourceProject, String branch) throws IOException {
    return jettyBuild(sourceProject)
      .setProperties(
        "sonar.pullrequest.key", branch,
        "sonar.pullrequest.branch", branch,
        "sonar.pullrequest.base", MAIN_BRANCH);
  }

  private static MavenBuild jettyBuild(String sourceProject) throws IOException {
    Path projectDir = new File(TestUtils.homeDir().getParentFile(), "sources/" + sourceProject).getCanonicalFile().toPath();
    String binaries = MODULES.stream()
      .map(module -> projectDir.resolve(module).resolve("target/classes").toString())
      .collect(Collectors.joining(","));
    return TestUtils.createMavenBuild()
      .setPom(projectDir.resolve("pom.xml").toFile())
      .setCleanPackageSonarGoals()
      .addArgument("-DskipTests")
      .addArgument("-Dpmd.skip=true")
      .addArgument("-Dcheckstyle.skip=true")
      .setProperty("sonar.projectKey", PROJECT_KEY)
      .setProperty("sonar.java.binaries", binaries)
      .setProperty("sonar.exclusions", "jetty-server/src/main/java/org/eclipse/jetty/server/HttpInput.java," +
        "jetty-osgi/jetty-osgi-boot/src/main/java/org/eclipse/jetty/osgi/boot/internal/serverfactory/ServerInstanceWrapper.java")
      .setProperty("sonar.scm.provider", "git")
      .setProperty("sonar.scm.disabled", "false")
      .setProperty("sonar.java.skipUnchanged", "true")
      .setProperty("sonar.analysisCache.enabled", "true")
      .setProperty("sonar.cpd.exclusions", "**/*")
      .setProperty("sonar.import_unknown_files", "true")
      .setProperty("sonar.skipPackageDesign", "true")
      .setProperty("sonar.internal.analysis.failFast", "true");
  }

  private static AnalysisResult analyze(MavenBuild build) {
    long start = System.nanoTime();
    BuildResult buildResult = ORCHESTRATOR.executeBuild(build);
    return new AnalysisResult(buildResult, (System.nanoTime() - start) / 1_000_000);
  }

  private static void assertCacheWasLeveraged(BuildResult result, String label, double filesCachedRatio) {
    CacheUsage usage = cacheUsage(result);
    logCacheUsage(label, usage);
    assertThat(result.getLogs())
      .as("%s should be analyzed in a context where unchanged files can be skipped", label)
      .contains(CAN_SKIP_UNCHANGED_FILES_LOG);
    assertThat(usage.total())
      .as("%s should report how many files the analyzer leveraged from the cache", label)
      .isPositive();
    assertThat(usage.ratio())
      .as("%s should leverage the cache for at least %d%% of files, but only %d out of %d did",
        label, Math.round(filesCachedRatio * 100), usage.fromCache(), usage.total())
      .isGreaterThanOrEqualTo(filesCachedRatio);
  }

  private static void logCacheUsage(String label, CacheUsage usage) {
    LOG.info("[incremental analysis] {}: the Java analyzer leveraged cached data for {} out of {} files ({}%).",
      label, usage.fromCache(), usage.total(), Math.round(usage.ratio() * 100));
  }

  /**
   * Sums the per-module cache counts so that assertions cover the whole multi-module project.
   */
  private static CacheUsage cacheUsage(BuildResult result) {
    Matcher matcher = FILES_LEVERAGED_FROM_CACHE.matcher(result.getLogs());
    int fromCache = 0;
    int total = 0;
    while (matcher.find()) {
      fromCache += Integer.parseInt(matcher.group(1));
      total += Integer.parseInt(matcher.group(2));
    }
    return new CacheUsage(fromCache, total);
  }

  private record AnalysisResult(BuildResult buildResult, long durationMillis) {
  }

  private record CacheUsage(int fromCache, int total) {
    double ratio() {
      return total == 0 ? 0d : (double) fromCache / total;
    }
  }
}
