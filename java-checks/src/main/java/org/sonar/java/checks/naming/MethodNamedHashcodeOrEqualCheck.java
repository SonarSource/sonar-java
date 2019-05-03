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
package org.sonar.java.checks.naming;

import java.util.Collections;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S1221")
public class MethodNamedHashcodeOrEqualCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    IdentifierTree methodIdentifier = ((MethodTree) tree).simpleName();
    switch (methodIdentifier.name()) {
      case "hashcode":
        report(methodIdentifier, "hashCode()");
        break;
      case "equal":
        report(methodIdentifier, "equals(Object obj)");
        break;
      case "tostring":
        report(methodIdentifier, "toString()");
        break;
      default:
        // do nothing
        break;
    }
  }

  private void report(IdentifierTree methodIdentifier, String substitute) {
    reportIssue(methodIdentifier, "Either override Object." + substitute + ", or totally rename the method to prevent any confusion.");
  }

}
