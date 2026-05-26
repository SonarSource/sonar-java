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
import java.util.Set;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.ExpressionsHelper;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.JavaVersionAwareVisitor;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.ImportTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S2143")
public class DateAndTimesCheck extends AbstractMethodDetection implements JavaVersionAwareVisitor {

  private static final MethodMatchers CALENDAR_GET_INSTANCE = MethodMatchers.create()
    .ofSubTypes("java.util.Calendar")
    .names("getInstance")
    .withAnyParameters()
    .build();

  private static final MethodMatchers DATE_CONSTRUCTOR = MethodMatchers.create()
    .ofSubTypes("java.util.Date")
    .constructor()
    .withAnyParameters()
    .build();

  private static final Set<String> KNOWN_JAVA_UTIL_DATE_TIME_CLASSES = Set.of(
    "java.util.Date", "java.util.Calendar",
    "java.sql.Date", "java.sql.Time", "java.sql.Timestamp",
    "java.util.GregorianCalendar");

  private static final String ISSUE_MESSAGE = "Use the \"java.time\" API for date and time.";
  private boolean issueAlreadyRaised;

  private void addIssueOnFile() {
    if (!issueAlreadyRaised) {
      issueAlreadyRaised = true;
      addIssueOnFile(ISSUE_MESSAGE);
    }
  }

  @Override
  public void setContext(JavaFileScannerContext context) {
    issueAlreadyRaised = false;
    super.setContext(context);
  }

  @Override
  public void leaveFile(JavaFileScannerContext context) {
    issueAlreadyRaised = false;
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
        || KNOWN_JAVA_UTIL_DATE_TIME_CLASSES.stream().anyMatch(qualifiedName::startsWith)) {
        addIssueOnFile();
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
    addIssueOnFile();
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    addIssueOnFile();
  }
}
