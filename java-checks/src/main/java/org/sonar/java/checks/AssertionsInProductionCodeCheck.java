/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.checks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import org.sonar.check.Rule;
import org.sonar.java.annotations.VisibleForTesting;
import org.sonar.java.checks.helpers.ExpressionsHelper;
import org.sonar.java.checks.helpers.UnitTestUtils;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.JavaFileScannerContext.Location;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.PackageDeclarationTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S5960")
public class AssertionsInProductionCodeCheck extends AbstractMethodDetection {

  @VisibleForTesting
  static final Pattern TEST_PACKAGE_REGEX = Pattern.compile("test|junit|assert|\\.it(?:\\.|$)");
  private final List<Tree> assertions = new ArrayList<>();
  private boolean packageNameNotRelatedToTests = true;

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.METHOD_INVOCATION, Tree.Kind.PACKAGE);
  }

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return UnitTestUtils.COMMON_ASSERTION_MATCHER;
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Tree.Kind.PACKAGE)) {
      String packageName = ExpressionsHelper.concatenate(((PackageDeclarationTree) tree).packageName());
      packageNameNotRelatedToTests = !TEST_PACKAGE_REGEX.matcher(packageName).find();
    }
    if (packageNameNotRelatedToTests) {
      super.visitNode(tree);
    }
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    assertions.add(mit);
  }

  @Override
  public void setContext(JavaFileScannerContext context) {
    super.setContext(context);
    assertions.clear();
    packageNameNotRelatedToTests = true;
  }

  @Override
  public void leaveFile(JavaFileScannerContext context) {
    if (!assertions.isEmpty()) {
      final Tree primaryLocation = assertions.get(0);
      List<Location> secondaryLocations = assertions.stream()
        .skip(1)
        .map(expr -> new Location("Assertion", expr))
        .toList();
      reportIssue(primaryLocation, "Remove this assertion from production code.", secondaryLocations, null);
    }
    assertions.clear();
    super.leaveFile(context);
  }

}
