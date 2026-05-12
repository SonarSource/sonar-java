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

import java.util.ArrayList;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.ExpressionsHelper;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.plugins.java.api.JavaVersionAwareVisitor;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Sema;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.ImportTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S2143")
public class DateAndTimesCheck extends AbstractMethodDetection implements JavaVersionAwareVisitor {

  private static final MethodMatchers CALENDAR_GET_INSTANCE =
    MethodMatchers.create()
      .ofSubTypes("java.util.Calendar")
      .names("getInstance")
      .withAnyParameters()
      .build();

  private static final MethodMatchers DATE_CONSTRUCTOR =
    MethodMatchers.create()
      .ofSubTypes("java.util.Date")
      .constructor()
      .withAnyParameters()
      .build();

  private void reportIssue(Tree tree) {
    reportIssue(tree, "Use the Java 8 Date and Time API instead." + context.getJavaVersion().java8CompatibilityMessage());
  }

  @Override
  public boolean isCompatibleWithJavaVersion(JavaVersion version) {
    return version.isJava8Compatible();
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    List<Tree.Kind> nodes = new ArrayList<>(super.nodesToVisit());
    nodes.add(Tree.Kind.IMPORT);
    return nodes;
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree instanceof ImportTree importTree) {
      String concatenatedName = ExpressionsHelper.concatenate((ExpressionTree) importTree.qualifiedIdentifier());
      String qualifiedName = importTree.isStatic() ? concatenatedName.substring(0, concatenatedName.lastIndexOf('.')) : concatenatedName;
      if (qualifiedName.startsWith("org.joda.time.")
        || isSubclassOf(qualifiedName, "java.util.Date")
        || isSubclassOf(qualifiedName, "java.util.Calendar")) {
        reportIssue(importTree);
      }
    }
    super.visitNode(tree);
  }

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return MethodMatchers.or(CALENDAR_GET_INSTANCE, DATE_CONSTRUCTOR);
  }

  @Override
  protected void onConstructorFound(NewClassTree newClassTree) {
    reportIssue(newClassTree);
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    reportIssue(mit);
  }

  private boolean isSubclassOf(String qualifiedName, String superclass) {
    return context.getSemanticModel() instanceof Sema semanticModel && semanticModel.getClassType(qualifiedName).isSubtypeOf(superclass);
  }


}
