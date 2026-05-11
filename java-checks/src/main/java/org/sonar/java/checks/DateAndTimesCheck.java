/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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

import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.ExpressionsHelper;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaVersionAwareVisitor;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.semantic.Sema;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.ImportTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S2143")
public class DateAndTimesCheck extends IssuableSubscriptionVisitor implements JavaVersionAwareVisitor {

  private void reportIssue(Tree tree) {
    reportIssue(tree, "Use the Java 8 Date and Time API instead." + context.getJavaVersion().java8CompatibilityMessage());
  }

  @Override
  public boolean isCompatibleWithJavaVersion(JavaVersion version) {
    return version.isJava8Compatible();
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.IMPORT);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree instanceof ImportTree importTree) {
      String qualifiedName = ExpressionsHelper.concatenate((ExpressionTree) importTree.qualifiedIdentifier());
      if (qualifiedName.startsWith("org.joda.time.") || isJavaUtilDateSubclass(qualifiedName) || isJavaUtilCalendarSubclass(qualifiedName)) {
        reportIssue(importTree);
      }
    }
  }

  private boolean isJavaUtilDateSubclass(String qualifiedName) {
    return context.getSemanticModel() instanceof Sema semanticModel && semanticModel.getClassType(qualifiedName).isSubtypeOf("java.util.Date");
  }

  private boolean isJavaUtilCalendarSubclass(String qualifiedName) {
    return context.getSemanticModel() instanceof Sema semanticModel && semanticModel.getClassType(qualifiedName).isSubtypeOf("java.util.Calendar");
  }

}
