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
package org.sonar.java.checks;

import org.sonar.api.rule.RuleKey;
import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.MethodTree;

@Rule(
  key = TooManyParameters_S00107_Check.RULE_KEY,
  priority = Priority.MAJOR,
  tags={"brain-overload"})
@BelongsToProfile(title = "Sonar way", priority = Priority.MAJOR)
public class TooManyParameters_S00107_Check extends BaseTreeVisitor implements JavaFileScanner {

  public static final String RULE_KEY = "S00107";
  private final RuleKey ruleKey = RuleKey.of(CheckList.REPOSITORY_KEY, RULE_KEY);

  private static final int DEFAULT_MAXIMUM = 7;

  @RuleProperty(
    key = "maximumMethodParameters",
    defaultValue = "" + DEFAULT_MAXIMUM)
  public int maximum = DEFAULT_MAXIMUM;

  private JavaFileScannerContext context;

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;
    scan(context.getTree());
  }

  @Override
  public void visitMethod(MethodTree tree) {
    int count = tree.parameters().size();
    if (count > maximum) {
      context.addIssue(tree, ruleKey, "Method has " + count + " parameters, which is greater than " + maximum + " authorized.");
    }

    super.visitMethod(tree);
  }

}
