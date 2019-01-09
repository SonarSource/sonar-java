/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
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
package org.sonar.java.checks;

import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.java.checks.helpers.ExpressionsHelper;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Rule(key = "S1191")
public class SunPackagesUsedCheck extends BaseTreeVisitor implements JavaFileScanner {

  private List<Tree> reportedTrees = new ArrayList<>();

  private static final String DEFAULT_EXCLUDE = "";

  @RuleProperty(
      key = "Exclude",
      description = "Comma separated list of Sun packages to be ignored by this rule. Example: com.sun.jna,sun.misc",
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
      .map(tree -> new JavaFileScannerContext.Location("", tree))
      .collect(Collectors.toList());

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
    return reference.startsWith("com.sun.") || reference.startsWith("sun.");
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
