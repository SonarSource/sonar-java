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
package org.sonar.java.checks.synchronization;

import org.sonar.check.Rule;
import org.sonar.java.checks.serialization.SerializableContract;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.model.ModifiersUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.SynchronizedStatementTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.Collections;
import java.util.List;

import static org.sonar.java.model.ModifiersUtils.hasModifier;
import static org.sonar.plugins.java.api.tree.Modifier.SYNCHRONIZED;
import static org.sonar.plugins.java.api.tree.Tree.Kind.METHOD;

@Rule(key = "S3042")
public class WriteObjectTheOnlySynchronizedMethodCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    if (!hasSemantic()) {
      return;
    }
    MethodTree methodTree = (MethodTree) tree;
    Symbol.TypeSymbol enclosingClass = methodTree.symbol().enclosingClass();
    String className = enclosingClass.type().fullyQualifiedName();
    MethodMatcher writeObjectMatcher = SerializableContract.writeObjectMatcher(className);
    if (writeObjectMatcher.matches(methodTree) && hasModifier(methodTree.modifiers(), SYNCHRONIZED)) {
      SynchronizationVisitor visitor = new SynchronizationVisitor(methodTree);
      enclosingClass.declaration().accept(visitor);
      if (!visitor.moreThanSingleLock) {
        reportIssue(ModifiersUtils.getModifier(methodTree.modifiers(), SYNCHRONIZED), "Remove this \"synchronized\" keyword.");
      }
    }
  }

  private static class SynchronizationVisitor extends BaseTreeVisitor {

    private final MethodTree writeObjectMethodTree;
    private boolean moreThanSingleLock;

    public SynchronizationVisitor(MethodTree writeObjectMethodTree) {
      this.writeObjectMethodTree = writeObjectMethodTree;
    }

    @Override
    public void visitSynchronizedStatement(SynchronizedStatementTree tree) {
      moreThanSingleLock = true;
    }

    @Override
    public void visitMethod(MethodTree tree) {
      if (tree.equals(writeObjectMethodTree) || moreThanSingleLock) {
        return;
      }
      if (hasModifier(tree.modifiers(), SYNCHRONIZED)) {
        moreThanSingleLock = true;
        return;
      }
      super.visitMethod(tree);
    }
  }
}
