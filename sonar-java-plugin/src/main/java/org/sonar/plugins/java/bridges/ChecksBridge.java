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
package org.sonar.plugins.java.bridges;

import com.google.common.collect.Sets;
import org.sonar.api.batch.rule.Checks;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.issue.Issuable;
import org.sonar.api.issue.Issue;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Directory;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.ActiveRule;
import org.sonar.java.SonarComponents;
import org.sonar.java.checks.CheckList;
import org.sonar.java.checks.PackageInfoCheck;
import org.sonar.plugins.java.Bridges;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.squidbridge.api.CheckMessage;
import org.sonar.squidbridge.api.CodeVisitor;
import org.sonar.squidbridge.api.SourceFile;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.io.File;
import java.util.Set;

public class ChecksBridge {

  private final Iterable<Checks<JavaCheck>> checks;
  private final ResourcePerspectives resourcePerspectives;
  private final RulesProfile rulesProfile;
  private Set<Directory> dirsWithoutPackageInfo;

  public ChecksBridge(SonarComponents sonarComponents, RulesProfile rulesProfile) {
    this.checks = sonarComponents.checks();
    this.resourcePerspectives = sonarComponents.getResourcePerspectives();
    this.rulesProfile = rulesProfile;
  }

  public void reportIssues(SourceFile squidFile, Resource sonarFile, @Nullable Bridges.ProjectIssue project) {
    if (squidFile.hasCheckMessages()) {
      Issuable issuable = resourcePerspectives.as(Issuable.class, sonarFile);
      Set<CheckMessage> messages = squidFile.getCheckMessages();
      if (issuable != null) {
        for (CheckMessage checkMessage : messages) {
          Object check = checkMessage.getCheck();

          RuleKey ruleKey = getRuleKey((JavaCheck) check);
          if (ruleKey == null) {
            throw new IllegalStateException("Cannot find rule key for instance of " + check.getClass());
          }
          if (project == null) {
            Issue issue = issuable.newIssueBuilder()
              .ruleKey(ruleKey)
              .line(checkMessage.getLine())
              .message(checkMessage.formatDefaultMessage())
              .effortToFix(checkMessage.getCost())
              .build();
            issuable.addIssue(issue);
          } else {
            project.addIssue(ruleKey, sonarFile.getKey(), checkMessage.getLine());
          }
        }
      }
      // Remove from memory:
      messages.clear();
    }
  }

  @CheckForNull
  private RuleKey getRuleKey(JavaCheck check) {
    for (Checks<JavaCheck> sonarChecks : checks) {
      RuleKey ruleKey = sonarChecks.ruleKey(check);
      if (ruleKey != null) {
        return ruleKey;
      }
    }
    return null;
  }

  @CheckForNull
  private JavaCheck checkInstanceOf(RuleKey ruleKey) {
    for (Checks<JavaCheck> sonarChecks : checks) {
      JavaCheck check = sonarChecks.of(ruleKey);
      if (check != null) {
        return check;
      }
    }
    return null;
  }

  public void reportIssueForPackageInfo(Directory directory, Project project, @Nullable Bridges.ProjectIssue projectIssue) {
    if (dirsWithoutPackageInfo == null) {
      initSetOfDirs(project);
    }
    if (dirsWithoutPackageInfo.contains(directory)) {
      RuleKey ruleKey = RuleKey.of(CheckList.REPOSITORY_KEY, PackageInfoCheck.RULE_KEY);
      if (projectIssue != null) {
        projectIssue.addIssue(ruleKey, directory.getKey(), null);
      } else {
        Issuable issuable = resourcePerspectives.as(Issuable.class, directory);
        if (issuable != null) {
          Issue issue = issuable.newIssueBuilder().ruleKey(ruleKey)
            .message("Add a 'package-info.java' file to document the '" + directory.getPath() + "' package").build();
          issuable.addIssue(issue);
        }
      }
    }
  }

  private void initSetOfDirs(Project project) {
    dirsWithoutPackageInfo = Sets.newHashSet();
    ActiveRule activeRule = rulesProfile.getActiveRule(CheckList.REPOSITORY_KEY, PackageInfoCheck.RULE_KEY);
    if (activeRule != null) {
      CodeVisitor check = checkInstanceOf(activeRule.getRule().ruleKey());
      if (check != null) {
        Set<File> dirs = ((PackageInfoCheck) check).getDirectoriesWithoutPackageFile();
        for (File dir : dirs) {
          dirsWithoutPackageInfo.add(Directory.fromIOFile(dir, project));
        }
      }
    }
  }
}
