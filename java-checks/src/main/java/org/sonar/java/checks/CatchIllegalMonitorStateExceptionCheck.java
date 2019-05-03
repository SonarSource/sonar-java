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
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.CatchTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.UnionTypeTree;

import java.util.Collections;
import java.util.List;

@Rule(key = "S2235")
public class CatchIllegalMonitorStateExceptionCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.CATCH);
  }

  @Override
  public void visitNode(Tree tree) {
    CatchTree catchTree = (CatchTree) tree;
    TypeTree parameterTypeTree = catchTree.parameter().type();
    if (parameterTypeTree.is(Kind.UNION_TYPE)) {
      UnionTypeTree unionTypeTree = (UnionTypeTree) parameterTypeTree;
      for (TypeTree exceptionTypeTree : unionTypeTree.typeAlternatives()) {
        checkExceptionType(exceptionTypeTree);
      }
    } else {
      checkExceptionType(parameterTypeTree);
    }
  }

  private void checkExceptionType(TypeTree exceptionTypeTree) {
    if (exceptionTypeTree.symbolType().is("java.lang.IllegalMonitorStateException")) {
      reportIssue(exceptionTypeTree, "Refactor this piece of code to not catch IllegalMonitorStateException");
    }
  }

}
