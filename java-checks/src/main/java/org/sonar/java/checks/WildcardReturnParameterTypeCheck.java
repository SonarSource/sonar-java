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

import org.apache.commons.lang.BooleanUtils;
import org.sonar.api.rule.RuleKey;
import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.model.declaration.MethodTreeImpl;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.WildcardTree;

@Rule(
    key = WildcardReturnParameterTypeCheck.RULE_KEY,
    priority = Priority.MAJOR)
@BelongsToProfile(title = "Sonar way", priority = Priority.MAJOR)
public class WildcardReturnParameterTypeCheck extends BaseTreeVisitor implements JavaFileScanner {


  public static final String RULE_KEY = "S1452";
  private final RuleKey ruleKey = RuleKey.of(CheckList.REPOSITORY_KEY, RULE_KEY);
  private JavaFileScannerContext context;
  private boolean checkingReturnType;

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;
    checkingReturnType = false;
    scan(context.getTree());
  }

  @Override
  public void visitMethod(MethodTree tree) {
    if (!isOverriding(tree)) {
      checkingReturnType = true;
      scan(tree.returnType());
      checkingReturnType = false;
    }
    super.visitMethod(tree);
  }

  private boolean isOverriding(MethodTree tree) {
    return BooleanUtils.isTrue(((MethodTreeImpl) tree).isOverriding());
  }

  @Override
  public void visitWildcard(WildcardTree tree) {
    if (checkingReturnType) {
      context.addIssue(tree, ruleKey, "Remove usage of generic wildcard type.");
    }
  }
}
