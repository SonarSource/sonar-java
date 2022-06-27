/*
 * SonarQube Java
 * Copyright (C) 2013-2022 SonarSource SA
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

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.MavenBuild;
import com.sonar.orchestrator.build.SonarScanner;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.locator.MavenLocation;
import java.io.File;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

public class AutoScanTest {

  private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
  private static final Type GSON_MAP_TYPE = new TypeToken<Map<String, List<Integer>>>() {}.getType();
  private static final Type GSON_LIST_ISSUE_DIFF_TYPE = new TypeToken<List<IssueDiff>>() {}.getType();

  private static final Logger LOG = LoggerFactory.getLogger(AutoScanTest.class);

  @ClassRule
  public static TemporaryFolder tmpDumpOldFolder = new TemporaryFolder();

  @ClassRule
  public static Orchestrator orchestrator = Orchestrator.builderEnv()
    .setSonarVersion(System.getProperty("sonar.runtimeVersion", "LATEST_RELEASE[9.3]"))
    .addPlugin(FileLocation.byWildcardMavenFilename(new File("../../sonar-java-plugin/target"), "sonar-java-plugin-*.jar"))
    .addPlugin(MavenLocation.of("org.sonarsource.sonar-lits-plugin", "sonar-lits-plugin", "0.10.0.2181"))
    .build();

  private static final String TARGET_ACTUAL = "target/actual/";
  private static final String PROJECT_LOCATION = "../../java-checks-test-sources/";
  private static final String PROJECT_KEY = "java-checks-test-sources";
  private static final String PROJECT_NAME = "Java Checks Test Sources";
  private static final String DIFF_FILE = "autoscan-diff-by-rules";

  private static final Comparator<String> RULE_KEY_COMPARATOR = (k1, k2) -> Integer.compare(
    // "S128" should be before "S1028"
    Integer.parseInt(k1.substring(1)),
    Integer.parseInt(k2.substring(1)));

  @Test
  public void javaCheckTestSources() throws Exception {
    List<String> ruleKeys = generateSonarWay(orchestrator);

    orchestrator.getServer().provisionProject(PROJECT_KEY, PROJECT_NAME);
    orchestrator.getServer().associateProjectToQualityProfile(PROJECT_KEY, "java", "rules");

    /**
     * 1. Run the analysis as maven project
     */
    String correctConfigIssues = absolutePathFor(TARGET_ACTUAL + PROJECT_KEY + "-mvn");

    MavenBuild mavenBuild = MavenBuild.create()
      .setPom(FileLocation.of(PROJECT_LOCATION + "pom.xml").getFile().getCanonicalFile())
      .setCleanPackageSonarGoals()
      .addArgument("-DskipTests")
      .addArgument("-Panalyze-tests")
      .setProperty("sonar.projectKey", PROJECT_KEY)
      .setProperty("sonar.projectName", PROJECT_NAME)
      // common properties
      .setProperty("sonar.cpd.exclusions", "**/*")
      .setProperty("sonar.skipPackageDesign", "true")
      .setProperty("sonar.internal.analysis.failFast", "true")
      // start with no known issues
      .setProperty("sonar.lits.dump.old", tmpDumpOldFolder.newFolder().getAbsolutePath())
      .setProperty("sonar.lits.dump.new", correctConfigIssues)
      .setProperty("sonar.lits.differences", absolutePathFor(TARGET_ACTUAL + PROJECT_KEY + "-mvn_differences"));

    orchestrator.executeBuild(mavenBuild);

    /**
     * 2. Execute the analysis as sonar-scanner project, without any bytecode nor dependencies/libraries
     */
    SonarScanner sonarScannerBuild = SonarScanner.create(FileLocation.of(PROJECT_LOCATION).getFile())
      .setProjectKey(PROJECT_KEY)
      .setProjectName(PROJECT_NAME)
      .setProjectVersion("0.1.0-SNAPSHOT")
      .setSourceEncoding("UTF-8")
      .setSourceDirs("src/main/java/")
      .setTestDirs("src/test/java/")
      .setProperty("sonar.java.source", "17")
      // common properties
      .setProperty("sonar.cpd.exclusions", "**/*")
      .setProperty("sonar.skipPackageDesign", "true")
      .setProperty("sonar.internal.analysis.failFast", "true")
      // force AutoScan mode
      .setProperty("sonar.internal.analysis.autoscan", "true")
      .setProperty("sonar.internal.analysis.autoscan.filtering", "false")
      // dummy sonar.java.binaries to pass validation
      .setProperty("sonar.java.binaries", tmpDumpOldFolder.newFolder().getAbsolutePath())
      // use as "old" issues the ones from Maven analysis
      .setProperty("sonar.lits.dump.old", correctConfigIssues)
      .setProperty("sonar.lits.dump.new", absolutePathFor(TARGET_ACTUAL + PROJECT_KEY + "-no-binaries"))
      .setProperty("sonar.lits.differences", absolutePathFor(TARGET_ACTUAL + PROJECT_KEY + "-no-binaries_differences"));

    orchestrator.executeBuild(sonarScannerBuild);

    /**
     * 3. Check if differences in expectations in terms of FP/FN/TP
     *
     * Missing issues =  FNs: we don't detect them without the help of the bytecode
     * New issues = FPs: without complete semantic we should not raise any new issue
     * Closed issues = TPs: the issue has been detected during both analysis
     */
    Map<String, RuleIssues> mvnIssues = loadIssues(PROJECT_KEY + "-mvn");
    Map<String, RuleIssues> noBinariesIssues = loadIssues(PROJECT_KEY + "-no-binaries");
    Collection<IssueDiff> newDiffs = calculateDifferences(ruleKeys, mvnIssues, noBinariesIssues).values();

    IssueDiff newTotal = IssueDiff.total(newDiffs);
    LOG.info("Comparing results for both runs:\n- Rules={}\n- TPs={}\n- FNs={}\n- FPs={}\n- Differences={}\n",
      newDiffs.size(),
      newTotal.truePositives,
      newTotal.falseNegatives,
      newTotal.falsePositives,
      newTotal.falsePositives + newTotal.falseNegatives);

    List<IssueDiff> rulesCausingFPs = newDiffs.stream().filter(IssueDiff::causesFPs).collect(Collectors.toList());
    LOG.info("{} rules causing FPs:\n{}", rulesCausingFPs.size(), IssueDiff.prettyPrint(rulesCausingFPs));

    List<IssueDiff> rulesNotReporting = newDiffs.stream().filter(IssueDiff::notReporting).collect(Collectors.toList());
    LOG.info("{} rules never reporting anything:\n{}", rulesNotReporting.size(), IssueDiff.prettyPrint(rulesNotReporting));

    List<IssueDiff> rulesSilenced = newDiffs.stream().filter(IssueDiff::onlyFNs).collect(Collectors.toList());
    LOG.info("{} rules silenced without binaries (only FNs):\n{}", rulesSilenced.size(), IssueDiff.prettyPrint(rulesSilenced));

    // store in a JSON file - serializable
    Files.writeString(pathFor(TARGET_ACTUAL + DIFF_FILE + ".json"), GSON.toJson(newDiffs));
    // store in a CSV file - can be easily imported in google sheets
    Files.writeString(pathFor(TARGET_ACTUAL + DIFF_FILE + ".csv"), IssueDiff.prettyPrint(newDiffs, newTotal));

    List<IssueDiff> knownDiffs = GSON.fromJson(Files.readString(pathFor("src/test/resources/autoscan/" + DIFF_FILE + ".json")), GSON_LIST_ISSUE_DIFF_TYPE);
    IssueDiff knownTotal = IssueDiff.total(knownDiffs);

    assertThat(newDiffs).containsExactlyInAnyOrderElementsOf(knownDiffs);
    assertThat(newTotal).isEqualTo(knownTotal);
    assertThat(rulesCausingFPs).hasSize(7);
    assertThat(rulesNotReporting).hasSize(6);
    assertThat(rulesSilenced).hasSize(64);

    /**
     * 4. Check total number of differences (FPs + FNs)
     *
     * No differences would mean that we find the same issues with and without the bytecode and libraries
     */
    String differences = Files.readString(pathFor(TARGET_ACTUAL + PROJECT_KEY + "-no-binaries_differences"));
    assertThat(differences).isEqualTo("Issues differences: 3075");
  }

  private static Path pathFor(String path) {
    return FileLocation.of(path).getFile().toPath();
  }

  private static String absolutePathFor(String path) {
    return FileLocation.of(path).getFile().getAbsolutePath();
  }

  private static List<String> generateSonarWay(Orchestrator orchestrator) {
    Set<String> results = new TreeSet<>(RULE_KEY_COMPARATOR);
    ProfileGenerator.generate(orchestrator, "Sonar Way", ImmutableMap.of(), Collections.emptySet(), Collections.emptySet(), results);
    return new ArrayList<>(results);
  }

  private static Map<String, RuleIssues> loadIssues(String resultsFolder) throws Exception {
    Map<String, RuleIssues> issues = new TreeMap<>(RULE_KEY_COMPARATOR);
    LOG.info("Reading issues for {}", resultsFolder);
    Files.list(FileLocation.of(TARGET_ACTUAL + resultsFolder).getFile().toPath()).forEach(path -> {
      String filename = path.toFile().getName();
      String ruleKey = filename.substring(/* removing "java-" */ 5, /* removing ".json" */ filename.indexOf('.'));
      try {
        String content = Files.readString(path)
          // fix JSON formatting of LITS results
          .replaceAll(",(\\v+)]", "$1]")
          .replaceAll("],(\\v+)}", "]$1}");
        Map<String, List<Integer>> results = GSON.fromJson(content, GSON_MAP_TYPE);
        issues.put(ruleKey, new RuleIssues(results));
      } catch (Exception e) {
        LOG.error("Unable to read " + filename);
      }
    });
    return issues;
  }

  private static Map<String, IssueDiff> calculateDifferences(List<String> ruleKeys, Map<String, RuleIssues> expected, Map<String, RuleIssues> actual) {
    Map<String, IssueDiff> differences = new TreeMap<>(RULE_KEY_COMPARATOR);

    Set<String> reportedRules = new HashSet<>();
    reportedRules.addAll(expected.keySet());
    reportedRules.addAll(actual.keySet());

    for (String ruleKey : reportedRules) {
      IssueDiff issueDiff = IssueDiff.create(ruleKey, expected.get(ruleKey), actual.get(ruleKey));
      differences.put(ruleKey, issueDiff);
    }

    // add the rules which reported nothing
    ruleKeys.stream()
      .filter(ruleKey -> !differences.containsKey(ruleKey))
      .forEach(ruleKey -> differences.put(ruleKey, new IssueDiff(ruleKey)));
    return differences;
  }

  private static class RuleIssues {
    private final Map<String, List<Integer>> issues;

    private RuleIssues(Map<String, List<Integer>> issues) {
      this.issues = issues;
    }

    private int issueCount() {
      return issues.values().stream().mapToInt(List::size).sum();
    }

    private Set<String> files() {
      return issues.keySet();
    }

    private List<Integer> lines(String file) {
      return issues.getOrDefault(file, Collections.emptyList());
    }
  }

  private static class IssueDiff {
    private static final String COLUMN_TITLES = "Rule;TP;FN;FP\n";
    private static final String SEPARATORS = "-----;-----;-----;-----\n";

    private final String ruleKey;
    private int truePositives;
    private int falseNegatives;
    private int falsePositives;

    private IssueDiff(String ruleKey) {
      this.ruleKey = ruleKey;
    }

    boolean causesFPs() {
      return falsePositives > 0;
    }

    boolean notReporting() {
      return (truePositives + falseNegatives + falsePositives) == 0;
    }

    boolean onlyFNs() {
      return (truePositives + falsePositives) == 0 && falseNegatives > 0;
    }

    @Override
    public int hashCode() {
      return Objects.hash(ruleKey, truePositives, falseNegatives, falsePositives);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      IssueDiff other = (IssueDiff) obj;
      return ruleKey.equals(other.ruleKey)
        && truePositives == other.truePositives
        && falseNegatives == other.falseNegatives
        && falsePositives == other.falsePositives;
    }

    private static IssueDiff onlyFalsePositives(String ruleKey, int countFP) {
      IssueDiff result = new IssueDiff(ruleKey);
      result.falsePositives += countFP;
      return result;
    }

    private static IssueDiff onlyFalseNegatives(String ruleKey, int countFN) {
      IssueDiff result = new IssueDiff(ruleKey);
      result.falseNegatives += countFN;
      return result;
    }

    public static IssueDiff total(Collection<IssueDiff> issueDiffs) {
      IssueDiff total = new IssueDiff("Total");
      for (IssueDiff issueDiff : issueDiffs) {
        total.truePositives += issueDiff.truePositives;
        total.falseNegatives += issueDiff.falseNegatives;
        total.falsePositives += issueDiff.falsePositives;
      }
      return total;
    }

    private static IssueDiff create(String ruleKey, @Nullable RuleIssues expected, @Nullable RuleIssues actual) {
      if (expected == null) {
        // only FPs
        return onlyFalsePositives(ruleKey, Objects.requireNonNull(actual).issueCount());
      }
      if (actual == null) {
        // only FNs
        return onlyFalseNegatives(ruleKey, Objects.requireNonNull(expected).issueCount());
      }
      // compare all issues manually
      return compare(ruleKey, expected, actual);

    }

    private static IssueDiff compare(String ruleKey, RuleIssues expected, RuleIssues actual) {
      IssueDiff issueDiff = new IssueDiff(ruleKey);
      Set<String> filesToVisit = new HashSet<>();
      filesToVisit.addAll(expected.files());
      filesToVisit.addAll(actual.files());

      for (String file : filesToVisit) {
        List<Integer> expectedLines = expected.lines(file);
        List<Integer> actualLines = actual.lines(file);

        List<Integer> falseNegatives = new ArrayList<>(expectedLines);
        falseNegatives.removeAll(actualLines);
        issueDiff.falseNegatives += falseNegatives.size();

        List<Integer> falsePositives = new ArrayList<>(actualLines);
        falsePositives.removeAll(expectedLines);
        issueDiff.falsePositives += falsePositives.size();

        List<Integer> truePositives = new ArrayList<>(expectedLines);
        truePositives.removeAll(falseNegatives);
        issueDiff.truePositives += truePositives.size();
      }

      return issueDiff;
    }

    @Override
    public String toString() {
      return String.format("[%s;TP=%s;FN=%d;FP=%d]", ruleKey, truePositives, falseNegatives, falsePositives);
    }

    private static String prettyPrint(Collection<IssueDiff> diffs) {
      return diffs.stream()
        .map(diff -> String.format("%s;%d;%d;%d", diff.ruleKey, diff.truePositives, diff.falseNegatives, diff.falsePositives))
        .collect(Collectors.joining("\n", "", "\n"));
    }

    private static String prettyPrint(Collection<IssueDiff> diffs, IssueDiff total) {
      return new StringBuilder()
        .append(COLUMN_TITLES)
        .append(SEPARATORS)
        .append(prettyPrint(diffs))
        .append(SEPARATORS)
        .append(COLUMN_TITLES)
        .append(SEPARATORS)
        .append(String.format("%d;%d;%d;%d\n", diffs.size(), total.truePositives, total.falseNegatives, total.falsePositives))
        .toString();
    }
  }
}
