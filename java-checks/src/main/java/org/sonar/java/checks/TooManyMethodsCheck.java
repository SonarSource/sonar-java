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

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.java.checks.helpers.ExpressionsHelper;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S1448")
public class TooManyMethodsCheck extends IssuableSubscriptionVisitor {

  private static final int DEFAULT_MAXIMUM = 35;

  @RuleProperty(
    key = "maximumMethodThreshold",
    description = "The maximum number of methods authorized in a class.",
    defaultValue = "" + DEFAULT_MAXIMUM
  )
  public int maximumMethodThreshold = DEFAULT_MAXIMUM;

  @RuleProperty(
    key = "countNonpublicMethods",
    description = "Whether or not to include non-public methods in the count.",
    defaultValue = "true"
  )
  public boolean countNonPublic = true;

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.CLASS, Tree.Kind.ENUM, Tree.Kind.INTERFACE, Tree.Kind.ANNOTATION_TYPE);
  }

  @Override
  public void visitNode(Tree tree) {
    if (!hasSemantic()) {
      return;
    }
    ClassTree classTree = (ClassTree) tree;
    List<Tree> methods = classTree.members().stream()
        .filter(member -> member.is(Tree.Kind.METHOD, Tree.Kind.CONSTRUCTOR) && (countNonPublic || ((MethodTree) member).symbol().isPublic()))
        .collect(Collectors.toList());

    if(shouldNotReportIssue(classTree, methods)) {
      return;
    }

    List<JavaFileScannerContext.Location> secondary = methods.stream()
      .map(element -> new JavaFileScannerContext.Location("Method + 1", element))
      .collect(Collectors.toList());

    String classDescription;
    if (classTree.simpleName() == null) {
      classDescription = "Anonymous class \"" + ((NewClassTree) classTree.parent()).identifier().symbolType().name() + "\"";
    } else {
      classDescription = classTree.declarationKeyword().text() + " \"" + classTree.simpleName() + "\"";
    }
    reportIssue(
      ExpressionsHelper.reportOnClassTree(classTree),
      String.format("%s has %d%s methods, which is greater than the %d authorized. Split it into smaller classes.",
        classDescription, methods.size(), countNonPublic ? "" : " public", maximumMethodThreshold),
      secondary,
      null);
  }

  private boolean shouldNotReportIssue(ClassTree classTree, List<Tree> methods) {
    return (classTree.simpleName() == null && methods.stream().filter(member -> !isOverriding((MethodTree) member)).count() == 0)
      ||  methods.size() <= maximumMethodThreshold;
  }

  private static boolean isOverriding(MethodTree member) {
    Symbol symbol = member.symbol();
    return symbol.isMethodSymbol() && ((Symbol.MethodSymbol) symbol).overriddenSymbol() != null;
  }

}
