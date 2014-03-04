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
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(
  key = PublicStaticFieldShouldBeFinalCheck.RULE_KEY,
  priority = Priority.MAJOR)
@BelongsToProfile(title = "Sonar way", priority = Priority.MAJOR)
public class PublicStaticFieldShouldBeFinalCheck extends BaseTreeVisitor implements JavaFileScanner {

  public static final String RULE_KEY = "S1444";
  private JavaFileScannerContext context;
  private final RuleKey ruleKey = RuleKey.of(CheckList.REPOSITORY_KEY, RULE_KEY);

  @Override
  public void scanFile(final JavaFileScannerContext context) {
    this.context = context;
    scan(context.getTree());
  }

  @Override
  public void visitClass(ClassTree tree) {
    if (tree.is(Tree.Kind.CLASS) || tree.is(Tree.Kind.ENUM)) {
      for (Tree member : tree.members()) {
        if (member.is(Tree.Kind.VARIABLE) && isPublicStaticNotFinal((VariableTree) member)) {
          context.addIssue(member, ruleKey, "Make this \"public static " + ((VariableTree) member).simpleName() + "\" field final");
        }
      }
    }
    super.visitClass(tree);
  }

  private boolean isPublicStaticNotFinal(VariableTree tree) {
    boolean isPublic = false, isStatic = false, isFinal = false;

    for (Modifier modifier : tree.modifiers().modifiers()) {
      if (modifier == Modifier.PUBLIC) {
        isPublic = true;
      } else if (modifier == Modifier.STATIC) {
        isStatic = true;
      } else if (modifier == Modifier.FINAL) {
        isFinal = true;
      }
    }
    return isPublic && isStatic && !isFinal;
  }
}
