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

import com.google.common.collect.ImmutableList;
import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.model.declaration.VariableTreeImpl;
import org.sonar.java.resolve.Symbol.VariableSymbol;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;

import java.util.List;

@Rule(
  key = "S2386",
  priority = Priority.CRITICAL,
  tags = {"unpredictable"})
@BelongsToProfile(title = "Sonar way", priority = Priority.CRITICAL)
public class InterfaceStaticMutableMemberCheck extends SubscriptionBaseVisitor {

  private static final String MESSAGE = "Move \"xxx\" to a class and lower its visibility";
  private static final String COLLECTION_QUALIFIED_NAME = "java.util.Collection";
  private static final String DATE_QUALIFIER_NAME = "java.util.Date";

  @Override
  public List<Kind> nodesToVisit() {
    return ImmutableList.of(Kind.INTERFACE);
  }

  @Override
  public void visitNode(Tree tree) {
    for (Tree member : ((ClassTree) tree).members()) {
      if (member.is(Kind.VARIABLE)) {
        VariableTreeImpl variableTree = (VariableTreeImpl) member;
        if (variableTree.modifiers().modifiers().contains(Modifier.STATIC)) {
          Tree variableType = variableTree.type();
          VariableSymbol symbol = variableTree.getSymbol();
          if (variableType.is(Kind.ARRAY_TYPE) ||
            (variableType.is(Kind.PARAMETERIZED_TYPE) && symbol.getType().isSubtypeOf(COLLECTION_QUALIFIED_NAME)) ||
            (variableType.is(Kind.IDENTIFIER) && (symbol.getType().is(DATE_QUALIFIER_NAME) || symbol.getType().isSubtypeOf(COLLECTION_QUALIFIED_NAME)))) {
            addIssue(variableTree, MESSAGE.replace("xxx", variableTree.simpleName().name()));
          }
        }
      }
    }
  }
}
