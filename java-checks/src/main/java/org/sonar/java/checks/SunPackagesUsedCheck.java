/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.checks;

import java.util.ArrayList;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.java.checks.helpers.ExpressionsHelper;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S1191")
public class SunPackagesUsedCheck extends BaseTreeVisitor implements JavaFileScanner {

  private List<Tree> reportedTrees = new ArrayList<>();

  private static final String DEFAULT_EXCLUDE = "";

  @RuleProperty(
      key = "Exclude",
      description = "Comma separated list of Sun packages to be ignored by this rule. Example: sun.misc,sun.security.validator",
      defaultValue = "" + DEFAULT_EXCLUDE)
  public String exclude = DEFAULT_EXCLUDE;
  private String[] excludePackages = null;
  

  @Override
  public void scanFile(JavaFileScannerContext context) {
    reportedTrees.clear();
    excludePackages = exclude.split(",");
    scan(context.getTree());

    if (!reportedTrees.isEmpty()) {
      reportIssueWithSecondaries(context);
    }
  }

  private void reportIssueWithSecondaries(JavaFileScannerContext context) {
    List<JavaFileScannerContext.Location> secondaries = reportedTrees.stream()
      .skip(1)
      .map(tree -> new JavaFileScannerContext.Location("Replace also this \"Sun\" reference.", tree))
      .toList();

    int effortToFix = reportedTrees.size();
    context.reportIssue(this, reportedTrees.get(0), "Use classes from the Java API instead of Sun classes.", secondaries, effortToFix);
  }

  @Override
  public void visitMemberSelectExpression(MemberSelectExpressionTree tree) {
    String reference = ExpressionsHelper.concatenate(tree);
    if (!isExcluded(reference) && isSunClass(reference)) {
      reportedTrees.add(tree);
    }
  }

  private static boolean isSunClass(String reference) {
    return reference.startsWith("sun.");
  }

  private boolean isExcluded(String reference) {
    for (String str : excludePackages) {
      if (!str.isEmpty() && reference.startsWith(str)) {
        return true;
      }
    }
    return false;
  }
}
