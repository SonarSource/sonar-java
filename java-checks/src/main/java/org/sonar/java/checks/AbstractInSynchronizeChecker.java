/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.model.ModifiersUtils;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.Deque;
import java.util.List;

public abstract class AbstractInSynchronizeChecker extends AbstractMethodDetection {
  private Deque<Boolean> withinSynchronizedBlock = Lists.newLinkedList();

  @Override
  public void scanFile(JavaFileScannerContext context) {
    withinSynchronizedBlock.push(false);
    super.scanFile(context);
    withinSynchronizedBlock.clear();
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.METHOD_INVOCATION, Tree.Kind.SYNCHRONIZED_STATEMENT, Tree.Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Tree.Kind.METHOD_INVOCATION)) {
      super.visitNode(tree);
    } else if (tree.is(Tree.Kind.METHOD)) {
      withinSynchronizedBlock.push(ModifiersUtils.hasModifier(((MethodTree) tree).modifiers(), Modifier.SYNCHRONIZED));
    } else if (tree.is(Tree.Kind.SYNCHRONIZED_STATEMENT)) {
      withinSynchronizedBlock.push(true);
    }
  }

  @Override
  public void leaveNode(Tree tree) {
    if (tree.is(Tree.Kind.METHOD, Tree.Kind.SYNCHRONIZED_STATEMENT)) {
      withinSynchronizedBlock.pop();
    }
  }

  public boolean isInSyncBlock() {
    return withinSynchronizedBlock.peek();
  }

  public boolean hasAnyParentSync() {
    return withinSynchronizedBlock.contains(true);
  }

}
