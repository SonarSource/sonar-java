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
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.Arrays;
import java.util.List;

@Rule(key = "S818")
public class UppercaseSuffixesCheck extends IssuableSubscriptionVisitor {

  @RuleProperty(
    key = "checkOnlyLong",
    description = "Set to \"true\" to ignore \"float\" and \"double\" declarations.",
    defaultValue = "false")
  public boolean checkOnlyLong = false;

  private static final char LONG = 'l';
  private static final char DOUBLE = 'd';
  private static final char FLOAT = 'f';

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.DOUBLE_LITERAL, Tree.Kind.FLOAT_LITERAL, Tree.Kind.LONG_LITERAL);
  }

  @Override
  public void visitNode(Tree tree) {
    String value = ((LiteralTree) tree).value();
    char suffix = value.charAt(value.length() - 1);
    switch (suffix) {
      case DOUBLE:
      case FLOAT:
        if (checkOnlyLong) {
          return;
        }
      case LONG:
        reportIssue(tree, "Upper-case this literal \"" + suffix + "\" suffix.");
        break;
      default:
        // do nothing
    }
  }
}
