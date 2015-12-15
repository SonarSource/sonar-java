/*
 * SonarQube Java
 * Copyright (C) 2012-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.model.ModifiersUtils;
import org.sonar.java.tag.Tag;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.List;

@Rule(
  key = "S2885",
  name = "\"Calendars\" and \"DateFormats\" should not be static",
  priority = Priority.CRITICAL,
  tags = {Tag.BUG, Tag.MULTI_THREADING})
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.SYNCHRONIZATION_RELIABILITY)
@SqaleConstantRemediation("15min")
public class StaticMultithreadedUnsafeFieldsCheck extends SubscriptionBaseVisitor {

  private static final String[] FORBIDDEN_TYPES = {"java.text.SimpleDateFormat", "java.util.Calendar"};

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.VARIABLE);
  }

  @Override
  public void visitNode(Tree tree) {
    VariableTree variableTree = (VariableTree) tree;
    if (ModifiersUtils.hasModifier(variableTree.modifiers(), Modifier.STATIC) && isForbiddenType(variableTree.type().symbolType())) {
      IdentifierTree identifierTree = variableTree.simpleName();
      addIssue(identifierTree, String.format("Make \"%s\" an instance variable.", identifierTree.name()));
    }
  }

  private static boolean isForbiddenType(Type type) {
    for (String name : FORBIDDEN_TYPES) {
      if (type.isSubtypeOf(name)) {
        return true;
      }
    }
    return false;
  }

}
