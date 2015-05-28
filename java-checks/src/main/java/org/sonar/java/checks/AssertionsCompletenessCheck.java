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

import com.google.common.base.Objects;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.MethodInvocationMatcher;
import org.sonar.java.checks.methods.MethodInvocationMatcherCollection;
import org.sonar.java.checks.methods.TypeCriteria;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

@Rule(
  key = "S2970",
  name = "Assertions should be complete",
  tags = {"junit"},
  priority = Priority.MAJOR)
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.UNIT_TESTABILITY)
@SqaleConstantRemediation("5min")
public class AssertionsCompletenessCheck extends BaseTreeVisitor implements JavaFileScanner {

  private static final MethodInvocationMatcher MOCKITO_VERIFY = MethodInvocationMatcher.create()
    .typeDefinition("org.mockito.Mockito").name("verify").withNoParameterConstraint();
  private static final MethodInvocationMatcherCollection FEST_ASSERT_THAT = MethodInvocationMatcherCollection.create(
    // Fest 1.X
    MethodInvocationMatcher.create().typeDefinition("org.fest.assertions.Assertions").name("assertThat").addParameter(TypeCriteria.anyType()),
    // Fest 2.X
    MethodInvocationMatcher.create().typeDefinition("org.fest.assertions.api.Assertions").name("assertThat").addParameter(TypeCriteria.anyType())
  );

  private static final MethodInvocationMatcherCollection FEST_EXCLUSIONS = MethodInvocationMatcherCollection.create(
    MethodInvocationMatcher.create().typeDefinition(TypeCriteria.anyType()).name("as").withNoParameterConstraint(),
    MethodInvocationMatcher.create().typeDefinition(TypeCriteria.anyType()).name("describedAs").withNoParameterConstraint(),
    MethodInvocationMatcher.create().typeDefinition(TypeCriteria.anyType()).name("overridingErrorMessage").withNoParameterConstraint()
  );

  private Boolean chainedToAnyMethodButFestExclusions = null;
  private JavaFileScannerContext context;

  @Override
  public void scanFile(final JavaFileScannerContext context) {
    this.context = context;
    scan(context.getTree());
  }

  @Override
  public void visitVariable(VariableTree tree) {
    // skip variable assignments
  }

  @Override
  public void visitReturnStatement(ReturnStatementTree tree) {
    // skip return statements
  }

  @Override
  public void visitMethodInvocation(MethodInvocationTree mit) {
    if (incompleteAssertion(mit)) {
      return;
    }
    Boolean previous = chainedToAnyMethodButFestExclusions;
    chainedToAnyMethodButFestExclusions = Objects.firstNonNull(chainedToAnyMethodButFestExclusions, false) || !FEST_EXCLUSIONS.anyMatch(mit);
    scan(mit.methodSelect());
    // skip arguments
    chainedToAnyMethodButFestExclusions = previous;
  }

  private boolean incompleteAssertion(MethodInvocationTree mit) {
    if (((FEST_ASSERT_THAT.anyMatch(mit) && (mit.arguments().size() == 1)) || MOCKITO_VERIFY.matches(mit)) && !Boolean.TRUE.equals(chainedToAnyMethodButFestExclusions)) {
      context.addIssue(mit, this, "Complete the assertion.");
      return true;
    }
    return false;
  }

}
