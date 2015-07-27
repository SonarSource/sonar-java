/*
 * SonarSource :: Java :: ITs :: Ruling
 * Copyright (C) 2013 ${owner}
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

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarRunner;
import com.sonar.orchestrator.locator.FileLocation;
import difflib.DiffUtils;
import difflib.Patch;
import org.apache.commons.io.FileUtils;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.wsclient.SonarClient;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.fest.assertions.Assertions.assertThat;

public class JavaRulingTest {

  Logger LOG = LoggerFactory.getLogger(JavaRulingTest.class);

  @ClassRule
  public static Orchestrator orchestrator = Orchestrator.builderEnv()
      .addPlugin("java")
      .setMainPluginKey("java")
      .build();
  private File actual;

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
  }

  @Test
  public void test() throws Exception {
    instantiateTemplateRuleS2253();
    File classes = new File("target/test-classes");
    File sslr_jdk7_source = FileLocation.ofShared("sslr/oracle-jdk-1.7.0.3").getFile();
    actual = new File(sslr_jdk7_source, "actual");
    actual.delete();
    actual.mkdir();
    SonarRunner build = SonarRunner.create(sslr_jdk7_source)
        .setProjectKey("project")
        .setProjectName("project")
        .setProjectVersion("1")
        .setSourceEncoding("UTF-8")
        .setProfile("rules")
        .setProperty("sonar.cpd.skip", "true")
        .setProperty("sonar.skipPackageDesign", "true")
        .setProperty("sonar.analysis.mode", "preview")
        .setProperty("sonar.java.jsonoutput", "true")
        .setProperty("sonar.java.jsonoutput.folder", "actual")
        .setProperty("sonar.java.libraries", classes.getAbsolutePath())
        .setEnvironmentVariable("SONAR_RUNNER_OPTS", "-Xmx2500m");
    orchestrator.executeBuild(build);

    assertThatNoDifferences();
  }

  private void instantiateTemplateRuleS2253() {
    SonarClient sonarClient = orchestrator.getServer().adminWsClient();
    sonarClient.post("/api/rules/create", ImmutableMap.<String, Object>builder()
        .put("name", "stringToCharArray")
        .put("markdown_description", "stringToCharArray")
        .put("severity", "INFO")
        .put("status", "READY")
        .put("template_key", "squid:S2253")
        .put("custom_key", "stringToCharArray")
        .put("prevent_reactivation", "true")
        .put("params", "name=\"stringToCharArray\";key=\"stringToCharArray\";markdown_description=\"stringToCharArray\";className=\"java.lang.String\";methodName=\"toCharArray\"")
        .build());
    String post = sonarClient.get("api/rules/app");
    Pattern pattern = Pattern.compile("java-rules-\\d+");
    Matcher matcher = pattern.matcher(post);
    if (matcher.find()) {
      String profilekey = matcher.group();
      sonarClient.post("api/qualityprofiles/activate_rule", ImmutableMap.<String, Object>of(
          "profile_key", profilekey,
          "rule_key", "squid:stringToCharArray",
          "severity", "INFO",
          "params", ""));
    }else {
      LOG.error("Could not retrieve profile key : Template rule S2253 has not been activated ");
    }
  }
  private void assertThatNoDifferences() throws IOException {

    List<File> expectedFiles = Lists.newArrayList(FileLocation.of("src/test/resources/expected").getFile().listFiles());
    List<File> actualFiles = Lists.newArrayList(actual.listFiles());
    Collections.sort(expectedFiles);
    Collections.sort(actualFiles);
    List<String> errors = new ArrayList<>();
    for (File actualFile : actualFiles) {
      File expectedFile = matchingExpectedFile(expectedFiles, actualFile);
      if(expectedFile == null) {
        errors.add("only in actual files "+actualFile.getName());
      } else {
        expectedFiles.remove(expectedFile);
        List<String> originalLines = FileUtils.readLines(expectedFile);
        Patch patch = DiffUtils.diff(originalLines, FileUtils.readLines(actualFile));
        if(!patch.getDeltas().isEmpty()) {
          errors.addAll(DiffUtils.generateUnifiedDiff(expectedFile.getName(), actualFile.getName(), originalLines, patch, 1));
        }
      }
    }
    //remaining expected file
    for (File expectedFile : expectedFiles) {
      errors.add("only in expected files "+expectedFile.getName());
    }
    assertThat(errors).as(Joiner.on("\n").join(errors)).isEmpty();
  }

  @Nullable
  private File matchingExpectedFile(List<File> expectedFiles, File actualFile) {
    for (File expectedFile : expectedFiles) {
      if(expectedFile.getName().equals(actualFile.getName())) {
        return expectedFile;
      }
    }
    return null;
  }


}
