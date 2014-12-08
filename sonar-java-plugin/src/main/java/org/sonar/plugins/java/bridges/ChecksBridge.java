/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
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
import org.sonar.java.checks.CheckList;
import org.sonar.java.checks.PackageInfoCheck;
import org.sonar.squidbridge.api.CheckMessage;
import org.sonar.squidbridge.api.CodeVisitor;
import org.sonar.squidbridge.api.SourceFile;

import java.io.File;
import java.util.Set;

public class ChecksBridge {

  private final Checks<CodeVisitor> checks;
  private final ResourcePerspectives resourcePerspectives;
  private final RulesProfile rulesProfile;
  private Set<Directory> dirsWithoutPackageInfo;

  public ChecksBridge(Checks<CodeVisitor> checks, ResourcePerspectives resourcePerspectives, RulesProfile rulesProfile) {
    this.checks = checks;
    this.resourcePerspectives = resourcePerspectives;
    this.rulesProfile = rulesProfile;
  }

  public void reportIssues(SourceFile squidFile, Resource sonarFile) {
    if (squidFile.hasCheckMessages()) {
      Issuable issuable = resourcePerspectives.as(Issuable.class, sonarFile);
      Set<CheckMessage> messages = squidFile.getCheckMessages();
      for (CheckMessage checkMessage : messages) {
        Object check = checkMessage.getCheck();
        RuleKey ruleKey;
        if (check instanceof RuleKey) {
          // VisitorsBridge uses RuleKey
          ruleKey = (RuleKey) check;
        } else {
          ruleKey = checks.ruleKey((CodeVisitor) checkMessage.getCheck());
        }
        if (ruleKey != null) {
          Issue issue = issuable.newIssueBuilder()
              .ruleKey(ruleKey)
              .line(checkMessage.getLine())
              .message(checkMessage.formatDefaultMessage()).build();
          issuable.addIssue(issue);
        }
      }
      // Remove from memory:
      messages.clear();
    }
  }

  public void reportIssueForPackageInfo(Directory directory, Project project) {
    if (dirsWithoutPackageInfo == null) {
      initSetOfDirs(project);
    }
    if (dirsWithoutPackageInfo.contains(directory)) {
      Issuable issuable = resourcePerspectives.as(Issuable.class, directory);
      if (issuable != null) {
        Issue issue = issuable.newIssueBuilder().ruleKey(RuleKey.of(CheckList.REPOSITORY_KEY, PackageInfoCheck.RULE_KEY))
            .message("Add a 'package-info.java' file to document the '" + directory.getPath() + "' package").build();
        issuable.addIssue(issue);
      }
    }
  }

  private void initSetOfDirs(Project project) {
    dirsWithoutPackageInfo = Sets.newHashSet();
    ActiveRule activeRule = rulesProfile.getActiveRule(CheckList.REPOSITORY_KEY, PackageInfoCheck.RULE_KEY);
    if (activeRule != null) {
      CodeVisitor check = checks.of(activeRule.getRule().ruleKey());
      if (check != null) {
        Set<File> dirs = ((PackageInfoCheck) check).getDirectoriesWithoutPackageFile();
        for (File dir : dirs) {
          dirsWithoutPackageInfo.add(Directory.fromIOFile(dir, project));
        }
      }
    }
  }
}
