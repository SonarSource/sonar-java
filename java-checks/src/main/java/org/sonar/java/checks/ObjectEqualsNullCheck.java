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
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(
  key = ObjectEqualsNullCheck.KEY,
  priority = Priority.CRITICAL)
@BelongsToProfile(title = "Sonar way", priority = Priority.CRITICAL)
public class ObjectEqualsNullCheck extends BaseTreeVisitor implements JavaFileScanner {

  public static final String KEY = "S1318";
  private static final RuleKey RULE_KEY = RuleKey.of(CheckList.REPOSITORY_KEY, KEY);

  private JavaFileScannerContext context;

  @Override
  public void scanFile(final JavaFileScannerContext context) {
    this.context = context;
    scan(context.getTree());
  }

  @Override
  public void visitMethodInvocation(MethodInvocationTree tree) {
    super.visitMethodInvocation(tree);

    if (isCallToEquals(tree.methodSelect()) &&
      tree.arguments().size() == 1 &&
      isNull(tree.arguments().get(0))) {
      context.addIssue(tree, RULE_KEY, "Use \"object == null\" instead of \"object.equals(null)\" to test for nullity to prevent null pointer exceptions.");
    }
  }

  private static boolean isCallToEquals(ExpressionTree tree) {
    if (!tree.is(Tree.Kind.MEMBER_SELECT)) {
      return false;
    }

    MemberSelectExpressionTree memberSelect = (MemberSelectExpressionTree) tree;
    return "equals".equals(memberSelect.identifier().name());
  }

  private static boolean isNull(ExpressionTree tree) {
    return tree.is(Tree.Kind.NULL_LITERAL);
  }

}
