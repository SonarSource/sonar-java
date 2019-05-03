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
package org.sonar.java.checks;

import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import java.util.Arrays;
import java.util.List;

import static org.sonar.java.checks.helpers.ExpressionsHelper.reportOnClassTree;

@Rule(key = "S1820")
public class ClassFieldCountCheck extends IssuableSubscriptionVisitor {

  private static final int DEFAULT_THRESHOLD = 20;
  private static final boolean DEFAULT_COUNT_NON_PUBLIC_FIELDS = true;

  @RuleProperty(key = "maximumFieldThreshold", description = "The maximum number of fields", defaultValue = "" + DEFAULT_THRESHOLD)
  private int threshold = DEFAULT_THRESHOLD;

  @RuleProperty(key = "countNonpublicFields", description = "Whether or not to include non-public fields in the count", defaultValue = "" + DEFAULT_COUNT_NON_PUBLIC_FIELDS)
  private boolean countNonPublicFields = DEFAULT_COUNT_NON_PUBLIC_FIELDS;

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.CLASS, Tree.Kind.INTERFACE, Tree.Kind.ENUM);
  }

  @Override
  public void visitNode(Tree tree) {
    if (!hasSemantic()) {
      return;
    }
    ClassTree classTree = (ClassTree) tree;
    long fieldCount = classTree.members().stream()
      .filter(member -> member.is(Tree.Kind.VARIABLE) && shouldBeCounted((VariableTree) member))
      .count();
    if (fieldCount > threshold) {
      String message = String.format("Refactor this class so it has no more than %d %sfields, rather than the %d it currently has.", threshold,
        countNonPublicFields ? "" : "public ", fieldCount);
      reportIssue(reportOnClassTree(classTree), message);
    }
  }

  private boolean shouldBeCounted(VariableTree variableTree) {
    Symbol symbol = variableTree.symbol();
    if (symbol.isStatic() && symbol.isFinal()) {
      return false;
    }
    return countNonPublicFields || symbol.isPublic();
  }

  public void setThreshold(int threshold) {
    this.threshold = threshold;
  }

  public void setCountNonPublicFields(boolean countNonPublicFields) {
    this.countNonPublicFields = countNonPublicFields;
  }
}
