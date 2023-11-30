/*
 * SonarQube Java
 * Copyright (C) 2013-2023 SonarSource SA
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonarsource.performance.measure.DurationMeasure;
import org.sonarsource.performance.measure.DurationMeasureFiles;

public class PerformanceStatistics {

  private static final Logger LOG = LoggerFactory.getLogger(PerformanceStatistics.class);

  public static void main(String[] args) throws IOException {
    PerformanceStatistics.generate(Paths.get("target", "performance"));
  }

  public static void generate(Path performanceDirectory) throws IOException {
    Map<String, String> categoryNames = new HashMap<>();
    categoryNames.put("Main", "1.main");
    categoryNames.put("Test", "1.test");
    categoryNames.put("Scanners", "2.scanners");
    categoryNames.put("IssuableSubscriptionVisitors", "2.subscription");
    categoryNames.put("SymbolicExecutionVisitor", "3.symbolic-execution");
    Predicate<String> groupedMeasurePredicate = name -> name.endsWith("Check");

    Path performanceJsonFile = performanceDirectory.resolve("sonar.java.performance.measure.json");
    DurationMeasure measure = DurationMeasureFiles.fromJsonWithoutObservationCost(performanceJsonFile);
    measure.recursiveMergeOnUpperLevel("RegexParser");
    measure.recursiveMergeOnUpperLevel("JavaWriteCache.write");
    Path performanceStatFile = performanceDirectory.resolve("sonar.java.performance.statistics.txt");
    DurationMeasureFiles.writeStatistics(performanceStatFile, measure, categoryNames, groupedMeasurePredicate);

    try {
      logMostTimeConsumingRules(measure);
    } catch (Exception e) {
      LOG.error("Error while logging most time consuming rules", e);
    }
  }

  private static void logMostTimeConsumingRules(DurationMeasure measure) throws IOException {
    if(System.getenv("CIRRUS_CI") != null) {
      return;
    }
    LOG.info("Logging most time consuming rules that were changed recently.");
    List<String> modifiedChecks = getModifiedChecks();
    if(modifiedChecks.isEmpty()){
      LOG.info("No checks were modified recently. No checks performance statistics will be logged.");
      return;
    }
    HashMap<String, DurationMeasure> checksMeasuresMap = new HashMap<>();

    // Contains the measures of all the Checks
    extractChecksMeasures(measure, checksMeasuresMap);
    List<DurationMeasure> checksMeasures = new ArrayList<>(checksMeasuresMap.values());
    checksMeasures.sort((check1, check2) -> Long.compare(check2.durationNanos(), check1.durationNanos()));
    int totalChecks = checksMeasures.size();
    LOG.info("Most time consuming rules:");
    checksMeasures.stream()
      .filter(check -> modifiedChecks.contains(check.name()))
      .forEach(check -> LOG.info("{}th out of {} rules, {}: {} ns", checksMeasures.indexOf(check), totalChecks ,check.name(), check.durationNanos()));
  }

  private static List<String> getModifiedChecks() throws IOException {
    List<DiffEntry> diffs;
    try (Git git = Git.open(new File("../../.git"))) {
      diffs = getLocalDiffsOnChecks(git);
      if (diffs.isEmpty()) {
        LOG.info("No local changes on checks, checking between last 2 commits");
        diffs = getDiffsBetweenLastTwoCommits(git);
        if (diffs.isEmpty()){
          LOG.info("No changes on checks between last 2 commits. No checks performance statistics will be logged.");
        }
      }
    }
    return diffs.stream().map(entry -> new File(entry.getNewPath()).getName().split("\\.")[0]).collect(Collectors.toList());
  }

  private static void extractChecksMeasures(DurationMeasure root, Map<String, DurationMeasure> result) {
    if (root.name().endsWith("Check")) {
      if(result.containsKey(root.name())){
        result.get(root.name()).merge(root);
      } else {
        result.put(root.name(), root);
      }
    }
    if (!root.children().isEmpty()) {
      root.children().forEach(child -> extractChecksMeasures(child, result));
    }
  }

  private static List<DiffEntry> getLocalDiffsOnChecks(Git git) {
    try (OutputStream outputStream = new FileOutputStream("temp")) {
      List<DiffEntry> diffEntries = git.diff().setOutputStream(outputStream).call();
      return diffEntries.stream().filter(entry -> entry.getNewPath().endsWith("Check.java")).collect(Collectors.toList());
    } catch (IOException | GitAPIException e) {
      return Collections.emptyList();
    }
  }

  private static List<DiffEntry> getDiffsBetweenLastTwoCommits(Git git) {
    Repository repository = git.getRepository();
    try (ObjectReader reader = repository.newObjectReader()) {
      CanonicalTreeParser prevTreeIter = getTreeParser(repository, "HEAD^1^{tree}");
      CanonicalTreeParser headTreeIter = getTreeParser(repository, "HEAD^{tree}");
      List<DiffEntry> entries = git.diff().setOldTree(prevTreeIter).setNewTree(headTreeIter).call();
      return entries.stream().filter(entry -> entry.getNewPath().endsWith("Check.java")).collect(Collectors.toList());
    } catch (Exception e) {
      return Collections.emptyList();
    }
  }

  private static CanonicalTreeParser getTreeParser(Repository repository, String ref) throws Exception {
    ObjectId head = repository.resolve(ref);
    try (ObjectReader reader = repository.newObjectReader()) {
      CanonicalTreeParser treeParser = new CanonicalTreeParser();
      treeParser.reset(reader, head);
      return treeParser;
    }
  }

  private static RevCommit getHeadCommit(Repository repository) throws Exception {
    try (Git git = new Git(repository)) {
      Iterable<RevCommit> history = git.log().setMaxCount(1).call();
      return history.iterator().next();
    }
  }

}
