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
import java.util.HashSet;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.ImportTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

/**
 *
 * Squid implementation for checkstyle <a href="checkstyle.sourceforge.net/config_imports.html#AvoidStarImport">AvoidStarImport</a> rule
 *
 */
@Rule(
  key = "AvoidStarImportCheck",
  name = "Checks that there are no import statements that use the * notation",
  tags = {"convention", "bug"},
  priority = Priority.MINOR)
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.READABILITY)
@SqaleConstantRemediation("1min")
public class AvoidStarImportCheck extends IssuableSubscriptionVisitor {

  private static final String DEFAULT_EXCLUDE = "";

  private static final boolean DEFAULT_ALLOW = false;

  @RuleProperty(
    key = "AllowClassImports",
    description = "Whether to allow starred class imports like import java.util.*;",
    defaultValue = "" + DEFAULT_ALLOW)
  public boolean allowClassImports = DEFAULT_ALLOW;

  @RuleProperty(
    key = "AllowStaticMemberImports",
    description = "whether to allow starred static member imports like import static org.junit.Assert.*",
    defaultValue = "" + DEFAULT_ALLOW)
  public boolean allowStaticMemberImports = DEFAULT_ALLOW;

  @RuleProperty(
    key = "Exclude",
    description = "Comma separated list of packages where star imports are allowed (Note that this property is not recursive, subpackages of excluded packages are not automatically excluded)",
    defaultValue = "" + DEFAULT_EXCLUDE)
  public String exclude = DEFAULT_EXCLUDE;
  private HashSet<String> excludePackages = new HashSet<>();

  @Override
  public List<Kind> nodesToVisit() {
    if (StringUtils.isNotBlank(exclude)) {
      // Add .* suffix to accelerate the match
      for (String p : exclude.split(",")) {
        excludePackages.add(p + ".*");
      }
    }
    return ImmutableList.of(Tree.Kind.IMPORT);
  }

  @Override
  public void visitNode(Tree tree) {
    // Visit import only => cast
    ImportTree importTree = (ImportTree) tree;
    String importName = fullQualifiedName(importTree.qualifiedIdentifier());

    // There is a star in the import
    if (importName.endsWith(".*")) {
      boolean allowed = allowClassImports && !importTree.isStatic();
      boolean allowedStatic = allowStaticMemberImports && importTree.isStatic();
      boolean excluded = !excludePackages.isEmpty() && excludePackages.contains(importName);
      if (!allowed && !allowedStatic && !excluded) {
        addIssue(importTree, String.format("Using the '.*' form of import should be avoided - %s", importName));
      }
    }
  }

  /**
   * Get full qualified name for tree
   *
   * @param tree Tree
   * @return String
   */
  public String fullQualifiedName(Tree tree) {
    if (tree.is(Tree.Kind.IDENTIFIER)) {
      return ((IdentifierTree) tree).name();
    } else if (tree.is(Tree.Kind.MEMBER_SELECT)) {
      MemberSelectExpressionTree m = (MemberSelectExpressionTree) tree;
      return fullQualifiedName(m.expression()) + "." + m.identifier().name();
    }
    throw new UnsupportedOperationException(String.format("Kind/Class '%s' not supported", tree.getClass()));
  }
}
