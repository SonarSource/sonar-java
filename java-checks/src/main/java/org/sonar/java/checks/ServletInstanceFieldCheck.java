/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
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
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.model.ModifiersUtils;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.ModifiersTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.List;

@Rule(
  key = "S2226",
  name = "Servlets should never have mutable instance fields",
  tags = {"bug", "cert", "multi-threading"},
  priority = Priority.CRITICAL)
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.SYNCHRONIZATION_RELIABILITY)
@SqaleConstantRemediation("30min")
public class ServletInstanceFieldCheck extends SubscriptionBaseVisitor {

  @Override
  public List<Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.VARIABLE);
  }

  @Override
  public void visitNode(Tree tree) {
    VariableTree variable = (VariableTree) tree;
    if (hasSemantic() && isOwnedByAServlet(variable) && !isStaticOrFinal(variable)) {
      addIssue(tree, "Remove this misleading mutable servlet instance fields or make it \"static\" and/or \"final\"");
    }
  }

  private static boolean isOwnedByAServlet(VariableTree variable) {
    Symbol owner = variable.symbol().owner();
    return owner.isTypeSymbol() && (owner.type().isSubtypeOf("javax.servlet.http.HttpServlet") || owner.type().isSubtypeOf("org.apache.struts.action.Action"));
  }

  private static boolean isStaticOrFinal(VariableTree variable) {
    ModifiersTree modifiers = variable.modifiers();
    return ModifiersUtils.hasModifier(modifiers, Modifier.STATIC) || ModifiersUtils.hasModifier(modifiers, Modifier.FINAL);
  }

}
