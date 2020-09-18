/*
 * SonarQube Java
 * Copyright (C) 2012-2020 SonarSource SA
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
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.CatchTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.InstanceOfTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Rule(key = "S1193")
public class InstanceofUsedOnExceptionCheck extends IssuableSubscriptionVisitor {

  private final Set<String> caughtVariables = new HashSet<>();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.CATCH, Tree.Kind.INSTANCE_OF);
  }

  @Override
  public void setContext(JavaFileScannerContext context) {
    caughtVariables.clear();
    super.setContext(context);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Tree.Kind.CATCH)) {
      caughtVariables.add(((CatchTree) tree).parameter().simpleName().name());
    } else {
      InstanceOfTree instanceOfTree = (InstanceOfTree) tree;
      if (isLeftOperandAndException(instanceOfTree)
        && instanceOfTree.type().symbolType().isSubtypeOf("java.lang.Throwable")) {
        reportIssue(instanceOfTree.instanceofKeyword(), "Replace the usage of the \"instanceof\" operator by a catch block.");
      }
    }
  }

  @Override
  public void leaveNode(Tree tree) {
    if(tree.is(Tree.Kind.CATCH)) {
      caughtVariables.remove(((CatchTree) tree).parameter().simpleName().name());
    }
  }

  private boolean isLeftOperandAndException(InstanceOfTree tree) {
    return tree.expression().is(Tree.Kind.IDENTIFIER)
      && caughtVariables.contains(((IdentifierTree) tree.expression()).name());
  }
}
