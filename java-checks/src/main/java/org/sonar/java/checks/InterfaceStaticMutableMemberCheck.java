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
import org.sonar.java.resolve.Type;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.VariableTree;

import java.text.MessageFormat;
import java.util.List;

@Rule(
  key = "S2386",
  priority = Priority.CRITICAL,
  tags = {"unpredictable"})
@BelongsToProfile(title = "Sonar way", priority = Priority.CRITICAL)
public class InterfaceStaticMutableMemberCheck extends SubscriptionBaseVisitor {

  @Override
  public List<Kind> nodesToVisit() {
    return ImmutableList.of(Kind.INTERFACE);
  }

  @Override
  public void visitNode(Tree tree) {
    for (Tree member : ((ClassTree) tree).members()) {
      if (member.is(Kind.VARIABLE)) {
        VariableTreeImpl variableTree = (VariableTreeImpl) member;
        if (isStaticMember(variableTree) && isMutableMember(variableTree)) {
          addIssue(variableTree, MessageFormat.format("Move \"{0}\" to a class and lower its visibility", variableTree.simpleName().name()));
        }
      }
    }
  }

  private boolean isStaticMember(VariableTree variableTree) {
    return variableTree.modifiers().modifiers().contains(Modifier.STATIC);
  }

  private boolean isMutableMember(VariableTreeImpl variableTree) {
    return isArray(variableTree.type()) || isDateOrCollection(variableTree.getSymbol().getType());
  }

  private boolean isArray(Tree typeTree) {
    return typeTree.is(Kind.ARRAY_TYPE);
  }

  private boolean isDateOrCollection(Type variableSymbolType) {
    return variableSymbolType.is("java.util.Date") || variableSymbolType.isSubtypeOf("java.util.Collection");
  }
}
