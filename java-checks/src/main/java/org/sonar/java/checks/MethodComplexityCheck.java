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
import org.sonar.check.RuleProperty;
import org.sonar.java.ast.visitors.ComplexityVisitorST;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.text.MessageFormat;
import java.util.List;

@Rule(key = "MethodCyclomaticComplexity", priority = Priority.MAJOR,
    tags = {"brain-overload"})
@BelongsToProfile(title = "Sonar way", priority = Priority.MAJOR)
public class MethodComplexityCheck extends SubscriptionBaseVisitor {

  private static final int DEFAULT_MAX = 10;

  @RuleProperty(defaultValue = "" + DEFAULT_MAX)
  private int max = DEFAULT_MAX;

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.METHOD, Tree.Kind.CONSTRUCTOR);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodTree methodTree = (MethodTree) tree;
    int complexity = new ComplexityVisitorST().scan(tree);
    if (complexity > max) {
      addIssue(tree, MessageFormat.format(
          "The Cyclomatic Complexity of this method \"{0}\" is {1,number,integer} which is greater than {2,number,integer} authorized.",
          methodTree.simpleName().name(), complexity, max));
    }
  }

  public void setMax(int max) {
    this.max = max;
  }
}
