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
import org.sonar.java.model.BaseTreeVisitor;
import org.sonar.java.model.ClassTree;
import org.sonar.java.model.JavaFileScanner;
import org.sonar.java.model.JavaFileScannerContext;
import org.sonar.java.model.MethodTree;
import org.sonar.java.model.Modifier;
import org.sonar.java.model.ModifiersTree;
import org.sonar.java.model.Tree;
import org.sonar.java.model.VariableTree;

@Rule(
  key = IncorrectOrderOfMembersCheck.RULE_KEY,
  priority = Priority.MINOR)
@BelongsToProfile(title = "Sonar way", priority = Priority.MINOR)
public class IncorrectOrderOfMembersCheck extends BaseTreeVisitor implements JavaFileScanner {

  public static final String RULE_KEY = "S1231";
  private final RuleKey ruleKey = RuleKey.of(CheckList.REPOSITORY_KEY, RULE_KEY);

  private JavaFileScannerContext context;

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;
    scan(context.getTree());
  }

  @Override
  public void visitClass(ClassTree tree) {
    int prev = 0;
    for (int i = 0; i < tree.members().size(); i++) {
      final Tree member = tree.members().get(i);
      final int typePriority;
      final ModifiersTree modifiers;
      if (member.is(Tree.Kind.VARIABLE)) {
        typePriority = 2;
        modifiers = ((VariableTree) member).modifiers();
      } else if (member.is(Tree.Kind.CONSTRUCTOR)) {
        typePriority = 3;
        modifiers = ((MethodTree) member).modifiers();
      } else if (member.is(Tree.Kind.METHOD)) {
        typePriority = 4;
        modifiers = ((MethodTree) member).modifiers();
      } else {
        continue;
      }
      boolean isStatic = false;
      int visibilityPriority = /* package local */ 2;
      for (Modifier modifier : modifiers.modifiers()) {
        switch (modifier) {
          case STATIC:
            isStatic = true;
            break;
          case PUBLIC:
            visibilityPriority = 0;
            break;
          case PROTECTED:
            visibilityPriority = 1;
            break;
          case PRIVATE:
            visibilityPriority = 3;
            break;
        }
      }
      int priority = typePriority;
      if (member.is(Tree.Kind.VARIABLE) && isStatic) {
        priority--;
      }
      priority *= 4;
      priority += visibilityPriority;

      if (priority < prev) {
        context.addIssue(tree, ruleKey, "Change order of members to comply with Java Coding conventions.");
        break;
      }
      prev = priority;
    }

    super.visitClass(tree);
  }

}
