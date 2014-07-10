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
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(
  key = EqualsNotOverridenWithCompareToCheck.RULE_KEY,
  priority = Priority.CRITICAL,
  tags={"bug"})
@BelongsToProfile(title = "Sonar way", priority = Priority.CRITICAL)
public class EqualsNotOverridenWithCompareToCheck extends BaseTreeVisitor implements JavaFileScanner {

  public static final String RULE_KEY = "S1210";
  private final RuleKey ruleKey = RuleKey.of(CheckList.REPOSITORY_KEY, RULE_KEY);
  private JavaFileScannerContext context;

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;
    scan(context.getTree());
  }

  @Override
  public void visitClass(ClassTree tree) {

    if (tree.is(Tree.Kind.CLASS) || tree.is(Tree.Kind.ENUM)) {
      boolean hasEquals = false;
      MethodTree compare = null;
      for (Tree member : tree.members()) {
        if (member.is(Tree.Kind.METHOD)) {
          MethodTree method = (MethodTree) member;
          if (method.parameters().size() == 1) {
            String name = method.simpleName().name();
            if (name.equals("equals")) {
              hasEquals = true;
            } else if (name.equals("compareTo")) {
              compare = method;
            }
          }
        }
      }
      if (compare != null && !hasEquals) {
        context.addIssue(compare, ruleKey, "Override \"equals(Object obj)\" to comply with the contract of the \"compareTo(T o)\" method.");
      }
    }
    super.visitClass(tree);
  }
}
