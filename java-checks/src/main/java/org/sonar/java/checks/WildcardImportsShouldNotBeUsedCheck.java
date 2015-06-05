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
import java.util.List;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.ImportTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

/**
 *
 * Squid implementation of <a href="http://jira.sonarsource.com/browse/RSPEC-2208">S2208</a> for Java (<a href="http://jira.sonarsource.com/browse/RSPEC-2860">RSPEC-2860</a>)</br>
 * Reference Checkstyle:<a href="checkstyle.sourceforge.net/config_imports.html#AvoidStarImport">AvoidStarImport</a> and Pylint:W0401
 *
 */
@Rule(
  key = "S2208",
  name = "Wildcard imports should not be used",
  tags = {"pitfall"},
  priority = Priority.MAJOR)
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.READABILITY)
@SqaleConstantRemediation("5min")
@ActivatedByDefault
public class WildcardImportsShouldNotBeUsedCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.IMPORT);
  }

  @Override
  public void visitNode(Tree tree) {
    ImportTree importTree = (ImportTree) tree;

    // See RSPEC-2208 : exception with static imports.
    if (fullQualifiedName(importTree.qualifiedIdentifier()).endsWith(".*") && !importTree.isStatic()) {
      addIssue(importTree, "Explicitly import the specific classes needed.");
    }
  }

  private static String fullQualifiedName(Tree tree) {
    if (tree.is(Tree.Kind.IDENTIFIER)) {
      return ((IdentifierTree) tree).name();
    } else if (tree.is(Tree.Kind.MEMBER_SELECT)) {
      MemberSelectExpressionTree m = (MemberSelectExpressionTree) tree;
      return fullQualifiedName(m.expression()) + "." + m.identifier().name();
    }
    throw new UnsupportedOperationException(String.format("Kind/Class '%s' not supported", tree.getClass()));
  }
}
