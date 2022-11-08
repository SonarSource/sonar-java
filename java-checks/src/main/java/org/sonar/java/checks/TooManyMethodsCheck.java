/*
 * SonarQube Java
 * Copyright (C) 2012-2022 SonarSource SA
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

import java.util.List;
import java.util.stream.Collectors;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.java.checks.helpers.ExpressionsHelper;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeTree;

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
    return List.of(Tree.Kind.CLASS, Tree.Kind.ENUM, Tree.Kind.INTERFACE, Tree.Kind.ANNOTATION_TYPE, Tree.Kind.RECORD);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree classTree = (ClassTree) tree;
    List<MethodTree> methods = classTree.members()
      .stream()
        .filter(member -> member.is(Tree.Kind.METHOD, Tree.Kind.CONSTRUCTOR))
        .map(MethodTree.class::cast)
        .filter(method ->  countNonPublic || method.symbol().isPublic())
        .collect(Collectors.toList());

    if(shouldNotReportIssue(classTree, methods)) {
      return;
    }

    List<JavaFileScannerContext.Location> secondary = methods.stream()
      .map(method -> new JavaFileScannerContext.Location("Method + 1", method.simpleName()))
      .collect(Collectors.toList());

    TypeTree reportTree = ExpressionsHelper.reportOnClassTree(classTree);
    String classType;
    String newTypes;
    if (isAnonnymousClass(classTree)) {
      classType = "Anonymous class";
      newTypes = "classes";
    } else {
      classType = classTree.declarationKeyword().text();
      newTypes = classType + (tree.is(Tree.Kind.CLASS) ? "es" : "s");
    }
    reportIssue(
      reportTree,
      String.format("%s \"%s\" has %d%s methods, which is greater than the %d authorized. Split it into smaller %s.",
        classType, reportTree.symbolType().name(), methods.size(), countNonPublic ? "" : " public", maximumMethodThreshold, newTypes),
      secondary,
      null);
  }

  private boolean shouldNotReportIssue(ClassTree classTree, List<MethodTree> methods) {
    return methods.size() <= maximumMethodThreshold
      || (isAnonnymousClass(classTree) && methods.stream().allMatch(TooManyMethodsCheck::isOverriding));
  }

  private static boolean isAnonnymousClass(ClassTree classTree) {
    return classTree.simpleName() == null;
  }

  private static boolean isOverriding(MethodTree methodTree) {
    // When it cannot be decided, isOverriding will return null, we consider it as an override
    return !Boolean.FALSE.equals(methodTree.isOverriding());
  }

}
