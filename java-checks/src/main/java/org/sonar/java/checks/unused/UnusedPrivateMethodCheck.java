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
package org.sonar.java.checks.unused;

import com.google.common.collect.ImmutableList;
import org.sonar.check.Rule;
import org.sonar.java.RspecKey;
import org.sonar.java.checks.serialization.SerializableContract;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.List;

@Rule(key = "UnusedPrivateMethod")
@RspecKey("S1144")
public class UnusedPrivateMethodCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.METHOD, Tree.Kind.CONSTRUCTOR);
  }

  @Override
  public void visitNode(Tree tree) {
    if(!hasSemantic()) {
      return;
    }
    MethodTree node = (MethodTree) tree;
    Symbol symbol = node.symbol();
    if (node.modifiers().annotations().isEmpty() && symbol.isPrivate() && symbol.usages().isEmpty()) {
      if (node.is(Tree.Kind.CONSTRUCTOR)) {
        if (!node.parameters().isEmpty()) {
          reportIssue(node.simpleName(), "Remove this unused private \"" + node.simpleName().name() + "\" constructor.");
        }
      } else if (!SerializableContract.SERIALIZABLE_CONTRACT_METHODS.contains(symbol.name())) {
        reportIssue(node.simpleName(), "Remove this unused private \"" + symbol.name() + "\" method.");
      }
    }
  }

}
