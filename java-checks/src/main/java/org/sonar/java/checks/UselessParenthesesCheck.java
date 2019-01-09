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
import org.sonar.java.RspecKey;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.ParenthesizedTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;

import java.util.Collections;
import java.util.List;

@Rule(key = "UselessParenthesesCheck")
@RspecKey("S1110")
public class UselessParenthesesCheck extends IssuableSubscriptionVisitor {

  @Override
  public void visitNode(Tree tree) {
    ParenthesizedTree parenthesizedTree = (ParenthesizedTree) tree;
    if (parenthesizedTree.expression().is(Kind.PARENTHESIZED_EXPRESSION)) {
      reportIssue(((ParenthesizedTree) parenthesizedTree.expression()).openParenToken(),
          "Remove these useless parentheses.",
          Collections.singletonList(new JavaFileScannerContext.Location("", parenthesizedTree.closeParenToken())), null);
    }
  }

  @Override
  public List<Kind> nodesToVisit() {
    return Collections.singletonList(Kind.PARENTHESIZED_EXPRESSION);
  }
}
