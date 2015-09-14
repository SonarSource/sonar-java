/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
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
package org.sonar.plugins.java;

import com.google.common.base.Charsets;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.checks.NoSonarFilter;
import org.sonar.api.config.Settings;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Directory;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.rule.RuleKey;
import org.sonar.java.JavaSquid;
import org.sonar.java.SonarComponents;
import org.sonar.java.bytecode.visitor.ResourceMapping;
import org.sonar.plugins.java.bridges.ChecksBridge;
import org.sonar.plugins.java.bridges.DesignBridge;
import org.sonar.squidbridge.api.SourceFile;

import javax.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class Bridges {

  private static final Logger LOG = LoggerFactory.getLogger(Bridges.class);
  private final JavaSquid squid;
  private final Settings settings;

  public Bridges(JavaSquid squid, Settings settings) {
    this.squid = squid;
    this.settings = settings;
  }

  public void save(SensorContext context, Project project, SonarComponents sonarComponents, ResourceMapping resourceMapping,
    NoSonarFilter noSonarFilter, RulesProfile rulesProfile) {
    boolean skipPackageDesignAnalysis = settings.getBoolean(CoreProperties.DESIGN_SKIP_PACKAGE_DESIGN_PROPERTY);
    // Design
    if (!skipPackageDesignAnalysis && squid.isBytecodeScanned()) {
      DesignBridge designBridge = new DesignBridge(context, squid.getGraph(), resourceMapping, sonarComponents.getResourcePerspectives());
      designBridge.saveDesign(project);
    }
    // Report Issues
    ChecksBridge checksBridge = new ChecksBridge(sonarComponents, rulesProfile);
    reportIssues(resourceMapping, noSonarFilter, checksBridge, project);
  }

  private void reportIssues(ResourceMapping resourceMapping, NoSonarFilter noSonarFilter, ChecksBridge checksBridge, Project project) {
    ProjectIssue projectIssue = null;
    if (StringUtils.isNotBlank(settings.getString(JavaPlugin.JSON_OUTPUT_FOLDER))) {
      projectIssue = new ProjectIssue();
    }
    for (Resource directory : resourceMapping.directories()) {
      checksBridge.reportIssueForPackageInfo((Directory) directory, project, projectIssue);
      for (Resource sonarFile : resourceMapping.files((Directory) directory)) {
        String key = resourceMapping.getFileKeyByResource((org.sonar.api.resources.File) sonarFile);
        // key would be null for test files as they are not in squid index.
        if (key != null) {
          SourceFile squidFile = (SourceFile) squid.search(key);
          if (squidFile != null) {
            noSonarFilter.addResource(sonarFile, squidFile.getNoSonarTagLines());
            checksBridge.reportIssues(squidFile, sonarFile, projectIssue);
          } else {
            LOG.error("Could not report issue on file: " + sonarFile.getKey());
          }
        }
      }
    }
    outputJson(projectIssue);
  }

  private void outputJson(@Nullable ProjectIssue projectIssue) {
    if (projectIssue != null) {
      File folder = new File(settings.getString(JavaPlugin.JSON_OUTPUT_FOLDER));
      folder.mkdir();
      LOG.info("Outputing json files to folder: " + folder.getAbsolutePath());
      for (Map.Entry<RuleKey, RuleIssues> entry : projectIssue.rules.entrySet()) {
        try (PrintWriter pw = new PrintWriter(folder.getAbsolutePath() + File.separator + "squid-" + entry.getKey().rule() + ".json", Charsets.UTF_8.name())) {
          writeIssueFile(pw, entry.getValue());
        } catch (IOException e) {
          LOG.error("Could not output json file for rule : " + entry.getKey(), e);
        }
      }
    }
  }

  private static void writeIssueFile(Writer pw, RuleIssues ruleIssues) throws IOException {
    pw.write("{\n");
    for (Map.Entry<String, FileIssues> stringFileIssuesEntry : ruleIssues.files.entrySet()) {
      FileIssues fileIssues = stringFileIssuesEntry.getValue();
      if (!fileIssues.getLines().isEmpty()) {
        pw.write("'project:" + stringFileIssuesEntry.getKey() + "':[\n");
        for (Integer line : fileIssues.getLines()) {
          pw.write(line + ",\n");
        }
        pw.write("],\n");
      }
    }
    pw.write("}\n");
  }

  public static class ProjectIssue {
    Map<RuleKey, RuleIssues> rules;

    public ProjectIssue() {
      this.rules = new HashMap<>();
    }

    public void addIssue(RuleKey ruleKey, String name, @Nullable Integer line) {
      RuleIssues ruleIssues = rules.get(ruleKey);
      if (ruleIssues == null) {
        ruleIssues = new RuleIssues();
        rules.put(ruleKey, ruleIssues);
      }
      ruleIssues.addIssue(name, line);
    }
  }

  private static class RuleIssues {
    Map<String, FileIssues> files = new TreeMap<>();

    public void addIssue(String name, @Nullable Integer line) {
      FileIssues fileIssues = files.get(name);
      if (fileIssues == null) {
        fileIssues = new FileIssues();
        files.put(name, fileIssues);
      }
      fileIssues.lines.add(line == null ? 0 : line);
    }
  }

  private static class FileIssues {
    List<Integer> lines = new ArrayList<>();

    public List<Integer> getLines() {
      Collections.sort(lines);
      return lines;
    }
  }

}
