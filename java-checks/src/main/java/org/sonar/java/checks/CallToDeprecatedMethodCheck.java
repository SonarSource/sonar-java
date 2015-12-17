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
import org.sonar.java.tag.Tag;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.List;

@Rule(
  key = "CallToDeprecatedMethod",
  name = "\"@Deprecated\" code should not be used",
  priority = Priority.MINOR,
  tags = {Tag.CWE, Tag.OBSOLETE, Tag.OWASP_A9, Tag.SECURITY})
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.SOFTWARE_RELATED_PORTABILITY)
@SqaleConstantRemediation("15min")
public class CallToDeprecatedMethodCheck extends SubscriptionBaseVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.IDENTIFIER);
  }

  @Override
  public void visitNode(Tree tree) {
    Symbol symbol = ((IdentifierTree) tree).symbol();
    if (symbol.metadata().isAnnotatedWith("java.lang.Deprecated")) {
      String name;
      if (symbol.isMethodSymbol() && "<init>".equals(symbol.name())) {
        name = symbol.owner().name();
      } else {
        name = symbol.name();
      }
      reportIssue(tree, "Remove this use of \"" + name + "\"; it is deprecated.");
    }
  }
}
